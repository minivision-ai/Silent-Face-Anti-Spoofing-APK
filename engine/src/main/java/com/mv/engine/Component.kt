package com.mv.engine


abstract class Component {

    init {
        try {
            System.loadLibrary("engine")
            libraryFound = true
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    abstract fun createInstance(): Long

    abstract fun destroy()

    companion object {
        var libraryFound: Boolean = false

        const val tag = "Component"
    }
}