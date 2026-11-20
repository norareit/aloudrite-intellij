#!/usr/bin/env bash
# Builds the native library of the Java binding of piper-phonemize for Linux x64 from the source of a release, the way
# the sherpa-onnx project builds its own: in the image quay.io/pypa/manylinux2014_x86_64, whose glibc is 2.17. The
# release's own library needs glibc 2.38. The options are those of the step "Build JNI" of jar.yaml and the packing is
# that of its step "Create native JAR". Writes the jar to native/ and prints the tag, the file and its SHA-256.
#
#   scripts/build-piper-phonemize-linux-x64.sh v1.5.9
#
# Fails, and copies nothing, where Docker is missing or the library needs a symbol version of glibc newer than 2.17.
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "usage: $0 <tag of piper-phonemize, such as v1.5.9>" >&2
  exit 2
fi
tag="$1"
image="quay.io/pypa/manylinux2014_x86_64"
project_folder="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_folder="$project_folder/native"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is missing, so the library cannot be built here. Run this script where Docker runs." >&2
  exit 1
fi

mkdir -p "$output_folder"
# The container runs as root, for yum, and gives the one file it copies out to the user of the host
docker run --rm -e TAG="$tag" -e HOST_USER="$(id -u):$(id -g)" -v "$output_folder:/output" "$image" bash -euo pipefail -c '
yum install -y java-11-openjdk-devel >/dev/null
export JAVA_HOME="$(ls -d /usr/lib/jvm/java-11-openjdk-*/ | head -1)"
git clone --quiet --depth 1 --branch "$TAG" https://github.com/csukuangfj/piper-phonemize /source
cd /source
echo "commit $(git rev-parse HEAD)"
cmake -Bbuild \
  -DBUILD_SHARED_LIBS=OFF \
  -DBUILD_PIPER_PHONEMIZE_PYTHON=OFF \
  -DBUILD_PIPER_PHONEMIZE_CORE_TESTS=OFF \
  -DBUILD_PIPER_PHONEMIZE_JNI=ON \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX=build/install
cmake --build build --config Release --target install

library="$(find build/install/lib -name libpiper-phonemize-jni.so | head -1)"
newest="$(objdump -T "$library" | grep -o "GLIBC_[0-9.]*" | sed "s/GLIBC_//" | sort -V | tail -1)"
echo "the library needs glibc $newest at most"
if [ "$(printf "%s\n2.17\n" "$newest" | sort -V | tail -1)" != "2.17" ]; then
  echo "The library needs glibc $newest, newer than 2.17, so nothing is copied" >&2
  exit 1
fi

version="$(grep "set(PIPER_PHONEMIZE_VERSION" ./CMakeLists.txt | sed "s/.*set(PIPER_PHONEMIZE_VERSION \(.*\))/\1/" | tr -d " \")")"
mkdir -p piper-phonemize/native/linux-x64
cp "$library" piper-phonemize/native/linux-x64/
jar="piper-phonemize-native-lib-linux-x64-$version.jar"
jar cf "$jar" -C . piper-phonemize/
cp "$jar" "/output/$jar"
chown "$HOST_USER" "/output/$jar"
echo "$jar"
' | tee /tmp/piper-phonemize-build.log

jar_name="$(tail -1 /tmp/piper-phonemize-build.log)"
echo "tag $tag"
echo "file native/$jar_name"
sha256sum "$output_folder/$jar_name"
