package io.github.christiandonatti.huginn.shared

/**
 * Files that belong to another file of the same game: the discs of a .m3u playlist, the tracks
 * of a .cue or .gdi. The library shows the playlist or sheet and hides these, so a two-disc
 * game is one game and not three. Names are relative to the folder of [fileName].
 */
fun referencedFiles(fileName: String, content: String): List<String> {
    val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
    return when (fileName.substringAfterLast('.').lowercase()) {
        "m3u" -> lines.filterNot { it.startsWith("#") }.toList()
        "cue" -> lines.mapNotNull { line ->
            if (!line.startsWith("FILE", ignoreCase = true)) return@mapNotNull null
            val rest = line.substring(4).trim()
            if (rest.startsWith("\"")) rest.drop(1).substringBefore('"')
            else rest.substringBeforeLast(' ').trim().ifEmpty { null }
        }.toList()
        "gdi" -> lines.drop(1).mapNotNull { line ->
            val m = Regex("""^\d+\s+\d+\s+\d+\s+\d+\s+(".*?"|\S+)""").find(line) ?: return@mapNotNull null
            m.groupValues[1].trim('"')
        }.toList()
        else -> emptyList()
    }
}

/** Extensions whose files can list other files ([referencedFiles]). */
val listingExtensions = setOf("m3u", "cue", "gdi")
