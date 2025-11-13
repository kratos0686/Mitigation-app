
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
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
  private lateinit var session: Session
  private lateinit var config: Config

  // rolling lists (last N frame extents)
  private val lastL = ArrayDeque<Double>()
  private val lastW = ArrayDeque<Double>()
  private val maxSamples = 30
  private val handler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // --- ARCore session (Depth + Planes H&V) ---
    session = Session(this)
    config = Config(session).apply {
      depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
      planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
    }
    session.configure(config)

    // Start a simple update loop (in a real app, drive via GL render loop)
    handler.post(updateRunnable)
  }

  private val updateRunnable = object : Runnable {
    override fun run() {
      try {
        val frame = session.update()
        aggregatePlanes(frame)
      } catch (_: Throwable) {}
      handler.postDelayed(this, 33) // ~30 FPS
    }
  }

  /**
   * Aggregate plane extents across frames and stabilize using rolling median.
   * For simplicity, we derive L from the max horizontal plane extentX and W from extentZ.
   */
  private fun aggregatePlanes(frame: Frame) {
    val planes = frame.getUpdatedTrackables(Plane::class.java)
      .filter { it.trackingState == TrackingState.TRACKING }

    var maxX = 0.0
    var maxZ = 0.0
    for (p in planes) {
      // extentX / extentZ are in meters; convert to feet (× 3.28084)
      val lx = (p.extentX * 3.28084).toDouble()
      val lz = (p.extentZ * 3.28084).toDouble()
      if (lx > maxX) maxX = lx
      if (lz > maxZ) maxZ = lz
    }
    if (maxX > 0 && maxZ > 0) {
      pushSample(lastL, maxX); pushSample(lastW, maxZ)
      val L = median(lastL)
      val W = median(lastW)

      // Export when stable enough (change < 0.2 ft across last few samples)
      if (isStable(lastL) && isStable(lastW)) {
        handler.removeCallbacks(updateRunnable)
        val dims = mapOf("L" to round1(L), "W" to round1(W), "H" to 8.0) // simple H placeholder
        val json = JSONObject(mapOf(
          "format" to "arcore-plan-v1",
          "dims" to dims,
          "device" to mapOf("model" to android.os.Build.MODEL, "depth" to config.depthMode.name)
        ))
        uploadAndRecord(json) { scanId -> openPwaDeepLink(123, scanId) }
      }
    }
  }

  private fun pushSample(q: ArrayDeque<Double>, v: Double) {
    if (q.size >= maxSamples) q.removeFirst()
    q.addLast(v)
  }
  private fun median(q: ArrayDeque<Double>): Double {
    val s = q.sorted(); val n = s.size
    return if (n == 0) 0.0 else if (n % 2 == 1) s[n/2] else (s[n/2-1] + s[n/2]) / 2.0
  }
  private fun isStable(q: ArrayDeque<Double>): Boolean {
    if (q.size < 10) return false
    val tail = q.takeLast(10)
    val span = (tail.maxOrNull() ?: 0.0) - (tail.minOrNull() ?: 0.0)
    return span < 0.2 // feet
  }
  private fun round1(v: Double) = kotlin.math.round(v * 10.0) / 10.0

  /** Upload JSON to Firebase Storage, fetch downloadUrl, write metadata to Realtime DB */
  private fun uploadAndRecord(payload: JSONObject, onDone:(String)->Unit) {
    val storage = Firebase.storage
    val db = Firebase.database

    val scanId = UUID.randomUUID().toString()
    val storagePath = "scans/$scanId.json"
    val ref = storage.reference.child(storagePath)
    val bytes = payload.toString().toByteArray(Charset.forName("UTF-8"))

    ref.putBytes(bytes)
      .addOnSuccessListener {
        // --- getDownloadUrl example ---
        ref.downloadUrl
          .addOnSuccessListener { uri ->
            val url = uri.toString()
            val record = mapOf(
              "scanId" to scanId,
              "storagePath" to storagePath,
              "downloadUrl" to url,
              "dims" to payload.getJSONObject("dims").toString(),
              "createdAt" to System.currentTimeMillis()
            )
            db.getReference("scans").child(scanId).setValue(record)
              .addOnCompleteListener { onDone(scanId) }
          }
          .addOnFailureListener { _ -> onDone(scanId) }
      }
      .addOnFailureListener { _ -> onDone(scanId) }
  }

  private fun openPwaDeepLink(jobId:Int, scanId:String){
    val uri = Uri.parse("https://app.yourdomain.com/#/import-scan?job=$jobId&scanId=$scanId")
    try { CustomTabsIntent.Builder().build().launchUrl(this, uri) }
    catch (_:Throwable){ startActivity(Intent(Intent.ACTION_VIEW, uri)) }
  }
}
