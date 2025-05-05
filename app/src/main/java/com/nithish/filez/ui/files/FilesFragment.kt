package com.nithish.filez.ui.files

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.nithish.filez.R
import com.nithish.filez.adapter.FileAdapter
import com.nithish.filez.databinding.FragmentFilesBinding
import com.nithish.filez.model.FileItem
import com.nithish.filez.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fileAdapter: FileAdapter
    private lateinit var currentDirectory: File
    private val fileStack = mutableListOf<File>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadFiles()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.msg_storage_permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFab()
        checkPermissionAndLoadFiles()
    }

    private fun setupRecyclerView() {
        // Apply recycler view optimizations
        binding.recyclerFiles.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null // Disable animations for smoother scrolling
        }
        
        fileAdapter = FileAdapter(
            onItemClick = { fileItem ->
                handleFileClick(fileItem)
            },
            onItemLongClick = { fileItem ->
                showFileOptionsMenu(fileItem)
                true
            }
        )
        binding.recyclerFiles.adapter = fileAdapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddMenu()
        }
    }

    private fun showAddMenu() {
        val popupMenu = PopupMenu(requireContext(), binding.fabAdd)
        popupMenu.menu.add(0, 0, 0, getString(R.string.action_create_folder))
        popupMenu.menu.add(0, 1, 0, getString(R.string.action_create_file))
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> showCreateFolderDialog()
                1 -> showCreateFileDialog()
            }
            true
        }
        
        popupMenu.show()
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(requireContext())
        editText.hint = getString(R.string.message_folder_name)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_create_folder)
            .setView(editText)
            .setPositiveButton(R.string.button_create) { _, _ ->
                val folderName = editText.text.toString()
                if (folderName.isNotEmpty()) {
                    val newFolder = FileUtils.createDirectory(currentDirectory, folderName)
                    if (newFolder != null) {
                        loadFiles()
                        Toast.makeText(
                            requireContext(),
                            R.string.msg_operation_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.msg_operation_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun showCreateFileDialog() {
        val editText = EditText(requireContext())
        editText.hint = getString(R.string.message_file_name)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_create_file)
            .setView(editText)
            .setPositiveButton(R.string.button_create) { _, _ ->
                val fileName = editText.text.toString()
                if (fileName.isNotEmpty()) {
                    val newFile = FileUtils.createFile(currentDirectory, fileName)
                    if (newFile != null) {
                        loadFiles()
                        Toast.makeText(
                            requireContext(),
                            R.string.msg_operation_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.msg_operation_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun showFileOptionsMenu(fileItem: FileItem) {
        val view = requireActivity().findViewById<View>(android.R.id.content)
        val popupMenu = PopupMenu(requireContext(), view)
        
        popupMenu.menu.add(0, 0, 0, getString(R.string.action_rename))
        popupMenu.menu.add(0, 1, 0, getString(R.string.action_delete))
        popupMenu.menu.add(0, 2, 0, getString(R.string.action_share))
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> showRenameDialog(fileItem)
                1 -> showDeleteConfirmation(fileItem)
                2 -> shareFile(fileItem)
            }
            true
        }
        
        popupMenu.show()
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val editText = EditText(requireContext())
        editText.setText(fileItem.name)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_rename)
            .setView(editText)
            .setPositiveButton(R.string.button_rename) { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotEmpty() && newName != fileItem.name) {
                    val newFile = File(fileItem.file.parent, newName)
                    val success = fileItem.file.renameTo(newFile)
                    if (success) {
                        loadFiles()
                        Toast.makeText(
                            requireContext(),
                            R.string.msg_operation_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            R.string.msg_operation_failed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(fileItem: FileItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete)
            .setMessage(R.string.message_delete_confirmation)
            .setPositiveButton(R.string.button_delete) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val success = FileUtils.delete(fileItem.file)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            loadFiles()
                            Toast.makeText(
                                requireContext(),
                                R.string.msg_operation_success,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                R.string.msg_operation_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun shareFile(fileItem: FileItem) {
        if (!fileItem.isDirectory) {
            val uri = FileUtils.getUriForFile(requireContext(), fileItem.file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = FileUtils.getMimeType(fileItem.file)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share file via"))
        }
    }

    private fun handleFileClick(fileItem: FileItem) {
        if (fileItem.isDirectory) {
            // Navigate to directory
            fileStack.add(currentDirectory)
            currentDirectory = fileItem.file
            loadFiles()
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

    private fun checkPermissionAndLoadFiles() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeDirectory()
                loadFiles()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.msg_storage_permission_required)
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    .setNegativeButton(R.string.button_cancel, null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun initializeDirectory() {
        currentDirectory = Environment.getExternalStorageDirectory()
    }

    private fun loadFiles() {
        // Show loading indicator
        binding.progressLoading.visibility = View.VISIBLE
        binding.recyclerFiles.visibility = View.GONE
        binding.tvEmptyDirectory.visibility = View.GONE
        
        // Update path immediately for better UI responsiveness
        binding.tvCurrentPath.text = currentDirectory.absolutePath
        
        // Load files in background thread
        lifecycleScope.launch(Dispatchers.IO) {
            val files = FileUtils.listFiles(currentDirectory)
            val fileItems = files.map { FileItem(it) }
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            
            // Update UI on main thread
            withContext(Dispatchers.Main) {
                fileAdapter.submitList(fileItems)
                
                binding.progressLoading.visibility = View.GONE
                binding.recyclerFiles.visibility = View.VISIBLE
                binding.tvEmptyDirectory.visibility = 
                    if (fileItems.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::currentDirectory.isInitialized) {
            loadFiles()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun onBackPressed(): Boolean {
        if (fileStack.isNotEmpty()) {
            currentDirectory = fileStack.removeAt(fileStack.size - 1)
            loadFiles()
            return true
        }
        return false
    }
} 