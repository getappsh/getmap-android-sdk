package com.example.example_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.arcgismaps.geometry.Point
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.GsonBuilder
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.WorldWindow
import gov.nasa.worldwind.geom.LookAt
import gov.nasa.worldwind.layer.BackgroundLayer
import gov.nasa.worldwind.layer.Layer
import gov.nasa.worldwind.layer.LayerFactory
import gov.nasa.worldwind.render.Color
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.MapData
import gov.nasa.worldwind.BasicWorldWindowController
import gov.nasa.worldwind.geom.Position
import gov.nasa.worldwind.gesture.GestureRecognizer
import gov.nasa.worldwind.gesture.PinchRecognizer
import gov.nasa.worldwind.render.ImageSource
import gov.nasa.worldwind.render.Renderable
import gov.nasa.worldwind.shape.Highlightable
import gov.nasa.worldwind.shape.Placemark
import gov.nasa.worldwind.shape.PlacemarkAttributes
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


@RequiresApi(Build.VERSION_CODES.R)
class MapActivity : AppCompatActivity() {
    lateinit var wwd: WorldWindow

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var service: GetMapService
    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val instance = MapServiceManager.getInstance()
        service = instance.service
        addGeoPkg()

        wwd = WorldWindow(this)
        wwd.worldWindowController = PickNavigateController(this)
        wwd.layers.addLayer(BackgroundLayer())

        val lookAt = LookAt().set(
            31.31,
            35.10,
            0.0,
            WorldWind.ABSOLUTE,
            630000.0,
            0.0,
            0.0,
            0.0
        )
        wwd.navigator.setAsLookAt(wwd.globe, lookAt)
        val globeLayout = findViewById<View>(R.id.mapView) as FrameLayout
        globeLayout.addView(wwd)

        val compass = findViewById<View>(R.id.arrow)
        compass.setOnClickListener {
            showNorth()
        }

        val delivery = findViewById<Button>(R.id.deliver)
        delivery.visibility = View.INVISIBLE

        var overlayView = findViewById<FrameLayout>(R.id.overlayView)
        val close = findViewById<Button>(R.id.close)
        close.visibility = View.INVISIBLE

        val back = findViewById<Button>(R.id.back)
        back.setOnClickListener {
            delivery.visibility = View.VISIBLE
            close.visibility = View.VISIBLE
            val backFrame = findViewById<View>(R.id.backFrame)
            backFrame.visibility = View.VISIBLE
            val blackBack = findViewById<View>(R.id.blackBack)
            blackBack.visibility = View.VISIBLE
            val backFrame2 = findViewById<View>(R.id.blackLabel)
            backFrame2.visibility = View.VISIBLE
            val backFrame3 = findViewById<View>(R.id.backFrame3)
            backFrame3.visibility = View.VISIBLE
            val backFrame4 = findViewById<View>(R.id.backFrame4)
            backFrame4.visibility = View.VISIBLE
            val frame = findViewById<FrameLayout>(R.id.overlayView)
            frame.visibility = View.VISIBLE
            back.visibility = View.INVISIBLE
        }

