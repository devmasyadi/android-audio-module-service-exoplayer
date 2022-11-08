package com.androidmodule.audiomoduleserviceexoplayer

import android.app.Activity
import android.content.Intent
import com.androidmodule.audiomodule.model.AudiosItem

object AudioUtils {

    private var currentPlaylist = ArrayList<AudiosItem>()
    private var currentIndex = 0
    var currentGroupName = ""
    var recentPlayedName = "recentPlayed"
    var currentAudio: AudiosItem? = null
    var detailAudioActivity: Class<*>? = null

    private fun createPlaylistAudioService(activity: Activity) {
        val mBoundServiceIntent = Intent(activity, AudioService::class.java)
        mBoundServiceIntent.action = AudioService.ACTION_CREATE
        activity.startService(mBoundServiceIntent)
    }

    private fun playAudioService(activity: Activity) {
        val mBoundServiceIntent = Intent(activity, AudioService::class.java)
        mBoundServiceIntent.action = AudioService.ACTION_PLAY
        activity.startService(mBoundServiceIntent)
    }

    fun getCurrentPlaylist() = currentPlaylist

    private fun setCurrentPlaylist(data: List<AudiosItem>) {
        currentPlaylist.clear()
        currentPlaylist.addAll(data)
    }

    fun updateItemPlaylist(indexAudio: Int, audiosItem: AudiosItem) {
        currentPlaylist[indexAudio] = audiosItem
    }

    private fun setCurrentIndex(index: Int) {
        currentIndex = index
    }

    fun getCurrentIndex() = currentIndex

    fun playAudio(
        indexAudio: Int,
        groupName: String,
        listAudio: List<AudiosItem>?,
        audiosItem: AudiosItem,
        activity: Activity,
    ) {
        setCurrentIndex(indexAudio)
        currentAudio = audiosItem
        if (currentGroupName != groupName && listAudio != null) {
            currentGroupName = groupName
            setCurrentPlaylist(listAudio)
            createPlaylistAudioService(activity)
        } else {
            playAudioService(activity)
        }

    }
}