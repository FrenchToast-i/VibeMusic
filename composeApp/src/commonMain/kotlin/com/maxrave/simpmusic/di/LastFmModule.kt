package com.maxrave.simpmusic.di

import com.maxrave.data.service.lastfm.LastFmService
import org.koin.dsl.module

val lastFmModule =
    module {
        single {
            LastFmService(
                httpClient = get(),
                apiKey = "", // Will be set dynamically via SettingsViewModel
                apiSecret = "", // Will be set dynamically via SettingsViewModel
            )
        }
    }
