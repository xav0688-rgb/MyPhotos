package com.photoviewer.samba.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.photoviewer.samba.data.model.SambaConfig
import com.photoviewer.samba.data.repository.SmbImageRequest
import com.photoviewer.samba.di.AppContainer
import com.photoviewer.samba.ui.ViewerState
import kotlinx.coroutines.launch

@Composable
fun ViewerScreen(
    state: ViewerState,
    config: SambaConfig,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val imageLoader = AppContainer.smbImageLoader
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var isDownloading by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var loadingState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    fun downloadCurrentImage() {
        if (isDownloading) return
        isDownloading = true
        scope.launch {
            val result = AppContainer.sambaRepository.downloadToGallery(
                context, state.current.smbUrl, config, state.current.name
            )
            isDownloading = false
            val message = if (result.isSuccess) "Image téléchargée" else "Échec du téléchargement"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) downloadCurrentImage()
        else Toast.makeText(context, "Permission refusée", Toast.LENGTH_SHORT).show()
    }

    fun onDownloadClick() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            downloadCurrentImage()
        }
    }

    LaunchedEffect(state.currentIndex) { scale = 1f; offset = Offset.Zero }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += panChange * scale
    }

    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(state.currentIndex, scale) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (scale <= 1.05f) {
                            when {
                                dragAccumulated > 120  -> onPrev()
                                dragAccumulated < -120 -> onNext()
                            }
                        }
                        dragAccumulated = 0f
                    },
                    onDragCancel = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDownloadClick()
                    },
                    onTap = { controlsVisible = !controlsVisible }
                )
            }
    ) {
        AsyncImage(
            model = SmbImageRequest(state.current.smbUrl, config),
            contentDescription = state.current.name,
            imageLoader = imageLoader,
            contentScale = if (scale > 1f) ContentScale.None else ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                .transformable(transformableState),
            onState = { loadingState = it }
        )

        when (loadingState) {
            is AsyncImagePainter.State.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            is AsyncImagePainter.State.Error ->
                Text("Impossible de charger l'image", color = Color.White, modifier = Modifier.align(Alignment.Center))
            else -> {}
        }

        // Boutons fermer / télécharger - toujours visibles en haut à droite
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onDownloadClick() },
                    enabled = !isDownloading,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Download, "Télécharger", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Fermer", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = state.current.name, color = Color.White,
                        style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 48.dp)
                    )
                    IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.Close, "Fermer", tint = Color.White)
                    }
                }

                Text(
                    text = "${state.currentIndex + 1} / ${state.images.size}",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                )

                if (state.hasPrev) NavArrowButton(Icons.AutoMirrored.Filled.ArrowBack, "Précédente",
                    Modifier.align(Alignment.CenterStart).padding(start = 8.dp), onPrev)
                if (state.hasNext) NavArrowButton(Icons.AutoMirrored.Filled.ArrowForward, "Suivante",
                    Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), onNext)
            }
        }
    }
}

@Composable
private fun NavArrowButton(icon: ImageVector, description: String, modifier: Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(52.dp).background(Color.Black.copy(alpha = 0.35f), CircleShape)
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
