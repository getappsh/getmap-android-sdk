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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.GsonBuilder
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.WorldWindow
import gov.nasa.worldwind.geom.LookAt
import gov.nasa.worldwind.layer.BackgroundLayer
import gov.nasa.worldwind.layer.Layer
import gov.nasa.worldwind.layer.LayerFactory
//import gov.nasa.worldwind.render.Color
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
    private lateinit var mapView: MapView

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var service: GetMapService
    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList[1].directory?.absoluteFile ?: ""
        Log.i("gfgffgf", "$volume")
        wwd = WorldWindow(this)
        wwd.layers.addLayer(BackgroundLayer())
        val geoPackage = "${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg"
        addGeoPkg(geoPackage)

        val lookAt = LookAt().set(
            31.21,
            34.150,
            0.0,
            WorldWind.ABSOLUTE,
            2e6,
            0.0,
            0.0,
            0.0
        )
        wwd.navigator.setAsLookAt(wwd.globe, lookAt)
        val globeLayout = findViewById<View>(R.id.mapView) as FrameLayout
        globeLayout.addView(wwd)

        val instance = MapServiceManager.getInstance()
        service = instance.service
    }

    fun showNorth(view: View) {
        val compass = findViewById<View>(R.id.arrow)
        compass.rotation = 270F
        wwd.navigator.heading = 0.0
        wwd.requestRedraw()
    }


    private fun addGeoPkg(geoPath: String) {
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
            })
    }
}