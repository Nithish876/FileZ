package com.nithish.filez.model

data class ZipEntryItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean = name.endsWith("/"),
    val depth: Int = path.count { it == '/' }
) 