package com.gabriel.mylibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gabriel.mylibrary.ui.navigation.MyLibraryApp
import com.gabriel.mylibrary.ui.theme.MyLibraryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LibraryApplication).container
        setContent {
            MyLibraryTheme {
                MyLibraryApp(container.repository, container.preferences, container.coverStore)
            }
        }
    }
}
