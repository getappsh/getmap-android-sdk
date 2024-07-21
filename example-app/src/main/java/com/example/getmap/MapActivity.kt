package com.example.getmap

import MapDataMetaData
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
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.models.DiscoveryItem
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
import java.math.BigDecimal
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
    private val loadedPolys: ArrayList<ArrayList<Position>> = ArrayList()
    private val allPolygon = mutableListOf<PolyObject>()

    private val TAG = MainActivity::class.qualifiedName
    private lateinit var service: GetMapService

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

        val overlayView = findViewById<FrameLayout>(R.id.overlayView)
        val delivery = findViewById<Button>(R.id.deliver)
        val date = findViewById<TextView>(R.id.dateText)
        delivery.visibility = View.INVISIBLE
        delivery.setOnClickListener {
            val blueBorderDrawableId = R.drawable.blue_border
            if (overlayView.background.constantState?.equals(ContextCompat.getDrawable(this, blueBorderDrawableId)?.constantState) == true) {
                checkBboxBeforeSent()
                if (overlayView.background.constantState?.equals(ContextCompat.getDrawable(this, blueBorderDrawableId)?.constantState) == false) {
                    Toast.makeText(this, date.text, Toast.LENGTH_SHORT).show()
                } else {
                    this.onDelivery()
                }
            } else {
                Toast.makeText(this, date.text, Toast.LENGTH_SHORT).show()
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
        val volume = storageList.getOrNull(1)?.directory?.absoluteFile ?: ""
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

    private fun onDelivery() {
        Log.d(TAG, "onDelivery: ")

        val pLeftTop =  getFourScreenPoints(wwd).leftTop
        val pRightBottom = getFourScreenPoints(wwd).rightBottom
        val pRightTop = getFourScreenPoints(wwd).rightTop
        val pLeftBottom = getFourScreenPoints(wwd).leftBottom

        GlobalScope.launch(Dispatchers.IO) {
            val props = MapProperties(
                "selectedProduct",
                "${pLeftTop.longitude},${pLeftTop.latitude},${pRightTop.longitude},${pRightTop.latitude},${pRightBottom.longitude},${pRightBottom.latitude},${pLeftBottom.longitude},${pLeftBottom.latitude},${pLeftTop.longitude},${pLeftTop.latitude}",
                false
            )
            val id = service.downloadMap(props)
            Log.d(TAG, "onDelivery: after download map have been called, id: $id")
        }
        val intent = Intent(this@MapActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun drawPolygons() {
        val gson = Gson()
        val renderableLayer = RenderableLayer()

        CoroutineScope(Dispatchers.IO).launch {
            service.getDownloadedMaps().forEach { g ->
                var endName = "בהורדה"
                if (g.statusMsg == "הסתיים") {
                    val jsonText = Gson().fromJson(g.getJson().toString(), MapDataMetaData::class.java)
                    val region = jsonText.region[0]
                    endName = g.fileName!!.substringAfterLast('_').substringBefore('Z') + "Z" + " " + region
                }
                if (g.statusMsg == "בהורדה" || g.statusMsg == "בקשה בהפקה" || g.statusMsg == "בקשה נשלחה") {
                    endName = g.statusMsg!!

                }
                if (g.statusMsg == "בהורדה" || g.statusMsg == "הסתיים" || g.statusMsg == "בקשה בהפקה" || g.statusMsg == "בקשה נשלחה") {
                    val polygon = createDownloadedPolygon(g, "green", endName).first
                    renderableLayer.addRenderable(polygon)

                    val label = createDownloadedPolygon(g, "green", endName).second
                    renderableLayer.addRenderable(label)
                } else {
                    val formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                    val formattedDownloadStart = g.downloadStop?.format(formatter)
                    endName = "${g.statusMsg} $formattedDownloadStart"

                    val polygon = createDownloadedPolygon(g, "red", endName).first
                    renderableLayer.addRenderable(polygon)

                    val label = createDownloadedPolygon(g, "red", endName).second
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
                        val polygon = Polygon(points, attrsColor("pink"))
                        polygon.displayName = ""
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
                        val polygon = Polygon(rings.flatten(), attrsColor("pink"))
                        polygon.displayName = ""
                        renderableLayer.addRenderable(polygon)
                    }
                }
            }
        }
        renderableLayer.displayName = ""
        wwd.layers.addLayer(renderableLayer)
        wwd.requestRedraw()
    }

    private fun createDownloadedPolygon(map: MapData, colorType: String , endName: String): Pair<Polygon, Label> {
        val nums = map.footprint?.split(",") ?: ArrayList()
        val coords = ArrayList<Position>()
        for (i in 0 until nums.size - 1 step 2) {
            val lon = nums[i].toDouble()
            val lat = nums[i + 1].toDouble()

            coords.add(Position.fromDegrees(lat, lon, 0.0))
        }
        val polygon = Polygon(coords, attrsColor(colorType))
        loadedPolys.add(coords)
        polygon.displayName = ""

        val polygonPoints = coords.map { Point(it.longitude, it.latitude) }
        val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)
        val centroid = GeometryEngine.labelPointOrNull(polygonEsri)

        val labelPosition = centroid?.let {
            Position.fromDegrees(it.y, it.x, 0.0)
        } ?: coords.first()

        val label = Label(
            labelPosition,
            endName,
            textAttributes()
        )
        label.displayName = ""

        return Pair(polygon, label)
    }

    private fun attrsColor(type: String): ShapeAttributes {
        return when (type) {
            "green" -> attributes(type)
            "red" -> attributes(type)
            else -> pinkAttributes()
        }
    }

    private fun attributes(type: String): ShapeAttributes {
        return ShapeAttributes().apply {
            outlineWidth = 5f
            if (type == "green"){
                outlineColor = Color(0f, 1f, 0f, 1f)
            } else if (type == "red") {
                outlineColor = Color(1f, 0f, 0f, 1f)
            }
            interiorColor = Color(0f, 0f, 1f, 0f)
            isDepthTest = false
            outlineImageSource = ImageSource.fromLineStipple(2, 0xF0F0.toShort())
        }
    }

    private fun pinkAttributes(): ShapeAttributes {
        return ShapeAttributes().apply {
            outlineWidth = 10f
            outlineColor = Color(240 / 255.0f, 26 / 255.0f, 133 / 255.0f, 1f)
            interiorColor = Color(0f, 0f, 1f, 0f)
            isDepthTest = false
        }
    }

    private fun textAttributes(): TextAttributes {
        val textAttrs = TextAttributes()
        textAttrs.textSize = 35.0F
        textAttrs.textColor = Color(0f, 1f, 0f, 1f)
        textAttrs.outlineWidth = 0F
        textAttrs.textOffset = Offset(WorldWind.OFFSET_FRACTION, 0.5, WorldWind.OFFSET_FRACTION, -0.3)

        return textAttrs
    }

    private fun checkBboxBeforeSent() {
        try {
            val pLeftTop =  getFourScreenPoints(wwd).leftTop
            val pRightBottom = getFourScreenPoints(wwd).rightBottom
            val pRightTop = getFourScreenPoints(wwd).rightTop
            val pLeftBottom = getFourScreenPoints(wwd).leftBottom

            val boxCoordinates = mutableListOf<Position>()
            boxCoordinates.add(pLeftTop)
            boxCoordinates.add(pRightTop)
            boxCoordinates.add(pRightBottom)
            boxCoordinates.add(pLeftBottom)

            val boxCoordinatesEsri = mutableListOf<Point>()
            boxCoordinatesEsri.add(Point(pLeftTop.longitude, pLeftTop.latitude))
            boxCoordinatesEsri.add(Point(pRightTop.longitude, pRightTop.latitude))
            boxCoordinatesEsri.add(Point(pRightBottom.longitude, pRightBottom.latitude))
            boxCoordinatesEsri.add(Point(pLeftBottom.longitude, pLeftBottom.latitude))

            val polygonBoxEsri = com.arcgismaps.geometry.Polygon(boxCoordinatesEsri)

            val showKm = findViewById<TextView>(R.id.kmShow)
            val showBm = findViewById<TextView>(R.id.showMb)
            var spaceMb = 0
            val date = findViewById<TextView>(R.id.dateText)
            val maxMb = service.config.maxMapSizeInMB.toInt()
            var downloadAble = false
            val area = (calculateDistance(pLeftTop, pRightTop) / 1000) * (calculateDistance(pLeftTop, pLeftBottom) / 1000)
            val formattedNum = String.format("%.2f", area)
            showKm.text = "שטח משוער :${formattedNum} קמ\"ר"

            allPolygon.clear()

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
                            detectPolygon(p, points, polygonBoxEsri, boxCoordinates)
                        }
                    } else if (type == "MultiPolygon") {
                        val productMultiPolyDTO = gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                        productMultiPolyDTO.coordinates.forEach { polyCoordinates ->
                            polyCoordinates.forEach { coordinates ->
                                val points: List<Position> = coordinates.map {
                                    Position.fromDegrees(it[1], it[0], 0.0)
                                }
                                detectPolygon(p, points, polygonBoxEsri, boxCoordinates)
                            }
                        }
                    }
                }
            }
            val interPolygon = service.config.mapMinInclusionPct.toDouble()
            var checkBetweenPolygon = true
            allPolygon.sortByDescending(PolyObject::date)
            var found = false
            val boxArea = calculatePolygonArea(boxCoordinates)
            for (polygon in allPolygon) {
                if (polygon.intersection / abs(boxArea) >= interPolygon / 100) {
                    val km = String.format("%.2f", abs(polygon.intersection * 10000))
                    spaceMb = calculateMB(km, polygon.resolution)
                    showKm.text = "שטח משוער :${km} קמ\"ר"
                    showBm.text = "נפח משוער :${spaceMb} מ\"ב"
                    date.text = "צולם : ${polygon.end} - ${polygon.start}"
                    found = true
                    downloadAble = true
                    checkBetweenPolygon = false
                    break
                }
            }
            if (checkBetweenPolygon && allPolygon.isNotEmpty()) {
                val unionGeometry = unionIntersections(allPolygon)
                val allPolygonArea = GeometryEngine.area(unionGeometry)

                if (allPolygon.size > 1) {
                    for (polygon in allPolygon) {
                        if (polygon.intersection / allPolygonArea >= interPolygon / 100) {
                            val km = String.format("%.2f", abs(polygon.intersection * 10000))
                            spaceMb = calculateMB(km, polygon.resolution)
                            showKm.text = "שטח משוער :${km} קמ\"ר"
                            showBm.text = "נפח משוער :${spaceMb} מ\"ב"
                            date.text = "צולם : ${polygon.end} - ${polygon.start}"
                            found = true
                            downloadAble = true
                            break
                        }
                    }
                }
            }
            if (!found && allPolygon.isNotEmpty()) {
                val firstPolyObject = allPolygon[0]
                val km = String.format("%.2f", abs(firstPolyObject.intersection * 10000))
                spaceMb = calculateMB(km, firstPolyObject.resolution)
                showKm.text = "שטח משוער :${km} קמ\"ר"
                showBm.text = "נפח משוער :${spaceMb} מ\"ב"
                date.text = "צולם : ${firstPolyObject.end} - ${firstPolyObject.start}"
                downloadAble = true
            }

            var inBbox = false
            loadedPolys.forEach { p ->

                val polygonPoints = p.map { Point(it.longitude, it.latitude) }
                val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)
                val intersection = GeometryEngine.intersectionOrNull(polygonEsri, polygonBoxEsri)
                if (intersection != null) {
                    val intersectionArea =  GeometryEngine.area(intersection)
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
                if (inBbox){
                    date.text = "בחר תיחום שאינו חותך בול קיים"
                } else if (spaceMb > maxMb && downloadAble) {
                    date.text = "תיחום גדול מנפח מקסימלי להורדה"
                } else {
                    date.text = "אין תוצר עדכני באזור זה"
                    showKm.text = "שטח משוער :אין נתון"
                    showBm.text = "נפח משוער :אין נתון"
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unionIntersections(polygons: MutableList<PolyObject>): Geometry {
        var unionPolygon = polygons[0].geometry
        for (i in 1 until polygons.size) {
            unionPolygon = GeometryEngine.union(unionPolygon, polygons[i].geometry)
        }
        return unionPolygon
    }

    private fun calculateMB(formattedNum : String,  resolution: BigDecimal): Int {
        var mb = 0
        mb = if (resolution.toDouble() == 1.34110450744629E-6 || resolution.toDouble() == 1.3411E-6) {
            (formattedNum.toDouble() * 10).toInt()
        } else if (resolution.toDouble() == 2.68220901489258E-6) {
            (formattedNum.toDouble() * 5).toInt()
        } else {
            (formattedNum.toDouble() * 2.5).toInt()
        }

        return mb
    }

    private fun detectPolygon(map: DiscoveryItem, points:  List<Position>, polygonBoxEsri: com.arcgismaps.geometry.Polygon, boxCoordinates: MutableList<Position>) {
        val polygonPoints = points.map { Point(it.longitude, it.latitude) }

        val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)

        val intersection = GeometryEngine.intersectionOrNull(polygonEsri, polygonBoxEsri)
        if (intersection != null) {
            val intersectionArea =  GeometryEngine.area(intersection)
            val boxArea = calculatePolygonArea(boxCoordinates)
            val firstOffsetDateTime = map.imagingTimeBeginUTC
            val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val firstDate = sdf.format(firstOffsetDateTime)
            val secondOffsetDateTime = map.imagingTimeEndUTC
            val secondDate = sdf.format(secondOffsetDateTime)

            if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                val polyObject = PolyObject(map.ingestionDate, abs(intersectionArea), firstDate, secondDate, map.maxResolutionDeg, intersection)
                allPolygon.add(polyObject)
            }
        }
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

    private fun intersectionOrNullNasa(polygon1: List<Position>, polygon2: List<Position>): List<Position>? {
        for (point in polygon1) {
            if (pointInPolygon(point, polygon2)) {
                return polygon1
            }
        }
        for (point in polygon2) {
            if (pointInPolygon(point, polygon1)) {
                return polygon2
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

            val intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
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

        area = abs(area) / 2.0
        return area
    }

    private fun getFourScreenPoints(wwd: WorldWindow): FourScreenPoints {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        val pLeftTop = wwd.pick(100f, height - 550f).terrainPickedObject().terrainPosition
        val pRightBottom = wwd.pick(width - 100f, 550f).terrainPickedObject().terrainPosition
        val pRightTop = wwd.pick(width - 100f, height - 550f).terrainPickedObject().terrainPosition
        val pLeftBottom = wwd.pick(100f, 550f).terrainPickedObject().terrainPosition

        return FourScreenPoints(pLeftTop, pRightBottom, pRightTop, pLeftBottom)
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
                date.text = ""
            }

            return if (!consumed) {
                super.onTouchEvent(event)
            } else consumed
        }
    }
}