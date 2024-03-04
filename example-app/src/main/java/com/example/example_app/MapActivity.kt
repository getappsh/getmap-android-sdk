package com.example.example_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.geometry.GeodeticCurveType
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.LinearUnit
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.example.example_app.MainActivity
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val TAG = MainActivity::class.qualifiedName
    private lateinit var service: GetMapService
    private val downloadStatusHandler :(MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById<MapView>(R.id.mapView)

        //Get the path of SDCard
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes;
        val volume = storageList[1].directory?.absoluteFile ?: ""
        val pathSd = ("${volume}/com.asio.gis/gis/maps/raster/מיפוי ענן")

        val cfg = Configuration(
//            "https://api-asio-getapp-2.apps.okd4-stage-getapp.getappstage.link",
            "https://api-asio-getapp-5.apps.okd4-stage-getapp.getappstage.link",
//            "http://192.168.2.26:3000",
//            "http://getapp-dev.getapp.sh:3000",
//            "http://localhost:3333",
            "rony@example.com",
            "rony123",
//            File("/storage/1115-0C18/com.asio.gis").path,
            pathSd,
            16,
            null
        )

        service = GetMapServiceFactory.createAsioSdkSvc(applicationContext, cfg)

        setApiKey()

//        service.getDownloadedMaps().forEach{
//            boolRender(pathSd + "/" + it.jsonName)
//        }

//        GlobalScope.launch(Dispatchers.IO) {
//            boolRender()
//        }

        lifecycle.addObserver(mapView)

        val delivery = findViewById<Button>(R.id.deliver)
        delivery.visibility = View.INVISIBLE
        delivery.setOnClickListener {
            this.deliver()
        }

        val back = findViewById<Button>(R.id.back)
        back.setOnClickListener {
//            val intent = Intent(this@MapActivity, MainActivity::class.java)
//            startActivity(intent)
            delivery.visibility = View.VISIBLE
            val backFrame = findViewById<View>(R.id.backFrame)
            backFrame.visibility = View.VISIBLE
            val backFrame2 = findViewById<View>(R.id.backFrame2)
            backFrame2.visibility = View.VISIBLE
            val backFrame3 = findViewById<View>(R.id.backFrame3)
            backFrame3.visibility = View.VISIBLE
            val backFrame4 = findViewById<View>(R.id.backFrame4)
            backFrame4.visibility = View.VISIBLE
            val frame = findViewById<FrameLayout>(R.id.overlayView)
            frame.visibility = View.VISIBLE
        }

        geoPackageRender()

//        // Create a map with the streets basemap
//        // Set the map to the MapView
//        mapView.map = map

//        val displayMetrics = DisplayMetrics()
//        windowManager.defaultDisplay.getMetrics(displayMetrics)
//        val height = displayMetrics.heightPixels
//        val width = displayMetrics.widthPixels
//
//        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
//        val leftTop = ScreenCoordinate(100.0, height - 550.0)
//        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
//
//        val point1 = mapView.screenToLocation(rightTop)!!
//        val point2 = mapView.screenToLocation(leftTop)!!
//        val point3 = mapView.screenToLocation(rightBottom)!!
//
////        val distance = GeometryEngine.distanceGeodeticOrNull(point1, point2)
//
//        mapView.setOnClickListener {
//
//        }
    }

    private fun deliver() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        val leftTop = ScreenCoordinate(100.0, height - 550.0)
        val rightTop = ScreenCoordinate(width -100.0, height - 550.0)
        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
        val leftBottom = ScreenCoordinate(100.0, 550.0)

        onDelivery(mapView.screenToLocation(leftTop)!!, mapView.screenToLocation(rightTop)!!, mapView.screenToLocation(rightBottom)!!,
            mapView.screenToLocation(leftBottom)!!)

        val intent = Intent(this@MapActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun onDelivery(first:Point, second:Point, third:Point, fourth:Point){

        Log.d(TAG, "onDelivery: ");
        GlobalScope.launch(Dispatchers.IO){

            val points = listOf<Point>(
                first,
                second,
                third,
                fourth
            )
            var minLon = first.x
            var maxLon = first.x
            var minLat = first.y
            var maxLat = first.y
            for (point in points) {
                if (point.x < minLon) minLon = point.x
                if (point.x > maxLon) maxLon = point.x
                if (point.y < minLat) minLat = point.y
                if (point.y > maxLat) maxLat = point.y
            }

            val props = MapProperties(
                "selectedProduct",
                "${minLon},${minLat},${maxLon},${maxLat}",
                false
            )
            val id = service.downloadMap(props, downloadStatusHandler);
            if(id == null) {
                this@MapActivity.runOnUiThread {
                    // This is where your UI code goes.
                    Toast.makeText(applicationContext, "The map already exists, please choose another Bbox", Toast.LENGTH_LONG).show()
                }
            }

            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
        }

    }


    private fun setApiKey() {
        // It is not best practice to store API keys in source code. We have you insert one here
        // to streamline this tutorial.

        ArcGISEnvironment.apiKey = ApiKey.create("AAPK9f60194290664c60b1e4e9c2f12731e2EnCmC48fwIqi_aWiIk2SX22TpgeXo5XIS013xIkAhhYX9EFwz1QooTqlN34eD0FM")

    }


    private fun boolRender() {
//        val geoPackage = GeoPackage(path)
//        lifecycleScope.launch {
//            geoPackage.load().onSuccess {
//                if (geoPackage.geoPackageRasters.isNotEmpty()) {
//                    val geoPackageRaster = geoPackage.geoPackageRasters.first()
//                    val rasterLayer = RasterLayer(geoPackageRaster)
//
//
//                    mapView.map?.operationalLayers?.add(rasterLayer)
//                }
//            }

            service.getDownloadedMaps().forEach {
                val footPrint = it.footprint ?: return@forEach

                val graphicsOverlay = GraphicsOverlay()
                val gson = Gson()
                val yellowOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.fromRgba(255, 255, 0), 3f)


                val productPolyDTO = gson.fromJson(footPrint, PolygonDTO::class.java)
                productPolyDTO.coordinates.forEach { it ->
                    val points: List<Point> = it.map { Point(it[0], it[1], SpatialReference.wgs84()) }
                    val polygon = Polygon(points)

                    val graphic = Graphic(polygon, yellowOutlineSymbol)
                    graphicsOverlay.graphics.add(graphic)

                }

                mapView.graphicsOverlays.add(graphicsOverlay)
            }
//        }
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun geoPackageRender() {
        val storageManager :StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes;
        val volume = storageList[1].directory?.absoluteFile ?: ""
        val geoPackage = GeoPackage("${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg")
        lifecycleScope.launch {
            geoPackage.load().onSuccess {
                if (geoPackage.geoPackageRasters.isNotEmpty()) {
                    val geoPackageRaster = geoPackage.geoPackageRasters.first()
                    val rasterLayer = RasterLayer(geoPackageRaster)

                    val basemap = Basemap(rasterLayer)
                    val map: ArcGISMap = ArcGISMap(basemap)
                    val graphicsOverlay = GraphicsOverlay()
                    val gson = Gson()
                    val yellowOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.fromRgba(255, 255, 0), 3f)
                    DiscoveryProductsManager.getInstance().products.forEach{p ->
                        run {
                            val json = JSONObject(p.footprint)
                            val type = json.getString("type")

                            if(type == "Polygon") {
                                val productPolyDTO = gson.fromJson(p.footprint, PolygonDTO::class.java)
                                productPolyDTO.coordinates.forEach { it ->
                                    val points: List<Point> = it.map { Point(it[0], it[1], SpatialReference.wgs84()) }
                                    val polygon = Polygon(points)

                                    val graphic = Graphic(polygon,yellowOutlineSymbol)
                                    graphicsOverlay.graphics.add(graphic)
                                }
                            } else {
                                val productPolyDTO = gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                                productPolyDTO.coordinates.forEach { polygonCoords ->
                                    val rings: MutableList<List<Point>> = mutableListOf()
                                    polygonCoords.forEach { ringCoords ->
                                        val points: MutableList<Point> = mutableListOf()
                                        ringCoords.forEach { coord ->
                                            val point = Point(coord[0], coord[1])
                                            points.add(point)
                                        }
                                        rings.add(points)
                                    }
                                    val polygon = Polygon(rings.flatten())
                                    val graphic = Graphic(polygon, yellowOutlineSymbol)
                                    graphicsOverlay.graphics.add(graphic)
                                }
                            }
                        }
                    }
                    mapView.graphicsOverlays.add(graphicsOverlay)
                    mapView.map = map
                    mapView.setViewpoint(Viewpoint(31.7270, 34.6, 2000000.0))
                } else {
                    Log.i("Error", "No feature tables found in the GeoPackage.")
                }
            }.onFailure { error ->
                Log.i("Error", "Error loading geopackage: ${error.message}")
            }
        }
    }
}
