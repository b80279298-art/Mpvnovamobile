package app.mpvnova.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import app.mpvnova.player.preferences.AppUpdateManager

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(UiScale.wrap(newBase))
    }

    private val updateManager by lazy { AppUpdateManager(this) }
    private var checkedForUpdatesThisSession = false
    private var appliedThemeValue = AppearanceTheme.DEFAULT_VALUE
    private var appliedUiScale = UiScale.DEFAULT_SCALE_PERCENT

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedThemeValue = AppearanceTheme.currentValue(this)
        appliedUiScale = UiScale.currentScalePercent(this)
        AppearanceTheme.applyFilePicker(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        supportActionBar?.setTitle(R.string.mpv_activity)

        if (savedInstanceState == null) {
            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, MainScreenFragment())
                commit()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (AppearanceTheme.currentValue(this) != appliedThemeValue ||
            UiScale.currentScalePercent(this) != appliedUiScale) {
            recreate()
            return
        }
        updateManager.resumePendingInstallIfAllowed()
    }

    fun checkForHomeUpdatesOnce() {
        if (checkedForUpdatesThisSession)
            return
        checkedForUpdatesThisSession = true
        updateManager.checkForUpdates(
            showIfCurrent = false,
            respectIgnored = true,
            showProgress = false
        )
    }
}
