package com.gobe.tv.data.system

import com.gobe.tv.domain.System

class SystemDetector {
    fun detect(path: String): System? {
        val name = path.substringAfterLast('/')
        if (!name.contains('.')) return null
        val ext = name.substringAfterLast('.')
        return System.fromExtension(ext)
    }
}
