package com.photoviewer.samba.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.photoviewer.samba.data.model.SambaConfig
import com.photoviewer.samba.data.model.SambaItem
import com.photoviewer.samba.data.repository.SmbImageRequest
import com.photoviewer.samba.di.AppContainer
import com.photoviewer.samba.ui.BrowseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    browseState: BrowseState,
    config: SambaConfig,
    navStack: List<String>,
    onOpenDirectory: (SambaItem) -> Unit,
    onOpenPhoto: (List<SambaItem>, Int) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onLoadRoot: () -> Unit
) {
    val imageLoader = AppContainer.smbImageLoader
    val title = if (navStack.isEmpty()) "Photos" else navStack.last().substringAfterLast('/')

    LaunchedEffect(Unit) {
        if (browseState is BrowseState.Idle) onLoadRoot()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (navStack.isNotEmpty()) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualiser") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Paramètres") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (browseState) {
                is BrowseState.Idle, is BrowseState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is BrowseState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                    Text(browseState.message, textAlign = TextAlign.Center)
                    Button(onClick = onOpenSettings) { Text("Ouvrir les paramètres") }
                }

                is BrowseState.Success -> {
                    val dirs   = browseState.items.filter { it.isDirectory }
                    val images = browseState.items.filter { !it.isDirectory }

                    if (browseState.items.isEmpty()) {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FolderOff, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Spacer(Modifier.height(12.dp))
                            Text("Dossier vide", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(3), contentPadding = PaddingValues(4.dp), modifier = Modifier.fillMaxSize()) {
                            if (dirs.isNotEmpty()) {
                                item(span = { GridItemSpan(3) }) {
                                    Text("Dossiers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                                }
                                items(dirs, span = { GridItemSpan(3) }, key = { it.path }) { dir ->
                                    FolderRow(dir) { onOpenDirectory(dir) }
                                }
                            }
                            if (images.isNotEmpty()) {
                                item(span = { GridItemSpan(3) }) {
                                    Text("${images.size} photo${if (images.size > 1) "s" else ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                                }
                                itemsIndexed(images, key = { _, item -> item.path }) { index, image ->
                                    PhotoThumbnail(image, config, imageLoader) { onOpenPhoto(images, index) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(item: SambaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(16.dp))
        Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
}

@Composable
private fun PhotoThumbnail(
    item: SambaItem,
    config: SambaConfig,
    imageLoader: coil.ImageLoader,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.padding(2.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        var state by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(SmbImageRequest(
                    smbUrl = item.smbUrl,
                    config = config,
                    thumbUrl = item.thumbUrl,
                    useThumbnail = true
                ))
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state = it }
        )
        when (state) {
            is AsyncImagePainter.State.Loading ->
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            is AsyncImagePainter.State.Error ->
                Icon(Icons.Default.BrokenImage, null, tint = Color.Gray)
            else -> {}
        }
    }
}
