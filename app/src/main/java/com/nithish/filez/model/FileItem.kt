package com.nithish.filez.model

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (isDirectory) 0 else file.length(),
    val lastModified: Long = file.lastModified(),
    val path: String = file.absolutePath
) 