import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.*

// Each pointer is a 24 bit address
private const val ADDRESS_LENGTH_BYTES = 3

// newline delimited list of words for your T9 dictionary
private const val DICT_FILE = "/dict.txt"

// where the custom library file will be written
private const val OUT_FILE = "library.t9l"

// a-z
private const val ALPHA_COUNT = 26

// helpers to check if a library entry matches our supported alphabet
private val alphabetRegex = Regex("^[a-zA-Z]*\$")
private fun String.isValid() = matches(alphabetRegex)

private const val MAX_LINE_LEN = 25
private fun String.hasValidLength() = length <= MAX_LINE_LEN

sealed class LibraryResult : Throwable() {
    data class Success(val numBytesWritten: Int) : LibraryResult()
    sealed class Error(override val message: String) : LibraryResult() {
        data class LineTooLongError(val line: String) : Error("Error: this file contains a line that is too long: ${line.take(25)}")
        data class InvalidLineError(val line: String) : Error("Error: this file contains a line that contains invalid characters: ${line.take(25)}")
    }
}

fun serializeLibraryFile(input: InputStream, output: OutputStream): LibraryResult {
    val tree = try {
        input
            .bufferedReader()
            .lineSequence()
            // clean each line
            .map { line ->
                if (!line.isValid()) throw LibraryResult.Error.InvalidLineError(line)
                if (!line.hasValidLength()) throw LibraryResult.Error.LineTooLongError(line)
                line.trim().lowercase(Locale.getDefault()).filter { it.isLetter() }
            }
            // insert each word into the Trie
            .fold<String, TrieNode>(NullNode) { acc, s -> acc.insert(s) }
    } catch (err: LibraryResult.Error) {
        return err
    }

    // serialize the tree
    val queue = ArrayDeque<TrieNode>().apply { addFirst(tree) }
    val serializedTreeAddresses = mutableListOf<UInt>()
    var offset = 0u
    while (!queue.isEmpty()) {
        val node = queue.pollFirst()
        val size = node.flattenAddresses(offset, queue, serializedTreeAddresses)
        offset += size
    }

    // convert UInts to 3 byte addresses
    val flatList = serializedTreeAddresses.flatMap { it.toBytes() }
    // write to output
    output.use { it.write(flatList.toByteArray()) }
    return LibraryResult.Success(flatList.size)
}

fun main() {
    val input = {}.javaClass.getResource(DICT_FILE).openStream()
    val output = FileOutputStream(File(OUT_FILE))
    val result = serializeLibraryFile(input, output)
    println(
        when (result) {
            is LibraryResult.Success -> "Wrote to file $OUT_FILE, size: ${result.numBytesWritten} bytes"
            is LibraryResult.Error.LineTooLongError -> "ERROR! Line was too long: ${result.line.take(25)}(...)"
            is LibraryResult.Error.InvalidLineError -> "ERROR! Line contains invalid characters: ${result.line.take(25)}(...)"
        }
    )
}

sealed class TrieNode {
    abstract fun fetch(char: Char): TrieNode

    private fun fetch(string: String): TrieNode {
        if (string.isEmpty()) return this
        return when (val next = fetch(string.first())) {
            is ParentNode -> next.fetch(string.rest)
            is OnlyWordNode -> if (string.length > 1) {
                NullNode
            } else {
                OnlyWordNode
            }
            is NullNode -> NullNode
        }
    }

    abstract fun insert(string: String): TrieNode

    fun contains(string: String): Boolean {
        return when (fetch(string)) {
            is WordParentNode -> true
            is OnlyParentNode -> false
            OnlyWordNode -> true
            NullNode -> false
        }
    }

    open fun flattenAddresses(offset: UInt, queue: Queue<TrieNode>, collector: MutableCollection<UInt>): UInt {
        return 0u
    }

    abstract val trieSize: Int
}

val Char.alphaIndex: Int
    get() {
        val alpha = this - 'a'
        if (alpha < 0) error("unable to insert: $this")
        return alpha
    }

val String.rest: String
    get() = removeRange(0, 1)

/**
 * Base class for nodes that have children
 * In the context of the dictionary trie - a node that represents a partial word
 */
