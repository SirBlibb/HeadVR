<b>HeadVR</b> (https://github.com/autoandshare/HeadVR) is an open source VR video player
for
android based on VLC.<br/><br/>

<br/><br/>
Features:<br/><br/>
- Support touch control, bluetooth controller and head control<br/><br/>
- Controlled by head motion, e.g., nod to play/pause. (Gyroscope sensor is
necessary) <br/><br/>
- Support playlist, viewing all the video's without taking off headset. <br/><br/>
- Support most multimedia files with underlying libvlc. <br/><br/>
- Local and network resources.<br/><br/>
- Rich settings to make video comfortable to eyes.<br/><br/>
- Force2D mode for stressful 3D video's.<br/><br/>
- Support audio and subtitle selection.<br/><br/>

To build, get the VLC fork (https://github.com/autoandshare/vlc-android) and put headvr
together with application/libvlc/medialibary. <br/>
In Android Studio, Build -> Generate Signed Bundle or APK... -> APK -> setup keys -> Choose variant "release" and V1 and V2 signatures.

## Building from the command line

HeadVR is a library module (`com.android.library`), so the APK is built from the
`vlc-android` checkout, which includes it as `:headvr`. Place both folders side by
side and link HeadVR into the VLC tree (`mklink /J vlc-android\headvr HeadVR` on
Windows).

Requirements: JDK 11 (Gradle 7.2 does not support newer JDKs) and the Android SDK.

```bat
set JAVA_HOME=C:\path\to\jdk-11
cd vlc-android
gradlew.bat assembleSignedRelease --console=plain
```

Output lands in `vlc-android\application\app\build\outputs\apk\signedRelease\`,
one APK per ABI. Use `assembleDebug` for a universal debug APK named
`HeadVR-<version>-debug-all.apk`.

Notes:

- Run the task without the `:application:app:` prefix. The build only enables
  per-ABI splits when the task name itself starts with `assemble`, otherwise you
  get one universal APK.
- After changing `versionName`/`versionCode`, run `gradlew clean` first. VLC's
  generated data-binding mappers go stale and compilation fails with
  `Unresolved reference: BR`.
- Release signing reads `keyStoreFile`, `storealias` and `storepwd` from
  `~/.gradle/gradle.properties` (see `application/app/build.gradle`). Keep the
  keystore and its password backed up: updates must be signed with the same key.
- The vendored GVR SDK is published into `libs/maven` so it resolves as a normal
  module dependency. AGP 8 rejects bare local `.aar` files in a library module.

Published builds are attached to GitHub releases:

```powershell
gh release create v<version> <apk files> --target master --title "HeadVR <version>" --notes-file notes.md
```


