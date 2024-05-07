package com.example.getMap

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.ngsoft.getapp.sdk.Configuration
import com.ngsoft.getapp.sdk.GetMapService
import com.ngsoft.getapp.sdk.GetMapServiceFactory
import kotlin.jvm.Throws

@RequiresApi(Build.VERSION_CODES.R)
class MapServiceManager {
    private var _service: GetMapService? = null
     var isInit = false
    val service get() = _service!!

    companion object {
        private var _instance: MapServiceManager? = null;

        fun getInstance(): MapServiceManager {
            if (_instance == null) {
                _instance = MapServiceManager()
            }

            return _instance!!
        }
    }

    @Throws(Exception::class)
    fun initService(ctx: Context, config: Configuration) {
        isInit = true
        if(_service != null) throw Exception("Can not initialize service more than one.")
        _service = GetMapServiceFactory.createAsioSdkSvc(ctx, config)
//        _service!!.setOnInventoryUpdatesListener {
//            val data = it.joinToString()
//            CoroutineScope(Dispatchers.Main).launch {
//                Toast.makeText(ctx, data, Toast.LENGTH_LONG).show()
//            }
//        }
    }
    fun resetService(){
        isInit = false
        _service = null
    }
}


