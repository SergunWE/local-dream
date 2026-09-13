param(
    [string]$NdkRoot,
    [string]$QnnSdkRoot,
    [string]$CMakeBin,
    [int]$Jobs = 8
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path "$PSScriptRoot/../../../..").Path
$androidSdk = "$env:LOCALAPPDATA/Android/Sdk"
if (!$NdkRoot) { $NdkRoot = "$androidSdk/ndk/28.2.13676358" }
if (!$QnnSdkRoot) {
    $QnnSdkRoot = $env:QNN_SDK_ROOT
    if (!$QnnSdkRoot) { $QnnSdkRoot = "$projectRoot/build/native-tools/qairt/2.39.0.250926" }
}
if (!$CMakeBin) { $CMakeBin = "$androidSdk/cmake/3.22.1/bin" }
foreach ($requiredPath in @("$NdkRoot/build/cmake/android.toolchain.cmake", "$QnnSdkRoot/include/QNN/QnnInterface.h", "$CMakeBin/cmake.exe", "$CMakeBin/ninja.exe")) {
    if (!(Test-Path -LiteralPath $requiredPath)) { throw "Missing build dependency: $requiredPath" }
}

$localRust = "$projectRoot/build/rust-tools"
if (Test-Path "$localRust/cargo/bin/cargo.exe") {
    $env:RUSTUP_HOME = "$localRust/rustup"
    $env:CARGO_HOME = "$localRust/cargo"
    $env:PATH = "$localRust/cargo/bin;$env:PATH"
}
$env:PATH = "$CMakeBin;$env:PATH"
$toolchainBin = "$NdkRoot/toolchains/llvm/prebuilt/windows-x86_64/bin"
$buildDir = "$PSScriptRoot/build/android"
$tokenizersDir = "$PSScriptRoot/3rdparty/tokenizers-cpp"
& git -C $tokenizersDir apply --reverse --check "$PSScriptRoot/tokenizers-rust.patch" 2>$null
if ($LASTEXITCODE) {
    & git -C $tokenizersDir apply "$PSScriptRoot/tokenizers-rust.patch"
    if ($LASTEXITCODE) { throw 'Rust tokenizer compatibility patch failed' }
}

# Build the Rust archive first: upstream tokenizers-cpp uses Unix compiler
# wrappers in its custom command, which cannot execute on Windows.
$env:CARGO_TARGET_DIR = "$buildDir/tokenizers"
$env:CC_aarch64_linux_android = "$toolchainBin/clang.exe"
$env:CXX_aarch64_linux_android = "$toolchainBin/clang++.exe"
$env:AR_aarch64_linux_android = "$toolchainBin/llvm-ar.exe"
$env:CFLAGS_aarch64_linux_android = '--target=aarch64-linux-android21'
$env:CXXFLAGS_aarch64_linux_android = '--target=aarch64-linux-android21'
& rustup target add aarch64-linux-android
if ($LASTEXITCODE) { throw 'Rust target installation failed' }
& cargo build --manifest-path "$PSScriptRoot/3rdparty/tokenizers-cpp/rust/Cargo.toml" --target aarch64-linux-android --release -j $Jobs
if ($LASTEXITCODE) { throw 'Rust tokenizer build failed' }

& "$CMakeBin/cmake.exe" -S $PSScriptRoot -B $buildDir -G Ninja `
    "-DCMAKE_MAKE_PROGRAM=$CMakeBin/ninja.exe" `
    "-DCMAKE_TOOLCHAIN_FILE=$NdkRoot/build/cmake/android.toolchain.cmake" `
    "-DCMAKE_ANDROID_NDK=$NdkRoot" -DANDROID_ABI=arm64-v8a `
    -DANDROID_PLATFORM=android-21 -DANDROID_NATIVE_API_LEVEL=21 `
    -DANDROID_STL=c++_static -DCMAKE_BUILD_TYPE=Release `
    "-DQNN_SDK_ROOT=$QnnSdkRoot" -DQNN_DEBUG_ENABLE=OFF
if ($LASTEXITCODE) { throw 'CMake configuration failed' }
& "$CMakeBin/cmake.exe" --build $buildDir --parallel $Jobs
if ($LASTEXITCODE) { throw 'Native engine build failed' }

$jniDir = "$PSScriptRoot/../jniLibs/arm64-v8a"
$qnnDir = "$PSScriptRoot/../assets/qnnlibs"
New-Item -ItemType Directory -Force $jniDir, $qnnDir | Out-Null
Copy-Item -LiteralPath "$buildDir/bin/arm64-v8a/libstable_diffusion_core.so" -Destination $jniDir
Get-ChildItem -LiteralPath "$buildDir/qnnlibs" -Filter '*.so' | Copy-Item -Destination $qnnDir
Write-Host "Native engine: $jniDir/libstable_diffusion_core.so"
Write-Host "QNN libraries: $qnnDir"
