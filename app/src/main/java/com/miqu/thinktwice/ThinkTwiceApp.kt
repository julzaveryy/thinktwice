package com.miqu.thinktwice

import android.app.Application
import android.content.Context
import com.miqu.thinktwice.data.DayClock
import com.miqu.thinktwice.data.LegacyImporter
import com.miqu.thinktwice.data.ProgressRepository
import com.miqu.thinktwice.data.content.ContentRepository
import com.miqu.thinktwice.data.db.ThinkTwiceDatabase
import com.miqu.thinktwice.data.prefs.SettingsStore

class ThinkTwiceApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container: one instance of each long-lived object for the whole app. */
class AppContainer(context: Context) {
    private val database = ThinkTwiceDatabase.create(context)
    val clock = DayClock()
    val content = ContentRepository(context)
    val settings = SettingsStore(context)
    val progress = ProgressRepository(database.progressDao(), clock)
    val legacyImporter = LegacyImporter(context, database, settings)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as ThinkTwiceApp).container