        close.setOnClickListener {
            delivery.visibility = View.INVISIBLE
            close.visibility = View.INVISIBLE
            val backFrame = findViewById<View>(R.id.backFrame)
            backFrame.visibility = View.INVISIBLE
            val blackBack = findViewById<View>(R.id.blackBack)
            blackBack.visibility = View.INVISIBLE
            val backFrame2 = findViewById<View>(R.id.blackLabel)
            backFrame2.visibility = View.INVISIBLE
            val backFrame3 = findViewById<View>(R.id.backFrame3)
            backFrame3.visibility = View.INVISIBLE
            val backFrame4 = findViewById<View>(R.id.backFrame4)
            backFrame4.visibility = View.INVISIBLE
            val frame = findViewById<FrameLayout>(R.id.overlayView)
            frame.visibility = View.INVISIBLE
            back.visibility = View.VISIBLE
        }
    }

    private fun addGeoPkg() {
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList[1].directory?.absoluteFile ?: ""
        Log.i("gfgffgf", "$volume")
        val geoPath = "${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg"

        val layerFactory = LayerFactory()
        layerFactory.createFromGeoPackage(
            geoPath,
            object : LayerFactory.Callback {
                override fun creationSucceeded(factory: LayerFactory?, layer: Layer?) {
                    wwd.layers.addLayer(layer)
                    Log.i("gov.nasa.worldwind", "GeoPackage layer creation succeeded")
                }

                override fun creationFailed(factory: LayerFactory?, layer: Layer?, ex: Throwable?) {
                    Log.e("gov.nasa.worldwind", "GeoPackage layer creation failed", ex)
                }
            }
        )
    }

    private fun calculateDistance(point1: Position, point2: Position): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Radius of the Earth in meters
        val radius = 6371000 // 6371 km converted to meters

        return radius * c
    }

    private fun onDelivery() {
        Log.d(TAG, "onDelivery: ")

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

//        val leftTop = ScreenCoordinate(100.0, height - 550.0)
//        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
//        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
//        val leftBottom = ScreenCoordinate(100.0, 550.0)
//
//        val pLeftTop = wwd.screenToLocation(leftTop)!!
//        val pRightBottom = wwd.screenToLocation(rightBottom)!!
//        val pRightTop = wwd.screenToLocation(rightTop)!!
//        val pLeftBottom = wwd.screenToLocation(leftBottom)!!

        val navCheck = wwd.worldWindowController.worldWindow.navigator
        GlobalScope.launch(Dispatchers.IO) {
            val props = MapProperties(
                "selectedProduct",
                "${pLeftTop.x},${pLeftTop.y},${pRightTop.x},${pRightTop.y},${pRightBottom.x},${pRightBottom.y},${pLeftBottom.x},${pLeftBottom.y},${pLeftTop.x},${pLeftTop.y}",
                false
            )
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


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this@MapActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showNorth() {
        val compass = findViewById<View>(R.id.arrow)
        compass.rotation = 0F
        wwd.navigator.heading = 0.0
        wwd.requestRedraw()
    }

    open inner class PickNavigateController (val context: AppCompatActivity) :
        BasicWorldWindowController() {

        override fun handleRotate(recognizer: GestureRecognizer?) {
            super.handleRotate(recognizer)
            var rotation = wwd.navigator.heading
            if (rotation < 0) {
                rotation += 360
            }

            val compass = findViewById<View>(R.id.arrow)
            compass.rotation = (180 - (rotation + 180)).toFloat()
        }

        private var pickGestureDetector = GestureDetector(
            context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (lookAt.range > 1000) {
                        val pickedObj = wwd.pick(e.x, e.y).terrainPickedObject()
                        val totalRange = lookAt.range / 2
                        val totalLat = lookAt.latitude - pickedObj.terrainPosition.latitude
                        val totalLon = lookAt.longitude - pickedObj.terrainPosition.longitude
                        GlobalScope.launch(Dispatchers.Default) {
                            for (i in 1..100) {
                                lookAt.range -= totalRange / 100
                                lookAt.latitude -= totalLat / 100
                                lookAt.longitude -= totalLon / 100
                                lookAt.heading = wwd.navigator.heading
                                wwd.navigator.setAsLookAt(wwd.globe, lookAt)
                                wwd.requestRedraw()
                                delay(1)
                            }
                        }
                    }
                    return true
                }
            }
        )
        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Allow pick listener to process the event first.
            Log.i("fsgxsx", "asdsxvsvz")
            val consumed = pickGestureDetector.onTouchEvent(event)

            // If event was not consumed by the pick operation, pass it on the globe navigation handlers
            return if (!consumed) {

                // The super class performs the pan, tilt, rotate and zoom
                super.onTouchEvent(event)
            } else consumed
        }
    }
    fun computeLatitude(y: Double, screenHeight: Double): Double {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        return ((screenHeight - y) / height) * 180 - 90
    }

    fun computeLongitude(x: Double, screenWidth: Double): Double {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        return (x / width) * 360 - 180
    }
}