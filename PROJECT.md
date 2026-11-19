# HorrorBro - Project State & Context

## Current Status
* Phase 0: GitHub Actions / Gradle build scaffolding added.
* Phase 1: Canvas, Compose UI shell and ThorVG GL setup implemented.
* Phase 2: Landscape lock, C++ point path storage, JNI touch input, Add Point and Freehand tools implemented. The canvas renders the current path as a black round-capped/round-joined stroke with no fill.
* Phase 2 math: Freehand uses the `lf_pencil.lua` moving-average stabilizer, a one-pass 3-point smoothing pass, and Douglas-Peucker point reduction. Add Point uses a `mr_curve_tool.lua`-style tangent/curvature cubic path through clicked points.
* Phase 2 touch: `GLSurfaceView` creation and touch forwarding live in `MainActivity.kt`; JNI touch calls report whether the path changed so Kotlin only calls `requestRender()` for actual geometry changes (plus initial/resume/recenter).
* Next Action: User tests the `app-debug.apk`; wait for "Phase complete, move to next Phase" before Phase 3.

## Architecture Rules (The Holy Trinity)
1. **Canvas:** OpenGL ES via ThorVG (C++).
2. **Math/Logic:** C++ (Vector manipulation, Bone matrices, event-driven).
3. **UI:** Kotlin & Jetpack Compose (Moho-style layout, green toggle states).

## Completed Phases
* (None yet)

## Compressed Context (For AI)
* (Empty - will be updated when context hits 90%)
