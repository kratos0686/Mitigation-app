
# Android ARCore Depth + Planes + Firebase (Storage + Realtime DB)

This starter demonstrates:

- **Per-frame plane aggregation** (H+V) to compute stable room L×W.
- **Firebase Storage** upload of the scan JSON + **getDownloadUrl()** example.
- **Firebase Realtime Database** write at `/scans/{scanId}` for quick lookup from the PWA.
- Deep-link to PWA: `https://app.yourdomain.com/#/import-scan?job=...&scanId=...`.

## Quick start
1. In **Firebase Console**, add Android app and drop `google-services.json` into `app/`.
2. Enable **Storage** and **Realtime Database**; keep rules secured for prod.
3. Replace domain endpoints in `MainActivity.kt` and test on a device with **Google Play Services for AR**.

**References**
- ARCore Depth & session update / plane tracking docs. 
- Firebase Storage upload & download URL. 
- Firebase Realtime Database write basics. 
