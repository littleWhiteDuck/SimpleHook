package me.simpleHook.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import me.simpleHook.R
import me.simpleHook.ui.WindowPreferencesManager
import me.simpleHook.ui.fragment.backup.BackupFragment


class BackupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val uri = initReceiveFileUri()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.settings, BackupFragment(uri))
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowPreferencesManager(this).applyEdgeToEdgePreference(window)

    }

    private fun initReceiveFileUri(): Uri? {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.apply {
                return this
            }
        }
        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}