package com.jeevashraya.j2rescue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jeevashraya.j2rescue.ui.screens.HomeScreen
import com.jeevashraya.j2rescue.ui.theme.J2RescueTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            J2RescueTheme {

                HomeScreen(
                    onStartRescue = {
                        // Navigation comes next
                    }
                )

            }
        }
    }
}