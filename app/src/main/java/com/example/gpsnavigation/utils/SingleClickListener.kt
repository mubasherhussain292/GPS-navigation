package com.example.gpsnavigation.utils

import android.view.View

private var lastClickTime = 0L

fun View.setDebouncedClickListener(interval: Long = 400L, onClick: (View) -> Unit) {
    this.setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= interval) {
            lastClickTime = currentTime
            onClick(it)
        }
    }
}
