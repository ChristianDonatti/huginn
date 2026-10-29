package io.github.christiandonatti.huginn.shared

/** Internal storage of the main Android user. */
const val PRIMARY_STORAGE = "/storage/emulated/0"

private const val SAF_DOCUMENT = "content://com.android.externalstorage.documents/document/"

/** Removes duplicated and trailing slashes and maps /sdcard to [primaryRoot]. */
fun normalizePath(path: String, primaryRoot: String = PRIMARY_STORAGE): String {
    var p = path.trim().replace(Regex("/+"), "/")
    if (p.length > 1) p = p.trimEnd('/')
    for (alias in listOf("/sdcard", "/storage/self/primary", "/mnt/sdcard")) {
        if (p == alias || p.startsWith("$alias/")) p = primaryRoot + p.removePrefix(alias)
    }
    return p
}

/**
 * The Storage Access Framework document id of a file: "primary:ROMs/gba/game.gba" for internal
 * storage, "1A2B-3C4D:ROMs/game.gba" for an SD card. Null outside shared storage.
 */
fun safDocumentId(path: String, primaryRoot: String = PRIMARY_STORAGE): String? {
    val p = normalizePath(path, primaryRoot)
    if (p == primaryRoot) return "primary:"
    if (p.startsWith("$primaryRoot/")) return "primary:" + p.removePrefix("$primaryRoot/")
    if (!p.startsWith("/storage/")) return null
    val rest = p.removePrefix("/storage/")
    val volume = rest.substringBefore('/')
    if (volume.isEmpty() || volume == "emulated" || volume == "self") return null
    return "$volume:" + rest.substringAfter('/', "")
}

/** The opposite of [safDocumentId]: a folder picked in the Android file picker as a path. */
fun pathFromDocumentId(documentId: String, primaryRoot: String = PRIMARY_STORAGE): String? {
    val colon = documentId.indexOf(':')
    if (colon < 0) return null
    val volume = documentId.substring(0, colon)
    val rest = documentId.substring(colon + 1).trim('/')
    val base = when (volume) {
        "primary" -> primaryRoot
        "home" -> "$primaryRoot/Documents"
        "" -> return null
        else -> "/storage/$volume"
    }
    return if (rest.isEmpty()) base else "$base/$rest"
}

/** The %ROMSAF% value for a file: a document URI of the external storage provider. */
fun safDocumentUri(path: String, primaryRoot: String = PRIMARY_STORAGE): String? =
    safDocumentId(path, primaryRoot)?.let { SAF_DOCUMENT + percentEncode(it) }

/** Percent-encodes everything but unreserved characters, like Android's Uri.encode. */
fun percentEncode(text: String): String {
    val sb = StringBuilder()
    for (b in text.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        val ch = c.toChar()
        if (c < 0x80 && (ch.isLetterOrDigit() || ch in "-_.!~*'()")) sb.append(ch)
        else sb.append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 0xF])
    }
    return sb.toString()
}
