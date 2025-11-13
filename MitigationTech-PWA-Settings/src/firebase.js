
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js';
import { getStorage, ref as sRef, uploadString, getDownloadURL } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-storage.js';
import { getDatabase, ref, set } from 'https://www.gstatic.com/firebasejs/10.12.2/firebase-database.js';

const firebaseConfig = { apiKey:'YOUR_API_KEY', authDomain:'YOUR_PROJECT.firebaseapp.com', databaseURL:'https://YOUR_PROJECT-default-rtdb.firebaseio.com', projectId:'YOUR_PROJECT', storageBucket:'YOUR_PROJECT.appspot.com', messagingSenderId:'', appId:'' };
const app = initializeApp(firebaseConfig);
const storage = getStorage(app); const db = getDatabase(app);

export async function uploadScanJSON(jobId, jsonObj){
  const scanId = crypto.randomUUID();
  const r = sRef(storage, `scans/${scanId}.json`);
  await uploadString(r, JSON.stringify(jsonObj), 'raw');
  let url=''; try{ url = await getDownloadURL(r); }catch(e){}
  await set(ref(db, `jobs/${jobId}/scans/${scanId}`), { scanId, jobId, dims: jsonObj.dims, downloadUrl: url, createdAt: Date.now() });
  return { scanId, downloadUrl: url };
}

export function setAiPath({mode='simple', provider='backend', model=''}={}){ localStorage.setItem('aiPath', JSON.stringify({mode,provider,model})); }
export function getAiPath(){ try{ return JSON.parse(localStorage.getItem('aiPath')) || {mode:'simple'} }catch{ return {mode:'simple'} } }
