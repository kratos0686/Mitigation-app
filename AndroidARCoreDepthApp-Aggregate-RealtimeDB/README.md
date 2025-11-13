
# Android ARCore Depth + Plane Aggregation + Firebase Realtime DB (Starter)

**What’s new in this build**
- Per-frame plane aggregation (sliding-window median of horizontal plane extents) for **stable L×W** estimates.
- **Firebase Realtime Database** write: `jobs/{jobId}/scans/{scanId}` index after Storage upload.
- Deep link to PWA: `https://app.yourdomain.com/#/import-scan?job=...&scanId=...`.

## Setup
1. In Firebase Console, add your Android app and download `google-services.json` into `app/`.
2. Enable **Cloud Storage** and **Realtime Database**. Set rules appropriately for your environment.
3. Replace `yourdomain.com` and `jobId` placeholder where needed.
4. Build & run on an ARCore-supported device.

> Plane tracking and depth must be enabled in the ARCore session config. Realtime Database writes use the KTX SDK and a simple schema under `/jobs`.
