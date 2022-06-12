@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalUnitApi::class
)

package com.foxwoosh.radio.ui.player

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import com.foxwoosh.radio.R
import com.foxwoosh.radio.copyToClipboard
import com.foxwoosh.radio.openURL
import com.foxwoosh.radio.player.PlayerService
import com.foxwoosh.radio.player.models.*
import com.foxwoosh.radio.ui.borderlessClickable
import com.foxwoosh.radio.ui.singleCondition
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private const val trackChangeDuration = 1_000

@Composable
fun PlayerScreen() {
    val config = LocalConfiguration.current
    val density = LocalDensity.current

    val viewModel = hiltViewModel<PlayerViewModel>()

    val trackData by viewModel.trackDataFlow.collectAsState()
    val playerState by viewModel.playerStateFlow.collectAsState()
    val previousTracks by viewModel.previousTracksFlow.collectAsState()
    val lyricsState by viewModel.lyricsStateFlow.collectAsState()

    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()

    val scope = rememberCoroutineScope()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val defaultBottomSheetPeekHeight = 80.dp + navigationBarHeight

    val colorAnimationSpec: AnimationSpec<Color> = tween(trackChangeDuration)
    val gradientOffsetAnimationSpec: AnimationSpec<Float> = tween(trackChangeDuration)
    val bottomSheetPeekHeightAnimationSpec: AnimationSpec<Dp> = spring()

    val title: String
    val artist: String
    val cover: Bitmap?
    val colors: PlayerColors
    val musicServices: MusicServicesData?

    val gradientOffsetX: Float
    val gradientOffsetY: Float
    val actualBottomSheetPeekHeight: Dp

    when (trackData) {
        TrackDataState.Idle -> {
            title = stringResource(R.string.player_title_idle)
            artist = ""
            cover = null
            colors = PlayerColors.default
            musicServices = null
            gradientOffsetX = 0f
            gradientOffsetY = 0f
            actualBottomSheetPeekHeight = 0.dp
        }
        TrackDataState.Loading -> {
            title = stringResource(R.string.player_title_loading)
            artist = ""
            cover = null
            colors = PlayerColors.default
            musicServices = null
            gradientOffsetX = 0f
            gradientOffsetY = 0f
            actualBottomSheetPeekHeight = 0.dp
        }
        is TrackDataState.Error -> {
            title = stringResource(
                id = when ((trackData as TrackDataState.Error).error) {
                    PlayerError.DEFAULT -> R.string.player_title_error_default
                    PlayerError.NO_INTERNET -> R.string.player_title_error_no_internet
                }
            )
            artist = ""
            cover = null
            colors = PlayerColors.default
            musicServices = null
            gradientOffsetX = 0f
            gradientOffsetY = 0f
            actualBottomSheetPeekHeight = 0.dp
        }
        is TrackDataState.Ready -> {
            val data = trackData as TrackDataState.Ready

            title = data.title
            artist = data.artist
            cover = data.cover
            colors = data.colors
            musicServices = data.musicServices
            gradientOffsetX = animateFloatAsState(
                targetValue = data.hashCode().absoluteValue %
                    with(density) { config.screenWidthDp.dp.toPx() },
                animationSpec = gradientOffsetAnimationSpec
            ).value
            gradientOffsetY = animateFloatAsState(
                targetValue = data.hashCode().absoluteValue %
                    with(density) { config.screenHeightDp.dp.toPx() },
                animationSpec = gradientOffsetAnimationSpec
            ).value
            actualBottomSheetPeekHeight = animateDpAsState(
                targetValue = defaultBottomSheetPeekHeight,
                animationSpec = bottomSheetPeekHeightAnimationSpec
            ).value
        }
    }

    LaunchedEffect(trackData) {
        if (bottomSheetScaffoldState.bottomSheetState.isExpanded
            && trackData is TrackDataState.Ready
        ) {
            viewModel.fetchLyricsForCurrentTrack()
        }
    }

    BackHandler(bottomSheetScaffoldState.bottomSheetState.isExpanded) {
        if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
            scope.launch { bottomSheetScaffoldState.bottomSheetState.collapse() }
        }
    }

    val surfaceColor by animateColorAsState(
        targetValue = colors.surfaceColor,
        animationSpec = colorAnimationSpec
    )
    val vibrantSurfaceColor by animateColorAsState(
        targetValue = colors.vibrantSurfaceColor,
        animationSpec = colorAnimationSpec
    )
    val primaryTextColor by animateColorAsState(
        targetValue = colors.primaryTextColor,
        animationSpec = colorAnimationSpec
    )
    val secondaryTextColor by animateColorAsState(
        targetValue = colors.secondaryTextColor,
        animationSpec = colorAnimationSpec
    )

    Surface {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetContent = {
                PlayerBottomSheetContent(
                    state = bottomSheetScaffoldState,
                    statusBarHeight = statusBarHeight,
                    backgroundColor = surfaceColor,
                    primaryTextColor = primaryTextColor,
                    secondaryTextColor = secondaryTextColor,
                    previousTracks = previousTracks,
                    lyricsState = lyricsState,
                    onPageBecomesVisible = { page ->
                        when (page) {
                            PlayerBottomSheetPage.LYRICS ->
                                viewModel.fetchLyricsForCurrentTrack()
                        }
                    }
                )
            },
            sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            sheetPeekHeight = actualBottomSheetPeekHeight,
            sheetElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(vibrantSurfaceColor, surfaceColor),
                            center = Offset(gradientOffsetX, gradientOffsetY),
                            tileMode = TileMode.Mirror
                        )
                    )
                    .padding(bottom = defaultBottomSheetPeekHeight)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CoverWithServices(
                    cover = cover,
                    musicServices = musicServices,
                    trackDataReady = trackData is TrackDataState.Ready,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                )

                TrackInfo(
                    title = title,
                    primaryTextColor = primaryTextColor,
                    artist = artist,
                    secondaryTextColor = secondaryTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                PlaybackController(
                    color = primaryTextColor,
                    playerState = playerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.15f)
                )
            }
        }
    }
}

