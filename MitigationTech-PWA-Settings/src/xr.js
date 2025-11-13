
export async function startWebXRScan(onStableDims, status){
  try{
    if(!('xr' in navigator)) throw new Error('WebXR not available');
    const session = await navigator.xr.requestSession('immersive-ar',{ requiredFeatures:['hit-test','local'], optionalFeatures:['anchors','depth-sensing','light-estimation'] });
    const canvas=document.createElement('canvas'); const gl=canvas.getContext('webgl',{xrCompatible:true}); await gl.makeXRCompatible?.();
    session.updateRenderState({ baseLayer: new XRWebGLLayer(session, gl)});
    const ref=await session.requestReferenceSpace('local'); const viewer=await session.requestReferenceSpace('viewer');
    const hitSrc = await session.requestHitTestSource({ space: viewer });
    const Ls=[], Ws=[]; const maxN=40; const feet=m=>m*3.28084; const push=(a,v)=>{a.push(v); if(a.length>maxN)a.shift();}; const med=a=>{const s=[...a].sort((x,y)=>x-y); const n=s.length; return !n?0:(n%2?s[(n-1)/2]:(s[n/2-1]+s[n/2])/2)}; const stable=a=>a.length>=12 && (Math.max(...a.slice(-12))-Math.min(...a.slice(-12)))<.2;
    const onFrame=(t,frame)=>{ const pose=frame.getViewerPose(ref); if(pose){ const hits=frame.getHitTestResults(hitSrc); if(hits.length){ const hp=hits[0].getPose(ref); const x=hp.transform.position.x, z=hp.transform.position.z; session._minX=Math.min(session._minX??x,x); session._maxX=Math.max(session._maxX??x,x); session._minZ=Math.min(session._minZ??z,z); session._maxZ=Math.max(session._maxZ??z,z); const L=feet(Math.abs(session._maxX-session._minX)); const W=feet(Math.abs(session._maxZ-session._minZ)); if(L>.5 && W>.5){ push(Ls,L); push(Ws,W);} if(stable(Ls)&&stable(Ws)){ onStableDims({L:+med(Ls).toFixed(1), W:+med(Ws).toFixed(1), H:8.0}); session.end(); return;} status?.(`Stabilizing… L≈${med(Ls).toFixed(1)}ft, W≈${med(Ws).toFixed(1)}ft`);} } session.requestAnimationFrame(onFrame); };
    session.requestAnimationFrame(onFrame);
  }catch(e){ status?.(`XR unavailable: ${e.message}. Use manual/QR/import.`); }
}
