package com.nithish.filez.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.nithish.filez.R
import com.nithish.filez.databinding.FragmentSettingsBinding
import com.nithish.filez.utils.FileUtils

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val PREFS_NAME = "com.nithish.filez.preferences"
    private val KEY_SHOW_HIDDEN = "show_hidden_files"
    private val KEY_LIST_VIEW = "use_list_view"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load setting values
        binding.cbShowHiddenFiles.isChecked = prefs.getBoolean(KEY_SHOW_HIDDEN, false)
        binding.cbListView.isChecked = prefs.getBoolean(KEY_LIST_VIEW, true)
        
        // Display default folder
        binding.tvDefaultFolder.text = FileUtils.getExternalStorageDir().absolutePath
    }
    
    private fun setupListeners() {
        // Save preferences on change
        binding.cbShowHiddenFiles.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_SHOW_HIDDEN, isChecked)
        }
        
        binding.cbListView.setOnCheckedChangeListener { _, isChecked ->
            savePreference(KEY_LIST_VIEW, isChecked)
        }
        
        // Clear recent files
        binding.btnClearRecent.setOnClickListener {
            clearRecentFiles()
            Toast.makeText(
                requireContext(),
                "Recent files cleared",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun savePreference(key: String, value: Boolean) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }
    
    private fun clearRecentFiles() {
        // In a real app, this would clear the recent files database or preferences
        // For our simple app, we'll just show a toast
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("last_cleared", System.currentTimeMillis()).apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        // Static methods to access settings from other fragments
        fun showHiddenFiles(context: Context): Boolean {
            return context.getSharedPreferences(
                "com.nithish.filez.preferences", 
                Context.MODE_PRIVATE
            ).getBoolean("show_hidden_files", false)
        }
        
        fun useListView(context: Context): Boolean {
            return context.getSharedPreferences(
                "com.nithish.filez.preferences", 
                Context.MODE_PRIVATE
            ).getBoolean("use_list_view", true)
        }
    }
} 