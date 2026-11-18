cmake_minimum_required(VERSION 3.22.1)
project(HorrorBro LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

include(FetchContent)

# ThorVG v0.12.0 is Meson-only upstream. Fetch the tagged source and build the
# exact GL-only source set here instead of enabling the SW/WG engines.
FetchContent_Declare(
    thorvg
    URL https://github.com/thorvg/thorvg/archive/refs/tags/v0.12.0.tar.gz
    URL_HASH SHA256=727a892e101cd9e00890d8841bbd3669b05253f97c3dcde1e1634bae4b2569ec
)
FetchContent_MakeAvailable(thorvg)

set(TVG_SRC ${thorvg_SOURCE_DIR})

file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/thorvg_config)
file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/thorvg_config/config.h
"#ifndef THORVG_CONFIG_H\n"
"#define THORVG_CONFIG_H\n"
"#define THORVG_VERSION_STRING \"0.12.0\"\n"
"#define THORVG_GL_RASTER_SUPPORT 1\n"
"#endif\n")

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
target_link_libraries(thorvg PUBLIC GLESv2)

add_library(horrorbro SHARED src/main/cpp/native-lib.cpp)

target_include_directories(horrorbro PRIVATE ${TVG_SRC}/inc)
target_link_libraries(horrorbro PRIVATE thorvg GLESv2 log android)
