<p align="center">
  <img src="docs/logo.png" alt="CSCrammer logo" width="160">
</p>

<h1 align="center">CSCrammer</h1>

<p align="center">An offline reviewer for the Philippine Civil Service Exam (CSC).</p>

---

CSCrammer comes with 2,500 practice questions across the five CSC exam subjects. Install it and start reviewing right away: no account, no internet, and nothing to import.

## Features

- **Quick review:** 10 random questions from any subject, with instant feedback and explanations.
- **Practice:** choose a subject, topics, difficulty and how many questions.
- **Mock exam:** a full 170-item exam in 3h 10m, or a 50-item mini in 55 minutes. It has a timer, a question navigator and flags, and submits itself when time runs out. You need 80% to pass.
- **Review:** re-drill questions you got wrong, plus any you bookmarked.
- **Progress:**
  - overall accuracy
  - accuracy per subject
  - weakest topics
  - daily streak
  - session history
- **Settings:** dark theme (the default), day theme, or follow the system. You can also reset your progress.

Subjects: Verbal Ability, Numerical Ability, Analytical Ability, General Information, and Philippine Constitution & Public Service Ethics.

Everything stays on your phone. The app asks for no permissions and never connects to the internet.

## Requirements

- Android 8.0 (API 26) or newer
- To build: JDK 17 or newer, and the Android SDK with platform 37

## Building

```sh
./gradlew assembleDebug        # debug APK
./gradlew test                 # unit tests
./gradlew connectedAndroidTest # instrumented tests (needs a device or emulator)
./gradlew lint
```

### Release builds

Release builds use R8 to shrink the code. To sign them, copy `keystore.properties.example` to `keystore.properties` and fill in your keystore details. Git ignores this file. Without it, release builds come out unsigned.

```sh
./gradlew assembleRelease   # APK
./gradlew bundleRelease     # Android App Bundle for Google Play
```

## Tech stack

Kotlin, Jetpack Compose with Material 3, Navigation Compose, Room, DataStore, and Coroutines/Flow. The questions are bundled in `app/src/main/assets/questions.csv` and loaded into Room the first time the app starts.

## License

Copyright (C) 2026 JamesLunaa

The source code is licensed under the [GNU General Public License v3.0](LICENSE). You may use, change and share it, but any version you distribute must also be released under GPL-3.0 with its source code.

**Exceptions:**

- **Question bank** (`app/src/main/assets/questions.csv`): included for review purposes only. It is not covered by the GPL, and no other license is granted for it.
- **Archivo font:** © The Archivo Project Authors, licensed under the [SIL Open Font License 1.1](app/src/main/assets/OFL-Archivo.txt).

## Disclaimer

CSCrammer is not affiliated with or endorsed by the Civil Service Commission of the Philippines. Practice questions may differ from those on the actual exam.
