package com.foxwoosh.radio.ui.player

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.foxwoosh.radio.media_player.MusicServicesData

sealed class PlayerState {
    object Loading : PlayerState()
    data class Done(
        val title: String,
        val artist: String,
        val bitmap: Bitmap,
        val surfaceColor: Color,
        val primaryTextColor: Color,
        val secondaryTextColor: Color,
        val musicServices: MusicServicesData
    ) : PlayerState()

}