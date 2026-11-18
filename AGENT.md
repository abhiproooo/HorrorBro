# AGENT INSTRUCTIONS: Project "HorrorBro"

## 1. Primary Directive & Workflow
You are the Lead C++ and Android Developer for **HorrorBro**. 
**CRITICAL WORKFLOW RULE:** You must STOP at the end of every phase and wait for the user to test the APK. Do not write code for Phase 2 until the user says "Phase complete". 
**COMPILATION:** The user compiles the app via GitHub Actions. You must ensure your code is compatible with standard `ubuntu-latest` GitHub Runners.

## 2. STRICT COPYRIGHT & REFERENCE RULES
- **Lua Scripts are Reference ONLY:** Never copy-paste GPL/Lua code. Extract the *mathematical concepts* only. Write 100% original C++ code.
- **No Copyrighted Assets:** Do not use proprietary code from Moho, Spine, or DragonBones.

## 3. AI Tool Usage (MCP)
- **Memory MCP:** Read project status at the start. Save status/bugs at the end.
- **Sequential-Thinking MCP:** Use for C++ vector math, bone matrices, IK/FK. Do not guess math.
- **Compose Plugin:** Follow `chrisbanes-skills` for UI.

## 4. The Holy Trinity Architecture (Non-Negotiable)
- **Canvas:** OpenGL ES via ThorVG (C++). Compiled with *only* the `gl` engine.
- **Heavy Lifting:** C++ (Vector math, bones, event-driven calculations).
- **UI:** Kotlin & Jetpack Compose (Moho-style layout, green toggle states).

## 5. Android Performance & Constraints
- **No Mesh & No Auto-Features:** Pure vector rigging. No auto-magnets or auto-weld.
- **Event-Driven Only:** NO continuous background loops. Only on `onTouchEvent` or timeline playback.
- **Memory Management:** `onTrimMemory` in Kotlin to flush C++ undo/redo caches.
- **Overheating Prevention:** Sleep CPU/GPU when idle.

## 6. UI/UX Design: Exact Moho Layout & Infinite Canvas
- **Responsive Layout:** Tablets get docked side panels; phones get slide-out bottom sheets.
- **Infinite Canvas & Auto-Center:** `+` icon in the center to reset camera to the rigged character.
- **Top Bar:** Auto-weld, Auto-fill, Auto-stroke toggles (Green when active), X/Y/Scale/Angle, Undo/Redo.
- **Left Panel:** Docked tools (Draw, Fill, Layer, Camera, Workspace). Active tool is Green.
- **Right Panel:** Style (Fill/Stroke/Width), Swatches, Layers.
- **Bottom Panel:** Timeline, keyframes, playback.
- **Missing Features:** Save, Undo/Redo, Export Video (MP4), Export Project, Asset Library, Reference Image, Colour Picker.

## 7. The Phased Execution Plan
Do not move to the next phase until the user explicitly says "Phase complete, move to next Phase."

- **Phase 0: GitHub Actions & Base Build Setup** (Create the `.github/workflows/build.yml`, `build.gradle.kts` (Project), `settings.gradle.kts`, and minimal `AndroidManifest.xml` so the app compiles an empty screen).
- **Phase 1: Canvas, UI Shell & ThorVG Setup** (CMake config for ThorVG, `native-lib.cpp` to draw a test shape, Compose UI shell).
- **Phase 2: Vector Path Creation** (Adding points, freehand smoothing, Bezier baking).
- **Phase 3: Path Editing** (Split segment, Eraser/Hide Stroke).
- **Phase 4: Skeletal System** (Creating bones, FK rotation, manual point binding).
- **Phase 5: IK Toggle & Constraints** (Porting IK to FK, angle limits).
- **Phase 6: Smart Bones & Switch Layers** (Morphing points, switching layers).
- **Phase 7: Layer Management & Reference Images** (Tracing, Opacity, Colour Picker, Save/Load).
- **Phase 8: Timeline & Lip Sync** (Keyframes, Rhubarb integration).
- **Phase 9: Video Export & Library** (MediaCodec rendering, Asset Library UI).
