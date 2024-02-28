package com.example.example_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.data.FieldType
import com.arcgismaps.data.GeoPackage
import com.arcgismaps.geometry.Geometry
import com.arcgismaps.geometry.GeometryType
import com.arcgismaps.geometry.Multipoint
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.Polygon
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.Basemap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.RasterLayer
import com.arcgismaps.mapping.symbology.FillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbol
import com.arcgismaps.mapping.symbology.SimpleFillSymbolStyle
import com.arcgismaps.mapping.symbology.SimpleLineSymbol
import com.arcgismaps.mapping.symbology.SimpleLineSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.MapView
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File


class MapActivity : AppCompatActivity() {
    lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById<MapView>(R.id.mapView)

        setApiKey()

        lifecycle.addObserver(mapView)



        val back = findViewById<Button>(R.id.back)
        back.setOnClickListener {
//            val intent = Intent(this@MapActivity, MainActivity::class.java)
//            startActivity(intent)
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
    }



    private fun setApiKey() {
        // It is not best practice to store API keys in source code. We have you insert one here
        // to streamline this tutorial.

        ArcGISEnvironment.apiKey = ApiKey.create("AAPK9f60194290664c60b1e4e9c2f12731e2EnCmC48fwIqi_aWiIk2SX22TpgeXo5XIS013xIkAhhYX9EFwz1QooTqlN34eD0FM")

    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun geoPackageRender() {
        // Instantiate the geopackage with the file path.
        val storageManager :StorageManager = getSystemService(STORAGE_SERVICE) as StorageManager
        val storageList = storageManager.storageVolumes;
        val volume = storageList[1].directory?.absoluteFile ?: ""
//        var directory = File(localGeoPackagePath)
//        var isdirec = directory.isDirectory
//        var exist = directory.exists()
//        var files:Array<File> = directory.listFiles()!!
        val geoPackage = GeoPackage("${volume}/com.asio.gis/gis/maps/orthophoto/אורתופוטו.gpkg")
        lifecycleScope.launch {
            // Load the geopackage.
            geoPackage.load().onSuccess {
                if (geoPackage.geoPackageRasters.isNotEmpty()) {
                    // Get the first feature table in the GeoPackage.
                    val geoPackageRaster = geoPackage.geoPackageRasters.first()
                    // Create a feature layer with the feature table.
                    val rasterLayer = RasterLayer(geoPackageRaster)
                    // Set the viewpoint to Denver, CO.

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