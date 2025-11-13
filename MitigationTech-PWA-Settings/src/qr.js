
export async function openQrScanner(onResult){
  const video=document.createElement('video'); video.playsInline=true; video.autoplay=true; video.muted=true; const area=document.getElementById('qrArea'); area.replaceChildren(video);
  const stream=await navigator.mediaDevices.getUserMedia({ video:{ facingMode:'environment' }}); video.srcObject=stream;
  const canvas=document.createElement('canvas'); const ctx=canvas.getContext('2d'); let det=null, jsqr=false; if('BarcodeDetector' in window) det=new window.BarcodeDetector({formats:['qr_code']});
  async function tick(){ if(video.readyState>=2){ canvas.width=video.videoWidth; canvas.height=video.videoHeight; ctx.drawImage(video,0,0); try{ if(det){ const codes=await det.detect(canvas); if(codes?.length){ finish(codes[0].rawValue); return; } } else { if(!jsqr){ await import('https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js'); jsqr=true; } const img=ctx.getImageData(0,0,canvas.width,canvas.height); const res=window.jsQR(img.data,img.width,img.height); if(res?.data){ finish(res.data); return; } } }catch{} } requestAnimationFrame(tick); }
  function finish(txt){ stream.getTracks().forEach(t=>t.stop()); onResult(txt); }
  tick();
}
export function makeQr(el,text){ import('https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js').then(()=>{ el.innerHTML=''; new window.QRCode(el,{text,width:196,height:196}); }); }
