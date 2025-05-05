package com.nithish.filez.ui.zip

import android.os.Bundle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException

/**
 * Arguments for the ZipViewerFragment
 */
data class ZipViewerFragmentArgs(val zipFilePath: String) : NavArgs {

    fun toBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("zipFilePath", zipFilePath)
        return bundle
    }

    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): ZipViewerFragmentArgs {
            bundle.classLoader = ZipViewerFragmentArgs::class.java.classLoader
            
            val zipFilePath = bundle.getString("zipFilePath")
                ?: throw IllegalArgumentException("Required argument \"zipFilePath\" is missing.")
                
            return ZipViewerFragmentArgs(zipFilePath)
        }
        
        /**
         * Builder for creating [ZipViewerFragmentArgs]
         */
        class Builder(private val zipFilePath: String) {
            fun build(): ZipViewerFragmentArgs {
                return ZipViewerFragmentArgs(zipFilePath)
            }
        }
    }
} 