package com.photoviewer.samba.data.model

data class SambaConfig(
    val serverIp: String = "",
    val shareName: String = "",
    val username: String = "",
    val password: String = ""
) {
    fun isValid() = serverIp.isNotBlank() && shareName.isNotBlank()

    fun toSmbUrl(path: String = ""): String {
        val cleanShare = shareName.trim('/')
        val cleanPath = path.trim('/')
        return if (cleanPath.isEmpty()) "smb://$serverIp/$cleanShare/"
               else "smb://$serverIp/$cleanShare/$cleanPath/"
    }
}

data class SambaItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val smbUrl: String
) {
    // URL de la miniature : même dossier + .thumbs/nom_fichier
    val thumbUrl: String get() {
        val lastSlash = smbUrl.trimEnd('/').lastIndexOf('/')
        return if (lastSlash < 0) smbUrl
        else {
            val dir  = smbUrl.trimEnd('/').substring(0, lastSlash)
            val file = smbUrl.trimEnd('/').substring(lastSlash + 1)
            "$dir/.thumbs/$file"
        }
    }
}
