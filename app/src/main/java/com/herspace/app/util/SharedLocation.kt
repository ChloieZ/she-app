package com.herspace.app.util

import android.location.Location

/** 全局共享的当前位置（由地图页面持续更新，投票页面读取） */
object SharedLocation {
    private var _location: Location? = null

    fun update(loc: Location) {
        _location = loc
    }

    fun get(): Location? = _location
}
