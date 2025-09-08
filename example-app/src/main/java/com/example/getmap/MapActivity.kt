package com.example.getmap

import MapDataMetaData
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryEngine
import com.arcgismaps.geometry.Point
import com.example.getmap.matomo.MatomoTracker
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.exceptions.MissingIMEIException
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import com.ngsoft.getapp.sdk.models.MapData
import com.ngsoft.getapp.sdk.models.MapProperties
import gov.nasa.worldwind.BasicWorldWindowController
import gov.nasa.worldwind.Navigator
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
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import timber.log.Timber
import java.io.File
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
    private var geoPackageName = ""
    private var orthophotoPackageName = ""
    private var controlPackageName = ""
    private var dMode = false
    private var sharedPreferences: SharedPreferences? = null
    private var sharedPreferencesEditor: SharedPreferences.Editor? = null
    private lateinit var controlSwitch: Switch

    private val showBm: TextView by lazy { findViewById(R.id.showMb) }
    private val showKm: TextView by lazy { findViewById(R.id.kmShow) }
    private val date: TextView by lazy { findViewById(R.id.dateText) }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        val tracker: Tracker?
        tracker = MatomoTracker.getTracker(this)

        sharedPreferences = baseContext.getSharedPreferences("navigator", Context.MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences?.edit()
        val instance = MapServiceManager.getInstance()
        service = instance.service

        orthophotoPackageName = findLargestGpkgByKeyword(service.config.ortophotoMapPath.toString(),service.config.ortophotoMapPattern.toString())
        controlPackageName =findLargestGpkgByKeyword(service.config.controlMapPath.toString(),service.config.controlMapPattern.toString())

        wwd = WorldWindow(this)
        wwd.worldWindowController = PickNavigateController(this)
        wwd.layers.addLayer(BackgroundLayer())
        geoPackageName = orthophotoPackageName
        addGeoPkg()

        controlSwitch = findViewById<Switch>(R.id.control)
        val controlText = findViewById<TextView>(R.id.controlText)
        val lastCompass = sharedPreferences?.getString("last_compass", "0.0F")
        val lastNavigator = sharedPreferences?.getString("last_navigator", "no data")
        val lastControlMap = sharedPreferences?.getString("last_control_map", "no data")
        val lastLookAt = sharedPreferences?.getString("LookAt", "no data")
        if ((lastNavigator != null && lastNavigator != "no data") && (lastLookAt != null && lastLookAt != "no data")
            && (lastControlMap != null && lastControlMap != "no data")
        ) {
            val gson = Gson()
            val newControlMap = gson.fromJson(lastControlMap, Boolean::class.java)
            val newCompass = gson.fromJson(lastCompass, Float::class.java)
            val newNavigator = gson.fromJson(lastNavigator, Navigator::class.java)
            val lastLookAtObj = gson.fromJson(lastLookAt, LookAt::class.java)
            val compass = findViewById<View>(R.id.arrow)
            controlSwitch.isChecked = newControlMap
            if (controlSwitch.isChecked) {
                geoPackageName = controlPackageName
                addGeoPkg()
            }
            compass.rotation = newCompass
            newNavigator.setAsLookAt(wwd.globe, lastLookAtObj)
            wwd.navigator = newNavigator
            if (newCompass == 0F){
                wwd.navigator.heading = 0.0
            }
            wwd.postDelayed({
                simulateTouch(wwd.x, wwd.y)
            }, 50)
        } else {
            val lookAt = LookAt().set(
                31.75,
                34.85,
                0.0,
                WorldWind.ABSOLUTE,
                300000.0,
                0.0,
                0.0,
                0.0
            )
            wwd.navigator.setAsLookAt(wwd.globe, lookAt)
        }


        val globeLayout = findViewById<View>(R.id.mapView) as FrameLayout
        globeLayout.addView(wwd)

        val compass = findViewById<View>(R.id.arrow)
        compass.setOnClickListener {
            showNorth()
        }

        val overlayView = findViewById<FrameLayout>(R.id.overlayView)
        val delivery = findViewById<Button>(R.id.deliver)
        delivery.visibility = View.INVISIBLE
        delivery.setOnClickListener {
            if (!dMode) {
                val pLeftTop = getFourScreenPoints(wwd).leftTop
                val pRightBottom = getFourScreenPoints(wwd).rightBottom
                val pRightTop = getFourScreenPoints(wwd).rightTop
                val pLeftBottom = getFourScreenPoints(wwd).leftBottom
                val latlonpLeftTop =
                    pLeftTop.latitude.toString() + " " + pLeftTop.longitude.toString()
                val latlonpLeftBottom =
                    pLeftBottom.latitude.toString() + " " + pLeftBottom.longitude.toString()
                val latlonpRightTop =
                    pRightTop.latitude.toString() + " " + pRightTop.longitude.toString()
                val latlonpRightBottom =
                    pRightBottom.latitude.toString() + " " + pRightBottom.longitude.toString()
                val generalLatLon =
                    "$latlonpLeftTop $latlonpRightTop $latlonpRightBottom $latlonpLeftBottom"
                TrackHelper.track()
                    .dimension(service.config.matomoDimensionId.toInt(), generalLatLon)
                    .event("מיפוי ענן", "ניהול בקשות").name("בקשה להורדת בול")
                    .with(tracker)
                saveLastPosition(true)
                val blueBorderDrawableId = R.drawable.blue_border
                if (overlayView.background.constantState?.equals(
                        ContextCompat.getDrawable(
                            this,
                            blueBorderDrawableId
                        )?.constantState
                    ) == true
                ) {
                    checkBboxBeforeSent()
                    if (overlayView.background.constantState?.equals(
                            ContextCompat.getDrawable(
                                this,
                                blueBorderDrawableId
                            )?.constantState
                        ) == false
                    ) {
                        Toast.makeText(this, date.text, Toast.LENGTH_SHORT).show()
                    } else {
                        this.onDelivery()
                    }
                } else {
                    Toast.makeText(this, date.text, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(applicationContext, "יש ליישר את המפה בחזרה", Toast.LENGTH_LONG)
                    .show()
            }
        }

        val close = findViewById<Button>(R.id.close)
        close.visibility = View.INVISIBLE
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
        overlayView.visibility = View.VISIBLE

        close.setOnClickListener {
            saveLastPosition(false)
            val intent = Intent(this@MapActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val mapSwitch = findViewById<View>(R.id.mapSwitch)
        mapSwitch.visibility = View.GONE

        controlText.setOnClickListener {
            controlSwitch.isChecked = !controlSwitch.isChecked
        }
        controlSwitch.setOnCheckedChangeListener { _, isChecked ->
            controlSwitch(isChecked, tracker)
        }

        drawPolygons()
    }

    private fun findLargestGpkgByKeyword(dirPath: String, keyword: String): String {
        val sm = getSystemService(STORAGE_SERVICE) as StorageManager
        val storage = sm.storageVolumes.getOrNull(1) ?: sm.storageVolumes.getOrNull(0)
        val volumeDir = storage?.directory?.absoluteFile ?: return ""

        val base = File(volumeDir, dirPath).absoluteFile
        val searchDir = when {
            base.isDirectory -> base
            base.isFile      -> base.parentFile ?: volumeDir
            else             -> volumeDir
        }

        val files = searchDir.listFiles() ?: return ""
        val candidates = files.filter { f ->
            f.isFile &&
                    f.extension.equals("gpkg", ignoreCase = true) &&
                    !f.name.contains("-journal", ignoreCase = true) &&
                    !f.name.endsWith("-wal", ignoreCase = true) &&
                    f.name.contains(keyword, ignoreCase = true)
        }
        val largest = candidates.maxByOrNull { it.length() } ?: return ""
        return largest.absolutePath
    }

    private fun controlSwitch(isChecked: Boolean, tracker: Tracker) {
        if (isChecked) {
            TrackHelper.track().event("מיפוי ענן", "שינוי הגדרות")
                .name("הצגת מפת שליטה")
                .with(tracker)
            geoPackageName = controlPackageName
            addGeoPkg()
        } else {
            geoPackageName = orthophotoPackageName
            addGeoPkg()
            TrackHelper.track().event("מיפוי ענן", "שינוי הגדרות")
                .name("הסתרת מפת שליטה")
                .with(tracker)

            // Optionally remove the BlueMarble layer or handle switch off action
            val blueMarbleLayer = wwd.layers.indexOfLayerNamed("BlueMarble")
            if (blueMarbleLayer == -1) {
                return
            } else {
                wwd.layers.removeLayer(wwd.layers.indexOfLayerNamed("BlueMarble"))
                wwd.requestRedraw()
            }
        }
    }

    private fun addGeoPkg() {
        val geoPath = geoPackageName

        val layerFactory = LayerFactory()
        layerFactory.createFromGeoPackage(
            geoPath,
            object : LayerFactory.Callback {
                override fun creationSucceeded(factory: LayerFactory?, layer: Layer?) {
                    if (controlSwitch.isChecked) {
                        layer!!.displayName = "BlueMarble"
                    }
                    wwd.layers.addLayer(layer)
                    Timber.tag("gov.nasa.worldwind").i("GeoPackage layer creation succeeded")
                }

                override fun creationFailed(factory: LayerFactory?, layer: Layer?, ex: Throwable?) {
                    Timber.tag("gov.nasa.worldwind").e("GeoPackage layer creation failed")
                    Toast.makeText(applicationContext, "בעיה בטעינת המפה", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun onDelivery() {
        Timber.d("onDelivery: ")

        val pLeftTop = getFourScreenPoints(wwd).leftTop
        val pRightBottom = getFourScreenPoints(wwd).rightBottom
        val pRightTop = getFourScreenPoints(wwd).rightTop
        val pLeftBottom = getFourScreenPoints(wwd).leftBottom

        GlobalScope.launch(Dispatchers.IO) {
            val props = MapProperties(
                "selectedProduct",
                "${pLeftTop.longitude},${pLeftTop.latitude},${pRightTop.longitude},${pRightTop.latitude},${pRightBottom.longitude},${pRightBottom.latitude},${pLeftBottom.longitude},${pLeftBottom.latitude},${pLeftTop.longitude},${pLeftTop.latitude}",
                false
            )
            try {

                val id = service.downloadMap(props)
                Timber.d("onDelivery: after download map have been called")
            } catch (e: MissingIMEIException) {
//                    TODO show missing imei dialog
            }
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
                    val jsonText =
                        Gson().fromJson(g.getJson().toString(), MapDataMetaData::class.java)
                    val region = jsonText.region[0]
                    endName = g.fileName!!.substringAfterLast('_')
                        .substringBefore('Z') + "Z" + " " + region
                }
                if (g.statusMsg == "בהורדה" || g.statusMsg == "בקשה בהפקה" || g.statusMsg == "בקשה נשלחה") {
                    endName = g.statusMsg!!
                }
                if (g.statusMsg == "בהורדה" || g.statusMsg == "הסתיים" || g.statusMsg == "בקשה בהפקה" || g.statusMsg == "בקשה נשלחה") {
                    val polygon = createDownloadedPolygon(g, "green", endName).first
                    renderableLayer.addRenderable(polygon)

                    val label = createDownloadedPolygon(g, "green", endName).second
                    renderableLayer.addRenderable(label)
                } else if (g.statusMsg == "בוטל") {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val formattedDownloadStart = g.downloadStop?.format(formatter)
                    endName = "השהייה: $formattedDownloadStart"
                    val polygon = createDownloadedPolygon(g, "red", endName).first
                    renderableLayer.addRenderable(polygon)

                    val label = createDownloadedPolygon(g, "red", endName).second
                    renderableLayer.addRenderable(label)

                } else {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
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

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.popup_menu, popup.menu)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.map_option1 -> {
                    // Handle option 1 click
                    wwd.layers.removeLayer(wwd.layers.indexOfLayerNamed("gpkg"))
                    wwd.requestRedraw()
                    geoPackageName = service.config.ortophotoMapPath.toString()
                    addGeoPkg()
                    true
                }

                R.id.map_option2 -> {
                    // Handle option 2 click
                    wwd.layers.removeLayer(wwd.layers.indexOfLayerNamed("gpkg"))
                    wwd.requestRedraw()
                    geoPackageName = service.config.controlMapPath.toString()
                    addGeoPkg()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun createDownloadedPolygon(
        map: MapData,
        colorType: String,
        endName: String,
    ): Pair<Polygon, Label> {
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
            if (type == "green") {
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
        textAttrs.textOffset =
            Offset(WorldWind.OFFSET_FRACTION, 0.5, WorldWind.OFFSET_FRACTION, -0.3)

        return textAttrs
    }

    private fun Position.toPoint(): Point {
        return Point(this.longitude, this.latitude)
    }

    private fun processPolygon(
        p: DiscoveryItem,
        polygon: List<List<List<Double>>>,
        polygonBoxEsri: com.arcgismaps.geometry.Polygon,
        boxCoordinates: MutableList<Position>
    ) {
        polygon.forEach { poly ->
            val points: List<Position> = poly.map {
                Position.fromDegrees(it[1], it[0], 0.0)
            }
            detectPolygon(p, points, polygonBoxEsri, boxCoordinates)
        }
    }

    private fun checkBboxBeforeSent() {
        try {
            dMode = false
            val fourPoints = getFourScreenPoints(wwd)
            val pLeftTop = fourPoints.leftTop
            val pRightBottom = fourPoints.rightBottom
            val pRightTop = fourPoints.rightTop
            val pLeftBottom = fourPoints.leftBottom

            Timber.i("⏱️ זמן חישוב נקודות מסך: ${System.currentTimeMillis() - t1}ms")
            Timber.i("⏱️ נקוודות פוליגון: $pLeftTop , $pLeftBottom, $pRightTop, $pRightBottom")


            val t2 = System.currentTimeMillis()

            val boxCoordinates = mutableListOf(pLeftTop, pRightTop, pRightBottom, pLeftBottom)

            val boxCoordinatesEsri = mutableListOf(
                pLeftTop.toPoint(),
                pRightTop.toPoint(),
                pRightBottom.toPoint(),
                pLeftBottom.toPoint()
            )

            val polygonBoxEsri = com.arcgismaps.geometry.Polygon(boxCoordinatesEsri)

            val area = (calculateDistance(pLeftTop, pRightTop) / 1000) * (calculateDistance(pLeftTop, pLeftBottom) / 1000)
            showKm.text = getString(R.string.calculate_area_with_value_text, area)

            allPolygon.clear()

            DiscoveryProductsManager.getInstance().products.forEach { p ->
                run {
                    val json = JSONObject(p.footprint)
                    val type = json.getString("type")

                    when (type) {
                        "Polygon" -> {
                            val productPolyDTO = Gson().fromJson(p.footprint, PolygonDTO::class.java)
                            processPolygon(p, productPolyDTO.coordinates, polygonBoxEsri, boxCoordinates)
                        }
                        "MultiPolygon" -> {
                            val productMultiPolyDTO = Gson().fromJson(p.footprint, MultiPolygonDto::class.java)
                            productMultiPolyDTO.coordinates.forEach { multiPoly ->
                                processPolygon(p, multiPoly, polygonBoxEsri, boxCoordinates)
                            }
                        }
                    }
                }
            }
            val maxMb = service.config.maxMapSizeInMB.toInt()
            val interPolygon = service.config.mapMinInclusionPct.toDouble()
            val maxArea = service.config.maxMapAreaSqKm.toInt()
            val boxArea = calculatePolygonArea(boxCoordinates)
            var spaceMb = 0
            var downloadAble = false
            var checkBetweenPolygon = true
            var isAreaValid = true
            var found = false

            allPolygon.sortByDescending(PolyObject::date)

            for (polygon in allPolygon) {
                if (polygon.intersection / abs(boxArea) >= interPolygon / 100) {
                    val km = abs(polygon.intersection * 10000)
                    if (km < maxArea) {
                        spaceMb = calculateMB(km, polygon.resolution)
                        showBm.text = getString(R.string.calculate_volume_with_num_text, spaceMb)
                    } else {
                        isAreaValid = false
                    }
                    showKm.text = getString(R.string.calculate_area_with_value_text, km)
                    if (polygon.end == polygon.start) {
                        date.text = "צולם : ${polygon.end}"
                        date.textSize = 15F
                    } else {
                        date.text = "צולם : ${polygon.end} - ${polygon.start}"
                        date.textSize = 15F
                    }
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
                            val km = abs(polygon.intersection * 10000)
                            if (km < maxArea) {
                                spaceMb = calculateMB(km, polygon.resolution)
                                showBm.text = getString(R.string.calculate_volume_with_num_text, spaceMb)
                            } else {
                                isAreaValid = false
                            }
                            showKm.text = getString(R.string.calculate_area_with_value_text, km)
                            if (polygon.end == polygon.start) {
                                date.text = "צולם : ${polygon.end}"
                            } else {
                                date.text = "צולם : ${polygon.end} - ${polygon.start}"
                            }
                            found = true
                            downloadAble = true
                            break
                        }
                    }
                }
            }
            if (!found && allPolygon.isNotEmpty()) {
                val firstPolyObject = allPolygon[0]
                val km =  abs(firstPolyObject.intersection * 10000)
                if (km < maxArea) {
                    spaceMb = calculateMB(km, firstPolyObject.resolution)
                    showBm.text = getString(R.string.calculate_volume_with_num_text, spaceMb)
                } else {
                    isAreaValid = false
                }
                showKm.text = getString(R.string.calculate_area_with_value_text, km)
                if (firstPolyObject.end == firstPolyObject.start) {
                    date.text = "צולם : ${firstPolyObject.end}"
                } else {
                    date.text = "צולם : ${firstPolyObject.end} - ${firstPolyObject.start}"
                }
                downloadAble = true
            }

            var inBbox = false
            loadedPolys.forEach { p ->

                val polygonPoints = p.map { Point(it.longitude, it.latitude) }
                val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)
                val intersection = GeometryEngine.intersectionOrNull(polygonEsri, polygonBoxEsri)
                if (intersection != null) {
                    val intersectionArea = GeometryEngine.area(intersection)
                    val boxArea = calculatePolygonArea(boxCoordinates)
                    if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                        downloadAble = false
                        inBbox = true
                    }
                }
            }

            val overlayView = findViewById<FrameLayout>(R.id.overlayView)
            if (spaceMb < maxMb && downloadAble && isAreaValid) {
                overlayView.setBackgroundResource(R.drawable.blue_border)
            } else {
                overlayView.setBackgroundResource(R.drawable.red_border)
                if (inBbox) {
                    date.text = "בחר תיחום שאינו חותך בול קיים"
                    date.textSize = 15F
                } else if (spaceMb > maxMb && downloadAble) {
                    date.text = "תיחום גדול מנפח מקסימלי להורדה"
                    date.textSize = 15F
                } else if (!isAreaValid) {
                    date.text = "שטח גדול מידי"
                    date.textSize = 15F
                } else {
                    date.text = "אין תוצר עדכני באזור זה"
                    date.textSize = 15F
                    val noData = getString(R.string.no_data_text)
                    showKm.text = getString(R.string.default_calculate_area_text, noData)
                    showBm.text = getString(R.string.calculate_volume_with_string_text, noData)
                }
            }
        } catch (e: Exception) {
            dMode = true
        }
    }

    private fun unionIntersections(polygons: MutableList<PolyObject>): Geometry {
        var unionPolygon = polygons[0].geometry
        for (i in 1 until polygons.size) {
            unionPolygon = GeometryEngine.union(unionPolygon, polygons[i].geometry)
        }
        return unionPolygon
    }

    private fun calculateMB(formattedNum: Double, resolution: BigDecimal): Int {
        var mb: Int
        val resolutionString = String.format("%.9f", resolution.toDouble())
        mb =
            when (resolutionString) {
                "0.000001341" -> (formattedNum * 9.5).toInt()
                "0.000002682" -> (formattedNum * 4.5).toInt()
                "0.000005364" -> (formattedNum * 2.5).toInt()
                else -> (formattedNum * 65.9 - 54.4).toInt()
            }

        return if (mb < 1) {
            1
        } else {
            mb
        }

    }

    private fun detectPolygon(
        map: DiscoveryItem,
        points: List<Position>,
        polygonBoxEsri: com.arcgismaps.geometry.Polygon,
        boxCoordinates: MutableList<Position>,
    ) {
        val polygonPoints = points.map { Point(it.longitude, it.latitude) }

        val polygonEsri = com.arcgismaps.geometry.Polygon(polygonPoints)

        val intersection = GeometryEngine.intersectionOrNull(polygonEsri, polygonBoxEsri)
        if (intersection != null) {
            val intersectionArea = GeometryEngine.area(intersection)
            val boxArea = calculatePolygonArea(boxCoordinates)
            val firstOffsetDateTime = map.imagingTimeBeginUTC
            val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
            val firstDate = sdf.format(firstOffsetDateTime)
            val secondOffsetDateTime = map.imagingTimeEndUTC
            val secondDate = sdf.format(secondOffsetDateTime)

            if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                val polyObject = PolyObject(
                    map.ingestionDate,
                    abs(intersectionArea),
                    firstDate,
                    secondDate,
                    map.maxResolutionDeg,
                    intersection
                )
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

    private fun intersectionOrNullNasa(
        polygon1: List<Position>,
        polygon2: List<Position>,
    ): List<Position>? {
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


    private fun simulateTouch(x: Float, y: Float) {

        val downEvent = MotionEvent.obtain(
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0
        )
        wwd.dispatchTouchEvent(downEvent)

        val upEvent = MotionEvent.obtain(
            System.currentTimeMillis(),
            System.currentTimeMillis(),
            MotionEvent.ACTION_UP,
            x,
            y,
            0
        )
        wwd.dispatchTouchEvent(upEvent)

        // Free the events for memory leaks
        downEvent.recycle()
        upEvent.recycle()
    }

    private fun saveLastPosition(isFromDownload: Boolean) {
        val gson = Gson()

        val lookAt = LookAt()
        wwd.navigator.getAsLookAt(wwd.globe, lookAt)
        var jsonStringLookat = ""
        if (isFromDownload) {
            lookAt.range += 1000
        }
        jsonStringLookat = gson.toJson(lookAt)
        sharedPreferencesEditor?.putString("LookAt", jsonStringLookat)?.apply()

        val jsonString = gson.toJson(wwd.navigator)
        sharedPreferencesEditor?.putString("last_navigator", jsonString)?.apply()
        saveCompass()
        saveControlMap()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveLastPosition(false)
        Timber.e("onBackPressed: ${sharedPreferences?.getString("last_navigator",  "No data")}")
        val intent = Intent(this@MapActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun saveCompass() {
        val compass = findViewById<View>(R.id.arrow)
        val rotation = compass.rotation
        val gson = Gson()
        val rotationString = gson.toJson(rotation)
        sharedPreferencesEditor?.putString("last_compass", rotationString)?.apply()
    }

    private fun saveControlMap() {
        val controlMap = findViewById<Switch>(R.id.control)
        val checked = controlMap.isChecked
        val gson = Gson()
        val rotationString = gson.toJson(checked)
        sharedPreferencesEditor?.putString("last_control_map", rotationString)?.apply()
    }


    private fun showNorth() {
        val compass = findViewById<View>(R.id.arrow)
        compass.rotation = 0F
        wwd.navigator.heading = 0.0
        wwd.requestRedraw()
    }

    open inner class PickNavigateController(val context: AppCompatActivity) :
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
                Timber.tag("ScrollEvent").i("Scroll detected: ${event.x}, ${event.y}")
                checkBboxBeforeSent()
            } else if (!consumed && event.action == MotionEvent.ACTION_MOVE) {
                val areaCal = getString(R.string.calculate_area_text)
                val volumeCal = getString(R.string.calculate_volume_text)
                showKm.text = getString(R.string.default_calculate_area_text, areaCal)
                showBm.text = getString(R.string.calculate_volume_with_string_text, volumeCal)
                date.text = ""
            }

            return if (!consumed) {
                super.onTouchEvent(event)
            } else consumed
        }
    }
}