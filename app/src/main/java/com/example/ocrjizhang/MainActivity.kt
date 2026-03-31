package com.example.ocrjizhang

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.example.ocrjizhang.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.statisticsFragment,
        R.id.profileFragment,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(topLevelDestinations)

        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id in topLevelDestinations
            binding.bottomNav.isVisible = showBottomNav
            binding.navHostFragment.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                bottomMargin = if (showBottomNav) {
                    resources.getDimensionPixelSize(R.dimen.bottom_nav_space)
                } else {
                    0
                }
            }
            binding.topAppBar.title = destination.label ?: getString(R.string.app_name)
        }
    }
}
