cmake_minimum_required(VERSION 3.22.1)
project(HorrorBro LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

include(FetchContent)

FetchContent_Declare(
    thorvg
    URL https://github.com/thorvg/thorvg/archive/refs/tags/v0.12.0.tar.gz
    URL_HASH SHA256=727a892e101cd9e00890d8841bbd3669b05253f97c3dcde1e1634bae4b2569ec
)
FetchContent_MakeAvailable(thorvg)

if(NOT thorvg_SOURCE_DIR OR NOT EXISTS "${thorvg_SOURCE_DIR}/src/renderer/tvgGlCanvas.cpp")
    message(FATAL_ERROR "ThorVG v0.12.0 sources were not populated correctly")
endif()

set(TVG_SRC "${thorvg_SOURCE_DIR}")

file(MAKE_DIRECTORY "${CMAKE_CURRENT_BINARY_DIR}/thorvg_config")
file(WRITE "${CMAKE_CURRENT_BINARY_DIR}/thorvg_config/config.h"
"#ifndef THORVG_CONFIG_H
#define THORVG_CONFIG_H
#define THORVG_VERSION_STRING \"0.12.0\"
#define THORVG_GL_RASTER_SUPPORT 1
#endif
")

# GLES 3.0 entry points (glBindVertexArray, glGetUniformBlockIndex,
# glDrawBuffers, glBlitFramebuffer, ...) are exported by libGLESv3.so, not
# libGLESv2.so. GLESv3 also contains the complete GLES 2.0 entry point set.
find_library(GLES_LIBRARY NAMES GLESv3 GLESv2)
find_library(EGL_LIBRARY NAMES EGL)

if(NOT GLES_LIBRARY)
    message(FATAL_ERROR "Android OpenGL ES library not found (GLESv3/GLESv2)")
endif()
if(NOT EGL_LIBRARY)
    message(FATAL_ERROR "Android EGL library not found")
endif()

set(TVG_SOURCES
    ${TVG_SRC}/src/common/tvgBezier.cpp
    ${TVG_SRC}/src/common/tvgCompressor.cpp
    ${TVG_SRC}/src/common/tvgMath.cpp
    ${TVG_SRC}/src/common/tvgStr.cpp
    ${TVG_SRC}/src/renderer/tvgAccessor.cpp
    ${TVG_SRC}/src/renderer/tvgAnimation.cpp
    ${TVG_SRC}/src/renderer/tvgCanvas.cpp
    ${TVG_SRC}/src/renderer/tvgFill.cpp
    ${TVG_SRC}/src/renderer/tvgGlCanvas.cpp
    ${TVG_SRC}/src/renderer/tvgInitializer.cpp
    ${TVG_SRC}/src/renderer/tvgLoader.cpp
    ${TVG_SRC}/src/renderer/tvgPaint.cpp
    ${TVG_SRC}/src/renderer/tvgPicture.cpp
    ${TVG_SRC}/src/renderer/tvgRender.cpp
    ${TVG_SRC}/src/renderer/tvgSaver.cpp
    ${TVG_SRC}/src/renderer/tvgScene.cpp
    ${TVG_SRC}/src/renderer/tvgShape.cpp
    ${TVG_SRC}/src/renderer/tvgSwCanvas.cpp
    ${TVG_SRC}/src/renderer/tvgTaskScheduler.cpp
    ${TVG_SRC}/src/renderer/tvgText.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlGeometry.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlGpuBuffer.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlProgram.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlRenderer.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlRenderPass.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlRenderTask.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlShader.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlShaderSrc.cpp
    ${TVG_SRC}/src/renderer/gl_engine/tvgGlTessellator.cpp
    ${TVG_SRC}/src/loaders/raw/tvgRawLoader.cpp
)

add_library(thorvg STATIC ${TVG_SOURCES})

target_include_directories(thorvg
    PUBLIC
        ${TVG_SRC}/inc
    PRIVATE
        ${TVG_SRC}/src/common
        ${TVG_SRC}/src/renderer
        ${TVG_SRC}/src/renderer/gl_engine
        ${TVG_SRC}/src/loaders/raw
        ${CMAKE_CURRENT_BINARY_DIR}/thorvg_config
)

target_compile_definitions(thorvg PUBLIC TVG_STATIC)
target_compile_options(thorvg PRIVATE -fno-exceptions -fno-rtti)
target_link_libraries(thorvg PUBLIC ${GLES_LIBRARY} ${EGL_LIBRARY})

add_library(horrorbo SHARED src/main/cpp/native-lib.cpp)

target_include_directories(horrorbo PRIVATE ${TVG_SRC}/inc)
target_link_libraries(horrorbo PRIVATE thorvg ${GLES_LIBRARY} ${EGL_LIBRARY} log android)