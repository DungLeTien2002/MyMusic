package com.example.mymusic.base.utils.extension

import android.os.Build

fun isSdk34() = isSdkUpSlideDownCake()
fun isSdkUpSlideDownCake() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE