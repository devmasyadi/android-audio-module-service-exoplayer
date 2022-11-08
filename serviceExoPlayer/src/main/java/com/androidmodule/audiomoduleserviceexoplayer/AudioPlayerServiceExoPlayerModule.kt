package com.androidmodule.audiomoduleserviceexoplayer

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val audioPlayerServiceExoPlayerModule = module {
    single {
        AudioPlayerViewModel()
    }
    viewModel { get() }
}