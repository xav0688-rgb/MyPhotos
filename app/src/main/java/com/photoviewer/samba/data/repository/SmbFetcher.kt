package com.photoviewer.samba.data.repository

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.photoviewer.samba.data.model.SambaConfig
import okio.Buffer

// useThumbnail=true pour les vignettes, false pour la visionneuse plein écran
data class SmbImageRequest(
    val smbUrl: String,
    val config: SambaConfig,
    val thumbUrl: String = "",
    val useThumbnail: Boolean = false
)

class SmbFetcher(
    private val data: SmbImageRequest,
    private val options: Options,
    private val repository: SambaRepository
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val bytes = if (data.useThumbnail && data.thumbUrl.isNotBlank()) {
            // Essaie la miniature, retombe sur l'original si absente
            repository.readThumbOrOriginal(data.thumbUrl, data.smbUrl, data.config).getOrThrow()
        } else {
            repository.readImageBytes(data.smbUrl, data.config).getOrThrow()
        }
        val buffer = Buffer().write(bytes)
        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = null,
            dataSource = DataSource.NETWORK
        )
    }

    class Factory(private val repository: SambaRepository) : Fetcher.Factory<SmbImageRequest> {
        override fun create(data: SmbImageRequest, options: Options, imageLoader: ImageLoader): Fetcher =
            SmbFetcher(data, options, repository)
    }
}

fun buildSmbImageLoader(context: Context, repository: SambaRepository): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(SmbFetcher.Factory(repository)) }
        .memoryCache {
            coil.memory.MemoryCache.Builder(context)
                .maxSizePercent(0.25) // 25% de la RAM pour le cache mémoire
                .build()
        }
        .diskCache {
            coil.disk.DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizeBytes(512L * 1024 * 1024) // 512 MB sur disque
                .build()
        }
        .crossfade(true)
        .build()
