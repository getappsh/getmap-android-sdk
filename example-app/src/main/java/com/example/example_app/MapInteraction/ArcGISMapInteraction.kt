package com.example.example_app.MapInteraction

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
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
import com.example.example_app.DiscoveryProductsManager
import com.example.example_app.MultiPolygonDto
import com.example.example_app.PolygonDTO
import com.google.gson.Gson
import com.ngsoft.getapp.sdk.GetMapService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.format.DateTimeFormatter
import kotlin.math.abs


class ArcGISMapInteraction(ctx: Context, service: GetMapService) : MapInteraction<MapView>(ctx, service) {

    private val TAG = ArcGISMapInteraction::class.qualifiedName

    private val loadedProductsPolys: ArrayList<Polygon> = ArrayList();


    override var mapView = MapView(ctx)

    init {
        setApiKey()
    }
    private fun setApiKey() {
        val keyId = "runtimelite,1000,rud1971484999,none,GB2PMD17JYCJ5G7XE200"
        ArcGISEnvironment.apiKey = ApiKey.create(keyId)
    }

    override fun setMapView(parent: FrameLayout, lifecycle: Lifecycle) {
        parent.addView(mapView)
        lifecycle.addObserver(mapView)
    }

    override fun generateBoundingInfo(): PolyObject? {
        val polygonPoints = getPolygonPoints() ?: return null

        val area = calculateArea(polygonPoints)

        val boxPolygon = Polygon(polygonPoints)
        val containingProducts = findContainingProducts(boxPolygon)
        val polyProduct = findBestProductIntersection(containingProducts, boxPolygon)

        val inOtherMap = inExistingMap(boxPolygon);
        return PolyObject(
            area = area,
            inCollision = inOtherMap,
            product = polyProduct,
            strPolygon = getPloyStr(polygonPoints)
        )
    }
    private fun getPloyStr(polygonPoints: List<Point>): String{
        val pLeftTop = polygonPoints[0]
        val pRightTop = polygonPoints[1]
        val pRightBottom = polygonPoints[2]
        val pLeftBottom = polygonPoints[3]

        return "${pLeftTop.x},${pLeftTop.y},${pRightTop.x},${pRightTop.y},${pRightBottom.x},${pRightBottom.y},${pLeftBottom.x},${pLeftBottom.y},${pLeftTop.x},${pLeftTop.y}"
    }

    private fun inExistingMap(boxPolygon: Polygon): Boolean{
        return loadedProductsPolys.any { p ->
            val intersection = GeometryEngine.intersectionOrNull(p, boxPolygon)
            if (intersection != null) {
                val intersectionArea = GeometryEngine.area(intersection)
                val boxArea = GeometryEngine.area(boxPolygon)
                (abs(intersectionArea) / abs(boxArea) > 0.0)
            }else
                false
        }
    }
    private fun getPolygonPoints(): List<Point>? {
        val height = ctx.resources.displayMetrics.heightPixels
        val width = ctx.resources.displayMetrics.widthPixels

        val leftTop = ScreenCoordinate(100.0, height - 550.0)
        val rightTop = ScreenCoordinate(width - 100.0, height - 550.0)
        val rightBottom = ScreenCoordinate(width - 100.0, 550.0)
        val leftBottom = ScreenCoordinate(100.0, 550.0)

        val screenToLocation = mapView::screenToLocation
        val points  = listOf(
            screenToLocation(leftTop) ?: return null,
            screenToLocation(rightTop) ?: return null,
            screenToLocation(rightBottom) ?: return null,
            screenToLocation(leftBottom) ?: return null
        )
        if (points.any { it == null }){
            return null
        }
        return points
    }

    private fun calculateArea(polygonPoints: List<Point>): Double {
        val pLeftTop = polygonPoints[0]
        val pRightTop = polygonPoints[1]
        val width = calculateDistance(pLeftTop.x, pLeftTop.y, pRightTop.x, pRightTop.y) / 1000
        val height = calculateDistance(pLeftTop.x, pLeftTop.y, polygonPoints[3].x,  polygonPoints[3].y) / 1000
        return width * height
    }

    private fun findContainingProducts(boxPolygon: Polygon): MutableList<Product> {
        val allProducts = mutableListOf<Product>()

        val gson = Gson()

        DiscoveryProductsManager.getInstance().products.forEach { p ->
            run {
                val json = JSONObject(p.footprint)
                val type = json.getString("type")

                var polygon: Polygon? = null
                when (type) {
                    "Polygon" -> {
                        val productPolyDTO = gson.fromJson(p.footprint, PolygonDTO::class.java)
                        productPolyDTO.coordinates.forEach { coordinates ->
                            val points: List<Point> = coordinates.map {
                                Point(it[0], it[1], SpatialReference.wgs84())
                            }
                            polygon = Polygon(points)
                        }
                    }
                    "MultiPolygon" -> {
                        val productMultiPolyDTO = gson.fromJson(p.footprint, MultiPolygonDto::class.java)
                        productMultiPolyDTO.coordinates.forEach { polyCoordinates ->
                            polyCoordinates.forEach { coordinates ->
                                val points: List<Point> = coordinates.map {
                                    Point(it[0], it[1], SpatialReference.wgs84())
                                }
                                polygon = Polygon(points)
                            }
                        }
                    }
                    else -> return allProducts
                }

                val firstOffsetDateTime = p.imagingTimeBeginUTC
                val sdf = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val firstDate = sdf.format(firstOffsetDateTime)
                val secondOffsetDateTime = p.imagingTimeEndUTC
                val secondDate = sdf.format(secondOffsetDateTime)
                val interPolygon = service.config.mapMinInclusionPct.toDouble() / 100

                val intersection = GeometryEngine.intersectionOrNull(polygon!!, boxPolygon)
                val intersectionArea = GeometryEngine.area(intersection!!)
                val boxArea = GeometryEngine.area(boxPolygon)

                if (abs(intersectionArea) / abs(boxArea) > 0.0) {
                    val product = Product(date = p.ingestionDate, intersection = abs(intersectionArea), start = firstDate, end = secondDate)
                    allProducts.add(product)
                }
            }
        }
        return allProducts
    }

    private fun findBestProductIntersection(allProducts: MutableList<Product>, boxPolygon: Polygon): Product? {
        allProducts.sortByDescending(Product::date)
        val interPolygon = service.config.mapMinInclusionPct.toDouble() / 100

        val boxArea = GeometryEngine.area(boxPolygon)
        val bestProduct = allProducts.find { prd ->  (prd.intersection / abs(boxArea) >= interPolygon)} ?: allProducts.getOrNull(0)

        return bestProduct
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override suspend fun renderBaseMap() {
        val geoPackage = GeoPackage(getBaseMapLocation())
        loadGeoPackageRasters(geoPackage)?.let { basemap ->
            val graphicsOverlay = createGraphicsOverlay()

            mapView.graphicsOverlays.add(graphicsOverlay)
            mapView.map = ArcGISMap(basemap)
            mapView.setViewpoint(Viewpoint(31.7270, 34.6, 2000000.0))
        }
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
                loadedProductsPolys.add(polygon)
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