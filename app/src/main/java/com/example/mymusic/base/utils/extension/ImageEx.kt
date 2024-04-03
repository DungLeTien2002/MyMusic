package com.example.mymusic.base.utils.extension

import android.content.Context
import android.widget.ImageView
import coil.Coil
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest

inline val Context.imageLoader: ImageLoader
    get() = Coil.imageLoader(this)

inline fun ImageView.load(
    data: Any?,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}