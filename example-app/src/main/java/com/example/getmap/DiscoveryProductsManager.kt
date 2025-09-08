package com.example.getmap

import com.google.gson.Gson
import com.ngsoft.getapp.sdk.models.DiscoveryItem
import org.json.JSONObject

sealed class ProductShape {
    data class Polygon(val polygonDTO: PolygonDTO) : ProductShape()
    data class MultiPolygon(val multiPolygonDTO: MultiPolygonDto) : ProductShape()
}

data class GeoProduct (
    val discoveryItem: DiscoveryItem,
) {
    val type: String = JSONObject(discoveryItem.footprint).getString("type")
    val productShapeDTO: ProductShape = run {
        val gson = Gson()
        when (type) {
            "Polygon" -> ProductShape.Polygon( gson.fromJson(discoveryItem.footprint, PolygonDTO::class.java) )
            else -> ProductShape.MultiPolygon( gson.fromJson(discoveryItem.footprint, MultiPolygonDto::class.java) )
        }
    }
}


class DiscoveryProductsManager {

    private var _products: List<GeoProduct> = ArrayList()

    val products: List<GeoProduct> get() = _products

    fun updateProducts(products: List<DiscoveryItem>) {
        _products = products.map { p -> GeoProduct(p) }
    }

    companion object {
        private var _instance: DiscoveryProductsManager? = null
        fun getInstance(): DiscoveryProductsManager {
            if (_instance == null) {
                _instance = DiscoveryProductsManager()
            }

            return _instance!!
        }
    }
}