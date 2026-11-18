#include <jni.h>
#include <android/log.h>
#include <GLES3/gl3.h>

#include <algorithm>
#include <memory>
#include <mutex>

#include <thorvg.h>

#define LOG_TAG "HorrorBroNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

std::mutex gMutex;
std::unique_ptr<tvg::GlCanvas> gCanvas;
int gWidth = 0;
int gHeight = 0;
bool gEngineReady = false;
bool gDrawFailureLogged = false;
float gCameraX = 0.0f;
float gCameraY = 0.0f;

void buildTestScene()
{
    if (!gCanvas || gWidth <= 0 || gHeight <= 0) return;

    gCanvas->clear(true);

    auto background = tvg::Shape::gen();
    background->appendRect(0.0f, 0.0f, static_cast<float>(gWidth), static_cast<float>(gHeight), 0.0f, 0.0f);
    background->fill(16, 16, 20);
    gCanvas->push(std::move(background));

    const float side = std::min(gWidth, gHeight) * 0.5f;
    const float radius = side * 0.2f;
    const float x = (static_cast<float>(gWidth) - side) * 0.5f + gCameraX;
    const float y = (static_cast<float>(gHeight) - side) * 0.5f + gCameraY;

    auto roundedRect = tvg::Shape::gen();
    roundedRect->appendRect(x, y, side, side, radius, radius);
    roundedRect->fill(255, 218, 185);
    roundedRect->stroke(6.0f);
    roundedRect->stroke(0, 0, 0);
    roundedRect->stroke(tvg::StrokeJoin::Round);
    gCanvas->push(std::move(roundedRect));

    if (gCanvas->update() != tvg::Result::Success) {
        LOGE("ThorVG canvas update failed");
    }
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
        // A new EGL context means the old canvas owns GL objects from a dead
        // context. Abandon the C++ object rather than deleting GL names that
        // no longer belong to the current context.
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

    buildTestScene();
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeRender(JNIEnv*, jobject)
{
    std::lock_guard<std::mutex> lock(gMutex);

    if (!gCanvas) return;

    glViewport(0, 0, gWidth, gHeight);
    glClearColor(0.05f, 0.05f, 0.06f, 1.0f);
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

    // Camera transform is not implemented yet. Reset the offset so Phase 2 can
    // rebuild the view around the rigged character.
    gCameraX = 0.0f;
    gCameraY = 0.0f;
}

extern "C" JNIEXPORT void JNICALL
Java_com_horrorbro_NativeRenderer_nativeTrimMemory(JNIEnv*, jobject, jint level)
{
    std::lock_guard<std::mutex> lock(gMutex);

    (void) level;
    // Undo/redo caches will be flushed here in a later phase.
}
