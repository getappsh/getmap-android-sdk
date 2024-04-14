package com.example.example_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.example_app.MapInteraction.ArcGISMapInteraction
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.R)
class MapActivity : AppCompatActivity() {
    private val TAG = MapActivity::class.qualifiedName

    private lateinit var mapContainer: FrameLayout
    private lateinit var overlayView: FrameLayout

    private lateinit var backFrame: View
    private lateinit var blackBack: View
    private lateinit var backFrame2: View
    private lateinit var backFrame3: View
    private lateinit var backFrame4: View

    private lateinit var deliveryButton: Button
    private lateinit var closeButton: Button
    private lateinit var backButton: Button

    private lateinit var showKm: TextView
    private lateinit var showBm: TextView
    private lateinit var dateTextView: TextView

    private var selectMode = false

    private lateinit var mapInteraction: ArcGISMapInteraction

    private lateinit var service: GetMapService
    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapContainer = findViewById(R.id.mapContainer)
        overlayView = findViewById(R.id.overlayView)
        backFrame = findViewById(R.id.backFrame)
        blackBack = findViewById(R.id.blackBack)
        backFrame2 = findViewById(R.id.blackLabel)
        backFrame3 = findViewById(R.id.backFrame3)
        backFrame4 = findViewById(R.id.backFrame4)

        showKm = findViewById(R.id.kmShow)
        showBm = findViewById(R.id.showMb)
        dateTextView = findViewById(R.id.dateText)

        val instance = MapServiceManager.getInstance()
        service = instance.service

        mapInteraction = ArcGISMapInteraction(this, service)

        mapInteraction.setMapView(mapContainer, lifecycle)

        deliveryButton = findViewById(R.id.deliver)
        closeButton = findViewById(R.id.close)
        backButton = findViewById(R.id.back)

        deliveryButton.visibility = View.INVISIBLE
        deliveryButton.setOnClickListener {
            val poly = mapInteraction.renderBBoxData()
//            poly?.let { updateUIWithIntersectionInfo(it) }
            if (poly == null || poly.inCollision){
                Toast.makeText(this, "התיחום שנבחר גדול מידי או מחוץ לתחום", Toast.LENGTH_SHORT).show()
            }else{
                GlobalScope.launch(Dispatchers.IO) {

                    val props = MapProperties("selectedProduct", poly.strPolygon, false)
                    val id = service.downloadMap(props, downloadStatusHandler)
                    if (id == null) {
                        this@MapActivity.runOnUiThread {
                            // This is where your UI code goes.
                            Toast.makeText(
                                applicationContext,
                                "The map already exists, please choose another Bbox",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    Log.d(TAG, "onDelivery: after download map have been called, id: $id")
                }
                val intent = Intent(this@MapActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        closeButton.visibility = View.INVISIBLE
        toggleViews()

        backButton.setOnClickListener {
            selectMode = true
            toggleViews()
        }

        closeButton.setOnClickListener {
            selectMode = false
            toggleViews()
        }

//        geoPackageRender()
        lifecycleScope.launch {
            mapInteraction.renderBaseMap()
        }

        GlobalScope.launch(Dispatchers.Main) {
            processMapPan()
        }
    }

    private suspend fun processMapPan(){
        mapInteraction.mapView.onPan.collect {
            if (!selectMode) {
                return@collect
            }
            val poly = mapInteraction.renderBBoxData() ?: return@collect
            updateUIWithIntersectionInfo(poly)
        }
    }
    private fun updateUIWithIntersectionInfo(poly: PolyObject) {
        val formattedNum = String.format("%.2f", poly.area)
        val spaceMb = (formattedNum.toDouble() * 9).toInt()
        val maxMb = service.config.maxMapSizeInMB.toInt()

        val downloadAble = !poly.inCollision && poly.product != null

        if (spaceMb < maxMb && downloadAble) {
            overlayView.setBackgroundResource(R.drawable.blue_border)
            poly.product?.let {
                dateTextView.text = "צולם : ${it.start} - ${it.end}"
            }
            showKm.text = "שטח משוער : $formattedNum קמ\"ר"
            showBm.text = "נפח משוער : $spaceMb מ\"ב"
        } else {
            overlayView.setBackgroundResource(R.drawable.red_border)
            if (poly.inCollision){
                dateTextView.text = "נמצא בתחום שכבר קיים במכשיר"
            } else {
                dateTextView.text = "מחוץ לטווח הבחירה"
            }
            showKm.text = "שטח משוער :אין נתון"
            showBm.text = "נפח משוער :אין נתון"
        }
    }

    private fun toggleViews(){
        val visibility = if (selectMode) View.VISIBLE else View.INVISIBLE
        deliveryButton.visibility = visibility
        closeButton.visibility = visibility
        backFrame.visibility = visibility
        blackBack.visibility = visibility
        backFrame2.visibility = visibility
        backFrame3.visibility = visibility
        backFrame4.visibility = visibility
        overlayView.visibility = visibility
        backButton.visibility = if (selectMode) View.INVISIBLE else View.VISIBLE
    }
}
