# jalkster

walky talky jalky jalky

![run run runnnn](app/src/main/res/drawable/jalkster_splash.png  "walkwalkwakl")

## Build

```
make build
make install
```

## Clean Repo

For a build-artifacts cleanup, run:

```
make clean
```

This does:

```
./gradlew clean
rm -rf build out
rm -f dist/*.zip
```

## Debug

```
adb logcat --pid=$(adb shell pidof -s com.example.jalkster)
```

## Scripts and Tools

`make` runs the common repo commands: build, install, clean.
macOS install: included with Xcode Command Line Tools.

`adb` installs the APK and reads device logs.
macOS install: `brew install android-platform-tools`

`./gradlew` runs the Android build without needing a separate Gradle
install.

`scripts/make_build_zip.sh` creates a source zip with the minimum files
needed to build the APK.
macOS install: no extra install; it is part of this repo.

## Android Security / Lint Tooling

See `Makefile` for the precommit target.

Android Lint checks Android-specific code and resource issues.
macOS install: nothing extra; use the Android Gradle plugin already in
the repo.
run: `./gradlew lint`

Semgrep does static analysis with general and Android-focused rules.
macOS install: `brew install semgrep`
run: `semgrep --config auto`
android rules: `semgrep --config p/android`

OSV-Scanner checks the repo for known vulnerable dependencies.
macOS install: `brew install osv-scanner`
run: `osv-scanner .`
