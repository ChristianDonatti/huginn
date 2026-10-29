package io.github.christiandonatti.huginn.shared

private val tagGroup = Regex("""\s*[(\[][^)\]]*[)\]]""")
private val spaces = Regex("""\s+""")
private val movedArticle = Regex("""^(.+?), (The|A|An)(\s+-\s+.*|:.*)?$""", RegexOption.IGNORE_CASE)
private val extension = Regex("""\.[A-Za-z0-9]{1,6}$""")

/** File name without its extension ("Game (USA).gba" becomes "Game (USA)"). */
fun stripExtension(fileName: String): String {
    val base = fileName.substringAfterLast('/')
    val m = extension.find(base) ?: return base
    return if (m.range.first == 0) base else base.substring(0, m.range.first)
}

/**
 * A readable title from a ROM file name: no extension, no (region) or [dump] tags, underscores
 * as spaces, and "Legend of Zelda, The - X" back to "The Legend of Zelda - X".
 */
fun cleanTitle(fileName: String): String {
    val base = stripExtension(fileName).replace('_', ' ')
    var title = tagGroup.replace(base, "").replace(spaces, " ").trim()
    movedArticle.matchEntire(title)?.let { m ->
        title = m.groupValues[2] + " " + m.groupValues[1] + m.groupValues[3]
    }
    return title.ifBlank { base.trim() }
}

/** The text inside every (...) and [...] of a file name, in order. */
fun titleTags(fileName: String): List<String> =
    tagGroup.findAll(stripExtension(fileName)).map { it.value.trim().drop(1).dropLast(1).trim() }.toList()

private val regions = listOf(
    "USA", "Europe", "Japan", "World", "Brazil", "Korea", "China", "Asia", "Australia",
    "France", "Germany", "Spain", "Italy", "Netherlands", "Sweden", "Canada", "UK", "Taiwan", "Russia",
    "U", "E", "J", "W", "B", "K",
)

private val shortRegions = mapOf("U" to "USA", "E" to "Europe", "J" to "Japan", "W" to "World", "B" to "Brazil", "K" to "Korea")

/** The region of a ROM ("USA", "Europe, Brazil"...), from No-Intro or GoodTools style tags. */
fun regionOf(fileName: String): String? {
    for (tag in titleTags(fileName)) {
        val parts = tag.split(',').map { it.trim() }
        if (parts.isNotEmpty() && parts.all { it in regions }) {
            return parts.joinToString(", ") { shortRegions[it] ?: it }
        }
    }
    return null
}

/** Key for alphabetical order that ignores case and a leading "The". */
fun sortKey(title: String): String {
    val t = title.trim().lowercase()
    return if (t.startsWith("the ")) t.removePrefix("the ") else t
}
