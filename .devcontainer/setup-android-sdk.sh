#!/usr/bin/env bash
set -e

export ANDROID_HOME="$PWD/.android-sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd "$ANDROID_HOME/cmdline-tools"

# Si esta versión ya no existe, toma la URL actual desde:
# https://developer.android.com/studio#command-tools ("Command line tools only" > Linux)
CMD_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

curl -sSL -o cmdline-tools.zip "$CMD_TOOLS_URL"
unzip -q cmdline-tools.zip
rm cmdline-tools.zip
mv cmdline-tools latest

export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

yes | sdkmanager --licenses > /dev/null
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

echo "sdk.dir=$ANDROID_HOME" > "$OLDPWD/local.properties"

{
  echo "export ANDROID_HOME=$ANDROID_HOME"
  echo "export ANDROID_SDK_ROOT=$ANDROID_HOME"
  echo "export PATH=\$PATH:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools"
} >> ~/.bashrc

echo "Android SDK listo en $ANDROID_HOME"
java -version
