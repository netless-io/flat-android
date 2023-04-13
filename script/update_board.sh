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

echo "$SHA"

ZIPNAME="src.zip"
DIRNAME="Whiteboard-bridge-$SHA"
TMP_DIR="Whiteboard-bridge-$SHA-tmp"
INJECT_CODE_NAME="injectCode.ts"
INJECT_CODE_PATH="$TMP_DIR/$INJECT_CODE_NAME"
WEBPACK_CONFIG_NAME="webpack.config.flat.js"
WEBPACK_CONFIG_PATH="$TMP_DIR/$WEBPACK_CONFIG_NAME"

# Download
mkdir "$TMP_DIR"
curl -o "$INJECT_CODE_PATH" https://raw.githubusercontent.com/netless-io/flat-native-bridge/main/injectCode.ts
curl -o "$WEBPACK_CONFIG_PATH" https://raw.githubusercontent.com/netless-io/flat-native-bridge/main/webpack.config.flat.js
curl -o $ZIPNAME https://codeload.github.com/netless-io/Whiteboard-bridge/zip/"$SHA"
unzip $ZIPNAME

cp $INJECT_CODE_PATH ./Whiteboard-bridge-$SHA/src/$INJECT_CODE_NAME
cp $WEBPACK_CONFIG_PATH ./Whiteboard-bridge-$SHA/$WEBPACK_CONFIG_NAME

# Inject code (macos shell)
sed -i '' -e "1i\\
import \'.././injectCode'" ./Whiteboard-bridge-$SHA/src/bridge/SDK.ts

# Build
cd $DIRNAME || exit

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

yarn buildWithoutGitHash --config ./webpack.config.flat.js

touch ./build/$SHA
echo "here are what injected into build" >>./build/$SHA
for item in "${apps[@]}"; do
  echo "$item" >>./build/$SHA
done

cp -rf ./build/* "$SCRIPT_BASE/../app/src/main/assets/flatboard/"

cd .. && rm -rf "$DIRNAME" $ZIPNAME "$TMP_DIR"
