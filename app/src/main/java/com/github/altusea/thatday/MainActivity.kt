package com.github.altusea.thatday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.altusea.thatday.ui.ThatDayApp
import com.github.altusea.thatday.ui.theme.ThatDayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mandatory on target 36+ (DESIGN.md §3.0): apps cannot opt out of edge-to-edge.
        enableEdgeToEdge()
        val app = application as ThatDayApplication
        setContent {
            ThatDayTheme {
                ThatDayApp(repository = app.repository, backupManager = app.backupManager)
            }
        }
    }
}
