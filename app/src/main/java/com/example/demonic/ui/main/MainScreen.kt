package com.example.demonic.ui.main

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.demonic.data.Gesture
import com.example.demonic.speech.SpeechState

// Premium Color Palette
val DarkBg = Color(0xFF0F0F14)
val DarkSurface = Color(0xFF1E1E26)
val NeonCyan = Color(0xFF00E5FF)
val DeepIndigo = Color(0xFF3F51B5)
val AccentGradient = Brush.linearGradient(listOf(NeonCyan, DeepIndigo))
val TextPrimary = Color(0xFFE2E2EC)
val TextSecondary = Color(0xFF8F909A)
val MicListeningColor = Color(0xFFFF5252)

@Composable
fun MainScreen(
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(application) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.setPermissionGranted(isGranted)
            if (isGranted) {
                viewModel.toggleSpeechRecognition()
            }
        }
    )

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setPermissionGranted(isGranted)
    }

    MainScreenContent(
        state = state,
        onInputTextChanged = { viewModel.onInputTextChanged(it) },
        onTranslateClicked = { viewModel.processText(state.inputText) },
        onMicClicked = {
            if (state.permissionGranted) {
                viewModel.toggleSpeechRecognition()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onGesturePlayingIndexChanged = { viewModel.setCurrentPlayingIndex(it) },
        onPlaybackPlayingChanged = { viewModel.setPlaybackPlaying(it) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreenContent(
    state: MainUiState,
    onInputTextChanged: (String) -> Unit,
    onTranslateClicked: () -> Unit,
    onMicClicked: () -> Unit,
    onGesturePlayingIndexChanged: (Int) -> Unit,
    onPlaybackPlayingChanged: (Boolean) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ANIMATION/VIDEO AREA (70% weight)
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(566f / 850f, matchHeightConstraintsFirst = true)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    if (state.resolvedGestures.isNotEmpty()) {
                        VideoPlayer(
                            gestures = state.resolvedGestures,
                            currentPlayingIndex = state.currentPlayingIndex,
                            isPlaying = state.isPlaying,
                            onIndexChanged = onGesturePlayingIndexChanged,
                            onPlayingChanged = onPlaybackPlayingChanged,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Sign Queue Empty",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Speak or type to play animations offline.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 2. UNRESOLVED SIGN AREA (10% weight)
            Box(
                modifier = Modifier
                    .weight(0.1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (state.unresolvedWords.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Unresolved: ",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(state.unresolvedWords) { word ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF3E2723))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "⚠ $word",
                                        color = Color(0xFFFFCC80),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }

            // 3. TYPING/INPUT AREA (20% weight)
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = onInputTextChanged,
                        placeholder = { Text("Type English phrase...", color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (state.inputText.isNotBlank()) {
                                    onTranslateClicked()
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF2E2E38),
                            focusedContainerColor = Color(0xFF16161F),
                            unfocusedContainerColor = Color(0xFF16161F)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (state.inputText.isNotBlank()) {
                                onTranslateClicked()
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGradient)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Translate",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Microphone control, status text and offline mode inline row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small Mic Icon Button
                    IconButton(
                        onClick = onMicClicked,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    state.speechState == SpeechState.Listening -> MicListeningColor
                                    state.speechState == SpeechState.Ready -> NeonCyan
                                    else -> Color(0xFF2E2E38)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = if (state.speechState == SpeechState.Listening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Live Speech or Engine status
                    val statusText = when (state.speechState) {
                        SpeechState.NotInitialized -> "Speech engine uninitialized"
                        SpeechState.Initializing -> "Loading model (40MB)..."
                        SpeechState.Ready -> "Tap mic to speak offline"
                        SpeechState.Listening -> "Listening..."
                        is SpeechState.Error -> "Speech error"
                    }
                    Text(
                        text = if (state.speechState == SpeechState.Listening && state.partialSpeechText.isNotBlank()) {
                            "Listening: \"${state.partialSpeechText}\""
                        } else {
                            statusText
                        },
                        fontSize = 11.sp,
                        color = if (state.speechState is SpeechState.Error) MicListeningColor else TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Offline Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF1E291E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline",
                            fontSize = 10.sp,
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeechSection(
    state: MainUiState,
    onMicClicked: () -> Unit
) {
    // Pulse animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (state.speechState == SpeechState.Listening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Display partial result live speech text
        if (state.speechState == SpeechState.Listening && state.partialSpeechText.isNotBlank()) {
            Text(
                text = "Listening: \"${state.partialSpeechText}\"",
                fontSize = 14.sp,
                color = NeonCyan,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        } else {
            val statusText = when (state.speechState) {
                SpeechState.NotInitialized -> "Offline speech engine uninitialized"
                SpeechState.Initializing -> "Loading speech model (40MB)..."
                SpeechState.Ready -> "Tap microphone to speak offline"
                SpeechState.Listening -> "Listening... speak now"
                is SpeechState.Error -> "Offline speech error: ${state.speechState.message}"
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (state.speechState is SpeechState.Error) MicListeningColor else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Pulse Ring
            if (state.speechState == SpeechState.Listening) {
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(60.dp)
                        .background(
                            MicListeningColor.copy(alpha = 0.25f),
                            CircleShape
                        )
                )
            }

            val isInitializing = state.speechState == SpeechState.Initializing
            val isListening = state.speechState == SpeechState.Listening
            val isReady = state.speechState == SpeechState.Ready

            val buttonColor = when {
                isListening -> MicListeningColor
                isReady -> NeonCyan
                else -> Color(0xFF2E2E38)
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(enabled = isReady || isListening) {
                        onMicClicked()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isInitializing) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone",
                        tint = if (isReady || isListening) Color.White else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    gestures: List<Gesture>,
    currentPlayingIndex: Int,
    isPlaying: Boolean,
    onIndexChanged: (Int) -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,  // Min buffer (5s)
                15000, // Max buffer (15s)
                1500,  // Buffer required to start playback (1.5s)
                2000   // Buffer required after rebuffer (2s)
            )
            .build()
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                volume = 0f // Mute player completely for silent sign language playback
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Set up listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer.currentMediaItemIndex
                if (index != currentPlayingIndex && index in gestures.indices) {
                    onIndexChanged(index)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                onPlayingChanged(playing)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onPlayingChanged(false)
                    onIndexChanged(-1)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Update media items when the gestures list changes
    LaunchedEffect(gestures) {
        if (gestures.isNotEmpty()) {
            val mediaItems = gestures.map { gesture ->
                MediaItem.Builder()
                    .setUri("asset:///${gesture.videoPath}")
                    .build()
            }
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
            if (isPlaying) {
                exoPlayer.seekTo(currentPlayingIndex.coerceAtLeast(0), 0)
                exoPlayer.play()
            }
        } else {
            exoPlayer.clearMediaItems()
        }
    }

    // Handle play/pause changes
    LaunchedEffect(isPlaying) {
        if (isPlaying && gestures.isNotEmpty()) {
            if (!exoPlayer.isPlaying) {
                if (exoPlayer.playbackState == Player.STATE_ENDED || exoPlayer.currentMediaItemIndex != currentPlayingIndex) {
                    exoPlayer.seekTo(currentPlayingIndex.coerceAtLeast(0), 0)
                }
                exoPlayer.play()
            }
        } else {
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
        }
    }

    // Handle index changes from user selection
    LaunchedEffect(currentPlayingIndex) {
        if (currentPlayingIndex in gestures.indices && exoPlayer.currentMediaItemIndex != currentPlayingIndex) {
            exoPlayer.seekTo(currentPlayingIndex, 0)
            if (isPlaying) {
                exoPlayer.play()
            }
        }
    }

    // Render the player view
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true // show play/pause and seek bar controls
            }
        },
        modifier = modifier
    )
}
