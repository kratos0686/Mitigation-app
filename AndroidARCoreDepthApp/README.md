# Android ARCore Depth Companion App

This is a starter Kotlin project for scanning rooms using ARCore Depth API and sharing the scan data with the PWA.

## Features
- Uses ARCore Depth API to capture depth maps and estimate room dimensions.
- Exports a simplified JSON with L, W, H and optional wall segments.
- Uploads scan JSON to backend and opens PWA via deep link.

## Setup
1. Install Android Studio and SDK 24+.
2. Add ARCore dependency in `build.gradle`:
   implementation "com.google.ar:core:1.43.0"
3. Enable Depth API in ARCore session configuration.
4. Implement plane detection and dimension estimation.
5. Share scan data via deep link to PWA.
