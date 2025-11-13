
export async function consumeShareTarget(){
  const params = new URLSearchParams(location.search);
  if (!params.has('share-target')) return null;
  const media = await caches.open('media-share-v1');
  const f = await media.match('/_shared/scan.json');
  if (f){ const txt = await f.text(); await media.delete('/_shared/scan.json'); return {type:'json', payload: txt}; }
  const l = await media.match('/_shared/link.txt');
  if (l){ const url = await l.text(); await media.delete('/_shared/link.txt'); const res = await fetch(url); return {type:'json', payload: await res.text()}; }
  return null;
}
