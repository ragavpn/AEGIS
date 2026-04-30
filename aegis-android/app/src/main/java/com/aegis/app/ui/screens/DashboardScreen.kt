package com.aegis.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import com.aegis.app.data.model.Article
import com.aegis.app.ui.viewmodel.ArticleListState
import com.aegis.app.ui.viewmodel.DashboardViewModel
import com.aegis.app.ui.viewmodel.PodcastState
import com.aegis.app.ui.viewmodel.PodcastViewModel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Colour palette ────────────────────────────────────────────────────────────
private val BackgroundDark = Color(0xFF0A0E1A)
private val SurfaceDark    = Color(0xFF111827)
private val CardSurface    = Color(0xFF1A2235)
private val AccentBlue     = Color(0xFF3B82F6)
private val AccentGold     = Color(0xFFF59E0B)
private val AccentRed      = Color(0xFFEF4444)
private val TextPrimary    = Color(0xFFF1F5F9)
private val TextSecondary  = Color(0xFF94A3B8)
private val ShimmerBase    = Color(0xFF1A2235)
private val ShimmerHigh    = Color(0xFF243044)

// ── Tier helpers ──────────────────────────────────────────────────────────────
private fun tierColor(modules: List<String>): Color = when {
    modules.any { it.contains("FLASH", ignoreCase = true) }    -> AccentRed
    modules.any { it.contains("PRIORITY", ignoreCase = true) } -> AccentGold
    else                                                        -> AccentBlue
}

private fun tierLabel(modules: List<String>): String = when {
    modules.any { it.contains("FLASH", ignoreCase = true) }    -> "FLASH"
    modules.any { it.contains("PRIORITY", ignoreCase = true) } -> "PRIORITY"
    else                                                        -> "ROUTINE"
}

private fun formatDate(iso: String?): String {
    if (iso == null) return ""
    return try {
        val zdt = ZonedDateTime.parse(iso)
        zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", Locale.US))
    } catch (e: Exception) { iso }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onArticleClick: (String) -> Unit = {},
    onDailyBriefingClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
    podcastViewModel: PodcastViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    val digestState by podcastViewModel.digestState.collectAsState()
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    // Sync isRefreshing with VM state so the PTR indicator dismisses correctly
    LaunchedEffect(state) {
        if (state !is ArticleListState.Loading) isRefreshing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AEGIS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = TextPrimary,
                            letterSpacing = 4.sp
                        )
                        Text(
                            "Intelligence Feed",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onDailyBriefingClick) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Daily Briefing", tint = AccentGold)
                    }
                    // Manual refresh (GET only, no generate) for the toolbar button
                    IconButton(onClick = { viewModel.loadArticles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.triggerGenerateAndRefresh()
            },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val s = state) {
                is ArticleListState.Loading -> {
                    // ── Shimmer skeleton (PLAN.md §7.2: "NOT a full-screen spinner") ──
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(6) { ShimmerArticleCard() }
                    }
                }

                is ArticleListState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = AccentRed, modifier = Modifier.padding(16.dp))
                            Button(
                                onClick = { viewModel.loadArticles() },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                            ) { Text("Retry") }
                        }
                    }
                }

                is ArticleListState.Success -> {
                    if (s.articles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No articles yet. Pull down to generate.", color = TextSecondary)
                        }
                    } else {
                        // Split articles: first 3 go to the horizontal "Curated" row, rest to the list
                        val curated = s.articles.take(3)
                        val feed    = s.articles.drop(3)

                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ── Daily Digest Podcast Banner ───────────────────────
                            item(key = "daily_digest_banner") {
                                DailyDigestBanner(
                                    state = digestState,
                                    onGenerate = { scale -> podcastViewModel.generateDailyDigest(scale) },
                                    onPlay = { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── "Curated For You" horizontal row ─────────────────
                            item(key = "curated_header") {
                                Text(
                                    "CURATED FOR YOU",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(curated, key = { "curated_${it.id}" }) { article ->
                                        CuratedArticleCard(
                                            article = article,
                                            onClick = { onArticleClick(article.id) }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                // Divider label for the vertical feed below
                                Text(
                                    "LATEST INTELLIGENCE",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                            }

                            // ── Vertical article feed ────────────────────────────
                            items(feed, key = { it.id }) { article ->
                                ArticleCard(
                                    article = article,
                                    onClick = { onArticleClick(article.id) },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Daily Digest Podcast Banner ───────────────────────────────────────────────
@Composable
fun DailyDigestBanner(
    state: PodcastState,
    onGenerate: (String) -> Unit,
    onPlay: (String) -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1A1040), Color(0xFF0D1B3E))
    )
    val durations = listOf("short" to "Short", "default" to "Default", "long" to "Long")
    var selectedScale by remember { mutableStateOf("default") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎙", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "AEGIS DAILY BRIEF",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp,
                            color = Color(0xFFF59E0B)
                        )
                        Text(
                            "Last 24 hours • Intelligence Digest",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Duration selector chips — only show when Idle or Error
                if (state is PodcastState.Idle || state is PodcastState.Error) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        durations.forEach { (value, label) ->
                            val isSelected = selectedScale == value
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF1E2D45),
                                modifier = Modifier
                                    .clickable { selectedScale = value }
                                    .weight(1f)
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0A0E1A) else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (state) {
                    is PodcastState.Idle -> {
                        Button(
                            onClick = { onGenerate(selectedScale) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF0A0E1A)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Daily Digest", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    is PodcastState.Generating -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFF59E0B),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Generating your briefing…",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                        }
                    }
                    is PodcastState.Ready -> {
                        Button(
                            onClick = { onPlay(state.audioUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play Daily Brief", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    is PodcastState.Error -> {
                        Text(
                            "⚠ ${state.message}",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        TextButton(onClick = { onGenerate(selectedScale) }) {
                            Text("Retry", color = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }
    }
}

// ── Shimmer skeleton card (replaces spinner during initial load) ───────────────
@Composable
private fun ShimmerArticleCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHigh, ShimmerBase),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tier badge placeholder
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(12.dp))
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(10.dp))
            // Summary placeholder (2 lines)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

// ── Curated article card (horizontal, fixed width) ────────────────────────────
@Composable
private fun CuratedArticleCard(article: Article, onClick: () -> Unit) {
    val tColor = tierColor(article.modules)
    val tLabel = tierLabel(article.modules)

    Card(
        modifier = Modifier
            .width(240.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Tier badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    tLabel,
                    color = tColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                article.title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            Text(
                article.summary,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Accent line at bottom
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(tColor.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

// ── Main vertical article card ────────────────────────────────────────────────
@Composable
private fun ArticleCard(article: Article, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tColor = tierColor(article.modules)
    val tLabel = tierLabel(article.modules)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Tier badge + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        tLabel,
                        color = tColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
                Text(formatDate(article.createdAt), color = TextSecondary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Title
            Text(
                article.title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Summary (2-line preview)
            Text(
                article.summary,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Module chips
            if (article.modules.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    article.modules.take(3).forEach { mod ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E3A5F))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(mod, color = AccentBlue, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Accent divider
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(tColor.copy(alpha = 0.6f), Color.Transparent)
                        )
                    )
            )
        }
    }
}
