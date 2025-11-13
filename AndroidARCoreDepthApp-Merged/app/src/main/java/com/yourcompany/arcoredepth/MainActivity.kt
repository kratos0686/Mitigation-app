
package com.yourcompany.arcoredepth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.ar.core.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.UUID

class MainActivity : AppCompatActivity() {
  private lateinit var session: Session
  private val handler = Handler(Looper.getMainLooper())

  // Config
  private val PWA_BASE = "https://app.yourdomain.com"
  private val JOB_ID = 123
  private val USE_DOWNLOAD_URL = true

  // Rolling extents (feet)
  private val Ls = ArrayDeque<Double>()
  private val Ws = ArrayDeque<Double>()
  private val maxN = 40

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    session = Session(this)
    val cfg = Config(session).apply {
      depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
      planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
    }
    session.configure(cfg)
    handler.post(updateTick)
  }

  private val updateTick = object : Runnable {
    override fun run() {
      try {
        val frame = session.update()
        aggregate(frame)
      } catch (_: Throwable) {}
      handler.postDelayed(this, 33)
    }
  }

  private fun aggregate(frame: Frame) {
    val planes = frame.getUpdatedTrackables(Plane::class.java).filter { it.trackingState == TrackingState.TRACKING }
    var maxX = 0.0; var maxZ = 0.0
    for (p in planes) {
      val lx = p.extentX * 3.28084
      val lz = p.extentZ * 3.28084
      if (lx > maxX) maxX = lx
      if (lz > maxZ) maxZ = lz
    }
    if (maxX > 0 && maxZ > 0) {
      push(Ls, maxX); push(Ws, maxZ)
      if (stable(Ls) && stable(Ws)) {
        handler.removeCallbacks(updateTick)
        val dims = mapOf("L" to r1(median(Ls)), "W" to r1(median(Ws)), "H" to 8.0)
        val json = JSONObject(mapOf("format" to "arcore-plan-v1", "dims" to dims))
        uploadAndRecord(JOB_ID, json)
      }
    }
  }

  private fun push(q: ArrayDeque<Double>, v: Double){ if (q.size >= maxN) q.removeFirst(); q.addLast(v) }
  private fun median(q: ArrayDeque<Double>): Double { val s=q.sorted(); val n=s.size; return if(n==0)0.0 else if(n%2==1) s[n/2] else (s[n/2-1]+s[n/2])/2.0 }
  private fun stable(q: ArrayDeque<Double>): Boolean { if(q.size<12) return false; val t=q.takeLast(12); return (t.maxOrNull()?:0.0)-(t.minOrNull()?:0.0) < 0.2 }
  private fun r1(v: Double)= kotlin.math.round(v*10.0)/10.0

  private fun uploadAndRecord(jobId:Int, payload: JSONObject){
    val scanId = UUID.randomUUID().toString()
    val storage = Firebase.storage
    val ref = storage.reference.child("scans/$scanId.json")
    val bytes = payload.toString().toByteArray(Charset.forName("UTF-8"))

    ref.putBytes(bytes).addOnSuccessListener {
      if (USE_DOWNLOAD_URL) {
        ref.downloadUrl.addOnSuccessListener { uri ->
          writeDb(jobId, scanId, payload, uri.toString())
        }.addOnFailureListener { writeDb(jobId, scanId, payload, "") }
      } else {
        writeDb(jobId, scanId, payload, "")
      }
    }.addOnFailureListener { writeDb(jobId, scanId, payload, "") }
  }

  private fun writeDb(jobId:Int, scanId:String, payload: JSONObject, url:String){
    val db = Firebase.database
    val dimsObj = payload.get("dims")
    val rec = mapOf(
      "scanId" to scanId,
      "jobId" to jobId,
      "dims" to dimsObj,
      "downloadUrl" to url,
      "createdAt" to System.currentTimeMillis()
    )
    db.getReference("jobs/$jobId/scans/$scanId").setValue(rec)
      .addOnCompleteListener { openPwa(jobId, scanId) }
  }

  private fun openPwa(jobId:Int, scanId:String){
    val uri = Uri.parse("$PWA_BASE/#/import-scan?job=$jobId&scanId=$scanId")
    try { CustomTabsIntent.Builder().build().launchUrl(this, uri) }
    catch(_:Throwable){ startActivity(Intent(Intent.ACTION_VIEW, uri)) }
  }
}
