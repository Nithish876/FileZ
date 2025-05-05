package com.nithish.filez.ui.zip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.nithish.filez.adapter.ZipEntryAdapter
import com.nithish.filez.databinding.FragmentZipViewerBinding
import com.nithish.filez.model.ZipEntryItem
import com.nithish.filez.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ZipViewerFragment : Fragment() {

    private var _binding: FragmentZipViewerBinding? = null
    private val binding get() = _binding!!
    
    private val args: ZipViewerFragmentArgs by navArgs()
    private lateinit var zipEntryAdapter: ZipEntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZipViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadZipContents()
    }

    private fun setupRecyclerView() {
        // Apply RecyclerView optimizations
        binding.recyclerZipEntries.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null // Disable animations for smoother scrolling
        }
        
        zipEntryAdapter = ZipEntryAdapter { zipEntry ->
            // For now just show a toast when clicking on a zip entry
            Toast.makeText(
                requireContext(),
                "Selected: ${zipEntry.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.recyclerZipEntries.adapter = zipEntryAdapter
    }

    private fun loadZipContents() {
        // Show loading indicator
        binding.progressZip.visibility = View.VISIBLE
        binding.recyclerZipEntries.visibility = View.GONE
        binding.tvEmptyZip.visibility = View.GONE
        
        val zipFile = File(args.zipFilePath)
        if (!zipFile.exists() || !FileUtils.isZipFile(zipFile)) {
            showError("Invalid or missing zip file")
            return
        }

        // Update zip name immediately for better UI responsiveness
        binding.tvZipName.text = zipFile.name

        // Process zip in background
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val entries = FileUtils.listZipEntries(zipFile)
                
                // Process entries and update UI on main thread
                withContext(Dispatchers.Main) {
                    if (entries.isEmpty()) {
                        binding.tvEmptyZip.visibility = View.VISIBLE
                        binding.recyclerZipEntries.visibility = View.GONE
                        binding.progressZip.visibility = View.GONE
                    } else {
                        // Process zip entries to create a structured view
                        // This could be done in the background as well for large zips
                        val zipEntries = processZipEntries(entries)
                        zipEntryAdapter.submitList(zipEntries)
                        
                        binding.progressZip.visibility = View.GONE
                        binding.tvEmptyZip.visibility = View.GONE
                        binding.recyclerZipEntries.visibility = View.VISIBLE
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showError("Error reading zip file: ${e.message}")
                }
            }
        }
    }

    private fun processZipEntries(entries: List<String>): List<ZipEntryItem> {
        // Use a more efficient approach with maps for large zip files
        val result = HashMap<String, ZipEntryItem>()
        
        // First pass: add all entries
        entries.forEach { entryPath ->
            // Extract directory parts for hierarchy
            val path = entryPath.trim('/')
            val isDirectory = entryPath.endsWith("/")
            
            // Get just the name without parent directories
            val name = path.substringAfterLast('/', path)
            
            result[path] = ZipEntryItem(name, path, isDirectory)
            
            // Process parent directories to create hierarchy
            var currentPath = path
            while (currentPath.contains("/")) {
                currentPath = currentPath.substringBeforeLast('/')
                if (currentPath.isNotEmpty() && !result.containsKey(currentPath)) {
                    val dirName = currentPath.substringAfterLast('/', currentPath)
                    result[currentPath] = ZipEntryItem(dirName, currentPath, true)
                }
            }
        }
        
        // Sort by path depth and name for proper hierarchy display
        return result.values.sortedWith(
            compareBy<ZipEntryItem> { it.path.count { c -> c == '/' } }
                .thenBy { it.path }
        )
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        binding.tvEmptyZip.text = message
        binding.tvEmptyZip.visibility = View.VISIBLE
        binding.recyclerZipEntries.visibility = View.GONE
        binding.progressZip.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 