package com.example.mymusic.base.utils.extension

import android.widget.TextView

fun TextView.setTextAnimation(text: String, duration: Long = 300, completion: (() -> Unit)? = null) {
    if (text != "null") {
        fadOutAnimation(duration) {
            this.text = text
            fadInAnimation(duration) {
                completion?.let {
                    it()
                }
            }
        }
    }
}