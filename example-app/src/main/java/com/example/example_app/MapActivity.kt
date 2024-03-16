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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.geometry.GeometryEngine
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
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


@RequiresApi(Build.VERSION_CODES.R)
class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private val loadedPolys: ArrayList<Polygon> = ArrayList();

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var service: GetMapService
    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        setApiKey()

        mapView = findViewById(R.id.mapView)
        val instance = MapServiceManager.getInstance()
        service = instance.service

        lifecycle.addObserver(mapView)

        val delivery = findViewById<Button>(R.id.deliver)
        var overlayView = findViewById<FrameLayout>(R.id.overlayView)
        delivery.visibility = View.INVISIBLE
        delivery.setOnClickListener {
            val blueBorderDrawableId = R.drawable.blue_border
            if (overlayView.background.constantState?.equals(
                    ContextCompat.getDrawable(
                        this,
                        blueBorderDrawableId
                    )?.constantState
                ) == true
            ) {
                this.onDelivery()
            } else {
                Toast.makeText(this, "התיחום שנבחר גדול מידי או מחוץ לתחום", Toast.LENGTH_SHORT)
                    .show()
            }
        }

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

        geoPackageRender()

        GlobalScope.launch(Dispatchers.Main) {
            mapView.onPan.collect {
                val displayMetrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                val height = displayMetrics.heightPixels
                val width = displayMetrics.widthPixels

                val leftTop = ScreenCoordinate(100.0, height - 550.0)
                val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
                val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
                val leftBottom = ScreenCoordinate(100.0, 550.0)

                val pLeftTop = mapView.screenToLocation(leftTop) ?: Point(0.0, 0.0)
                val pRightBottom = mapView.screenToLocation(rightBottom) ?: Point(0.0, 0.0)
                val pRightTop = mapView.screenToLocation(rightTop) ?: Point(0.0, 0.0)
                val pLeftBottom = mapView.screenToLocation(leftBottom) ?: Point(0.0, 0.0)

                val boxCoordinates = mutableListOf<Point>()
                boxCoordinates.add(pLeftTop)
                boxCoordinates.add(pRightTop)
                boxCoordinates.add(pRightBottom)
                boxCoordinates.add(pLeftBottom)
                boxCoordinates.add(pLeftTop)

                val boxPolygon = Polygon(boxCoordinates)

                val area = (calculateDistance(pLeftTop, pRightTop) / 1000) * (calculateDistance(
                    pLeftTop,
                    pLeftBottom
                ) / 1000)
                val showKm = findViewById<TextView>(R.id.kmShow)
                val showBm = findViewById<TextView>(R.id.showMb)
                val formattedNum = String.format("%.2f", area)
                val spaceMb = (formattedNum.toDouble() * 9).toInt()
                showKm.text = "שטח משוער :${formattedNum} קמ\"ר"
                showBm.text = "נפח משוער :${spaceMb} מ\"ב"
                val date = findViewById<TextView>(R.id.dateText)
                val maxMb = service.config.maxMapSizeInMB.toInt()
                var zoom = 0
                var downloadAble = false

                DiscoveryProductsManager.getInstance().products.forEach { p ->
                    run {
                        val json = JSONObject(p.footprint)
                        val type = json.getString("type")
                        val gson = Gson()

                        if (type == "Polygon") {
                            val productPolyDTO = gson.fromJson(p.footprint, PolygonDTO::class.java)
                            productPolyDTO.coordinates.forEach { coordinates ->
                                val points: List<Point> = coordinates.map {
                                    Point(it[0], it[1], SpatialReference.wgs84())
                                }
                                val polygon = Polygon(points)

                                val intersection =
                                    GeometryEngine.intersectionOrNull(polygon, boxPolygon)
                                val intersectionArea = GeometryEngine.area(intersection!!)
                                val boxArea = GeometryEngine.area(boxPolygon)
                                val date = findViewById<TextView>(R.id.dateText)
                                val firstOffsetDateTime = p.imagingTimeBeginUTC
                                val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                                val a = sdf.format(firstOffsetDateTime)
                                val secondOffsetDateTime = p.imagingTimeEndUTC
                                val b = sdf.format(secondOffsetDateTime)

                                val interPolygon = service.config.mapMinInclusionPct.toDouble()
                                if (abs(intersectionArea) / abs(boxArea) >= interPolygon/100) {
                                    downloadAble = true
                                    date.text = "צולם : ${b} - ${a}"
                                    zoom = p.maxResolutionDeg.toInt()
                                }
                            }
                        } else if (type == "MultiPolygon") {
                            val productMultiPolyDTO =
                                gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                            productMultiPolyDTO.coordinates.forEach { polyCoordinates ->
                                polyCoordinates.forEach { coordinates ->
                                    val points: List<Point> = coordinates.map {
                                        Point(
                                            it[0],
                                            it[1],
                                            SpatialReference.wgs84()
                                        )
                                    }
                                    val polygon = Polygon(points)

                                    val intersection = GeometryEngine.intersectionOrNull(polygon, boxPolygon)
                                    val intersectionArea = GeometryEngine.area(intersection!!)
                                    val boxArea = GeometryEngine.area(boxPolygon)

                                    val firstOffsetDateTime = p.imagingTimeBeginUTC
                                    val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                                    val a = sdf.format(firstOffsetDateTime)
                                    val secondOffsetDateTime = p.imagingTimeEndUTC
                                    val b = sdf.format(secondOffsetDateTime)

                                    val interPolygon = service.config.mapMinInclusionPct.toDouble()
                                    if (abs(intersectionArea) / abs(boxArea) >= interPolygon/100) {
                                        downloadAble = true
                                        date.text = "צולם : ${b} - ${a}"
                                        zoom = p.maxResolutionDeg.toInt()
                                    }
                                }
                            }
                        }
                    }
                }
                loadedPolys.forEach { p ->

                    val intersection = GeometryEngine.intersectionOrNull(p, boxPolygon)
                    if (intersection != null) {
                        val intersectionArea = GeometryEngine.area(intersection)
                        val boxArea = GeometryEngine.area(boxPolygon)
                        if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                            downloadAble = false
                        }
                    }
                }

                if (spaceMb < maxMb && downloadAble) {
                    overlayView.setBackgroundResource(R.drawable.blue_border)
                } else {
                    overlayView.setBackgroundResource(R.drawable.red_border)
                    date.text = "אין נתון"
//                    showKm.text = "שטח משוער :אין נתון"
                    showBm.text = "נפח משוער :אין נתון"
                }
            }
        }

    }


    private fun calculateDistance(point1: Point, point2: Point): Double {
        val lat1 = Math.toRadians(point1.y)
        val lon1 = Math.toRadians(point1.x)
        val lat2 = Math.toRadians(point2.y)
        val lon2 = Math.toRadians(point2.x)

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

        Log.i("selected points", "${calculateDistance(pLeftTop, pRightTop) / 1000}")
        Log.i("selected points", "${calculateDistance(pLeftTop, pLeftBottom) / 1000}")
        Log.i(
            "selected points",
            "${
                (calculateDistance(pLeftTop, pRightTop) / 1000) * (calculateDistance(
                    pLeftTop,
                    pLeftBottom
                ) / 1000)
            }"
        )


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

    private fun setApiKey() {
        val keyId = "runtimelite,1000,rud1971484999,none,GB2PMD17JYCJ5G7XE200"
        ArcGISEnvironment.apiKey = ApiKey.create(keyId)
    }


    private fun geoPackageRender() {
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList[1].directory?.absoluteFile ?: ""
        Log.i("gfgffgf", "$volume")
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
                    val pinkOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(240, 26, 133), 3f)
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
                            loadedPolys.add(polygon)
                            val endName = g.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
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
