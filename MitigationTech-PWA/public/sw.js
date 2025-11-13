
const APP_CACHE = 'app-shell-v1';
const ASSETS = ['/', '/index.html', '/manifest.json', '/src/app.js', '/src/xr.js', '/src/firebase.js', '/src/qr.js', '/src/share-intake.js'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(APP_CACHE).then(c => c.addAll(ASSETS)));
});
self.addEventListener('activate', e => e.waitUntil(self.clients.claim()));

self.addEventListener('fetch', (e) => {
  const url = new URL(e.request.url);

  // Web Share Target intake
  if (url.pathname === '/share/ingest' && e.request.method === 'POST') {
    e.respondWith((async () => {
      const formData = await e.request.formData();
      const file = formData.get('scan');
      const text = formData.get('text') || formData.get('url') || '';
      const cache = await caches.open('media-share-v1');
      if (file && file.size) {
        await cache.put('/_shared/scan.json', new Response(file));
      } else if (text) {
        await cache.put('/_shared/link.txt', new Response(text, {headers:{'Content-Type':'text/plain'}}));
      }
      return Response.redirect('/?share-target=1', 303);
    })());
    return;
  }

  e.respondWith((async () => {
    const cached = await caches.match(e.request);
    try {
      const fresh = await fetch(e.request);
      const c = await caches.open(APP_CACHE);
      c.put(e.request, fresh.clone());
      return fresh;
    } catch {
      return cached || new Response('Offline', {status:503});
    }
  })());
});
