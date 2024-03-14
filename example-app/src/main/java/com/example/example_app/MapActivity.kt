package com.example.example_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.util.Log
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
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.CompositeSymbol
import com.arcgismaps.mapping.symbology.HorizontalAlignment
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.symbology.TextSymbol
import com.arcgismaps.mapping.symbology.VerticalAlignment
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.arcgismaps.mapping.view.ScreenCoordinate
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@RequiresApi(Build.VERSION_CODES.R)
class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var service: GetMapService
    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)

        //Get the path of SDCard
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList[1].directory?.absoluteFile ?: ""
        val pathSd = ("${volume}/com.asio.gis/gis/maps/raster/מיפוי ענן")
        val cfg = Configuration(
            "https://api-asio-getapp-6.apps.okd4-stage-getapp.getappstage.link",
            "rony@example.com",
            "rony123",
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
//            Log.i("ffffffd", "${downloadAvailable()}")

            this.onDelivery()
        }

        val back = findViewById<Button>(R.id.back)
        back.setOnClickListener {
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

//        Create a map with the streets basemap
//        Set the map to the MapView
        geoPackageRender()

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

//    private fun getBoundingBoxArea(topLeft: Point, width: Double, height: Double, angle: Float): Polygon {
//        val absCosRA = abs(cos(angle))
//        val absSinRA = abs(sin(angle))
//
//        val bbW = width * absCosRA + height * absSinRA
//        val bbH = width * absSinRA + height * absCosRA
//
//        val bbX = topLeft.x - (bbW - width) / 2
//        val bbY = topLeft.y - (bbH - height) / 2
//
//        val bBox = Polygon()
//        bBox.startPath(bbX, bbY)
//        bBox.lineTo(bbX + bbW, bbY)
//        bBox.lineTo(bbX + bbW, bbY + bbH)
//        bBox.lineTo(bbX, bbY + bbH)
//        bBox.lineTo(bbX, bbY)
//
//        return bBox
//    }

//    private fun downloadAvailable() : Double{
//        val displayMetrics = DisplayMetrics()
//        windowManager.defaultDisplay.getMetrics(displayMetrics)
//        val height = displayMetrics.heightPixels
//        val width = displayMetrics.widthPixels
//
//        val leftTop = ScreenCoordinate(100.0, height - 550.0)
//        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
//        val leftBottom = ScreenCoordinate(100.0, 550.0)
//
//        val point1 = mapView.screenToLocation(leftTop)!!
//        val point3 = mapView.screenToLocation(rightTop)!!
//        val point4 = mapView.screenToLocation(leftBottom)!!
//
//        val d1 = sqrt((point3.x - point1.x).pow(2.0) + (point3.y - point1.y).pow(2.0))
//        val d2 = sqrt((point4.x - point1.x).pow(2.0) + (point4.y - point1.y).pow(2.0))
//
//        val areaData = (d2 * d1) / 1000000
//
//        val top =  GeometryEngine.distanceOrNull(point1, point3)!!
//        val side =  GeometryEngine.distanceOrNull(point1, point4)!!
//
//        return areaData.toDouble()
//    }


    private fun calculateDistance(point1: ScreenCoordinate, point2: ScreenCoordinate): Int {
        val lat1 = Math.toRadians(point1.y)
        val lon1 = Math.toRadians(point1.x)
        val lat2 = Math.toRadians(point2.y)
        val lon2 = Math.toRadians(point2.x)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // Radius of the Earth in kilometers
        val radius = 6371

        return (radius * c / 1000).toInt()
    }

    private fun onDelivery() {
        Log.d(TAG, "onDelivery: ")

        //val d1 = sqrt((point3.x - point1.x).pow(2.0) + (point3.y - point1.y).pow(2.0))
//        val d2 = sqrt((point4.x - point1.x).pow(2.0) + (point4.y - point1.y).pow(2.0))
//        val areaDouble = getBoundingBoxArea(point1, d1, d2, currentRotation)

        val currentRotation = mapView.rotation

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        val leftTop = ScreenCoordinate(100.0, height - 550.0)
        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
        val leftBottom = ScreenCoordinate(100.0, 550.0)

        val pLeftTop = mapView.screenToLocation(leftTop)!!
        val pRightBottom = mapView.screenToLocation(rightBottom)!!
        val pRightTop = mapView.screenToLocation(rightTop)!!
        val pLeftBottom = mapView.screenToLocation(leftBottom)!!

        Log.i("selected points", "${pLeftTop.x},${pLeftTop.y}")
        Log.i("selected points", "${pRightTop.x},${pRightTop.y}")
        Log.i("selected points", "${pRightBottom.x},${pRightBottom.y}")
        Log.i("selected points", "${pLeftBottom.x},${pLeftBottom.y}")


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
    }

    private fun setApiKey() {
        val keyId =
            "AAPK9f60194290664c60b1e4e9c2f12731e2EnCmC48fwIqi_aWiIk2SX22TpgeXo5XIS013xIkAhhYX9EFwz1QooTqlN34eD0FM"
        ArcGISEnvironment.apiKey = ApiKey.create(keyId)
    }

//    private fun boolRender() {
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

//            service.getDownloadedMaps().forEach {
//                val footPrint = it.footprint ?: return@forEach
//
//                val graphicsOverlay = GraphicsOverlay()
//                val gson = Gson()
//                val yellowOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.fromRgba(255, 255, 0), 3f)
//
//
//                val productPolyDTO = gson.fromJson(footPrint, PolygonDTO::class.java)
//                productPolyDTO.coordinates.forEach { it ->
//                    val points: List<Point> = it.map { Point(it[0], it[1], SpatialReference.wgs84()) }
//                    val polygon = Polygon(points)
//
//                    val graphic = Graphic(polygon, yellowOutlineSymbol)
//                    graphicsOverlay.graphics.add(graphic)
//
//                }
//
//                mapView.graphicsOverlays.add(graphicsOverlay)
//            }
//        }
//    }


    private fun geoPackageRender() {
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
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
                    val yellowOutlineSymbol = SimpleLineSymbol(
                        SimpleLineSymbolStyle.Dash,
                        Color.fromRgba(255, 255, 0),
                        3f
                    )
                    val pinkOutlineSymbol = SimpleLineSymbol(
                        SimpleLineSymbolStyle.Dash,
                        Color.fromRgba(240, 26, 133),
                        3f
                    )
                    CoroutineScope(Dispatchers.IO).launch {
                        service.getDownloadedMaps().forEach { g ->
                            val nums = g.footprint?.split(",") ?: ArrayList()
                            val coords = ArrayList<Point>()
                            for (i in 0 until nums.size - 1 step 2) {
                                val lon = nums[i].toDouble()
                                val lat = nums[i + 1].toDouble()

                                coords.add(Point(lon, lat, SpatialReference.wgs84()))
                            }
                            val polygon = Polygon(coords)

                            var endName = ""
                            if (g.fileName?.length == 60 || g.fileName?.length == 63 || g.fileName?.length == 61) {
                                endName = g.fileName?.takeLast(11)?.slice(IntRange(0, 3)).toString()
                            } else {
                                endName = g.fileName?.takeLast(9)?.slice(IntRange(0, 3)).toString()
                            }
                            val textSymbol = TextSymbol(
                                endName,
                                Color.fromRgba(255, 255, 0),
                                14f,
                                HorizontalAlignment.Center,
                                VerticalAlignment.Top
                            )

                            val compositeSymbol = CompositeSymbol().apply {
                                symbols.add(yellowOutlineSymbol)
                                symbols.add(textSymbol)
                            }

                            val graphic = Graphic(polygon, compositeSymbol)

                            graphicsOverlay.graphics.add(graphic)
                        }

                    }


                    DiscoveryProductsManager.getInstance().products.forEach { p ->
                        run {
                            val json = JSONObject(p.footprint)
                            val type = json.getString("type")

                            if (type == "Polygon") {
                                val productPolyDTO =
                                    gson.fromJson(p.footprint, PolygonDTO::class.java)
                                productPolyDTO.coordinates.forEach { it ->
                                    val points: List<Point> =
                                        it.map { Point(it[0], it[1], SpatialReference.wgs84()) }
                                    val polygon = Polygon(points)

                                    val graphic = Graphic(polygon, pinkOutlineSymbol)
                                    graphicsOverlay.graphics.add(graphic)
                                }
                            } else {
                                val productPolyDTO =
                                    gson.fromJson(p.footprint, MultiPolygonDto::class.java)
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
                                    val graphic = Graphic(polygon, pinkOutlineSymbol)
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
                Log.i("Error", "Error loading package: ${error.message}")
            }
        }
    }
}
