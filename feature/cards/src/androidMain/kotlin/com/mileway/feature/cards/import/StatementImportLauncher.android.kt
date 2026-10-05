package com.mileway.feature.cards.import

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Storage Access Framework reads run off the main thread with the parser's input-size limit. */
@Composable
internal actual fun rememberStatementImportLauncher(
    onPicked: (String, String) -> Unit,
    onError: () -> Unit,
): (() -> Unit)? {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    try {
                        val picked =
                            withContext(Dispatchers.IO) {
                                val resolver = context.contentResolver
                                val name =
                                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                                        if (it.moveToFirst()) it.getString(0) else null
                                    } ?: "statement"
                                val text =
                                    requireNotNull(resolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { reader ->
                                        val value = StringBuilder()
                                        val buffer = CharArray(8192)
                                        var count = reader.read(buffer)
                                        while (count != -1) {
                                            require(value.length + count <= StatementParser.MaxCharacters)
                                            value.append(buffer, 0, count)
                                            count = reader.read(buffer)
                                        }
                                        value.toString()
                                    }
                                name to text
                            }
                        onPicked(picked.first, picked.second)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        onError()
                    }
                }
            }
        }
    return remember(launcher) { { launcher.launch("*/*") } }
}
