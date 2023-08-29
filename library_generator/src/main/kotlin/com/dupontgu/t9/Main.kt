import com.dupontgu.t9.*
import com.dupontgu.t9.web.circuitPythonBundle
import kotlinx.coroutines.runBlocking
import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

private const val OUT_DIR = "output"
private const val GLOBAL_CHAR_MAP_FILE = "character_map.py"
const val KEY_MAP_FILE = "key_map.py"

/**
 * Downloads a default list of words for each of the supported [Languages],
 * and generates T9 library files and accompanying key map files.
 *
 * Also generates the global character map file, which is used directly by the firmware.
 */
fun main() {
    val outputDir = File(OUT_DIR).apply {
        if (!exists()) {
            mkdir()
        }
    }

    runBlocking {
        val res = circuitPythonBundle(
            kotlin.io.path.createTempFile("1").toFile(),
            kotlin.io.path.createTempFile("2").toFile(),
            KeyboardLayout.EN_US
        )
        println(res)
    }

//    Languages.values().forEach {
//        val languageDir = File(outputDir, it.languageCode).apply {
//            if (!exists()) {
//                mkdir()
//            }
//        }
//        val tempFile = kotlin.io.path.createTempFile("library-${it.languageCode}").toFile()
//        URL(it.librarySourceUrl).downloadFileTo(tempFile)
//        val libraryOutputFile = File(languageDir, "library.t9l2")
//        val result = serializeLibraryFile(
//            tempFile.inputStream(),
//            FileOutputStream(libraryOutputFile),
//            removeInvalidLines = true
//        )
//        when (result) {
//            is LibraryResult.Success -> {
//                println(
//                    """
//                    Wrote to file $libraryOutputFile, size: ${result.numBytesWritten} bytes
//                    ${result.charSet} (${result.charSet.size})
//                    skipped: ${result.skippedWords}
//                    """.trimIndent()
//                )
//                writeKeyMapFile(result, File(languageDir, KEY_MAP_FILE))
//            }
//            is LibraryResult.Error.LineTooLongError -> println("ERROR! Line was too long: ${result.line.take(25)}")
//            is LibraryResult.Error.InvalidLineError -> println(
//                "ERROR! Line contains invalid characters: ${result.line.take(25)}"
//            )
//        }
//        println()
//    }
//
//    /**
//     * Write global character_map Python file, which can be loaded by T9 FW
//     * The character_map is provides the index of each character's flag within a node header's bitset
//     */
//    File(outputDir, GLOBAL_CHAR_MAP_FILE).outputStream().bufferedWriter().use { writer ->
//        val mappings = charMap
//            .map {
//                // take care of any characters that might not get written to a string properly
//                val unescapedKey = StringEscapeUtils.escapeEcmaScript(it.key.toString())
//                "\"$unescapedKey\" : ${it.value}"
//            }
//            .joinToString(",")
//        writer.write("character_map = {$mappings}")
//    }
}

fun URL.downloadFileTo(file: File) {
    openStream().use { inp ->
        BufferedInputStream(inp).use { bis ->
            file.outputStream().use { fos ->
                val data = ByteArray(1024)
                var count: Int
                while (bis.read(data, 0, 1024).also { count = it } != -1) {
                    fos.write(data, 0, count)
                }
            }
        }
    }
}