@Composable
fun CoverWithServices(
    cover: Bitmap?,
    musicServices: MusicServicesData?,
    trackDataReady: Boolean,
    modifier: Modifier = Modifier
) {
    var musicServicesMenuOpened by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier) {
        PlayerMusicServices(
            musicServices = musicServices,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .scale(animateFloatAsState(if (musicServicesMenuOpened) 1f else 0f).value),
            musicServiceSelected = { url ->
                context.openURL(url)
                musicServicesMenuOpened = false
            }
        )

        val smallScaledCover = musicServicesMenuOpened
            && trackDataReady
            && musicServices?.hasSomething == true

        val scale by animateFloatAsState(
            targetValue = if (smallScaledCover) 0.5f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        )

        val crossfadeLoader = ImageLoader.Builder(context)
            .crossfade(durationMillis = trackChangeDuration)
            .build()
        AsyncImage(
            model = cover,
            imageLoader = crossfadeLoader,
            contentDescription = "Cover",
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .aspectRatio(1f)
                .graphicsLayer(
                    shadowElevation = animateFloatAsState(
                        if (smallScaledCover) 30f else 0f
                    ).value,
                    scaleX = scale,
                    scaleY = scale
                )
                .singleCondition(trackDataReady) {
                    clickable { musicServicesMenuOpened = !musicServicesMenuOpened }
                }
        )
    }
}

