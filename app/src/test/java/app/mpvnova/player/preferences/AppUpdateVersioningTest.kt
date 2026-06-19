package app.mpvnova.player.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUpdateVersioningTest {
    @Test
    fun chooseBestApkAssetNameForFireTvStickAbi() {
        val selected = chooseBestApkAssetName(
            assetNames = listOf(
                "mpvNova-2026-05-10-arm64-v8a.apk",
                "mpvNova-2026-05-10-armeabi-v7a.apk",
                "mpvNova-2026-05-10-universal.apk",
            ),
            supportedAbis = listOf("armeabi-v7a", "armeabi")
        )

        assertEquals("mpvNova-2026-05-10-armeabi-v7a.apk", selected)
    }

    @Test
    fun chooseBestApkAssetNamePrefersApi29UniversalForFireOsSdk28() {
        val selected = chooseBestApkAssetName(
            assetNames = listOf(
                "app-api29-universal-release.apk",
                "app-default-arm64-v8a-release.apk",
                "app-default-armeabi-v7a-release.apk",
                "app-default-universal-release.apk",
                "app-default-x86-release.apk",
                "app-default-x86_64-release.apk",
            ),
            supportedAbis = listOf("armeabi-v7a", "armeabi"),
            sdkInt = 28
        )

        assertEquals("app-api29-universal-release.apk", selected)
    }

    @Test
    fun chooseBestApkAssetNameStillPrefersExactAbiForModernAndroidTv() {
        val selected = chooseBestApkAssetName(
            assetNames = listOf(
                "app-api29-universal-release.apk",
                "app-default-arm64-v8a-release.apk",
                "app-default-armeabi-v7a-release.apk",
                "app-default-universal-release.apk",
            ),
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
            sdkInt = 33
        )

        assertEquals("app-default-arm64-v8a-release.apk", selected)
    }

    @Test
    fun chooseBestApkAssetNameFallsBackToUniversal() {
        val selected = chooseBestApkAssetName(
            assetNames = listOf(
                "mpvNova-2026-05-10-x86_64.apk",
                "mpvNova-2026-05-10-universal.apk",
            ),
            supportedAbis = listOf("armeabi-v7a")
        )

        assertEquals("mpvNova-2026-05-10-universal.apk", selected)
    }

    @Test
    fun chooseBestApkAssetNameFallsBackToAbiNeutralName() {
        val selected = chooseBestApkAssetName(
            assetNames = listOf(
                "mpvNova-2026-05-10-x86.apk",
                "mpvNova-2026-05-10-release.apk",
            ),
            supportedAbis = listOf("armeabi-v7a")
        )

        assertEquals("mpvNova-2026-05-10-release.apk", selected)
    }

    @Test
    fun chooseBestApkAssetNameReturnsNullForEmptyAssets() {
        assertNull(chooseBestApkAssetName(emptyList(), listOf("armeabi-v7a")))
    }
}
