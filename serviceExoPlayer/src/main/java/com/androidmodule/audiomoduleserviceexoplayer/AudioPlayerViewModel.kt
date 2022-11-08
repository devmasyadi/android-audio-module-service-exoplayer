package com.androidmodule.audiomoduleserviceexoplayer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.androidmodule.audiomodule.model.AudiosItem

class AudioPlayerViewModel : ViewModel() {
    val currentPlaying: MutableLiveData<AudiosItem> by lazy {
        MutableLiveData<AudiosItem>()
    }
}