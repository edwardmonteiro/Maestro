# Validation · 0.1.0

## Passed in the build environment

- Java sources and Android resources compiled against Android API 36 with JDK 21.
- ARM64 native llama.cpp JNI engine compiled with NDK r27c; 16 KB ELF segment alignment verified.
- Native engine is linked into the APK; weights are downloaded separately. No private key/API token embedded.
- APK v3 signature verified by Android apksigner (minimum SDK 28).
- JVM HTTP tests exercised actual loopback requests: invalid token rejected, browser Origin rejected, correct token accepted, authenticated body passed to handler.
- Plan validation rejects incomplete model output and option cards lacking a map query.
- OpenClaw plugin contract tests: exactly four optional tools, loopback-only requests, missing token rejected, unsupported actions rejected, queued actions never reported as executed.
- Shell scripts and plugin JavaScript syntax checked.

## Device validation

An Android 35 software emulator was attempted. It failed before application launch with emulator CPU/main-loop watchdog hangs in this environment (hardware virtualization unavailable). Therefore no successful emulator launch or visual inspection is claimed. The Android UI, on-device model load and full phone flow still require physical-device verification.

## Not yet verified

- Installation and sustained behavior on Edward's physical Samsung.
- Live OpenAI billing/model access, web-search output and complete cloud task execution: requires the user's API key.
- Download and Qwen generation on the physical Samsung, including throughput, battery use and thermal behavior. No token/s estimate is claimed.
- Full OpenClaw runtime + Maestro integration on Android. The adapter is implemented against the documented HTTP endpoint and plugin contract; Android runtime installation/onboarding is a separate user step.
- JEV/Laya integration: no implementation/model repository supplied.

## Manual acceptance on the phone

1. Launch; open Motores and Registro; rotate the phone and open the keyboard. No content should overlap system bars.
2. Load the clearly labeled non-AI preview; save a note and find it in Registro. Open sharing and cancel; no sent status should appear.
3. Configure OpenAI; plan an outing and revise the group size/budget. Sources should open. API errors must remain visible, not turn into fabricated results.
4. Download Qwen, interrupt, resume, and verify completion. Enable airplane mode, choose Qwen and generate a short activity.
5. Start the separate OpenClaw Gateway and test authentication. Ask it to save a note; verify the file appears in Registro.
6. Ask OpenClaw to prepare sharing. Verify it remains pending until reviewed in Maestro and never sends without the user's final action in the destination app.
7. Stop a local inference. A late response must not overwrite a newer mission. Gateway runs may continue remotely within the other local app after a client disconnect, as stated in the UI.
