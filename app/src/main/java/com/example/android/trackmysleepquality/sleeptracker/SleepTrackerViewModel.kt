/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {
    private var viewmodelJob= Job()
    private val uiScope= CoroutineScope(Dispatchers.Main+viewmodelJob)
    private var Tonight= MutableLiveData<SleepNight>()
    private val _nights=database.getAllNight()
    val nights:LiveData<List<SleepNight>>
    get()=_nights

    val startButtonVisible=Transformations.map(Tonight) {
        it==null
    }
    val stopButtonVisible=Transformations.map(Tonight){
        it!=null
    }
    val clearButtonVisible=Transformations.map(nights){
        it?.isNotEmpty()
    }
    private val _showSnackBar=MutableLiveData<Boolean>()
    val showSnackbar:LiveData<Boolean>
    get() {
        return _showSnackBar
    }
    fun doneShowingSnackBar()
    {
        _showSnackBar.value=false
    }
    private val _navigateToSleepQuality=MutableLiveData<SleepNight>()
    val navigateToSleepQuality:LiveData<SleepNight>
    get() {
        return _navigateToSleepQuality
    }
    init {
        intializeTonight()
    }

    fun doneNavigation()
    {
        _navigateToSleepQuality.value=null
    }
    private fun intializeTonight()
    {
        uiScope.launch {
            Tonight.value=getTonightFromDataBase()
        }
    }
    private suspend fun getTonightFromDataBase():SleepNight?
    {
        return withContext(Dispatchers.IO)
        {
            var night=database.getTonight()
            if(night?.enTimeMilli!=night?.startTimeMilli)
                night=null
            night
        }


    }
     fun onStartTracking()
    {
        uiScope.launch {
            var newNight=SleepNight()
            insert(newNight)
            Tonight.value=getTonightFromDataBase()
        }

    }
    private suspend fun insert(newNight:SleepNight)
    {
        withContext(Dispatchers.IO)
        {
            database.insert(newNight)

        }
    }
    fun onStopTracking()
    {
        uiScope.launch {
            val oldNight=Tonight.value?:return@launch
             oldNight.enTimeMilli=System.currentTimeMillis()
            update(oldNight)
            _navigateToSleepQuality.value=oldNight

        }
    }
    private suspend fun update(oldNight: SleepNight)
    {
        withContext(Dispatchers.IO)
        {
            database.update(oldNight)
        }
    }
    fun onClear()
    {
        uiScope.launch {
            clear()
            Tonight.value=null
        }
    }
    private suspend fun clear()
    {
        withContext(Dispatchers.IO)
        {
            database.clear()
        }
        _showSnackBar.value=true

    }
    private var _navigateToSleepDataQuality=MutableLiveData<Long>()
    val navigateToSleepDataQuality
    get() = _navigateToSleepDataQuality

    fun onSleepNightClicked(id:Long)
    {
        _navigateToSleepDataQuality.value=id
    }
    fun onNavigateToSleepDataQualityDone()
    {
        _navigateToSleepDataQuality.value=null
    }
    override fun onCleared() {
        super.onCleared()
        viewmodelJob.cancel()
    }
}

