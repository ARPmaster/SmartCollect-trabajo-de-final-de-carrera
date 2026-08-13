package com.example.aicollect.presentation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.aicollect.R
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.data.DarkModePreferences
import com.example.aicollect.databinding.ActivityMainBinding
import com.example.aicollect.presentation.collection.FilterBottomSheetFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var latestBottomInset = 0
    private val bottomBarBaseHeightPx by lazy { (80 * resources.displayMetrics.density).roundToInt() }

    /** Auth screens render full-screen, without the drawer/toolbar/bottom-nav chrome (brief Sección 2/7). */
    private val authDestinationIds = setOf(R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setUpInsets()

        binding.btnOpenMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnOpenFilters.setOnClickListener {
            FilterBottomSheetFragment().show(supportFragmentManager, FilterBottomSheetFragment.TAG)
        }

        binding.navView.btnCloseDrawer.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        // Destinations not implemented yet (Editar Perfil, Seguridad, Ayuda, Sobre per brief).
        listOf(
            binding.navView.rowEditProfile,
            binding.navView.rowSecurity,
            binding.navView.rowHelp,
            binding.navView.rowAbout,
        ).forEach { row ->
            row.setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        binding.navView.btnLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            lifecycleScope.launch {
                authRepository.signOut()
                navController.navigate(
                    R.id.loginFragment,
                    null,
                    navOptions { popUpTo(R.id.nav_graph) { inclusive = true } },
                )
            }
        }

        val bottomBar = binding.bottomNavBar.root
        bottomBar.findViewById<View>(R.id.btn_nav_home).setOnClickListener {
            navController.navigate(R.id.homeFragment)
        }
        bottomBar.findViewById<View>(R.id.btn_nav_add).setOnClickListener {
            navController.navigate(R.id.addFragment)
        }
        bottomBar.findViewById<View>(R.id.btn_nav_stats).setOnClickListener {
            navController.navigate(R.id.statsFragment)
        }

        val activeColor = ContextCompat.getColor(this, R.color.collect_gold)
        val inactiveColor = ContextCompat.getColor(this, R.color.collect_text_secondary_50)
        val homeIcon = bottomBar.findViewById<ImageView>(R.id.iv_nav_home_icon)
        val homeLabel = bottomBar.findViewById<TextView>(R.id.tv_nav_home_label)
        val statsIcon = bottomBar.findViewById<ImageView>(R.id.iv_nav_stats_icon)
        val statsLabel = bottomBar.findViewById<TextView>(R.id.tv_nav_stats_label)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val homeColor = if (destination.id == R.id.homeFragment) activeColor else inactiveColor
            val statsColor = if (destination.id == R.id.statsFragment) activeColor else inactiveColor
            homeIcon.imageTintList = ColorStateList.valueOf(homeColor)
            homeLabel.setTextColor(homeColor)
            statsIcon.imageTintList = ColorStateList.valueOf(statsColor)
            statsLabel.setTextColor(statsColor)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            applyChromeVisibility(isAuthDestination = destination.id in authDestinationIds)
        }

        setUpDarkModeToggle()
    }

    private fun applyChromeVisibility(isAuthDestination: Boolean) {
        val chromeVisibility = if (isAuthDestination) View.GONE else View.VISIBLE
        binding.appBarLayout.visibility = chromeVisibility
        binding.bottomNavBar.root.visibility = chromeVisibility
        binding.drawerLayout.setDrawerLockMode(
            if (isAuthDestination) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED,
        )
        binding.navHostFragment.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            bottomMargin = if (isAuthDestination) 0 else bottomBarBaseHeightPx + latestBottomInset
        }
    }

    private fun setUpDarkModeToggle() {
        val toggle = binding.navView.toggleDarkMode
        val thumb = binding.navView.ivDarkModeThumb
        val thumbTravel = 20 * resources.displayMetrics.density

        fun render(dark: Boolean) {
            toggle.setBackgroundResource(
                if (dark) R.drawable.bg_drawer_switch_track else R.drawable.bg_drawer_switch_track_off,
            )
            thumb.translationX = if (dark) thumbTravel else 0f
        }

        var isDark = DarkModePreferences.isDarkModeEnabled(this)
        render(isDark)

        toggle.setOnClickListener {
            isDark = !isDark
            DarkModePreferences.setDarkModeEnabled(this, isDark)
            render(isDark)
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
            )
        }
    }

    /**
     * The app targets SDK 36, so Android 15+ enforces edge-to-edge: our own views are
     * responsible for staying clear of the status bar / camera cutout / gesture nav bar
     * instead of the system reserving that space for us.
     */
    private fun setUpInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val topSafeArea = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val bottomSafeArea = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            latestBottomInset = bottomSafeArea.bottom

            binding.appBarLayout.updatePadding(top = topSafeArea.top)
            binding.bottomNavBar.root.updatePadding(bottom = bottomSafeArea.bottom)
            binding.navView.root.updatePadding(top = topSafeArea.top, bottom = bottomSafeArea.bottom)

            val currentDestinationId = navController.currentDestination?.id
            applyChromeVisibility(isAuthDestination = currentDestinationId != null && currentDestinationId in authDestinationIds)

            insets
        }
    }
}
