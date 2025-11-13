
  # Android ARCore Depth + Planes (Merged)

  **What’s included**
  - ARCore Session with **Depth** + **Plane** tracking
  - **Per-frame plane aggregation** (rolling median + stability gate) for L×W
  - Firebase **Storage** upload with optional **getDownloadUrl()**
  - Firebase **Realtime Database** write at `jobs/{jobId}/scans/{scanId}`
  - Deep link to the PWA `https://app.yourdomain.com/#/import-scan?job=...&scanId=...`

  ## Toggle getDownloadUrl
  Set `USE_DOWNLOAD_URL = true` to resolve a public URL after upload; set to `false` to skip.

  ## Setup
  1) Add `google-services.json` under `app/` (Firebase).
2) Replace `PWA_BASE` and `JOB_ID`.
3) Build & run on an ARCore-capable device.
