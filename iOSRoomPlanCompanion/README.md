
# iOS RoomPlan Companion (Starter)

- Scans a room with Apple RoomPlan (iOS 16+ on LiDAR devices)
- Shares a minimal JSON to your PWA via Universal Link/deep link

## Setup
1) In Xcode, enable **RoomPlan** capability and add **Associated Domains** (applinks:app.yourdomain.com).
2) Replace the share URL in `ContentView.shareToPWA`.
3) Add your AASA file at `https://app.yourdomain.com/.well-known/apple-app-site-association`.
