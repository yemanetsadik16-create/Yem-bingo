# 👑 Yem Bingo - Ethiopia's #1 Live Multiplayer 75-Ball Bingo

[![Android CI/CD](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME/actions/workflows/build-and-publish.yml/badge.svg)](https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME/actions/workflows/build-and-publish.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.00-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%203-Latest-blue.svg)](https://m3.material.io/)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-orange.svg)](https://firebase.google.com/)

Yem Bingo is a production-grade, real-time multiplayer Android application bringing the excitement of classic 75-Ball Bingo to Ethiopia. Players join live betting rooms, daub their 5x5 cards, and compete in real time against opponents on different devices with live ball caller synchronization, automated timers, and integrated Ethiopian mobile money wallets.

---

## 🌟 Key Features

1. **Live Cross-Device Multiplayer**:
   - Real-time multiplayer synchronization via **Cloud Firestore**.
   - Live room presence and active player tracking in `/rooms/{roomId}/players/{userId}`.
   - Synchronized 1–75 ball caller state shared instantly across all devices.
   - Real-time winning claims and instant prize announcements broadcast to all players in the room.

2. **Automated 3-Minute Room Countdown**:
   - Each betting room features a synchronized 3-minute (`03:00`) round countdown.
   - Automated start kicks off when 2 or more players join.
   - Rooms gracefully wait and reset if fewer than 2 players are present.

3. **80% Prize & 20% House Commission**:
   - Automatic pool and commission calculation across 5 tiered rooms:
     - **Mini Starter**: 10 ETB stake (Prize: 80%, Commission: 20%)
     - **Standard Room**: 20 ETB stake
     - **High Roller Suite**: 50 ETB stake
     - **Mega Addis Jackpot**: 100 ETB stake
     - **Habesha VIP Club**: 200 ETB stake

4. **Secure Ethiopian Birr Wallet**:
   - **Ethio telecom Telebirr**: 0% fee rate, quick chips (`+50`, `+100`, `+200`, `+500`, `+1000`, `+2500` ETB), interactive USSD prompt simulation (`*127#`), and one-tap PIN authorization.
   - **Commercial Bank of Ethiopia (CBE Birr)**: Dynamic fee calculation (0% deposit, 1% withdrawal) with interactive 6-digit SMS OTP verification.
   - 256-bit SSL encryption indicator, Ethiopian phone validation (+251), and official receipts with reference IDs (`TLB-ET-2026-` / `CBE-ET-2026-`).

5. **Authentication & Live Tournament Leaderboard**:
   - Google Sign-In via Jetpack `CredentialManager`.
   - Real-time tournament leaderboards streaming verified earnings directly from Firestore.
   - Zero mock/demo account bypasses — clean, authenticated player profiles.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: Clean Architecture / MVVM with `ViewModel`, `StateFlow`, and Repository Pattern
- **Cloud Backend**:
  - Google Firebase Cloud Firestore (Enterprise schema with zero-trust security rules)
  - Google Firebase Authentication
- **Audio & Haptics**: Low-latency `SoundPool` for ball calling and winning fanfare
- **Testing**: Robolectric + JUnit 4 (14/14 local JVM and Firestore rules tests passing)

---

## 🚀 Building the APK Locally

### Prerequisites
- Android Studio Ladybug / Meerkat (or JDK 17 / JDK 21 installed)
- Android SDK 36 (minSdk 24)

### Build Commands

```bash
# Clone the repository
git clone https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME.git
cd YOUR_REPO_NAME

# Grant execute permissions to Gradle wrapper
chmod +x gradlew

# Run the full test suite (Unit & Firestore Rule tests)
./gradlew testDebugUnitTest

# Assemble the Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 GitHub Actions CI/CD Workflow

A complete GitHub Actions workflow is pre-configured at:
`.github/workflows/build-and-publish.yml`

### What the Workflow Does Automatically:
1. **On Every Push or Pull Request** (to `main` or `master`):
   - Sets up JDK 17 (Temurin) and restores `debug.keystore` from `debug.keystore.base64`.
   - Restores `app/google-services.json` from the `GOOGLE_SERVICES_JSON` repository secret (if configured).
   - Executes unit and Robolectric tests (`ExampleUnitTest`, `ExampleRobolectricTest`).
   - Builds the Android APK (`./gradlew assembleDebug`).
   - Uploads the APK as a downloadable workflow artifact (`YemBingo-Debug-APK`) accessible under the GitHub Actions tab.

2. **Enabling Full Firebase & Google Sign-In in CI Builds**:
   - In your GitHub repository, open **Settings -> Secrets and variables -> Actions -> New repository secret**.
   - Name: `GOOGLE_SERVICES_JSON`
   - Value: Paste the full JSON contents of your `app/google-services.json` file.

3. **On Git Tag Push (e.g., `v1.0.0`)**:
   - Automatically builds the APK and creates a new **GitHub Release**.
   - Attaches `YemBingo-v1.0-debug.apk` directly to the release page for user download.

4. **Manual Run (`workflow_dispatch`)**:
   - Go to your repository on GitHub -> **Actions** tab -> **Build and Publish Android APK** -> **Run workflow**.

### How to Publish to Your GitHub:

```bash
# 1. Initialize git (if not already done)
git init
git add .
git commit -m "feat: complete Yem Bingo live multiplayer with Telebirr/CBE wallet and GitHub CI/CD"

# 2. Add your GitHub remote repository
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/YOUR_REPO_NAME.git
git branch -M main
git push -u origin main

# 3. (Optional) Create a release tag to trigger automated GitHub Release with APK
git tag v1.0.0
git push origin v1.0.0
```

---

## 📄 License

Distributed under the Apache 2.0 License.
