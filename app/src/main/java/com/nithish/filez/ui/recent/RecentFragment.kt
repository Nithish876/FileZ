package com.nithish.filez.ui.recent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nithish.filez.adapter.FileAdapter
import com.nithish.filez.databinding.FragmentRecentBinding
import com.nithish.filez.model.FileItem
import com.nithish.filez.ui.files.FilesFragmentDirections
import com.nithish.filez.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class RecentFragment : Fragment() {

    private var _binding: FragmentRecentBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fileAdapter: FileAdapter
    
    // Number of days to consider for recent files
    private val recentDaysThreshold = 7L
    
    // Maximum number of recent files to show
    private val maxRecentFiles = 50
    
    // Cache for recent files to avoid scanning on every resume
    private var recentFilesCache: List<FileItem>? = null
    private var lastCacheTime: Long = 0
    private val cacheValidityPeriod = TimeUnit.MINUTES.toMillis(2) // Cache valid for 2 minutes

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadRecentFiles()
    }

    private fun setupRecyclerView() {
        // Apply RecyclerView optimizations
        binding.recyclerRecent.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null // Disable animations for smoother scrolling
        }
        
        fileAdapter = FileAdapter(
            onItemClick = { fileItem ->
                handleFileClick(fileItem)
            },
            onItemLongClick = { fileItem ->
                // No long press action for recent files
                false
            }
        )
        binding.recyclerRecent.adapter = fileAdapter
    }

    private fun loadRecentFiles() {
        // Show loading state
        binding.progressRecent.visibility = View.VISIBLE
        binding.recyclerRecent.visibility = View.GONE
        binding.tvNoRecent.visibility = View.GONE
        
        // Check if we can use the cache
        val currentTime = System.currentTimeMillis()
        if (recentFilesCache != null && (currentTime - lastCacheTime < cacheValidityPeriod)) {
            // Use cached data if it's still valid
            updateUIWithFiles(recentFilesCache!!)
            return
        }
        
        // Load files in background using coroutines
        lifecycleScope.launch(Dispatchers.IO) {
            // Get cutoff time for recent files (e.g., 7 days ago)
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(recentDaysThreshold)
            
            // Start from external storage directory
            val rootDir = FileUtils.getExternalStorageDir()
            
            val recentFiles = findRecentFiles(rootDir, cutoffTime)
                .filter { !it.isDirectory }
                .sortedByDescending { it.lastModified() }
                .take(maxRecentFiles)
                .map { FileItem(it) }
            
            // Cache the results
            recentFilesCache = recentFiles
            lastCacheTime = currentTime
            
            // Update UI on main thread
            withContext(Dispatchers.Main) {
                updateUIWithFiles(recentFiles)
            }
        }
    }
    
    private fun updateUIWithFiles(files: List<FileItem>) {
        fileAdapter.submitList(files)
        
        binding.progressRecent.visibility = View.GONE
        binding.recyclerRecent.visibility = View.VISIBLE
        binding.tvNoRecent.visibility = 
            if (files.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun findRecentFiles(directory: File, cutoffTime: Long): List<File> {
        val result = mutableListOf<File>()
        
        try {
            val files = directory.listFiles() ?: return emptyList()
            
            for (file in files) {
                // Quick check before diving deeper
                if (result.size > maxRecentFiles * 2) {
                    break
                }
                
                if (file.isFile && file.lastModified() > cutoffTime) {
                    result.add(file)
                } else if (file.isDirectory && !file.name.startsWith(".")) {
                    // Only recurse into non-hidden directories to avoid system folders
                    result.addAll(findRecentFiles(file, cutoffTime))
                }
            }
        } catch (e: Exception) {
            // Ignore access exceptions
        }
        
        return result
    }

    private fun handleFileClick(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            // Do nothing for directories in recent view
        } else {
            // Handle file open
            if (FileUtils.isZipFile(fileItem.file)) {
                // Open zip viewer
                val action = FilesFragmentDirections.actionNavigateToZipViewer(fileItem.path)
                findNavController().navigate(action)
            } else {
                // Open file with appropriate app
                try {
                    val uri = FileUtils.getUriForFile(requireContext(), fileItem.file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, FileUtils.getMimeType(fileItem.file))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "No app found to open this file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Only reload if the cache is invalid or if cache was cleared
        val prefs = requireContext().getSharedPreferences(
            "com.nithish.filez.preferences", 
            android.content.Context.MODE_PRIVATE
        )
        val lastCleared = prefs.getLong("last_cleared", 0)
        
        if (recentFilesCache == null || lastCleared > lastCacheTime) {
            loadRecentFiles()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 