@Composable
fun TrackInfo(
    title: String,
    primaryTextColor: Color,
    artist: String, secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = primaryTextColor,
            fontSize = TextUnit(24f, TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist,
            color = secondaryTextColor,
            fontSize = TextUnit(14f, TextUnitType.Sp),
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun PlayerMusicServices(
    musicServices: MusicServicesData?,
    modifier: Modifier = Modifier,
    musicServiceSelected: (url: String) -> Unit
) {
    if (musicServices == null) return

    if (musicServices.hasSomething) {
        val context = LocalContext.current

        Row(
            modifier = modifier.animateContentSize(),
            horizontalArrangement = Arrangement.Center
        ) {
            if (!musicServices.youtubeMusic.isNullOrEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_youtube_music),
                    contentDescription = "Youtube Music",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .borderlessClickable(
                            onClick = { musicServiceSelected(musicServices.youtubeMusic) },
                            onLongClick = { context.copyToClipboard(musicServices.youtubeMusic) }
                        )
                )
            }
            if (!musicServices.youtube.isNullOrEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_youtube_play),
                    contentDescription = "Youtube",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .borderlessClickable(
                            onClick = { musicServiceSelected(musicServices.youtube) },
                            onLongClick = { context.copyToClipboard(musicServices.youtube) }
                        )
                )
            }
            if (!musicServices.spotify.isNullOrEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_spotify),
                    contentDescription = "Spotify",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .borderlessClickable(
                            onClick = { musicServiceSelected(musicServices.spotify) },
                            onLongClick = { context.copyToClipboard(musicServices.spotify) }
                        )
                )
            }
            if (!musicServices.iTunes.isNullOrEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_itunes),
                    contentDescription = "iTunes",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .borderlessClickable(
                            onClick = { musicServiceSelected(musicServices.iTunes) },
                            onLongClick = { context.copyToClipboard(musicServices.iTunes) }
                        )
                )
            }
            if (!musicServices.yandexMusic.isNullOrEmpty()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_yandex_music),
                    contentDescription = "Yandex Music",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(48.dp)
                        .borderlessClickable(
                            onClick = { musicServiceSelected(musicServices.yandexMusic) },
                            onLongClick = { context.copyToClipboard(musicServices.yandexMusic) }
                        )
                )
            }
        }
    }
}

@Composable
fun PlaybackController(
    color: Color,
    playerState: PlayerState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .height(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = playerState == PlayerState.IDLE,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                OutlinedButton(onClick = { PlayerService.selectSource(context, Station.ULTRA) }) {
                    Text(
                        text = "Just play the only radio my lazy ass could bring to this application and sorry I don\'t have fucking fantasy to do a cool station selector",
                        textAlign = TextAlign.Center
                    )
                }
            }

            AnimatedVisibility(
                visible = playerState == PlayerState.PLAYING,
                enter = scaleIn(),
                exit = fadeOut()
            ) {
                PlayerControllerButton(
                    picRes = R.drawable.ic_player_pause_filled,
                    contentDescription = "Pause",
                    color = color,
                    onClick = { PlayerService.pause(context) }
                )
            }

            AnimatedVisibility(
                visible = playerState == PlayerState.PAUSED,
                enter = fadeIn(),
                exit = scaleOut()
            ) {
                PlayerControllerButton(
                    picRes = R.drawable.ic_player_play_filled,
                    contentDescription = "Play",
                    color = color,
                    onClick = { PlayerService.play(context) }
                )
            }

            AnimatedVisibility(
                visible = playerState == PlayerState.BUFFERING,
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                CircularProgressIndicator(
                    color = color,
                    modifier = Modifier
                        .size(48.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = playerState == PlayerState.PLAYING || playerState == PlayerState.PAUSED,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = "Info",
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(42.dp)
                        .borderlessClickable(
                            onClick = {
                                Toast
                                    .makeText(context, "Info", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        )
                )
                Spacer(modifier = Modifier.width(120.dp))
                Image(
                    painter = painterResource(id = R.drawable.ic_player_stop_filled),
                    contentDescription = "Stop",
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(42.dp)
                        .borderlessClickable(onClick = { PlayerService.stop(context) })
                )
            }
        }

    }
}

@Composable
fun PlayerControllerButton(
    @DrawableRes picRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(picRes),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .padding(8.dp)
            .size(72.dp)
            .borderlessClickable(onClick = onClick)
    )
}