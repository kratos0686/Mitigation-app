
package com.yourcompany.arcoredepth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.json.JSONObject
import java.nio.charset.Charset
import java.util.UUID

class MainActivity : AppCompatActivity() {
  private lateinit var session: Session
  private val planeAgg = PlaneAggregator(windowSize = 45)
  private val handler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Init ARCore session with Depth + Plane detection
    session = Session(this)
    val config = Config(session).apply {
      depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
        Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
      planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
    }
    session.configure(config)

    // Start a simple polling loop (placeholder for a GL render loop)
    handler.post(updateTick)
  }

  private val updateTick = object : Runnable {
    override fun run() {
      try {
        val frame: Frame = session.update()
        planeAgg.accumulate(frame)

        if (planeAgg.hasStableEstimate(minSamples = 15)) {
          val (L, W) = planeAgg.estimateDims()
          val json = buildScanJson(L, W, 8.0)
          // Upload to Firebase Storage and also write an index record to Realtime DB
          uploadAndRecord(jobId = 123, payload = json)
          // Stop loop after first stable upload (demo). Remove this break for continuous capture
          return
        }
      } catch (_: Throwable) {}
      handler.postDelayed(this, 33) // ~30 fps
    }
  }

  private fun buildScanJson(L: Double, W: Double, H: Double): JSONObject {
    return JSONObject(mapOf(
      "format" to "arcore-plan-v1",
      "dims" to mapOf("L" to round1(L), "W" to round1(W), "H" to H),
      "device" to mapOf("model" to android.os.Build.MODEL)
    ))
  }

  private fun round1(v: Double) = kotlin.math.round(v * 10.0) / 10.0

  private fun uploadAndRecord(jobId:Int, payload: JSONObject) {
    val scanId = UUID.randomUUID().toString()

    // 1) Upload JSON to Firebase Storage
    val storage = Firebase.storage
    val ref = storage.reference.child("scans/$scanId.json")
    val bytes = payload.toString().toByteArray(Charset.forName("UTF-8"))
    ref.putBytes(bytes)
      .addOnSuccessListener {
        // 2) Write an index to Firebase Realtime Database for quick lookup by job
        val db = Firebase.database
        val record = mapOf(
          "scanId" to scanId,
          "jobId" to jobId,
          "dims" to payload.getJSONObject("dims").toString(),
          "createdAt" to System.currentTimeMillis()
        )
        db.getReference("jobs/$jobId/scans/$scanId").setValue(record)
          .addOnSuccessListener {
            openPwa(jobId, scanId)
          }
          .addOnFailureListener { openPwa(jobId, scanId) }
      }
      .addOnFailureListener {
        // Still proceed to PWA with local scanId; PWA can fetch later or show local result
        openPwa(jobId, scanId)
      }
  }

  private fun openPwa(jobId:Int, scanId:String) {
    val uri = Uri.parse("https://app.yourdomain.com/#/import-scan?job=$jobId&scanId=$scanId")
    try { CustomTabsIntent.Builder().build().launchUrl(this, uri) }
    catch (_:Throwable) { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
  }
}
