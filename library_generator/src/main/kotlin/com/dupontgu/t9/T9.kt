package com.dupontgu.t9

import org.apache.commons.text.StringEscapeUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.*

// Each pointer is a 24 bit address
private const val ADDRESS_LENGTH_BYTES = 3

// Each node will have a header with "metadata", 9 bytes long
private const val HEADER_LENGTH_WORDS = 3
private const val HEADER_LENGTH_BYTES = HEADER_LENGTH_WORDS * ADDRESS_LENGTH_BYTES

private fun String.isValid() = all { charMap.containsKey(it.lowercaseChar()) }

private const val MAX_LINE_LEN = 30
private fun String.hasValidLength() = length <= MAX_LINE_LEN

val Char.alphaIndex: Int
    // safe because we should be pre-checking all words for invalid chars
    get() = charMap[this]!!

sealed class LibraryResult : Throwable() {
    data class Success(
        val numBytesWritten: Long,
        val charSet: Set<Char>,
        val skippedWords: Set<String>
    ) : LibraryResult()

    sealed class Error(override val message: String) : LibraryResult() {
        data class LineTooLongError(val line: String) :
            Error("Error: this file contains a line that is too long: ${line.take(25)}")

        data class InvalidLineError(val line: String) :
            Error("Error: this file contains a line that contains invalid characters: ${line.take(25)}.\nSupported characters are:\n${charMap.keys}")
    }
}

fun serializeLibraryFile(input: InputStream, output: OutputStream, removeInvalidLines: Boolean = false): LibraryResult {
    val charSet = mutableSetOf<Char>()
    val skippedWords = mutableSetOf<String>()
    val tree = try {
        input
            .bufferedReader()
            .lineSequence()
            .filter { word ->
                if (!removeInvalidLines) return@filter true
                word.isValid().also { if (!it) skippedWords.add(word) }
            }
            .map { line ->
                if (!line.isValid()) throw LibraryResult.Error.InvalidLineError(line)
                if (!line.hasValidLength()) throw LibraryResult.Error.LineTooLongError(line)
                line.trim().lowercase(Locale.getDefault()).onEach(charSet::add)
            }
            // insert each word into the Trie
            .fold<String, TrieNode>(NullNode) { acc, s -> acc.insert(s) }
    } catch (err: LibraryResult.Error) {
        return err
    }

    val queue = ArrayDeque<TrieNode>().apply { addFirst(tree) }

    var offset = queue.peekFirst().nodeSize.toUInt()
    output.use { stream ->
        while (!queue.isEmpty()) {
            val node = queue.pollFirst()
            val size = node.serializeTo(offset, queue) { stream.write(it.toByteArray()) }
            offset += size
        }
    }

    return LibraryResult.Success((offset.toLong() * ADDRESS_LENGTH_BYTES), charSet, skippedWords)
}

/**
 * The first [HEADER_LENGTH_BYTES] bytes of every node
 * Contains 1 byte indicating whether the node marks the end of a valid word in the library
 * Followed by a bitset, where each bit represents the presence of a child node.
 * The indices of the bits correspond to the value set of the global [charMap].
 */
class NodeHeader(
    private val nodeIsWord: Boolean
) {
    private val flags = BitSet()
    fun serializeTo(output: SerializedOutput) {
        val bytes = MutableList<Byte>(HEADER_LENGTH_BYTES) { 0 }
        bytes[0] = if (nodeIsWord) 1 else 0
        var i = HEADER_LENGTH_BYTES - 1
        flags.toByteArray().forEach { bytes[i--] = it }
        output.write(bytes)
    }

    operator fun set(index: Int, value: Boolean) {
        flags[index] = value
    }
}

fun interface SerializedOutput {
    fun write(bytes: Collection<Byte>)
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
            is OnlyWordNode -> true
            is NullNode -> false
        }
    }

    open fun serializeTo(offset: UInt, queue: Queue<TrieNode>, output: SerializedOutput): UInt {
        return 0u
    }

    abstract val nodeSize: Int
}

val String.rest: String
    get() = removeRange(0, 1)

/**
 * Base class for nodes that have children
 * In the context of the dictionary trie - a node that represents a partial word
 */
sealed class ParentNode(
    protected val chars: MutableList<TrieNode> = MutableList(charMap.size) { NullNode }
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

    private val populatedChars
        get() = chars.count { it !is NullNode }

    override val nodeSize: Int
        get() = populatedChars + HEADER_LENGTH_WORDS

    override fun serializeTo(offset: UInt, queue: Queue<TrieNode>, output: SerializedOutput): UInt {
        val header = NodeHeader(isWord)
        val sizes = chars.scan(0u) { acc, trieNode -> acc + trieNode.nodeSize.toUInt() }
        val thisRow = chars.flatMapIndexed { index, node ->
            val address = when (node) {
                is ParentNode -> (offset + sizes[index])
                is OnlyWordNode -> 0xFFFFFFu
                is NullNode -> null
            }
            header[index] = address != null
            return@flatMapIndexed address?.toBytes().orEmpty()
        }
        queue.addAll(chars)
        header.serializeTo(output)
        output.write(thisRow)
        return sizes.last()
    }
}

/**
 * A trie node that represents a valid word, and is also the prefix for at least one other word
 */
class WordParentNode(
    chars: MutableList<TrieNode> = MutableList(charMap.size) { NullNode }
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
    chars: MutableList<TrieNode> = MutableList(charMap.size) { NullNode }
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
        if (string.isEmpty()) return this
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

    override val nodeSize: Int = 0
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

    override val nodeSize: Int = 0
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

/**
 *  Each language has a key_map file, which maps the T9 buttons (1, 2, 3...) to an array of type-able characters
 *  These maps should only contain the characters relevant to that language. The set of characters is
 *  collected as a part of [serializeLibraryFile] and then passed here.
 *
 *  As a shortcut, we write a valid Python dictionary file that can be imported directly by the T9 FW.
 */
internal fun writeKeyMapFile(libraryResult: LibraryResult.Success, outputFile: File) {
    val languageCharacterSet = libraryResult.charSet + universalCharacters
    val filteredKeyMap = globalKeyMap
        .mapValues { (_, value) -> value.filter { languageCharacterSet.contains(it) } }
        .map { (key, value) ->
            val escapedList = value.joinToString(",") {
                val escapedChar = StringEscapeUtils.escapeEcmaScript(it.toString())
                "\"$escapedChar\""
            }
            "\'$key\':[$escapedList]"
        }
        .let { "key_map = {${it.joinToString(",")}}" }

    outputFile.outputStream().bufferedWriter().use { writer ->
        writer.write(filteredKeyMap)
    }
}