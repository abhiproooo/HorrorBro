#include <jni.h>
#include <android/log.h>
#include <GLES3/gl3.h>

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <memory>
#include <mutex>
#include <utility>
#include <vector>

#include <thorvg.h>

#define LOG_TAG "HorrorBroNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct Point {
    float x;
    float y;
};

enum class DrawTool {
    None,
    AddPoint,
    Freehand,
    Weld
};

enum class PathMode {
    AddPoint,
    Freehand
};

constexpr float kPointRadius = 4.0f;
constexpr float kPreviewStrokeWidth = 2.0f;
constexpr float kDefaultStrokeWidth = 6.0f;
constexpr uint8_t kTealR = 0;
constexpr uint8_t kTealG = 150;
constexpr uint8_t kTealB = 136;
constexpr float kFreehandMinDistance = 2.0f;
constexpr float kFreehandStabilizer = 0.77f;
constexpr float kFreehandSmoothingBlend = 0.25f;
constexpr int kFreehandSmoothingPasses = 1;
constexpr float kFreehandSimplifyTolerance = 1.0f;
constexpr float kCurveTangentScale = 1.0f / 6.0f;
constexpr float kTwoThirds = 2.0f / 3.0f;

std::mutex gMutex;
std::unique_ptr<tvg::GlCanvas> gCanvas;
int gWidth = 0;
int gHeight = 0;
bool gEngineReady = false;
bool gDrawFailureLogged = false;
float gCameraX = 0.0f;
float gCameraY = 0.0f;

DrawTool gTool = DrawTool::AddPoint;
PathMode gPathMode = PathMode::AddPoint;
std::vector<Point> gPoints;
Point gFreehandStable = {0.0f, 0.0f};
bool gFreehandActive = false;
bool gAutoStroke = false;
float gStrokeWidth = kDefaultStrokeWidth;
bool gSceneDirty = true;

void appendAddPointPath(tvg::Shape& shape, const std::vector<Point>& points)
{
    const auto count = points.size();
    if (count == 0) return;

    shape.moveTo(points[0].x, points[0].y);
    if (count == 1) {
        shape.lineTo(points[0].x, points[0].y);
        return;
    }

    for (size_t i = 0; i + 1 < count; ++i) {
        const Point& p0 = points[i > 0 ? i - 1 : i];
        const Point& p1 = points[i];
        const Point& p2 = points[i + 1];
        const Point& p3 = points[i + 2 < count ? i + 2 : i + 1];

        const Point c1 = {
            p1.x + (p2.x - p0.x) * kCurveTangentScale,
            p1.y + (p2.y - p0.y) * kCurveTangentScale
        };
        const Point c2 = {
            p2.x - (p3.x - p1.x) * kCurveTangentScale,
            p2.y - (p3.y - p1.y) * kCurveTangentScale
        };

        shape.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y);
    }
}

void smoothFreehandPoints(std::vector<Point>& points)
{
    if (points.size() < 3) return;

    for (int pass = 0; pass < kFreehandSmoothingPasses; ++pass) {
        for (size_t i = 1; i + 1 < points.size(); ++i) {
            const Point previous = points[i - 1];
            const Point current = points[i];
            const Point next = points[i + 1];
            const Point average = {
                (previous.x + current.x + next.x) / 3.0f,
                (previous.y + current.y + next.y) / 3.0f
            };

            points[i].x = current.x + (average.x - current.x) * kFreehandSmoothingBlend;
            points[i].y = current.y + (average.y - current.y) * kFreehandSmoothingBlend;
        }
    }
}

