package com.example.example_app.MapInteraction

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
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


class ArcGISMapInteraction(ctx: Context, service: GetMapService) : MapInteraction<MapView>(ctx, service) {

    private val TAG = ArcGISMapInteraction::class.qualifiedName

    private val loadedProductsPolys: ArrayList<Polygon> = ArrayList();


    override var mapView = MapView(ctx)

    override fun setMapView(parent: FrameLayout, lifecycle: Lifecycle) {
        parent.addView(mapView)
        lifecycle.addObserver(mapView)
    }

    override fun checkBBoxBeforeSent() {
        TODO("Not yet implemented")
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

    override fun onDelivery() {
        TODO("Not yet implemented")
    }
}