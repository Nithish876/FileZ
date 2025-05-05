package com.nithish.filez.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.LruCache
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object FileUtils {
    
    // Cache for mime types to avoid repeated lookups
    private val mimeTypeCache = ConcurrentHashMap<String, String>()
    
    // Cache for formatted file sizes
    private val fileSizeCache = LruCache<Long, String>(100)
    
    // Date formatter - created once and reused
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    // Get the external storage directory (public)
    fun getExternalStorageDir(): File {
        return Environment.getExternalStorageDirectory()
    }

    // List files in a directory
    fun listFiles(directory: File): List<File> {
        val files = directory.listFiles()
        return files?.toList() ?: emptyList()
    }

    // Get file size in human-readable format
    fun getReadableFileSize(size: Long): String {
        // Check cache first
        var result = fileSizeCache.get(size)
        if (result != null) return result
        
        // Calculate if not in cache
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        result = DecimalFormat("#,##0.#").format(
            size / Math.pow(1024.0, digitGroups.toDouble())
        ) + " " + units[digitGroups]
        
        // Store in cache
        fileSizeCache.put(size, result)
        
        return result
    }

    // Get file's last modified date in readable format
    fun getLastModifiedDate(file: File): String {
        val date = Date(file.lastModified())
        return dateFormatter.format(date)
    }

    // Get file mime type based on extension
    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        
        // Check cache first
        mimeTypeCache[extension]?.let { return it }
        
        // Calculate if not in cache
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
        
        // Store in cache
        mimeTypeCache[extension] = mimeType
        
        return mimeType
    }

    // Check if a file is a zip archive
    fun isZipFile(file: File): Boolean {
        return file.extension.equals("zip", ignoreCase = true)
    }

    // Extract entries from a zip file
    fun listZipEntries(zipFile: File): List<String> {
        val entries = mutableListOf<String>()
        try {
            ZipInputStream(FileInputStream(zipFile).buffered()).use { zis ->
                var zipEntry: ZipEntry?
                while (zis.nextEntry.also { zipEntry = it } != null) {
                    zipEntry?.name?.let { entries.add(it) }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return entries
    }

    // Create a new file
    fun createFile(parent: File, name: String): File? {
        val newFile = File(parent, name)
        return try {
            if (newFile.createNewFile()) newFile else null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Create a new directory
    fun createDirectory(parent: File, name: String): File? {
        val newDir = File(parent, name)
        return if (newDir.mkdir()) newDir else null
    }

    // Delete a file or directory
    fun delete(file: File): Boolean {
        return if (file.isDirectory) {
            file.listFiles()?.forEach { delete(it) }
            file.delete()
        } else {
            file.delete()
        }
    }

    // Copy file
    fun copyFile(source: File, destination: File): Boolean {
        try {
            FileInputStream(source).buffered().use { input ->
                FileOutputStream(destination).buffered().use { output ->
                    val buffer = ByteArray(8192)  // Use a larger buffer for better performance
                    var length: Int
                    while (input.read(buffer).also { length = it } > 0) {
                        output.write(buffer, 0, length)
                    }
                }
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }

    // Get content URI for a file using FileProvider
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
} 