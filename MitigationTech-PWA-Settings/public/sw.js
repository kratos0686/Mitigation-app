
const APP_CACHE='app-shell-v1';
const ASSETS=['/','/index.html','/manifest.json','/src/app.js','/src/xr.js','/src/firebase.js','/src/qr.js','/src/share-intake.js'];
self.addEventListener('install',e=>{e.waitUntil(caches.open(APP_CACHE).then(c=>c.addAll(ASSETS)))});
self.addEventListener('activate',e=>e.waitUntil(self.clients.claim()));
self.addEventListener('fetch',e=>{
  const url=new URL(e.request.url);
  if(url.pathname==='/share/ingest' && e.request.method==='POST'){
    e.respondWith((async()=>{const fd=await e.request.formData(); const f=fd.get('scan'); const t=fd.get('text')||fd.get('url')||''; const c=await caches.open('media-share-v1'); if(f&&f.size){await c.put('/_shared/scan.json', new Response(f));} else if(t){await c.put('/_shared/link.txt', new Response(t,{headers:{'Content-Type':'text/plain'}}));} return Response.redirect('/?share-target=1',303);})()); return;}
  e.respondWith((async()=>{const cached=await caches.match(e.request); try{const fresh=await fetch(e.request); const c=await caches.open(APP_CACHE); c.put(e.request,fresh.clone()); return fresh;}catch{return cached||new Response('Offline',{status:503});}})());
});
