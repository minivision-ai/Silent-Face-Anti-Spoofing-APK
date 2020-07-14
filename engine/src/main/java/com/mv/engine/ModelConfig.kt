package com.mv.engine

import androidx.annotation.Keep

@Keep
data class ModelConfig(
    var scale: Float = 0F,
    var shift_x: Float = 0F,
    var shift_y: Float = 0F,
    var height: Int = 0,
    var width: Int = 0,
    var name: String = "",
    var org_resize: Boolean = false
)