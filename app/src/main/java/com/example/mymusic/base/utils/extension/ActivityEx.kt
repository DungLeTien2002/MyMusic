package com.example.mymusic.base.utils.extension

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.mymusic.R

fun Activity.finishWithSlide() {
    finish()
    if (isSdk34()) {
        overrideActivityTransition(
            OVERRIDE_TRANSITION_CLOSE,
            R.anim.slide_in_left,
            R.anim.slide_out_right,
            Color.TRANSPARENT
        )
    } else {
        @Suppress("DEPRECATION") overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
    }
}

fun ComponentActivity.handleBackPressed(action: () -> Unit) {
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            action()
        }
    })
}