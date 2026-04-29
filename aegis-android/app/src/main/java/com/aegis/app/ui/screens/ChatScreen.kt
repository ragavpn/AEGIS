package com.aegis.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aegis.app.ui.viewmodel.ChatUiMessage
import com.aegis.app.ui.viewmodel.ChatViewModel
import com.aegis.app.utils.VoiceInputHandler
import com.aegis.app.utils.VoiceOutputHandler

// ── Colour palette ────────────────────────────────────────────────────────────
private val BgDark     = Color(0xFF0A0E1A)
private val SrfDark    = Color(0xFF111827)
private val CardSrf    = Color(0xFF1A2235)
private val UserBubble = Color(0xFF1D4ED8)
private val BotBubble  = Color(0xFF1A2235)
private val ErrBubble  = Color(0xFF450A0A)
private val AccBlue    = Color(0xFF3B82F6)
private val AccRed     = Color(0xFFEF4444)
private val TxtPrimary = Color(0xFFF1F5F9)
private val TxtSecond  = Color(0xFF94A3B8)
private val InputSrf   = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    isBriefingMode: Boolean = false
) {
    val messages        by viewModel.messages.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val vmError         by viewModel.error.collectAsState()
    var inputText       by remember { mutableStateOf("") }
    val listState       = rememberLazyListState()
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val context          = LocalContext.current
    val voiceInputHandler  = remember { VoiceInputHandler(context) }
    val voiceOutputHandler = remember { VoiceOutputHandler(context) }
    var isVoiceMode      by remember { mutableStateOf(false) }
    val isListening      by voiceInputHandler.isListening.collectAsState()
    val partialText      by voiceInputHandler.partialText.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceInputHandler.startListening(
                onResult = { text -> viewModel.sendMessage(text) },
                onError  = { /* no-op */ }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceInputHandler.destroy()
            voiceOutputHandler.destroy()
        }
    }

    LaunchedEffect(isBriefingMode) {
        if (isBriefingMode && messages.isEmpty()) {
            isVoiceMode = true
            viewModel.sendMessage("Give me today's briefing")
        }
    }

    // Auto-scroll to newest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            val last = messages.last()
            if (last.role == "assistant" && isVoiceMode && !last.isLoading) {
                voiceOutputHandler.speak(last.content)
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    if (!isListening) {
                        voiceInputHandler.startListening(
                            onResult = { text -> viewModel.sendMessage(text) },
                            onError  = {}
                        )
                    }
                }
            }
        }
    }

    // Stop TTS when user starts speaking again
    LaunchedEffect(partialText) {
        if (partialText.isNotBlank()) voiceOutputHandler.stop()
    }

    // Show ViewModel errors as snackbars (network failures, clear-history errors, etc.)
    LaunchedEffect(vmError) {
        vmError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ── Clear history confirmation dialog ─────────────────────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History", color = TxtPrimary, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "This will permanently delete all your conversations from the server. This cannot be undone.",
                    color = TxtSecond,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearHistory()
                }) { Text("Delete", color = AccRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TxtSecond)
                }
            },
            containerColor = CardSrf
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AEGIS Analyst",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 20.sp,
                            color      = TxtPrimary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "RAG-powered intelligence",
                            fontSize = 11.sp,
                            color    = TxtSecond
                        )
                    }
                },
                actions = {
                    // Voice toggle
                    IconButton(onClick = {
                        isVoiceMode = !isVoiceMode
                        if (!isVoiceMode) voiceOutputHandler.stop()
                    }) {
                        Icon(
                            imageVector   = if (isVoiceMode) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle voice mode",
                            tint          = if (isVoiceMode) AccBlue else TxtSecond
                        )
                    }
                    // Clear history — only visible when there are messages
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear history",
                                tint = TxtSecond
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SrfDark)
            )
        },
        bottomBar = {
            ChatInputBar(
                value         = if (isListening) partialText else inputText,
                isLoading     = isLoading,
                isListening   = isListening,
                onValueChange = { if (!isListening) inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                onMicClick = {
                    if (isListening) {
                        voiceInputHandler.stopListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
        },
        containerColor = BgDark
    ) { padding ->
        if (messages.isEmpty()) {
            // ── Empty state ───────────────────────────────────────────────────
            Box(
                modifier        = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚡", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Ask Aegis anything",
                        color      = TxtPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Geopolitical analysis, financial signals,\ncausal intelligence — all at your fingertips.",
                        color      = TxtSecond,
                        fontSize   = 14.sp,
                        lineHeight = 20.sp,
                        textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    SuggestionChips(onSelect = { viewModel.sendMessage(it) })
                }
            }
        } else {
            LazyColumn(
                state           = listState,
                contentPadding  = PaddingValues(
                    top    = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 12.dp,
                    start  = 16.dp,
                    end    = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg -> ChatBubble(msg) }
            }
        }
    }
}

// ── Suggestion chips ──────────────────────────────────────────────────────────
@Composable
private fun SuggestionChips(onSelect: (String) -> Unit) {
    val suggestions = listOf(
        "What are the latest geopolitical risks?",
        "Summarize recent energy market signals",
        "What is the Russia-Ukraine impact on grain prices?"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { suggestion ->
            OutlinedButton(
                onClick = { onSelect(suggestion) },
                shape   = RoundedCornerShape(12.dp),
                border  = BorderStroke(1.dp, AccBlue.copy(alpha = 0.5f)),
                colors  = ButtonDefaults.outlinedButtonColors(contentColor = AccBlue)
            ) {
                Text(suggestion, fontSize = 13.sp)
            }
        }
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(msg: ChatUiMessage) {
    val isUser  = msg.role == "user"
    val isError = msg.role == "error"

    Row(
        modifier           = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier        = Modifier.size(32.dp).clip(CircleShape).background(AccBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = AccBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart    = if (isUser) 18.dp else 4.dp,
                        topEnd      = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd   = 18.dp
                    )
                )
                .background(
                    when {
                        isUser  -> UserBubble
                        isError -> ErrBubble
                        else    -> BotBubble
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (msg.isLoading) {
                LoadingDots()
            } else {
                Text(
                    msg.content,
                    color      = if (isError) AccRed else TxtPrimary,
                    fontSize   = 14.sp,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

// ── Animated loading dots ─────────────────────────────────────────────────────
@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { idx ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(durationMillis = 500, delayMillis = idx * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$idx"
            )
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(TxtSecond.copy(alpha = alpha))
            )
        }
    }
}

// ── Chat input bar ────────────────────────────────────────────────────────────
@Composable
private fun ChatInputBar(
    value: String,
    isLoading: Boolean,
    isListening: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Surface(color = SrfDark, tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment      = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                modifier      = Modifier.weight(1f),
                placeholder   = { Text("Ask the analyst...", color = TxtSecond, fontSize = 14.sp) },
                shape         = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor     = AccBlue,
                    unfocusedBorderColor   = Color(0xFF334155),
                    focusedTextColor       = TxtPrimary,
                    unfocusedTextColor     = TxtPrimary,
                    cursorColor            = AccBlue,
                    unfocusedContainerColor = InputSrf,
                    focusedContainerColor  = InputSrf
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines        = 4,
                singleLine      = false
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if ((value.isBlank() && !isListening) || isLoading)
                            Color(0xFF1E293B) else AccBlue
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading   -> CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = TxtSecond,
                        strokeWidth = 2.dp
                    )
                    value.isBlank() && !isListening -> IconButton(onClick = onMicClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = TxtSecond)
                    }
                    isListening -> IconButton(onClick = onMicClick) {
                        Icon(Icons.Default.Mic, contentDescription = "Stop listening", tint = AccRed)
                    }
                    else -> IconButton(onClick = onSend, enabled = value.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}
