
// WebXR AR + Hit-Test with simple per-frame aggregation
export async function startWebXRScan(onStableDims, status){
  try{
    if (!('xr' in navigator)) throw new Error('WebXR not available on this device/browser');

    const session = await navigator.xr.requestSession('immersive-ar', {
      requiredFeatures:['hit-test','local'],
      optionalFeatures:['anchors','depth-sensing','light-estimation']
    });

    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl', { xrCompatible:true });
    await gl.makeXRCompatible?.();
    session.updateRenderState({ baseLayer: new XRWebGLLayer(session, gl)});

    const refSpace = await session.requestReferenceSpace('local');
    const viewerSpace = await session.requestReferenceSpace('viewer');
    const hitSource = await session.requestHitTestSource({ space: viewerSpace });

    const Ls=[], Ws=[]; const maxN=40; const feet=m=>m*3.28084;
    const push=(a,v)=>{a.push(v); if(a.length>maxN) a.shift();};
    const median=a=>{const s=[...a].sort((x,y)=>x-y); const n=s.length; return !n?0:(n%2?s[(n-1)/2]:(s[n/2-1]+s[n/2])/2)};
    const stable=a=> a.length>=12 && (Math.max(...a.slice(-12))-Math.min(...a.slice(-12))) < .2;

    const onFrame=(t, frame)=>{
      const pose = frame.getViewerPose(refSpace);
      if (pose){
        const hits = frame.getHitTestResults(hitSource);
        if (hits.length){
          const hit = hits[0].getPose(refSpace);
          const x = hit.transform.position.x;
          const z = hit.transform.position.z;
          session._minX = Math.min(session._minX??x, x);
          session._maxX = Math.max(session._maxX??x, x);
          session._minZ = Math.min(session._minZ??z, z);
          session._maxZ = Math.max(session._maxZ??z, z);
          const Lft = feet(Math.abs(session._maxX - session._minX));
          const Wft = feet(Math.abs(session._maxZ - session._minZ));
          if (Lft>0.5 && Wft>0.5){ push(Ls,Lft); push(Ws,Wft); }
          if (stable(Ls) && stable(Ws)){
            onStableDims({ L:+median(Ls).toFixed(1), W:+median(Ws).toFixed(1), H:8.0 });
            session.end(); return;
          }
          status?.(`Stabilizing… L≈${median(Ls).toFixed(1)}ft, W≈${median(Ws).toFixed(1)}ft`);
        }
      }
      session.requestAnimationFrame(onFrame);
    };
    session.requestAnimationFrame(onFrame);
  }catch(e){ status?.(`XR unavailable: ${e.message}. Use manual/QR/import.`); }
}
