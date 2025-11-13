
export async function consumeShareTarget(){
  const p=new URLSearchParams(location.search); if(!p.has('share-target')) return null; const c=await caches.open('media-share-v1'); const f=await c.match('/_shared/scan.json'); if(f){ const txt=await f.text(); await c.delete('/_shared/scan.json'); return {type:'json', payload:txt}; } const l=await c.match('/_shared/link.txt'); if(l){ const url=await l.text(); await c.delete('/_shared/link.txt'); const res=await fetch(url); return {type:'json', payload: await res.text()}; } return null;
}
