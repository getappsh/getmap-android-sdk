package com.example.example_app

import android.annotation.SuppressLint
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
    private val TAG = MapActivity::class.qualifiedName

    private lateinit var mapView: MapView
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

    private var isMapScrolling = false
    private var selectMode = false
    private val loadedPolys: ArrayList<Polygon> = ArrayList();

    private lateinit var service: GetMapService
    private val downloadStatusHandler: (MapData) -> Unit = { data ->
        Log.d("DownloadStatusHandler", "${data.id} status is: ${data.deliveryState.name}")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        setApiKey()

        mapView = findViewById(R.id.mapView)
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

        lifecycle.addObserver(mapView)

        deliveryButton = findViewById(R.id.deliver)
        closeButton = findViewById(R.id.close)
        backButton = findViewById(R.id.back)

        deliveryButton.visibility = View.INVISIBLE
        deliveryButton.setOnClickListener {
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

        geoPackageRender()

        GlobalScope.launch(Dispatchers.Main) {
            processMapPan()
        }
    }

    private suspend fun processMapPan(){
        mapView.onPan.collect {
            if (!selectMode) {
                return@collect
            }
            val polygonPoints = createPolygonPoints(mapView)

            val area = calculateArea(polygonPoints)

            val boxPolygon = Polygon(polygonPoints)
            val allPolygons = extractPolygonsFromProducts(boxPolygon)

            val polyProduct = findNewestPolygonWithIntersection(allPolygons, boxPolygon)
            val inBBox = isInBBox(boxPolygon);

            updateUIWithIntersectionInfo(inBBox, polyProduct, area)

        }
    }
    private fun createPolygonPoints(mapView: MapView): List<Point> {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        val leftTop = ScreenCoordinate(100.0, height - 550.0)
        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
        val leftBottom = ScreenCoordinate(100.0, 550.0)

        val screenToLocation = mapView::screenToLocation
        return listOf(
            screenToLocation(leftTop) ?: Point(1.0, 0.0),
            screenToLocation(rightTop) ?: Point(0.5, 0.0),
            screenToLocation(rightBottom) ?: Point(0.0, 1.0),
            screenToLocation(leftBottom) ?: Point(0.0, 0.5)
        )
    }

    private fun calculateArea(polygonPoints: List<Point>): Double {
        val pLeftTop = polygonPoints[0]
        val pRightTop = polygonPoints[1]
        val width = calculateDistance(pLeftTop, pRightTop) / 1000
        val height = calculateDistance(pLeftTop, polygonPoints[3]) / 1000
        return width * height
    }
    private fun extractPolygonsFromProducts(boxPolygon: Polygon): MutableList<PolyObject> {
        val allPolygons = mutableListOf<PolyObject>()

        val gson = Gson()

        DiscoveryProductsManager.getInstance().products.forEach { p ->
            run {
                val json = JSONObject(p.footprint)
                val type = json.getString("type")

                if (type == "Polygon") {
                    val productPolyDTO = gson.fromJson(p.footprint, PolygonDTO::class.java)
                    productPolyDTO.coordinates.forEach { coordinates ->
                        val points: List<Point> = coordinates.map {
                            Point(it[0], it[1], SpatialReference.wgs84())
                        }
                        val polygon = Polygon(points)

                        val intersection = GeometryEngine.intersectionOrNull(polygon, boxPolygon)
                        val intersectionArea = GeometryEngine.area(intersection!!)
                        val boxArea = GeometryEngine.area(boxPolygon)
                        val firstOffsetDateTime = p.imagingTimeBeginUTC
                        val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                        val firstDate = sdf.format(firstOffsetDateTime)
                        val secondOffsetDateTime = p.imagingTimeEndUTC
                        val secondDate = sdf.format(secondOffsetDateTime)
                        val interPolygon = service.config.mapMinInclusionPct.toDouble() / 100

                        if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                            val polyObject = PolyObject(p.ingestionDate, abs(intersectionArea), firstDate, secondDate)
                            allPolygons.add(polyObject)
                        }
                    }
                } else if (type == "MultiPolygon") {
                    val productMultiPolyDTO = gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                    productMultiPolyDTO.coordinates.forEach { polyCoordinates ->
                        polyCoordinates.forEach { coordinates ->
                            val points: List<Point> = coordinates.map {
                                Point(it[0], it[1], SpatialReference.wgs84())
                            }
                            val polygon = Polygon(points)

                            val intersection = GeometryEngine.intersectionOrNull(polygon, boxPolygon)
                            val intersectionArea = GeometryEngine.area(intersection!!)
                            val boxArea = GeometryEngine.area(boxPolygon)

                            val firstOffsetDateTime = p.imagingTimeBeginUTC
                            val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                            val firstDate = sdf.format(firstOffsetDateTime)
                            val secondOffsetDateTime = p.imagingTimeEndUTC
                            val secondDate = sdf.format(secondOffsetDateTime)

                            if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                                val polyObject = PolyObject(p.ingestionDate, abs(intersectionArea), firstDate, secondDate)
                                allPolygons.add(polyObject)
                            }
                        }
                    }
                }
            }
        }
        return allPolygons
    }

    private fun findNewestPolygonWithIntersection(allPolygons: MutableList<PolyObject>, boxPolygon: Polygon): PolyObject? {
        allPolygons.sortByDescending(PolyObject::date)
        val interPolygon = service.config.mapMinInclusionPct.toDouble() / 100

        val boxArea = GeometryEngine.area(boxPolygon)
        val polyProduct = allPolygons.find { poly ->  (poly.intersection / abs(boxArea) >= interPolygon)} ?: allPolygons.getOrNull(0)

        return polyProduct
    }

    private fun isInBBox(boxPolygon: Polygon): Boolean{
        return loadedPolys.any { p ->
            val intersection = GeometryEngine.intersectionOrNull(p, boxPolygon)
            if (intersection != null) {
                val intersectionArea = GeometryEngine.area(intersection)
                val boxArea = GeometryEngine.area(boxPolygon)
                (abs(intersectionArea) / abs(boxArea) > 0.0)
            }else
                false
        }
    }

    private fun updateUIWithIntersectionInfo(inBBox: Boolean, polyProduct: PolyObject?, area: Double) {
        val formattedNum = String.format("%.2f", area)
        val spaceMb = (formattedNum.toDouble() * 9).toInt()
        val maxMb = service.config.maxMapSizeInMB.toInt()

        val downloadAble = !inBBox && polyProduct != null

        if (spaceMb < maxMb && downloadAble) {
            overlayView.setBackgroundResource(R.drawable.blue_border)
            polyProduct?.let {
                dateTextView.text = "צולם : ${it.start} - ${it.end}"
            }
            showKm.text = "שטח משוער : $formattedNum קמ\"ר"
            showBm.text = "נפח משוער : $spaceMb מ\"ב"
        } else {
            overlayView.setBackgroundResource(R.drawable.red_border)
            if (inBBox){
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
        val geoPackage = GeoPackage(getBaseMapLocation())
        lifecycleScope.launch {
            loadGeoPackageRasters(geoPackage)?.let { basemap ->
                val graphicsOverlay = createGraphicsOverlay()

                mapView.graphicsOverlays.add(graphicsOverlay)
                mapView.map = ArcGISMap(basemap)
                mapView.setViewpoint(Viewpoint(31.7270, 34.6, 2000000.0))
            }
        }
    }

    private fun getBaseMapLocation(): String{
        val storageManager: StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes
        val volume = storageList.getOrNull(1)?.directory?.absoluteFile ?: ""
        Log.i(TAG, "$volume")

        return "${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg"
    }

    private suspend fun loadGeoPackageRasters(geoPackage: GeoPackage): Basemap? {
        return geoPackage.load().fold(
            onSuccess = {
                if (geoPackage.geoPackageRasters.isNotEmpty()){
                    val geoPackageRaster = geoPackage.geoPackageRasters.first()
                    RasterLayer(geoPackageRaster).let { rasterLayer ->
                        Basemap(rasterLayer)
                    }
                }else{
                    Log.e(TAG, "Error: No feature tables found in the GeoPackage.")
                    null
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to load GeoPackage: ${error.message}")
                null
            }
        )

    }

    private fun createGraphicsOverlay(): GraphicsOverlay {
        val graphicsOverlay = GraphicsOverlay()
        val yellowOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.fromRgba(255, 255, 0), 3f)
        val pinkOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Solid, Color.fromRgba(240, 26, 133), 3f)
        val redOutlineSymbol = SimpleLineSymbol(SimpleLineSymbolStyle.Dash, Color.fromRgba(255, 0, 0), 3f)

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
                var endName = "בהורדה"
                if (g.statusMsg == "הסתיים") {
                    endName = g.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                }
                val textSymbol = TextSymbol(
                    endName,
                    Color.fromRgba(255, 255, 0),
                    14f,
                    HorizontalAlignment.Center,
                    VerticalAlignment.Top
                )

                val formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                val compositeSymbol = CompositeSymbol().apply {
                    if (g.statusMsg == "בהורדה" || g.statusMsg == "הסתיים" || g.statusMsg == "בקשה בהפקה" || g.statusMsg == "בקשה נשלחה") {
                        symbols.add(yellowOutlineSymbol)
                        symbols.add(textSymbol)
                    } else {
                        symbols.add(redOutlineSymbol)
                        val formattedDownloadStart = g.downloadStart?.format(formatter)
                        textSymbol.text = "${g.statusMsg} $formattedDownloadStart"
                        symbols.add(textSymbol)
                    }
                }

                val graphic = Graphic(polygon, compositeSymbol)

                graphicsOverlay.graphics.add(graphic)
            }

        }

        val gson = Gson()
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

        return graphicsOverlay
    }
}
