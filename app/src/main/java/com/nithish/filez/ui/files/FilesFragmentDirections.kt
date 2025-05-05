package com.nithish.filez.ui.files

import android.os.Bundle
import androidx.navigation.NavDirections
import com.nithish.filez.R

/**
 * Navigation directions for the FilesFragment
 */
class FilesFragmentDirections private constructor() {
    companion object {
        fun actionNavigateToZipViewer(zipFilePath: String): NavDirections {
            return ActionZipViewer(zipFilePath)
        }
    }

    class ActionZipViewer(private val zipFilePath: String) : NavDirections {
        override val actionId: Int = R.id.action_navigate_to_zip_viewer

        override val arguments: Bundle
            get() {
                val bundle = Bundle()
                bundle.putString("zipFilePath", zipFilePath)
                return bundle
            }
    }
} 