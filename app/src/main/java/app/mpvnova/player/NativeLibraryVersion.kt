package app.mpvnova.player

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

internal object NativeLibraryVersion {
    fun find(file: File, marker: String): String? {
        val markerBytes = marker.toByteArray(Charsets.ISO_8859_1)
        file.inputStream().buffered(VERSION_SCAN_BUFFER_SIZE).use { stream ->
            return scan(stream, markerBytes)
        }
    }

    private fun scan(stream: InputStream, markerBytes: ByteArray): String? {
        val versionBytes = ByteArrayOutputStream()
        val buffer = ByteArray(VERSION_SCAN_BUFFER_SIZE)
        var foundMarker = false
        var carry = ByteArray(0)

        while (versionBytes.size() < VERSION_SCAN_MAX_BYTES) {
            val chunk = readChunk(stream, buffer, carry) ?: break
            val startIndex = if (foundMarker) 0 else indexOfBytes(chunk, markerBytes)
            if (startIndex >= 0) {
                foundMarker = true
                if (appendUntilLineEnd(chunk, startIndex, versionBytes))
                    return versionBytes.versionString()
            }
            carry = if (foundMarker) ByteArray(0) else carryBytes(chunk, markerBytes)
        }

        return versionBytes.versionStringOrNull()
    }

    private fun readChunk(stream: InputStream, buffer: ByteArray, carry: ByteArray): ByteArray? {
        val read = stream.read(buffer)
        if (read == -1)
            return null
        return if (carry.isEmpty()) buffer.copyOf(read) else carry + buffer.copyOf(read)
    }

    private fun appendUntilLineEnd(chunk: ByteArray, startIndex: Int, output: ByteArrayOutputStream): Boolean {
        var index = startIndex
        while (index < chunk.size && output.size() < VERSION_SCAN_MAX_BYTES) {
            val byte = chunk[index]
            if (byte.isVersionLineEnd())
                return true
            output.write(byte.toInt())
            index++
        }
        return false
    }

    private fun ByteArrayOutputStream.versionStringOrNull(): String? {
        return if (size() > 0) versionString() else null
    }

    private fun ByteArrayOutputStream.versionString(): String {
        return toString(Charsets.ISO_8859_1.name()).trim()
    }

    private fun carryBytes(chunk: ByteArray, marker: ByteArray): ByteArray {
        return if (chunk.size > marker.size) {
            chunk.copyOfRange(chunk.size - marker.size, chunk.size)
        } else {
            chunk
        }
    }

    private fun Byte.isVersionLineEnd(): Boolean {
        return this == NUL_BYTE || this == CARRIAGE_RETURN_BYTE || this == LINE_FEED_BYTE
    }

    private fun indexOfBytes(haystack: ByteArray, needle: ByteArray): Int {
        val limit = haystack.size - needle.size
        var i = 0
        outer@ while (i <= limit) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) {
                    i++
                    continue@outer
                }
            }
            return i
        }
        return -1
    }

    private const val VERSION_SCAN_BUFFER_SIZE = 16_384
    private const val VERSION_SCAN_MAX_BYTES = 240
    private const val NUL_BYTE = 0.toByte()
    private const val CARRIAGE_RETURN_BYTE = '\r'.code.toByte()
    private const val LINE_FEED_BYTE = '\n'.code.toByte()
}
