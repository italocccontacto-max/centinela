package com.centinela.app.contract

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class NightlyContractActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightlyContractScreen(onContractSaved = { finish() })
        }
    }
}
