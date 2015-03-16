#!/bin/bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
-keystore $JENKINS_HOME/sleddingSmash.keystore android/build/outputs/apk/android-release-unsigned.apk alias_name \
-keypass railwaygames113 -storepass railwaygames113

$ANDROID_HOME/build-tools/20.0.0/zipalign -v 4 android/build/outputs/apk/android-release-unsigned.apk sleddingSmash.apk