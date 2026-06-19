package app.mpvnova.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldHi10pNameHintTest {

    @Test
    fun hi10ReleaseShorthandIsDetected() {
        assertTrue(
            "[Saiki] The Disastrous Life of Saiki K. - S01E03 (BD 1080p Hi10 FLAC) [Dual-Audio].mkv"
                .indicatesH264TenBitByName()
        )
        assertTrue("Show.S01E01.1080p.BluRay.Hi10P.x264.mkv".indicatesH264TenBitByName())
    }

    @Test
    fun hi10IsCaseInsensitive() {
        assertTrue("SHOW.1080P.HI10.MKV".indicatesH264TenBitByName())
    }

    @Test
    fun genericTenBitWithH264IsDetected() {
        assertTrue("Show.S01E01.1080p.x264.10bit.mkv".indicatesH264TenBitByName())
        assertTrue("Show.S01E01.1080p.AVC.10-bit.mkv".indicatesH264TenBitByName())
    }

    // The key guarantee: HEVC Main10 decodes in hardware on Shield, so it must not be
    // forced to software by the name heuristic.
    @Test
    fun hevcTenBitIsNotTreatedAsH264Hi10p() {
        assertFalse("Show.S01E01.1080p.x265.10bit.mkv".indicatesH264TenBitByName())
        assertFalse("Show.S01E01.2160p.HEVC.10-bit.mkv".indicatesH264TenBitByName())
        assertFalse("Show.S01E01.1080p.H.265.10bit.mkv".indicatesH264TenBitByName())
    }

    @Test
    fun ambiguousTenBitWithoutCodecIsNotAssumed() {
        assertFalse("Show.S01E01.1080p.10bit.mkv".indicatesH264TenBitByName())
    }

    @Test
    fun eightBitContentIsNotDetected() {
        assertFalse("Show.S01E01.1080p.x264.mkv".indicatesH264TenBitByName())
        assertFalse("Movie.2021.2160p.x265.HDR.mkv".indicatesH264TenBitByName())
    }
}
