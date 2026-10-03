#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME}"
DEPS=${MAESTRO_DEPS:-$ROOT/build/deps}
mkdir -p "$DEPS"
PIN=836d57176dc699a726c55418e4f96b8ca628e1bf
if [ ! -d "$DEPS/llama.cpp/.git" ]; then git clone --filter=blob:none https://github.com/ggml-org/llama.cpp.git "$DEPS/llama.cpp"; fi
git -C "$DEPS/llama.cpp" checkout "$PIN"
cmake -S "$ROOT/app/src/main/cpp" -B "$ROOT/build/native" -G Ninja \
 -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake" \
 -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 -DANDROID_STL=c++_static \
 -DLLAMA_SOURCE_DIR="$DEPS/llama.cpp" -DCMAKE_BUILD_TYPE=Release
cmake --build "$ROOT/build/native" -j 4
mkdir -p "$ROOT/app/src/main/jniLibs/arm64-v8a"
cp "$ROOT/build/native/libmaestro_llm.so" "$ROOT/app/src/main/jniLibs/arm64-v8a/"
"$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$ROOT/app/src/main/jniLibs/arm64-v8a/libmaestro_llm.so"
