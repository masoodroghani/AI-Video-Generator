# AI Video Studio — Android + backend starter

## Included
- Android app in Kotlin/Jetpack Compose.
- Text-to-video and image-to-video modes.
- Prompt, duration, aspect-ratio and image selection UI.
- Runway-backed FastAPI backend with task polling.
- GitHub Actions workflow to build a debug APK.
- Generated video link opens in the device's browser/video handler.

## Provider recommendation
This project uses the Runway API as the first provider because its official API supports both text-to-video and image-to-video, task IDs/status polling, and base64 image data URIs. Review the official docs and current model pricing before use:
- https://docs.dev.runwayml.com/guides/using-the-api/
- https://docs.dev.runwayml.com/api/
- https://docs.dev.runwayml.com/assets/inputs/

Video generation is a paid third-party service and requires a Runway developer account/API key. Costs depend on model, duration, and settings.

## Connect the app
1. Deploy the `backend` folder to a server with HTTPS.
2. Set `RUNWAY_API_KEY` as a server environment variable. Never put this key in Android source or the APK.
3. Follow `backend/README.md` to run the API.
4. In `app/src/main/java/com/example/aivideogenerator/MainActivity.kt`, replace `https://YOUR_BACKEND_HOST` with your backend's HTTPS base URL.
5. Build and install the APK.

## Build APK with GitHub Actions
1. Push this folder's contents to a GitHub repository.
2. Open **Actions** and enable workflows.
3. Run **Build Android APK**.
4. Download the `AI-Video-Studio-debug-apk` artifact from the completed workflow run.

## Build locally
Requires JDK 17, Android SDK, and Gradle 8.9+ (or use Android Studio to sync Gradle). Run:
`gradle assembleDebug`

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Before production
This is a starter project, not a production-ready public service. Add authentication, quotas/billing, request limits, secure storage, analytics/privacy notices, and better task persistence. Validate API model support, billing, and provider response shape with your own developer account. The APK has not been compiled or tested in this environment because Gradle is not installed.
