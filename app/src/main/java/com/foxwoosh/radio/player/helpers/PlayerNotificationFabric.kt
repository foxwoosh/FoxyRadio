package com.foxwoosh.radio.player.helpers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.session.MediaSession
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import com.foxwoosh.radio.MainActivity
import com.foxwoosh.radio.R
import com.foxwoosh.radio.player.models.PlayerState
import com.foxwoosh.radio.player.models.TrackDataState

class PlayerNotificationFabric(private val context: Context) {

    companion object {
        const val notificationID = 777
        const val notificationChannelID = "player_channel_id"

        const val ACTION_PLAYER_PLAY = "b100252a-5bc4-4232-825b-634e36725423"
        const val ACTION_PLAYER_PAUSE = "e3ce5c9f-80a1-4059-a0a8-c7dd71a22fec"
        const val ACTION_PLAYER_STOP = "55104f5f-768c-4365-908b-4e3f97cf99e6"
    }

    private val playAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_player_play),
            context.getString(R.string.notification_action_play),
            PendingIntent.getBroadcast(
                context,
                128459,
                Intent(ACTION_PLAYER_PLAY),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    } else {
        Notification.Action.Builder(
            R.drawable.ic_player_play,
            context.getString(R.string.notification_action_play),
            PendingIntent.getBroadcast(
                context,
                128459,
                Intent(ACTION_PLAYER_PLAY),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }.build()

    private val pauseAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_player_pause),
            context.getString(R.string.notification_action_stop),
            PendingIntent.getBroadcast(
                context,
                98741,
                Intent(ACTION_PLAYER_PAUSE),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    } else {
        Notification.Action.Builder(
            R.drawable.ic_player_pause,
            context.getString(R.string.notification_action_stop),
            PendingIntent.getBroadcast(
                context,
                98741,
                Intent(ACTION_PLAYER_PAUSE),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }.build()

    private val stopAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_player_stop),
            context.getString(R.string.notification_action_stop),
            PendingIntent.getBroadcast(
                context,
                1488228,
                Intent(ACTION_PLAYER_STOP),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    } else {
        Notification.Action.Builder(
            R.drawable.ic_player_stop,
            context.getString(R.string.notification_action_stop),
            PendingIntent.getBroadcast(
                context,
                1488228,
                Intent(ACTION_PLAYER_STOP),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }.build()

    fun getNotification(
        trackData: TrackDataState,
        mediaSessionToken: MediaSession.Token?,
        playerState: PlayerState
    ): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, notificationChannelID)
        } else {
            Notification.Builder(context)
        }

        val image: Bitmap?
        val artist: String
        val title: String
        val color: Color

        when (trackData) {
            TrackDataState.Idle -> {
                image = context.getDrawable(R.drawable.ic_no_music_playing)?.toBitmap()
                artist = ""
                title = context.getString(R.string.player_title_idle)
                color = Color.Black
            }
            TrackDataState.Loading -> {
                image = context.getDrawable(R.drawable.ic_no_music_playing)?.toBitmap()
                artist = ""
                title = context.getString(R.string.player_title_loading)
                color = Color.Black
            }
            is TrackDataState.Ready -> {
                image = trackData.cover
                artist = trackData.artist
                title = trackData.title
                color = trackData.colors.surfaceColor
            }
        }

        return builder
            .setColor(color.value.toInt())
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(image)
            .addAction(
                when (playerState) {
                    PlayerState.PLAYING -> pauseAction
                    PlayerState.PAUSED -> playAction
                    else -> null
                }
            )
            .addAction(stopAction)
            .setSmallIcon(R.drawable.ic_foxy_radio_logo)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(playerState != PlayerState.IDLE)
            .setShowWhen(false)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    11111,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setStyle(
                Notification.MediaStyle().also {
                    it.setMediaSession(mediaSessionToken)
                    it.setShowActionsInCompactView(0, 1)
                }
            )
            .build()
    }
}