#include <jni.h>
#include <android/log.h>
#include <GLES3/gl3.h>

#include <cmath>
#include <memory>
#include <mutex>
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
    Freehand
};

enum class PathMode {
    AddPoint,
    Freehand
};

constexpr float kStrokeWidth = 6.0f;
constexpr float kFreehandMinDistance = 2.0f;
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
            p1.x + (p2.x - p0.x) / 6.0f,
            p1.y + (p2.y - p0.y) / 6.0f
        };
        const Point c2 = {
            p2.x - (p3.x - p1.x) / 6.0f,
            p2.y - (p3.y - p1.y) / 6.0f
        };

        shape.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y);
    }
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
        auto stroke = tvg::Shape::gen();
        if (stroke) {
            if (gPathMode == PathMode::Freehand) {
                appendFreehandPath(*stroke, gPoints);
            } else {
                appendAddPointPath(*stroke, gPoints);
            }
            stroke->stroke(kStrokeWidth);
            stroke->stroke(0, 0, 0);
            stroke->stroke(tvg::StrokeCap::Round);
            stroke->stroke(tvg::StrokeJoin::Round);
            gCanvas->push(std::move(stroke));
        }
    }

    if (gCanvas->update() != tvg::Result::Success) {
        LOGE("ThorVG canvas update failed");
    }
    gSceneDirty = false;
}

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_horrorbro_NativeRenderer_nativeInit(JNIEnv*, jobject)
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
        gCanvas.release();
        gCanvas = nullptr;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeResize(JNIEnv*, jobject, jint width, jint height)
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
Java_com_horrorbro_NativeRenderer_nativeRender(JNIEnv*, jobject)
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
Java_com_horrorbro_NativeRenderer_nativeRecenter(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);

    gCameraX = 0.0f;
    gCameraY = 0.0f;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeTrimMemory(JNIEnv*, jobject, jint level)
{
    std::lock_guard<std::mutex> lock(gMutex);

    (void) level;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeSetTool(JNIEnv*, jobject, jint toolId)
{
    std::lock_guard<std::mutex> lock(gMutex);

    switch (toolId) {
        case 0:
            gTool = DrawTool::AddPoint;
            break;
        case 1:
            gTool = DrawTool::Freehand;
            break;
        default:
            gTool = DrawTool::None;
            break;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeTouchDown(JNIEnv*, jobject, jfloat x, jfloat y)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (gTool == DrawTool::None) return;
    if (!std::isfinite(x) || !std::isfinite(y)) return;

    if (gTool == DrawTool::Freehand) {
        gPoints.clear();
        gPathMode = PathMode::Freehand;
    } else {
        gPathMode = PathMode::AddPoint;
    }

    gPoints.push_back({x, y});
    gSceneDirty = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeTouchMove(JNIEnv*, jobject, jfloat x, jfloat y)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (gTool != DrawTool::Freehand) return;
    if (!std::isfinite(x) || !std::isfinite(y)) return;

    if (gPoints.empty()) {
        gPathMode = PathMode::Freehand;
        gPoints.push_back({x, y});
        gSceneDirty = true;
        return;
    }

    const Point& last = gPoints.back();
    const float dx = x - last.x;
    const float dy = y - last.y;
    if (dx * dx + dy * dy < kFreehandMinDistance * kFreehandMinDistance) return;

    gPoints.push_back({x, y});
    gSceneDirty = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeTouchUp(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);
}
