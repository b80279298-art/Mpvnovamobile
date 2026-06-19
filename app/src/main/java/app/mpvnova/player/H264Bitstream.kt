package app.mpvnova.player

private val H264_HIGH_PROFILE_IDS = setOf(
    100,
    110,
    122,
    244,
    44,
    83,
    86,
    118,
    128,
    138,
    139,
    134,
    135,
)
private const val H264_NAL_TYPE_MASK = 0x1F
private const val H264_NAL_TYPE_SPS = 7

internal fun h264CsdIndicatesTenBit(csd: ByteArray): Boolean {
    if (csd.isEmpty())
        return false
    return h264NalUnits(csd).any(::h264SpsNalIndicatesTenBit)
}

private fun h264NalUnits(data: ByteArray): Sequence<ByteArray> {
    return when {
        data.indices.any { data.annexBStartCodeLengthAt(it) > 0 } -> annexBNalUnits(data)
        data.size > AVC_CONFIG_HEADER_SIZE &&
            data[0].toInt() == AVC_CONFIG_VERSION -> avcConfigSpsNalUnits(data)
        else -> sequenceOf(data)
    }
}

private fun annexBNalUnits(data: ByteArray): Sequence<ByteArray> = sequence {
    var start = data.nextAnnexBStartCode(0)
    while (start >= 0) {
        val nalStart = start + data.annexBStartCodeLengthAt(start)
        val next = data.nextAnnexBStartCode(nalStart)
        val nalEnd = if (next >= 0) next else data.size
        if (nalEnd > nalStart)
            yield(data.copyOfRange(nalStart, nalEnd))
        start = next
    }
}

private fun ByteArray.nextAnnexBStartCode(from: Int): Int {
    var index = from.coerceAtLeast(0)
    while (index < size) {
        if (annexBStartCodeLengthAt(index) > 0)
            return index
        index++
    }
    return -1
}

private fun ByteArray.annexBStartCodeLengthAt(index: Int): Int = when {
    matchesBytesAt(index, ANNEX_B_THREE_BYTE_START_CODE) -> ANNEX_B_THREE_BYTE_START_CODE.size
    matchesBytesAt(index, ANNEX_B_FOUR_BYTE_START_CODE) -> ANNEX_B_FOUR_BYTE_START_CODE.size
    else -> 0
}

private fun ByteArray.matchesBytesAt(index: Int, expected: ByteArray): Boolean =
    index + expected.size <= size &&
        expected.indices.all { expectedIndex -> this[index + expectedIndex] == expected[expectedIndex] }

private fun avcConfigSpsNalUnits(data: ByteArray): Sequence<ByteArray> = sequence {
    var offset = AVC_CONFIG_HEADER_SIZE
    val sequenceParameterSetCount = data[offset++].toInt() and AVC_CONFIG_SPS_COUNT_MASK
    repeat(sequenceParameterSetCount) {
        if (offset + AVC_CONFIG_NAL_LENGTH_BYTES > data.size)
            return@sequence
        val length = data.readUnsignedShort(offset)
        offset += AVC_CONFIG_NAL_LENGTH_BYTES
        if (length <= 0 || offset + length > data.size)
            return@sequence
        yield(data.copyOfRange(offset, offset + length))
        offset += length
    }
}

private fun ByteArray.readUnsignedShort(offset: Int): Int =
    ((this[offset].toInt() and BYTE_MASK) shl Byte.SIZE_BITS) or
        (this[offset + 1].toInt() and BYTE_MASK)

private fun h264SpsNalIndicatesTenBit(nal: ByteArray): Boolean =
    nal.isNotEmpty() &&
        (nal[0].toInt() and H264_NAL_TYPE_MASK) == H264_NAL_TYPE_SPS &&
        h264SpsBitDepthMinus8(nal)?.let { bitDepthMinus8 ->
            bitDepthMinus8.luma > 0 || bitDepthMinus8.chroma > 0
        } == true

@Suppress("ReturnCount")
private fun h264SpsBitDepthMinus8(nal: ByteArray): H264BitDepthMinus8? {
    val rbsp = nal.copyOfRange(1, nal.size).removeEmulationPreventionBytes()
    val bits = BitReader(rbsp)
    val profileIdc = bits.readBits(Byte.SIZE_BITS) ?: return null
    if (!bits.skipBits(Byte.SIZE_BITS) || !bits.skipBits(Byte.SIZE_BITS))
        return null
    bits.readUnsignedExpGolomb() ?: return null
    if (profileIdc !in H264_HIGH_PROFILE_IDS)
        return H264BitDepthMinus8(luma = 0, chroma = 0)

    val chromaFormatIdc = bits.readUnsignedExpGolomb() ?: return null
    if (chromaFormatIdc == H264_CHROMA_FORMAT_444 && !bits.skipBits(1))
        return null
    val bitDepthLumaMinus8 = bits.readUnsignedExpGolomb() ?: return null
    val bitDepthChromaMinus8 = bits.readUnsignedExpGolomb() ?: return null
    return H264BitDepthMinus8(luma = bitDepthLumaMinus8, chroma = bitDepthChromaMinus8)
}

private data class H264BitDepthMinus8(val luma: Int, val chroma: Int)

private fun ByteArray.removeEmulationPreventionBytes(): ByteArray {
    val output = ArrayList<Byte>(size)
    var zeroCount = 0
    for (byte in this) {
        if (zeroCount >= 2 && byte == EMULATION_PREVENTION_BYTE) {
            zeroCount = 0
            continue
        }
        output.add(byte)
        zeroCount = if (byte == 0.toByte()) zeroCount + 1 else 0
    }
    return output.toByteArray()
}

private class BitReader(private val data: ByteArray) {
    private var bitOffset = 0

    fun readBits(count: Int): Int? {
        if (count < 0 || bitOffset + count > data.size * Byte.SIZE_BITS)
            return null
        var value = 0
        repeat(count) {
            val byteIndex = bitOffset / Byte.SIZE_BITS
            val bitIndex = BIT_INDEX_MSB - (bitOffset % Byte.SIZE_BITS)
            value = (value shl 1) or ((data[byteIndex].toInt() shr bitIndex) and 1)
            bitOffset++
        }
        return value
    }

    fun skipBits(count: Int): Boolean = readBits(count) != null

    @Suppress("ReturnCount")
    fun readUnsignedExpGolomb(): Int? {
        var leadingZeroBits = 0
        while (true) {
            val bit = readBits(1) ?: return null
            if (bit == 1)
                break
            leadingZeroBits++
            if (leadingZeroBits > MAX_EXP_GOLOMB_LEADING_ZEROES)
                return null
        }
        val suffix = if (leadingZeroBits == 0) 0 else readBits(leadingZeroBits) ?: return null
        return ((1 shl leadingZeroBits) - 1) + suffix
    }
}

private const val AVC_CONFIG_VERSION = 1
private const val AVC_CONFIG_HEADER_SIZE = 5
private const val AVC_CONFIG_NAL_LENGTH_BYTES = 2
private const val AVC_CONFIG_SPS_COUNT_MASK = 0x1F
private val ANNEX_B_THREE_BYTE_START_CODE = byteArrayOf(0, 0, 1)
private val ANNEX_B_FOUR_BYTE_START_CODE = byteArrayOf(0, 0, 0, 1)
private const val H264_CHROMA_FORMAT_444 = 3
private const val BYTE_MASK = 0xFF
private const val BIT_INDEX_MSB = 7
private const val MAX_EXP_GOLOMB_LEADING_ZEROES = 30
private const val EMULATION_PREVENTION_BYTE = 0x03.toByte()
