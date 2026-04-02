package com.example.ocrjizhang

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
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
    private val motionIn by lazy {
        AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in)
    }
    private val motionOut by lazy {
        AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_linear_in)
    }

    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.statisticsFragment,
        R.id.profileFragment,
    )

    private val toolbarHiddenDestinations = setOf(
        R.id.splashFragment,
        R.id.loginFragment,
        R.id.registerFragment,
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
            val showToolbar = destination.id !in toolbarHiddenDestinations

            updateChromeVisibility(
                view = binding.bottomNav,
                visible = showBottomNav,
                hiddenTranslationY = resources.displayMetrics.density * 24f,
            )
            updateChromeVisibility(
                view = binding.topAppBar,
                visible = showToolbar,
                hiddenTranslationY = -resources.displayMetrics.density * 16f,
            )
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

    private fun updateChromeVisibility(
        view: View,
        visible: Boolean,
        hiddenTranslationY: Float,
    ) {
        view.animate().cancel()

        if (visible) {
            if (!view.isVisible) {
                view.alpha = 0f
                view.translationY = hiddenTranslationY
                view.isVisible = true
            }
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220L)
                .setInterpolator(motionIn)
                .start()
        } else if (view.isVisible) {
            view.animate()
                .alpha(0f)
                .translationY(hiddenTranslationY)
                .setDuration(180L)
                .setInterpolator(motionOut)
                .withEndAction {
                    view.isVisible = false
                }
                .start()
        }
    }
}