float pointSegmentDistanceSquared(const Point& point, const Point& start, const Point& end)
{
    const float dx = end.x - start.x;
    const float dy = end.y - start.y;
    const float lengthSquared = dx * dx + dy * dy;

    float t = 0.0f;
    if (lengthSquared > 1e-6f) {
        t = ((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared;
        t = std::max(0.0f, std::min(1.0f, t));
    }

    const float nearestX = start.x + t * dx;
    const float nearestY = start.y + t * dy;
    const float offsetX = point.x - nearestX;
    const float offsetY = point.y - nearestY;
    return offsetX * offsetX + offsetY * offsetY;
}

void simplifyFreehandPoints(std::vector<Point>& points)
{
    const size_t count = points.size();
    if (count < 3) return;

    const float toleranceSquared = kFreehandSimplifyTolerance * kFreehandSimplifyTolerance;
    std::vector<bool> keep(count, false);
    keep.front() = true;
    keep.back() = true;

    std::vector<std::pair<size_t, size_t>> pending;
    pending.emplace_back(0, count - 1);

    while (!pending.empty()) {
        const auto range = pending.back();
        pending.pop_back();

        const size_t first = range.first;
        const size_t last = range.second;
        if (last <= first + 1) continue;

        float maxDistanceSquared = 0.0f;
        size_t maxIndex = first;
        for (size_t i = first + 1; i < last; ++i) {
            const float distanceSquared =
                pointSegmentDistanceSquared(points[i], points[first], points[last]);
            if (distanceSquared > maxDistanceSquared) {
                maxDistanceSquared = distanceSquared;
                maxIndex = i;
            }
        }

        if (maxDistanceSquared > toleranceSquared) {
            keep[maxIndex] = true;
            pending.emplace_back(first, maxIndex);
            pending.emplace_back(maxIndex, last);
        }
    }

    std::vector<Point> simplified;
    simplified.reserve(count);
    for (size_t i = 0; i < count; ++i) {
        if (keep[i]) simplified.push_back(points[i]);
    }
    points.swap(simplified);
}

void appendFreehandPath(tvg::Shape& shape, const std::vector<Point>& points)
{
    const auto count = points.size();
    if (count == 0) return;

    shape.moveTo(points[0].x, points[0].y);
    if (count == 1) {
        shape.lineTo(points[0].x, points[0].y);
        return;
    }
    if (count == 2) {
        shape.lineTo(points[1].x, points[1].y);
        return;
    }

    Point current = points[0];
    for (size_t i = 1; i + 1 < count; ++i) {
        const Point& control = points[i];
        const Point& next = points[i + 1];
        const Point mid = {
            (control.x + next.x) * 0.5f,
            (control.y + next.y) * 0.5f
        };
        const Point c1 = {
            current.x + (control.x - current.x) * kTwoThirds,
            current.y + (control.y - current.y) * kTwoThirds
        };
        const Point c2 = {
            mid.x + (control.x - mid.x) * kTwoThirds,
            mid.y + (control.y - mid.y) * kTwoThirds
        };

        shape.cubicTo(c1.x, c1.y, c2.x, c2.y, mid.x, mid.y);
        current = mid;
    }

    const Point& last = points[count - 1];
    shape.lineTo(last.x, last.y);
}

void rebuildSceneLocked()
{
    if (!gCanvas || gWidth <= 0 || gHeight <= 0) return;

    gCanvas->clear(true);

    auto background = tvg::Shape::gen();
    if (background) {
        background->appendRect(0.0f, 0.0f,
                               static_cast<float>(gWidth),
                               static_cast<float>(gHeight),
                               0.0f, 0.0f);
        background->fill(255, 255, 255);
        gCanvas->push(std::move(background));
    }

    if (!gPoints.empty()) {
        auto path = tvg::Shape::gen();
        if (path) {
            if (gPathMode == PathMode::Freehand) {
                appendFreehandPath(*path, gPoints);
            } else {
                appendAddPointPath(*path, gPoints);
            }

            if (gAutoStroke) {
                path->stroke(std::max(gStrokeWidth, 0.5f));
                path->stroke(0, 0, 0);
            } else {
                path->stroke(kPreviewStrokeWidth);
                path->stroke(kTealR, kTealG, kTealB);
            }
            path->stroke(tvg::StrokeCap::Round);
            path->stroke(tvg::StrokeJoin::Round);
            gCanvas->push(std::move(path));
        }

        auto points = tvg::Shape::gen();
        if (points) {
            for (const Point& point : gPoints) {
                points->appendCircle(point.x, point.y, kPointRadius, kPointRadius);
            }
            points->fill(kTealR, kTealG, kTealB);
            gCanvas->push(std::move(points));
        }
    }

    if (gCanvas->update() != tvg::Result::Success) {
        LOGE("ThorVG canvas update failed");
    }
    gSceneDirty = false;
}

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_horrorbro_MainActivity_nativeInit(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (!gEngineReady) {
        if (tvg::Initializer::init(tvg::CanvasEngine::Gl, 0) != tvg::Result::Success) {
            LOGE("ThorVG GL engine initialization failed");
            return JNI_FALSE;
        }
        gEngineReady = true;
        LOGI("ThorVG GL engine initialized");
    } else if (gCanvas) {
        gCanvas.reset();
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_nativeResize(JNIEnv*, jobject, jint width, jint height)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (!gEngineReady || width <= 0 || height <= 0) return;

    gWidth = width;
    gHeight = height;
    gDrawFailureLogged = false;

    gCanvas.reset();
    gCanvas = tvg::GlCanvas::gen();
    if (!gCanvas) {
        LOGE("ThorVG GlCanvas creation failed");
        return;
    }

    const auto stride = static_cast<uint32_t>(gWidth) * 4u;
    if (gCanvas->target(nullptr, stride, static_cast<uint32_t>(gWidth), static_cast<uint32_t>(gHeight))
        != tvg::Result::Success) {
        LOGE("ThorVG GlCanvas target failed");
        gCanvas.reset();
        return;
    }

    gSceneDirty = true;
    rebuildSceneLocked();
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_nativeRender(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (!gCanvas) return;

    if (gSceneDirty) {
        rebuildSceneLocked();
    }

    glViewport(0, 0, gWidth, gHeight);
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    const auto result = gCanvas->draw();
    if (result != tvg::Result::Success && !gDrawFailureLogged) {
        LOGE("ThorVG draw failed with code %d", static_cast<int>(result));
        gDrawFailureLogged = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_nativeRecenter(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);

    gCameraX = 0.0f;
    gCameraY = 0.0f;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_nativeTrimMemory(JNIEnv*, jobject, jint level)
{
    std::lock_guard<std::mutex> lock(gMutex);

    (void) level;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_setActiveTool(JNIEnv*, jobject, jint toolId)
{
    std::lock_guard<std::mutex> lock(gMutex);

    switch (toolId) {
        case 0:
            gTool = DrawTool::AddPoint;
            break;
        case 1:
            gTool = DrawTool::Freehand;
            break;
        case 2:
            gTool = DrawTool::Weld;
            break;
        default:
            gTool = DrawTool::None;
            break;
    }

    if (gTool != DrawTool::Freehand) {
        gFreehandActive = false;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_setAutoStroke(JNIEnv*, jobject, jboolean enabled)
{
    std::lock_guard<std::mutex> lock(gMutex);

    gAutoStroke = (enabled == JNI_TRUE);
    gSceneDirty = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_setStrokeWidth(JNIEnv*, jobject, jfloat width)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (!std::isfinite(width)) return;

    gStrokeWidth = std::max(0.5f, std::min(width, 64.0f));
    gSceneDirty = true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_horrorbro_MainActivity_onTouchDown(JNIEnv*, jobject, jfloat x, jfloat y)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (gTool == DrawTool::None) return JNI_FALSE;
    if (!std::isfinite(x) || !std::isfinite(y)) return JNI_FALSE;

    if (gTool == DrawTool::Freehand) {
        gPoints.clear();
        gPathMode = PathMode::Freehand;
        gFreehandStable = {x, y};
        gFreehandActive = true;
        gPoints.push_back(gFreehandStable);
    } else {
        gPathMode = PathMode::AddPoint;
        gFreehandActive = false;
        gPoints.push_back({x, y});
    }

    gSceneDirty = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_horrorbro_MainActivity_onTouchMove(JNIEnv*, jobject, jfloat x, jfloat y)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (gTool != DrawTool::Freehand || !gFreehandActive) return JNI_FALSE;
    if (!std::isfinite(x) || !std::isfinite(y)) return JNI_FALSE;

    if (gPoints.empty()) {
        gPathMode = PathMode::Freehand;
        gFreehandStable = {x, y};
        gPoints.push_back(gFreehandStable);
        gSceneDirty = true;
        return JNI_TRUE;
    }

    const float alpha = 1.0f - kFreehandStabilizer;
    gFreehandStable.x += (x - gFreehandStable.x) * alpha;
    gFreehandStable.y += (y - gFreehandStable.y) * alpha;

    const Point& last = gPoints.back();
    const float dx = gFreehandStable.x - last.x;
    const float dy = gFreehandStable.y - last.y;
    if (dx * dx + dy * dy < kFreehandMinDistance * kFreehandMinDistance) {
        return JNI_FALSE;
    }

    gPoints.push_back(gFreehandStable);
    gSceneDirty = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_horrorbro_MainActivity_onTouchUp(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (!gFreehandActive) return JNI_FALSE;
    gFreehandActive = false;

    if (gPoints.size() < 3) return JNI_FALSE;

    smoothFreehandPoints(gPoints);
    simplifyFreehandPoints(gPoints);
    gSceneDirty = true;
    return JNI_TRUE;
}
