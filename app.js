
import { startWebXRScan } from './xr.js';
import { uploadScanJSON, setAiPath, getAiPath } from './firebase.js';
import { openQrScanner, makeQr } from './qr.js';
import { consumeShareTarget } from './share-intake.js';

const xrBtn = document.getElementById('btnStartXR');
const statusEl = document.getElementById('xrStatus');
const inbox = document.getElementById('inbox');
const btnOpenQr = document.getElementById('btnOpenQr');
const btnMakeQr = document.getElementById('btnMakeQr');
const qrOut = document.getElementById('qrOut');
const qrLink = document.getElementById('qrLink');
const aiBadge = document.getElementById('aiPathBadge');

const JOB_ID = Number(new URLSearchParams(location.search).get('job')) || 123;

function addInboxItem(rec){
  const li = document.createElement('li');
  const d = rec.dims || {}; const text = `${rec.scanId} — ${d.L||'?'}×${d.W||'?'}×${d.H||'?'} ft`;
  li.textContent = text; inbox.prepend(li);
}

function refreshAiBadge(){
  const path = getAiPath();
  if (!path || path.mode==='simple') { aiBadge.textContent = 'AI: Gemini (API key)'; aiBadge.style.background='#065f46'; return; }
  aiBadge.textContent = `AI: ${path.provider}${path.model? ' '+path.model:''}`;
  aiBadge.style.background = '#1f2937';
}
refreshAiBadge();

xrBtn.onclick = () => startWebXRScan(async (dims)=>{
  statusEl.textContent = `Stable: ${dims.L}×${dims.W}×${dims.H} ft. Uploading...`;
  const { scanId } = await uploadScanJSON(JOB_ID, {format:'webxr-plan-v1', dims});
  addInboxItem({ scanId, dims });
  statusEl.textContent = `Uploaded scan ${scanId}.`;
}, msg => statusEl.textContent = msg);

btnOpenQr.onclick = () => openQrScanner(text => {
  if (/^https?:/.test(text)) location.href = text; else alert('Scanned: '+text);
});

btnMakeQr.onclick = () => makeQr(qrOut, (qrLink.value||'').trim());

// Process Web Share Target payloads
(async () => {
  const shared = await consumeShareTarget();
  if (shared?.type==='json') {
    try{
      const obj = JSON.parse(shared.payload);
      const { scanId } = await uploadScanJSON(JOB_ID, obj);
      addInboxItem({ scanId, dims: obj.dims || {} });
    }catch(e){ console.warn('Share import failed', e); }
  }
})();
