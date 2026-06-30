package com.photoviewer.samba.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.photoviewer.samba.data.model.SambaConfig
import com.photoviewer.samba.data.model.SambaItem
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

class SambaRepository {

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "tiff", "tif"
    )

    private fun buildContext(config: SambaConfig): jcifs.CIFSContext {
        val props = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.resolveOrder", "DNS")
            setProperty("jcifs.smb.client.responseTimeout", "10000")
        }
        val base = BaseContext(PropertyConfiguration(props))
        return if (config.username.isNotBlank())
            base.withCredentials(NtlmPasswordAuthenticator(config.username, config.password))
        else
            base.withAnonymousCredentials()
    }

    suspend fun listDirectory(config: SambaConfig, relativePath: String = ""): Result<List<SambaItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = buildContext(config)
                val url = config.toSmbUrl(relativePath)
                val smbFile = SmbFile(url, ctx)
                smbFile.listFiles()
                    ?.mapNotNull { file ->
                        val name = file.name.trim('/')
                        if (name.startsWith('.')) return@mapNotNull null
                        val isDir = file.isDirectory
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (!isDir && ext !in imageExtensions) return@mapNotNull null
                        val childPath = if (relativePath.isEmpty()) name else "$relativePath/$name"
                        SambaItem(name = name, path = childPath, isDirectory = isDir, smbUrl = file.canonicalPath)
                    }
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?: emptyList()
            }
        }

    suspend fun readImageBytes(smbUrl: String, config: SambaConfig): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = buildContext(config)
                SmbFile(smbUrl, ctx).inputStream.use { it.readBytes() }
            }
        }

    // Tente de lire la miniature, retombe sur l'original si elle n'existe pas
    suspend fun readThumbOrOriginal(thumbUrl: String, originalUrl: String, config: SambaConfig): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ctx = buildContext(config)
                val thumb = SmbFile(thumbUrl, ctx)
                if (thumb.exists()) {
                    thumb.inputStream.use { it.readBytes() }
                } else {
                    SmbFile(originalUrl, ctx).inputStream.use { it.readBytes() }
                }
            }
        }

    private fun mimeTypeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "tif", "tiff" -> "image/tiff"
            else -> "image/*"
        }

    // Télécharge l'image plein format dans la galerie (album "MyPhotos")
    suspend fun downloadToGallery(context: Context, smbUrl: String, config: SambaConfig, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = readImageBytes(smbUrl, config).getOrThrow()
                val resolver = context.contentResolver
                val mimeType = mimeTypeFor(fileName)

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyPhotos")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val uri = resolver.insert(collection, values) ?: error("Impossible de créer le fichier image")
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Impossible d'écrire l'image")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }
        }
}
