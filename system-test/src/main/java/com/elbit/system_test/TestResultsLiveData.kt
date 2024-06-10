package com.elbit.system_test

import android.util.Log
import androidx.lifecycle.LiveData
import com.ngsoft.getapp.sdk.SystemTest

class TestResultsLiveData: LiveData<HashMap<Int, SystemTest.TestResults?>>() {
    fun update(data: HashMap<Int, SystemTest.TestResults?>) {
        Log.d("TestResultsLiveData", "update")
        postValue(data)
    }

    class LiveDataManager{
        companion object{
            private val testResultsLiveData = TestResultsLiveData()

            fun updateResults(data: HashMap<Int, SystemTest.TestResults?>){
                testResultsLiveData.update(data)
            }

            fun testResults() = testResultsLiveData
        }

    }
}