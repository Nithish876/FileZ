package com.nithish.filez

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nithish.filez.databinding.ActivityMainBinding
import com.nithish.filez.ui.files.FilesFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_files, R.id.navigation_recent, R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        
        // Handle back navigation in file browser
        handleBackNavigation()
    }
    
    private fun handleBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = findNavController(R.id.nav_host_fragment_activity_main)
                
                // Check if we're in the files fragment
                val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
                    ?.childFragmentManager?.fragments?.get(0)
                
                if (currentFragment is FilesFragment) {
                    // Let the files fragment handle back navigation through directories
                    if (!currentFragment.onBackPressed()) {
                        // If files fragment didn't handle it, follow normal back behavior
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    }
                } else {
                    // Normal back navigation
                    if (!navController.popBackStack()) {
                        finish()
                    }
                }
            }
        })
    }
    
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}