sealed class ParentNode(
    protected val chars: MutableList<TrieNode> = MutableList(ALPHA_COUNT) { NullNode }
) : TrieNode() {

    override fun fetch(char: Char): TrieNode {
        return chars.getOrElse(char.alphaIndex) { NullNode }
    }

    fun insertChar(char: Char) {
        chars[char.alphaIndex] = OnlyWordNode
    }

    abstract val name: String
    abstract val isWord: Boolean

    override fun toString(): String {
        return "$name(\n" +
                chars.joinToString(separator = "\n") { s ->
                    s.toString().lines().joinToString("\n") { "  $it" }
                } +
                "\n)"
    }

    override val trieSize: Int = ALPHA_COUNT + 1

    override fun flattenAddresses(offset: UInt, queue: Queue<TrieNode>, collector: MutableCollection<UInt>): UInt {
        val sizes = chars.scan(0u) { acc, trieNode -> acc + trieNode.trieSize.toUInt() }
        val thisRow = chars.mapIndexed { index, node ->
            return@mapIndexed when (node) {
                is ParentNode -> offset + chars.size.toUInt() + 1u + sizes[index]
                OnlyWordNode -> 0xFFFFFFu
                NullNode -> 0x00u
            }
        }
        queue.addAll(chars)
        collector.add(if (isWord) 1u else 0u)
        collector.addAll(thisRow)
        return sizes.last().toUInt()
    }
}

/**
 * A trie node that represents a valid word, and is also the prefix for at least one other word
 */
class WordParentNode(
    chars: MutableList<TrieNode> = MutableList(ALPHA_COUNT) { NullNode }
) : ParentNode(chars) {
    override fun insert(string: String): TrieNode {
        if (string.isNotEmpty()) {
            val first = string.first()
            chars[first.alphaIndex] = fetch(first).insert(string.rest)
        }
        return this
    }

    override val name: String = "WordParent"

    override val isWord: Boolean = true
}

/**
 * A trie node that represents a prefix for at least one other word
 */
class OnlyParentNode(
    chars: MutableList<TrieNode> = MutableList(ALPHA_COUNT) { NullNode }
) : ParentNode(chars) {
    override fun insert(string: String): TrieNode {
        if (string.isNotEmpty()) {
            val first = string.first()
            chars[first.alphaIndex] = fetch(first).insert(string.rest)
            return this
        }
        return WordParentNode(chars)
    }

    override val name: String = "OnlyParent"

    override val isWord: Boolean = false
}

/**
 * A trie node that represents a valid word that is NOT a prefix for any other words
 */
object OnlyWordNode : TrieNode() {
    override fun fetch(char: Char): TrieNode = NullNode

    override fun insert(string: String): TrieNode {
        if (string.isEmpty()) return OnlyWordNode
        val first = string.first()
        val newParent = WordParentNode()
        if (string.length == 1) {
            newParent.insertChar(first)
        } else {
            newParent.insert(string)
        }
        return newParent
    }

    override fun toString(): String = "ChildNode"

    override val trieSize: Int = 0
}

/**
 * A trie node that is neither a word, or a prefix for a word. A dead end.
 */
object NullNode : TrieNode() {
    override fun fetch(char: Char): TrieNode = NullNode

    override fun insert(string: String): TrieNode {
        if (string.isEmpty()) {
            return OnlyWordNode
        }
        return OnlyParentNode().insert(string)
    }

    override fun toString(): String = "Null"

    override val trieSize: Int = 0
}

/**
 * Convert UInt into a byte array
 */
fun UInt.toBytes(isBigEndian: Boolean = true, wordLength: Int = ADDRESS_LENGTH_BYTES): List<Byte> {
    val bytes = mutableListOf<Byte>()
    var n = this
    if (n == 0x00u) {
        bytes += n.toByte()
    } else {
        while (n != 0x00u) {
            bytes += n.toByte()
            n = n.shr(Byte.SIZE_BITS)
        }
    }

    val paddings = if (bytes.size > wordLength) {
        emptyList()
    } else {
        List<Byte>(wordLength - bytes.size) { 0x00 }
    }
    return paddings + if (isBigEndian) bytes.reversed() else bytes
}

