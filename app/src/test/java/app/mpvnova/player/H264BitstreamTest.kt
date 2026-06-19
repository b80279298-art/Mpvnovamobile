package app.mpvnova.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class H264BitstreamTest {
    @Test
    fun detectsTenBitHighProfileSps() {
        assertTrue(
            h264CsdIndicatesTenBit(spsNal(profileIdc = H264_PROFILE_HIGH_10, bitDepthMinus8 = 2))
        )
    }

    @Test
    fun rejectsEightBitHighProfileSps() {
        assertFalse(
            h264CsdIndicatesTenBit(spsNal(profileIdc = H264_PROFILE_HIGH, bitDepthMinus8 = 0))
        )
    }

    @Test
    fun readsAnnexBWrappedSps() {
        val annexB = byteArrayOf(0, 0, 0, 1) +
            spsNal(profileIdc = H264_PROFILE_HIGH_10, bitDepthMinus8 = 2)

        assertTrue(h264CsdIndicatesTenBit(annexB))
    }

    @Test
    fun readsAvcDecoderConfigurationRecordSps() {
        val avcConfig = avcConfigRecord(
            spsNal(profileIdc = H264_PROFILE_HIGH_10, bitDepthMinus8 = 2)
        )

        assertTrue(h264CsdIndicatesTenBit(avcConfig))
    }

    @Test
    fun rejectsNonSpsNal() {
        assertFalse(h264CsdIndicatesTenBit(byteArrayOf(0x68.toByte(), 0x00, 0x00)))
    }

    private fun spsNal(profileIdc: Int, bitDepthMinus8: Int): ByteArray {
        val bits = BitWriter()
        bits.writeBits(profileIdc, Byte.SIZE_BITS)
        bits.writeBits(0, Byte.SIZE_BITS)
        bits.writeBits(40, Byte.SIZE_BITS)
        bits.writeUnsignedExpGolomb(0)
        if (profileIdc == H264_PROFILE_HIGH || profileIdc == H264_PROFILE_HIGH_10) {
            bits.writeUnsignedExpGolomb(1)
            bits.writeUnsignedExpGolomb(bitDepthMinus8)
            bits.writeUnsignedExpGolomb(bitDepthMinus8)
        }
        bits.writeBit(1)
        return byteArrayOf(0x67.toByte()) + bits.toByteArray()
    }

    private fun avcConfigRecord(sps: ByteArray): ByteArray =
        byteArrayOf(
            1,
            H264_PROFILE_HIGH_10.toByte(),
            0,
            40,
            0xFF.toByte(),
            0xE1.toByte(),
            (sps.size shr Byte.SIZE_BITS).toByte(),
            sps.size.toByte(),
        ) + sps

    private class BitWriter {
        private val bits = mutableListOf<Int>()

        fun writeBit(bit: Int) {
            bits += bit and 1
        }

        fun writeBits(value: Int, count: Int) {
            for (index in count - 1 downTo 0)
                writeBit(value shr index)
        }

        fun writeUnsignedExpGolomb(value: Int) {
            val codeNum = value + 1
            val bitLength = Integer.SIZE - Integer.numberOfLeadingZeros(codeNum)
            repeat(bitLength - 1) { writeBit(0) }
            writeBits(codeNum, bitLength)
        }

        fun toByteArray(): ByteArray {
            while (bits.size % Byte.SIZE_BITS != 0)
                bits += 0
            return bits
                .chunked(Byte.SIZE_BITS)
                .map { byteBits ->
                    byteBits.fold(0) { acc, bit -> (acc shl 1) or bit }.toByte()
                }
                .toByteArray()
        }
    }

    private companion object {
        private const val H264_PROFILE_HIGH = 100
        private const val H264_PROFILE_HIGH_10 = 110
    }
}
