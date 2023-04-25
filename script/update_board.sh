#!/bin/sh

if [ -z "$1" ]; then
  echo "./script/update_board.sh <version>"
  exit 1
fi

SCRIPT_BASE=$(
  cd $(dirname "$0")
  pwd -P
)
cd "$SCRIPT_BASE" || exit 2

VERSION=$1

# update build.gradle
sed -i "" \
  "s/\"com.github.netless-io:whiteboard-android:[^\"]*\"/\"com.github.netless-io:whiteboard-android:$VERSION\"/g" \
  ../app/build.gradle

SHA=$(curl -sSL "https://raw.githubusercontent.com/netless-io/whiteboard-android/$VERSION/carrot.yml" |
  grep commit: | awk '{print $2}')

ZIPNAME="src.zip"
ESBUILDNAME="esbuild.mjs"
WHITEBOARD_DIRNAME="Whiteboard-bridge-$SHA"
INJECT_CODE_NAME="injectCode.ts"
INJECT_CODE_PATH="./${WHITEBOARD_DIRNAME}/src/$INJECT_CODE_NAME"
ESBUILD_SCRIPT_NAME="./${WHITEBOARD_DIRNAME}/$ESBUILDNAME"

curl -o $ZIPNAME https://codeload.github.com/netless-io/Whiteboard-bridge/zip/"$SHA"
unzip $ZIPNAME

curl -o "$INJECT_CODE_PATH" https://raw.githubusercontent.com/netless-io/flat-native-bridge/main/injectCode.ts
curl -o "$ESBUILD_SCRIPT_NAME" https://raw.githubusercontent.com/netless-io/flat-native-bridge/main/esbuild.mjs

# Inject code (macos shell)
sed -i '' -e "1i\\
import \'.././injectCode'" ./Whiteboard-bridge-$SHA/src/bridge/SDK.ts

# Build
cd $WHITEBOARD_DIRNAME || exit

apps=(
  "@netless/app-countdown@0.0.7"
  "@netless/app-dice@0.1.1"
  "@netless/app-geogebra@0.0.6"
  "@netless/app-iframe-bridge@0.0.2"
  "@netless/app-mindmap@0.1.1"
  "@netless/app-monaco@0.2.0-canary.1"
  "@netless/app-quill@0.1.1"
  "@netless/app-selector@0.0.3"
)

for item in "${apps[@]}"; do
  yarn add "$item"
done

yarn add esbuild
node esbuild.mjs

touch ./build/$SHA
echo "here are what injected into build" >>./build/$SHA
for item in "${apps[@]}"; do
  echo "$item" >>./build/$SHA
done

rm -rf "$SCRIPT_BASE/../app/src/main/assets/flatboard/*"
cp -rf ./build/* "$SCRIPT_BASE/../app/src/main/assets/flatboard/"

cd .. && rm -rf "$WHITEBOARD_DIRNAME" $ZIPNAME && cd ..
