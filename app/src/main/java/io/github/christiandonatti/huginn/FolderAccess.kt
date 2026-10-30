package io.github.christiandonatti.huginn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.christiandonatti.huginn.shared.pathFromDocumentId
import io.github.christiandonatti.huginn.shared.safDocumentId

/**
 * Folders the person released to Huginn through Android's folder picker. Like Cocoon and
 * Daijisho, Huginn then hands emulators a document URI inside that folder together with the
 * permission to read it, so the emulator does not need its own access to the ROM folder.
 */
object FolderAccess {
    private const val AUTHORITY = "com.android.externalstorage.documents"

    /** Goes up when a folder is released, so the screens check again. */
    var version by mutableStateOf(0)
        private set

    /** Released folders as (path, tree URI). */
    fun trees(context: Context): List<Pair<String, Uri>> =
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.uri.authority == AUTHORITY }
            .mapNotNull { perm ->
                val id = runCatching { DocumentsContract.getTreeDocumentId(perm.uri) }.getOrNull() ?: return@mapNotNull null
                pathFromDocumentId(id)?.let { it to perm.uri }
            }

    /** The broadest released folder that holds [path], or null. */
    fun treeFor(context: Context, path: String): Pair<String, Uri>? =
        trees(context).filter { (root, _) -> path == root || path.startsWith("$root/") }.minByOrNull { it.first.length }

    fun isReleased(context: Context, path: String): Boolean = treeFor(context, path) != null

    /** content://…/tree/<folder>/document/<file>: the form emulators get from Cocoon and Daijisho. */
    fun documentUri(tree: Uri, path: String): Uri? =
        safDocumentId(path)?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it) }

    /** Where the folder picker should open for a path. */
    fun initialUri(path: String): Uri? = safDocumentId(path)?.let { DocumentsContract.buildDocumentUri(AUTHORITY, it) }

    /** Keeps the permission for a picked folder across restarts; returns its path. */
    fun onPicked(context: Context, tree: Uri): String? {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure {
            runCatching { context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        version++
        return runCatching { pathFromDocumentId(DocumentsContract.getTreeDocumentId(tree)) }.getOrNull()
    }
}
