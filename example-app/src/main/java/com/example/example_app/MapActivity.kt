package com.example.example_app

import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import gov.nasa.worldwind.BasicWorldWindowController
import gov.nasa.worldwind.WorldWind
import gov.nasa.worldwind.WorldWindow
import gov.nasa.worldwind.geom.LookAt
import gov.nasa.worldwind.geom.Offset
import gov.nasa.worldwind.geom.Position
import gov.nasa.worldwind.gesture.GestureRecognizer
import gov.nasa.worldwind.layer.BackgroundLayer
import gov.nasa.worldwind.layer.Layer
import gov.nasa.worldwind.layer.LayerFactory
import gov.nasa.worldwind.layer.RenderableLayer
import gov.nasa.worldwind.render.Color
import gov.nasa.worldwind.render.ImageSource
import gov.nasa.worldwind.shape.Label
import gov.nasa.worldwind.shape.Polygon
import gov.nasa.worldwind.shape.ShapeAttributes
import gov.nasa.worldwind.shape.TextAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
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
    lateinit var wwd: WorldWindow
    private val loadedPolys: ArrayList<kotlin.collections.ArrayList<Position>> = ArrayList()

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

        var overlayView = findViewById<FrameLayout>(R.id.overlayView)
        val delivery = findViewById<Button>(R.id.deliver)
        delivery.visibility = View.INVISIBLE
        delivery.setOnClickListener {
            val blueBorderDrawableId = R.drawable.blue_border
            if (overlayView.background.constantState?.equals(ContextCompat.getDrawable(this, blueBorderDrawableId)?.constantState) == true) {
                checkBboxBeforeSent()
                if (overlayView.background.constantState?.equals(ContextCompat.getDrawable(this, blueBorderDrawableId)?.constantState) == false) {
                    Toast.makeText(this, "התיחום שנבחר גדול מידי או מחוץ לתחום", Toast.LENGTH_SHORT).show()
                } else {
                    this.onDelivery()
                }
            } else {
                Toast.makeText(this, "התיחום שנבחר גדול מידי או מחוץ לתחום", Toast.LENGTH_SHORT).show()
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
        drawPolygons()
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

        val pLeftTop = wwd.pick(100f, height - 550f).terrainPickedObject().terrainPosition
        val pRightBottom = wwd.pick(width - 100f, 550f).terrainPickedObject().terrainPosition
        val pRightTop = wwd.pick(width - 100f, height - 550f).terrainPickedObject().terrainPosition
        val pLeftBottom = wwd.pick(100f, 550f).terrainPickedObject().terrainPosition

        GlobalScope.launch(Dispatchers.IO) {
            val props = MapProperties(
                "selectedProduct",
                "${pLeftTop.longitude},${pLeftTop.latitude},${pRightTop.longitude},${pRightTop.latitude},${pRightBottom.longitude},${pRightBottom.latitude},${pLeftBottom.longitude},${pLeftBottom.latitude},${pLeftTop.longitude},${pLeftTop.latitude}",
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

    private fun drawPolygons() {
        val attrs = ShapeAttributes()
        attrs.outlineWidth = 10f
        val r = 240 / 255.0f
        val g = 26 / 255.0f
        val b = 133 / 255.0f
        attrs.outlineColor = Color(r, g, b, 1f)
        attrs.interiorColor = Color(0f, 0f, 1f, 0f)
        attrs.isDepthTest = false

        val yellowAttrs = ShapeAttributes()
        yellowAttrs.outlineWidth = 5f
        yellowAttrs.outlineColor = Color(1f, 1f, 0f, 1f)
        yellowAttrs.interiorColor = Color(0f, 0f, 1f, 0f)
        yellowAttrs.isDepthTest = false
        yellowAttrs.setOutlineImageSource(ImageSource.fromLineStipple(2, 0xF0F0.toShort()))

        val redAttrs = ShapeAttributes()
        redAttrs.outlineWidth = 5f
        redAttrs.outlineColor = Color(1f, 0f, 0f, 1f)
        redAttrs.interiorColor = Color(0f, 0f, 1f, 0f)
        redAttrs.isDepthTest = false
        redAttrs.setOutlineImageSource(ImageSource.fromLineStipple(2, 0xF0F0.toShort()))

        val textAttrs = TextAttributes()
        textAttrs.textSize = 35.0F
        textAttrs.textColor = Color(1f, 1f, 0f, 1f)
//        textAttrs.outlineColor = Color(0f, 0f, 1f, 0f)
        textAttrs.outlineWidth = 0F
        textAttrs.textOffset = Offset(WorldWind.OFFSET_FRACTION, 0.5, WorldWind.OFFSET_FRACTION, 1.0)

        val gson = Gson()
        val renderableLayer = RenderableLayer()

        CoroutineScope(Dispatchers.IO).launch {
            service.getDownloadedMaps().forEach { g ->
                var endName = "בהורדה"
                if (g.statusMsg == "הסתיים") {
                    endName = g.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z"
                }
                val formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")

                if (g.statusMsg == "בהורדה" || g.statusMsg == "הסתיים" || g.statusMsg == "בקשה בהפקה" || g.statusMsg == "בקשה נשלחה") {
                    val nums = g.footprint?.split(",") ?: ArrayList()
                    val coords = ArrayList<Position>()
                    for (i in 0 until nums.size - 1 step 2) {
                        val lon = nums[i].toDouble()
                        val lat = nums[i + 1].toDouble()

                        coords.add(Position.fromDegrees(lat, lon, 0.0))
                    }
                    val polygon = Polygon(coords, yellowAttrs)
                    loadedPolys.add(coords)
                    renderableLayer.addRenderable(polygon)

                    val label = Label(
                        coords.first(),
                        endName,
                        textAttrs
                    )
                    label.displayName = ""
                    renderableLayer.addRenderable(label)
                } else {
                    val formattedDownloadStart = g.downloadStart?.format(formatter)
                    endName = "${g.statusMsg} $formattedDownloadStart"

                    val nums = g.footprint?.split(",") ?: ArrayList()
                    val coords = ArrayList<Position>()
                    for (i in 0 until nums.size - 1 step 2) {
                        val lon = nums[i].toDouble()
                        val lat = nums[i + 1].toDouble()

                        coords.add(Position.fromDegrees(lat, lon, 0.0))
                    }
                    val polygon = Polygon(coords, redAttrs)
                    loadedPolys.add(coords)
                    renderableLayer.addRenderable(polygon)

                    val label = Label(
                        coords.first(),
                        endName,
                        textAttrs
                    )
                    label.displayName = ""
                    renderableLayer.addRenderable(label)
                }
            }
        }

        DiscoveryProductsManager.getInstance().products.forEach { p ->
            run {
                val json = JSONObject(p.footprint)
                val type = json.getString("type")

                if (type == "Polygon") {
                    val productPolyDTO = gson.fromJson(p.footprint, PolygonDTO::class.java)
                    productPolyDTO.coordinates.forEach { it ->
                        val points: List<Position> = it.map {
                            Position.fromDegrees(it[1], it[0], 0.0)
                        }
                        val polygon = Polygon(points, attrs)

                        renderableLayer.addRenderable(polygon)
                    }
                } else {
                    val productPolyDTO = gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                    productPolyDTO.coordinates.forEach { polygonCoords ->
                        val rings: MutableList<List<Position>> = mutableListOf()
                        polygonCoords.forEach { ringCoords ->
                            val points: MutableList<Position> = ringCoords.map {
                                Position.fromDegrees(it[1], it[0], 0.0)
                            }.toMutableList()
                            rings.add(points)
                        }
                        val polygon = Polygon(rings.flatten(), attrs)

                        renderableLayer.addRenderable(polygon)
                    }
                }
            }
        }
        wwd.layers.addLayer(renderableLayer)
        wwd.requestRedraw()
    }

    private fun checkBboxBeforeSent() {
        try {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels

            //        val leftTop = ScreenCoordinate(100.0, height - 550.0)
//        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
//        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
//        val leftBottom = ScreenCoordinate(100.0, 550.0)

            val pLeftTop = wwd.pick(100f, height - 550f).terrainPickedObject().terrainPosition
            val pRightBottom = wwd.pick(width - 100f, 550f).terrainPickedObject().terrainPosition
            val pRightTop = wwd.pick(width - 100f, height - 550f).terrainPickedObject().terrainPosition
            val pLeftBottom = wwd.pick(100f, 550f).terrainPickedObject().terrainPosition

            val boxCoordinates = mutableListOf<Position>()
            boxCoordinates.add(pLeftTop)
            boxCoordinates.add(pRightTop)
            boxCoordinates.add(pRightBottom)
            boxCoordinates.add(pLeftBottom)
//                boxCoordinates.add(pLeftTop)

            val boxPolygon = Polygon(boxCoordinates)

            val boxCoordinatesEsri = mutableListOf<Point>()
            boxCoordinates.add(pLeftTop)
            boxCoordinates.add(pRightTop)
            boxCoordinates.add(pRightBottom)
            boxCoordinates.add(pLeftBottom)

            val polygonBoxEsri = com.arcgismaps.geometry.Polygon(boxCoordinatesEsri)


            val area = (calculateDistance(pLeftTop, pRightTop) / 1000) * (calculateDistance(pLeftTop, pLeftBottom) / 1000)
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

            val allPolygon = mutableListOf<PolyObject>()

            DiscoveryProductsManager.getInstance().products.forEach { p ->
                run {
                    val json = JSONObject(p.footprint)
                    val type = json.getString("type")
                    val gson = Gson()


                    if (type == "Polygon") {
                        val productPolyDTO = gson.fromJson(p.footprint, PolygonDTO::class.java)
                        productPolyDTO.coordinates.forEach { it ->
                            val points: List<Position> = it.map {
                                Position.fromDegrees(it[1], it[0], 0.0)
                            }
                            val polygon = Polygon(points)

//                          convert position to points in esri
                            val polygonPoints: List<Point> = it.map {
                                Point(it[0], it[1], SpatialReference.wgs84())
                            }
                            val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)

                            val intersection = intersectionOrNullNasa(points, boxCoordinates)
                            if (intersection != null) {
                                val newGeometry = GeometryEngine.intersectionOrNull(polygonEsri, polygonBoxEsri)
                                val intersectionArea =  GeometryEngine.area(newGeometry!!)
                                val boxArea = calculatePolygonArea(boxCoordinates)
                                val firstOffsetDateTime = p.imagingTimeBeginUTC
                                val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                                val firstDate = sdf.format(firstOffsetDateTime)
                                val secondOffsetDateTime = p.imagingTimeEndUTC
                                val secondDate = sdf.format(secondOffsetDateTime)
                                val interPolygon = service.config.mapMinInclusionPct.toDouble() / 100
//
                                if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                                    val polyObject = PolyObject(p.ingestionDate, abs(intersectionArea), firstDate, secondDate)
                                    allPolygon.add(polyObject)
                                }
                            }
                        }
                    } else if (type == "MultiPolygon") {
                        val productMultiPolyDTO = gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                        productMultiPolyDTO.coordinates.forEach { polyCoordinates ->
                            polyCoordinates.forEach { coordinates ->
                                val points: List<Position> = coordinates.map {
                                    Position.fromDegrees(it[1], it[0], 0.0)
                                }
                                val polygon = Polygon(points)

    //                          convert position to points in esri
                                val polygonPoints: List<Point> = coordinates.map {
                                    Point(it[0], it[1], SpatialReference.wgs84())
                                }
                                val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)

                                val intersection = intersectionOrNullNasa(points, boxCoordinates)
                                if (intersection != null) {
                                    val newGeometry = GeometryEngine.intersectionOrNull(polygonEsri, polygonBoxEsri)
                                    val intersectionArea =  GeometryEngine.area(newGeometry!!)
                                    val boxArea = calculatePolygonArea(boxCoordinates)
                                    val firstOffsetDateTime = p.imagingTimeBeginUTC
                                    val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                                    val firstDate = sdf.format(firstOffsetDateTime)
                                    val secondOffsetDateTime = p.imagingTimeEndUTC
                                    val secondDate = sdf.format(secondOffsetDateTime)
                                    val interPolygon = service.config.mapMinInclusionPct.toDouble() / 100
//
                                    if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                                        val polyObject = PolyObject(p.ingestionDate, abs(intersectionArea), firstDate, secondDate)
                                        allPolygon.add(polyObject)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val dateTextView = findViewById<TextView>(R.id.dateText)
            val interPolygon = service.config.mapMinInclusionPct.toDouble()
            allPolygon.sortByDescending(PolyObject::date)
            var found = false
            val boxArea = calculatePolygonArea(boxCoordinates)
            for (polygon in allPolygon) {
                if (polygon.intersection / abs(boxArea) >= interPolygon / 100) {
                    dateTextView.text = "צולם : ${polygon.start} - ${polygon.end}"
                    found = true
                    downloadAble = true
                    break
                }
            }
            if (!found && allPolygon.isNotEmpty()) {
                val firstPolyObject = allPolygon[0]
                dateTextView.text = "צולם : ${firstPolyObject.start} - ${firstPolyObject.end}"
                downloadAble = true
            }

            var inBbox = false
            loadedPolys.forEach { p ->

                val intersection = intersectionOrNullNasa(p, boxCoordinates)
                if (intersection != null) {
                    val intersectionArea = calculatePolygonArea(intersection)
                    val boxArea = calculatePolygonArea(boxCoordinates)
                    if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                        downloadAble = false
                        inBbox = true
                    }
                }
            }

            val overlayView = findViewById<FrameLayout>(R.id.overlayView)
            if (spaceMb < maxMb && downloadAble) {
                overlayView.setBackgroundResource(R.drawable.blue_border)
            } else {
                overlayView.setBackgroundResource(R.drawable.red_border)
                date.text = "אין תוצר עדכני באזור זה"
                if (inBbox){
                    date.text = "בחר תיחום שאינו חותך בול קיים"
                } else if (spaceMb > maxMb && downloadAble) {
                    date.text = "תיחום גדול מנפח מקסימלי להורדה"
                }
                showKm.text = "שטח משוער :אין נתון"
                showBm.text = "נפח משוער :אין נתון"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "bbox לא נבדק", Toast.LENGTH_SHORT).show()
        }
    }

    private fun intersectionOrNullNasa(polygon1: List<Position>, polygon2: List<Position>): List<Position>? {
        val p = polygon1.intersect(polygon2)
        val i: Polygon = Polygon(polygon1 + polygon2)
        Log.i("ScrollEvent", i.toString())
        for (point in polygon1) {
            if (pointInPolygon(point, polygon2)) {
                return polygon1 // Return the whole polygon1 as the intersection
            }
        }
        for (point in polygon2) {
            if (pointInPolygon(point, polygon1)) {
                return polygon2 // Return the whole polygon2 as the intersection
            }
        }
        return null
    }

    private fun pointInPolygon(point: Position, polygon: List<Position>): Boolean {
        val x = point.latitude
        val y = point.longitude
        var inside = false
        for (i in polygon.indices) {
            val xi = polygon[i].latitude
            val yi = polygon[i].longitude
            val j = if (i == 0) polygon.size - 1 else i - 1
            val xj = polygon[j].latitude
            val yj = polygon[j].longitude

            val intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
        }
        return inside
    }

    private fun calculatePolygonArea(vertices: List<Position>): Double {
        var area = 0.0
        val n = vertices.size

           for (i in 0 until n) {
            val j = (i + 1) % n
            val vi = vertices[i]
            val vj = vertices[j]
            area += (vi.latitude * vj.longitude - vj.latitude * vi.longitude)
        }

        area = Math.abs(area) / 2.0
        return area
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
            val consumed = pickGestureDetector.onTouchEvent(event)

            if (!consumed && event.action == MotionEvent.ACTION_UP) {
                Log.i("ScrollEvent", "Scroll detected: ${event.x}, ${event.y}")
                checkBboxBeforeSent()
            } else if (!consumed && event.action == MotionEvent.ACTION_MOVE) {
                val showKm = findViewById<TextView>(R.id.kmShow)
                val showBm = findViewById<TextView>(R.id.showMb)
                val date = findViewById<TextView>(R.id.dateText)
                showKm.text = "שטח משוער : מחשב שטח"
                showBm.text = "נפח משוער : מחשב נפח"
                date.text = "בשביל לסיים חישוב יש להרים את האצבע"
            }

            // If event was not consumed by the pick operation, pass it on the globe navigation handlers
            return if (!consumed) {
                super.onTouchEvent(event)
            } else consumed
        }
    }
}