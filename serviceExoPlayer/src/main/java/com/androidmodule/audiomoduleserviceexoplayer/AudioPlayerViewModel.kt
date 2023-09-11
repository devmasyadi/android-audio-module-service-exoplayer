package com.androidmodule.audiomoduleserviceexoplayer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.material.audio.model.AudiosItem

class AudioPlayerViewModel : ViewModel() {
    val currentPlaying: MutableLiveData<AudiosItem> by lazy {
        MutableLiveData<AudiosItem>()
    }
}