
# Mitigation Tech PWA (WebXR + Firebase + QR + Share Target)

## Quick Start
1. Serve `public/` over HTTPS (or localhost):
   ```bash
   npx serve public
   ```
2. Open on Android Chrome / Desktop Chrome for WebXR, or iOS Safari for install testing.
3. Add your Firebase config in `src/firebase.js`.

## iOS Testing
- In Safari on iPhone: open your URL → Share → **Add to Home Screen** → launch from Home Screen.
- For debugging on a Mac: enable **Web Inspector** on iPhone (Settings → Safari → Advanced), connect via USB, and inspect from Safari’s **Develop** menu.

## AI Path Badge
- Stored in `localStorage` via `setAiPath({ mode:'simple'|'advanced', provider, model })`.
- Badge displays **Gemini (API key)** for simple mode, or `provider model` for advanced.
