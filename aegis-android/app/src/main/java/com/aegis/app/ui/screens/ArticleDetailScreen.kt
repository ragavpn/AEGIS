package com.aegis.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.aegis.app.ui.viewmodel.ArticleDetailState
import com.aegis.app.ui.viewmodel.ArticleInteractionViewModel
import com.aegis.app.ui.viewmodel.DashboardViewModel
import com.aegis.app.ui.viewmodel.PodcastState
import com.aegis.app.ui.viewmodel.PodcastViewModel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import com.aegis.app.ui.components.AudioPlayer

// ── Same palette as Dashboard ─────────────────────────────────────────────────
private val BgDark      = Color(0xFF0A0E1A)
private val SrfDark     = Color(0xFF111827)
private val AccBlue     = Color(0xFF3B82F6)
private val AccGold     = Color(0xFFF59E0B)
private val AccRed      = Color(0xFFEF4444)
private val TxtPrimary  = Color(0xFFF1F5F9)
private val TxtSecond   = Color(0xFF94A3B8)
private val TxtBody     = Color(0xFFCBD5E1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    onBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
    interactionViewModel: ArticleInteractionViewModel = hiltViewModel(),
    podcastViewModel: PodcastViewModel = hiltViewModel()
) {
    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
        interactionViewModel.load(articleId)
    }

    val state by viewModel.detailState.collectAsState()
    val interaction by interactionViewModel.interaction.collectAsState()
    val podcastState by podcastViewModel.state.collectAsState()

    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    var isPlaying by remember { mutableStateOf(false) }

    // ── Shared scroll state: used for verticalScroll AND scroll-depth calculation ──
    val scrollState = rememberScrollState()

    // Read-time tracking: capture start time once
    val readStartMs = remember { System.currentTimeMillis() }

    // On leaving the screen: record read duration AND scroll depth (PLAN.md §7.3)
    DisposableEffect(articleId) {
        onDispose {
            val durationSeconds = ((System.currentTimeMillis() - readStartMs) / 1000).toInt()
            if (durationSeconds > 2) {
                interactionViewModel.recordReadDuration(articleId, durationSeconds)
            }

            // Compute scroll depth as a percentage of how far down the content the user scrolled
            val maxScroll = scrollState.maxValue.takeIf { it > 0 } ?: 1
            val depthPercent = ((scrollState.value.toFloat() / maxScroll) * 100).toInt()
                .coerceIn(0, 100)
            if (depthPercent > 0) {
                interactionViewModel.recordScrollDepth(articleId, depthPercent)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(podcastState) {
        if (podcastState is PodcastState.Ready) {
            val url = (podcastState as PodcastState.Ready).audioUrl
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.play()
            isPlaying = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AEGIS",
                        fontWeight = FontWeight.ExtraBold,
                        color = TxtPrimary,
                        letterSpacing = 4.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AccBlue
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { interactionViewModel.toggleLike(articleId) }) {
                        Icon(
                            if (interaction?.liked == true) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (interaction?.liked == true) AccRed else TxtSecond
                        )
                    }
                    IconButton(onClick = { interactionViewModel.toggleBookmark(articleId) }) {
                        Icon(
                            if (interaction?.bookmarked == true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (interaction?.bookmarked == true) AccGold else TxtSecond
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SrfDark)
            )
        },
        floatingActionButton = {},
        containerColor = BgDark
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                is ArticleDetailState.Idle, is ArticleDetailState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AccBlue
                    )
                }
                is ArticleDetailState.Error -> {
                    Text(
                        s.message,
                        color = AccRed,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is ArticleDetailState.Success -> {
                    val article = s.article
                    val tColor = when {
                        article.modules.any { it.contains("FLASH", true) }    -> AccRed
                        article.modules.any { it.contains("PRIORITY", true) } -> AccGold
                        else -> AccBlue
                    }
                    val tLabel = when {
                        article.modules.any { it.contains("FLASH", true) }    -> "FLASH"
                        article.modules.any { it.contains("PRIORITY", true) } -> "PRIORITY"
                        else -> "ROUTINE"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)   // uses shared scrollState for depth tracking
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Tier badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tColor.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                tLabel,
                                color = tColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Title
                        Text(
                            article.title,
                            color = TxtPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 30.sp
                        )

                        Spacer(Modifier.height(16.dp))

                        if (podcastState is PodcastState.Ready) {
                            AudioPlayer(
                                isPlaying = isPlaying,
                                onPlayPauseToggle = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    isPlaying = !isPlaying
                                }
                            )
                        } else {
                            Button(
                                onClick = { podcastViewModel.generatePodcast(articleId) },
                                enabled = podcastState !is PodcastState.Generating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccBlue,
                                    disabledContainerColor = AccBlue.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (podcastState is PodcastState.Generating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generating...")
                                } else {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate Podcast")
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Summary card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2235))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "SUMMARY",
                                    color = AccBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    article.summary,
                                    color = TxtBody,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Full body
                        Text(
                            "FULL INTELLIGENCE REPORT",
                            color = TxtSecond,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            article.body,
                            color = TxtBody,
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )

                        // Module chips
                        if (article.modules.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "MODULES",
                                color = TxtSecond,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                article.modules.forEach { mod ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E3A5F))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(mod, color = AccBlue, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Sources
                        if (article.sources.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "SOURCES",
                                color = TxtSecond,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            article.sources.forEachIndexed { idx, src ->
                                Text(
                                    "${idx + 1}. ${src.title ?: src.url ?: "Unknown"}",
                                    color = AccBlue,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}
