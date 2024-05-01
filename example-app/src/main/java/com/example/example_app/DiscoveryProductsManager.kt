package com.example.example_app

import com.ngsoft.getapp.sdk.models.DiscoveryItem

class DiscoveryProductsManager {

    private var _products: List<DiscoveryItem> = ArrayList()

    val products: List<DiscoveryItem> get() = _products

    fun updateProducts(products: List<DiscoveryItem>) {
        _products = products
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