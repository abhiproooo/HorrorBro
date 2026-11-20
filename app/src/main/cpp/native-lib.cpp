#include <jni.h>
#include <android/log.h>
#include <GLES2/gl2.h>
#include <thorvg.h>
#include <vector>
#include <mutex>

#define LOG_TAG "HorrorBroNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

struct Point { float x, y; };
std::vector<Point> currentPath;
std::mutex pathMutex;
bool autoStroke = true;
int activeTool = 0; // 0 = none, 1 = add point, 2 = freehand

extern "C" {

JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_initThorVG(JNIEnv *env, jobject /* this */) {
    tvg::Initializer::init(tvg::CanvasEngine::Gl, 0);
    LOGI("ThorVG Initialized");
}

JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_setActiveTool(JNIEnv *env, jobject, jint toolId) {
    activeTool = toolId;
}

JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_setAutoStroke(JNIEnv *env, jobject, jboolean enabled) {
    autoStroke = enabled;
}

JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_onTouchDown(JNIEnv *env, jobject, jfloat x, jfloat y) {
    if (activeTool == 1 || activeTool == 2) {
        std::lock_guard<std::mutex> lock(pathMutex);
        if (activeTool == 1) currentPath.clear(); // Add point clears old path
        currentPath.push_back({x, y});
    }
}

JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_onTouchMove(JNIEnv *env, jobject, jfloat x, jfloat y) {
    if (activeTool == 2) { // Freehand
        std::lock_guard<std::mutex> lock(pathMutex);
        currentPath.push_back({x, y});
    }
}

JNIEXPORT void JNICALL
Java_com_horrorbro_MainActivity_drawFrame(JNIEnv *env, jobject, jint width, jint height) {
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // White canvas
    glClear(GL_COLOR_BUFFER_BIT);

    auto glCanvas = tvg::GlCanvas::gen();
    uint32_t buffer[1000 * 1000]; 
    glCanvas->target(0, width, height, width);

    std::lock_guard<std::mutex> lock(pathMutex);
    
    if (currentPath.size() > 1) {
        auto shape = tvg::Shape::gen();
        shape->moveTo(currentPath[0].x, currentPath[0].y);
        for (size_t i = 1; i < currentPath.size(); i++) {
            shape->lineTo(currentPath[i].x, currentPath[i].y);
        }
        if (autoStroke) shape->strokeFill(0, 0, 0, 255);
        else shape->strokeFill(0, 150, 136, 255); // Teal
        shape->strokeWidth(4.0f);
        glCanvas->push(std::move(shape));
    }

    // Draw teal points
    for (const auto& p : currentPath) {
        auto dot = tvg::Shape::gen();
        dot->appendCircle(p.x, p.y, 4, 4);
        dot->fill(0, 150, 136, 255);
        glCanvas->push(std::move(dot));
    }

    glCanvas->draw();
    glCanvas->sync();
}
}
