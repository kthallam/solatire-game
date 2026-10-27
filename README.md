# Quiet Spider

An original, offline Android Spider Solitaire game. No ads, accounts, analytics,
third-party runtime libraries, or internet permission. It uses familiar Spider
rules without Microsoft branding or assets.

## Play

Choose 1, 2, or 4 suits from **New**. Tap a face-up card or same-suit descending
sequence, then tap its destination column. Build King through Ace in one suit to
clear a run; clear all eight to win. Any card or movable sequence can fill an
empty column. Fill empty columns before dealing another row.

**Undo** reverses moves and deals in the current session (up to 200 actions).
**Hint** highlights a legal move; it does not guarantee a solution. Progress and
difficulty save after actions and when the app pauses. Undo history does not
persist across process restarts. New games require confirmation.

## Build

Open this directory in Android Studio, install Android SDK 35 and Build Tools
35.0.0, and use JDK 17. Gradle 8.11.1 and Android Gradle Plugin 8.9.2 are pinned.
The app supports Android 6.0 (API 23) and later.

In the prepared cloud environment:

```sh
source scripts/cloud-env.sh
./gradlew --no-daemon assembleDebug lintDebug
./scripts/test-rules.sh
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. Install on a connected
phone with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, or transfer
that APK to your phone and allow installation from that source. This is a debug
build, not a signed Play Store release.

The isolated cloud checkout is already the working copy; do not create a worktree
unless explicitly requested.

## Validation and limits

The standalone Java rules tests cover deck distribution, moves, suit restrictions,
dealing, automatic completion, card reveal, undo, save restoration, and randomized
legal play. Run `scripts/test-rules.sh` with a JDK on PATH.

A physical-device playtest is still needed for touch ergonomics, small screens,
TalkBack support, and Android lifecycle behavior. The custom-drawn table currently
uses tap-to-move, not dragging, and does not expose individual cards as TalkBack
nodes. There are no daily challenges, sound effects, statistics history, or cloud
sync in this first version.
