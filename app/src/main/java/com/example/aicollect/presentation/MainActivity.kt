/** Activity única de la app: aloja el NavHostFragment, el drawer lateral, la barra inferior y la
 * app bar compartidos, y decide para cada destino de navegación si se muestran o si la pantalla
 * va a pantalla completa.*/
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
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import coil.load
import com.example.aicollect.R
import com.example.aicollect.data.DarkModePreferences
import com.example.aicollect.data.FilterSessionState
import com.example.aicollect.databinding.ActivityMainBinding
import com.example.aicollect.presentation.collection.FilterBottomSheetFragment
import com.example.aicollect.presentation.newpost.NewPostViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val newPostViewModel: NewPostViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var latestTopInset = 0
    private var latestBottomInset = 0
    private val bottomBarBaseHeightPx by lazy { (80 * resources.displayMetrics.density).roundToInt() }

    private val appBarContentHeightPx by lazy { (66 * resources.displayMetrics.density).roundToInt() }

    private val fullScreenDestinationIds = setOf(
        R.id.loginFragment,
        R.id.registerFragment,
        R.id.editProfileFragment,
        R.id.securityFragment,
        R.id.helpFragment,
        R.id.aboutFragment,
        R.id.newPostFragment,
        R.id.newPostDisambiguationFragment,
        R.id.editItemFragment,
    )

    private val backButtonDestinationIds = setOf(R.id.itemDetailFragment)

    private val filterVisibleDestinationIds = setOf(R.id.homeFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setUpInsets()

        binding.btnOpenMenu.setOnClickListener {
            if (navController.currentDestination?.id in backButtonDestinationIds) {
                navController.popBackStack()
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        binding.btnOpenFilters.setOnClickListener {
            FilterBottomSheetFragment().show(supportFragmentManager, FilterBottomSheetFragment.TAG)
        }

        binding.navView.btnCloseDrawer.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        listOf(
            binding.navView.rowEditProfile to R.id.editProfileFragment,
            binding.navView.rowSecurity to R.id.securityFragment,
            binding.navView.rowHelp to R.id.helpFragment,
            binding.navView.rowAbout to R.id.aboutFragment,
        ).forEach { (row, destinationId) ->
            row.setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                navController.navigate(destinationId)
            }
        }

        binding.navView.btnLogout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            lifecycleScope.launch {
                mainViewModel.signOut()
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
            if (isOnline()) {
                newPostViewModel.reset()
                navController.navigate(R.id.newPostFragment)
            } else {
                Snackbar.make(binding.root, R.string.new_post_no_internet_error, Snackbar.LENGTH_LONG)
                    .apply { anchorView = bottomBar }
                    .show()
            }
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
            if (destination.id !in filterVisibleDestinationIds) {
                FilterSessionState.reset()
            }
            applyChromeVisibility(
                showAppBar = destination.id !in fullScreenDestinationIds,
                showBottomNav = destination.id !in fullScreenDestinationIds,
                lockDrawer = destination.id in fullScreenDestinationIds || destination.id in backButtonDestinationIds,
            )
            val isBackButtonMode = destination.id in backButtonDestinationIds
            binding.btnOpenMenu.setImageResource(if (isBackButtonMode) R.drawable.ic_drawer_back else R.drawable.ic_hamburger)
            binding.btnOpenMenu.contentDescription =
                getString(if (isBackButtonMode) R.string.cd_item_detail_back else R.string.cd_open_menu)
            binding.btnOpenFilters.visibility =
                if (destination.id in filterVisibleDestinationIds) View.VISIBLE else View.GONE
            refreshDrawerProfile()
        }

        setUpDarkModeToggle()
    }

    private fun refreshDrawerProfile() {
        val profile = mainViewModel.drawerProfile()
        binding.navView.tvDrawerUserName.text = profile.displayNameOrFallback ?: getString(R.string.drawer_user_name)
        binding.navView.ivDrawerAvatar.load(profile.photoUrl) {
            placeholder(R.drawable.drawer_avatar)
            error(R.drawable.drawer_avatar)
            fallback(R.drawable.drawer_avatar)
        }
    }

    private fun applyChromeVisibility(showAppBar: Boolean, showBottomNav: Boolean, lockDrawer: Boolean) {
        binding.appBarLayout.visibility = if (showAppBar) View.VISIBLE else View.GONE
        binding.bottomNavBar.root.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        binding.drawerLayout.setDrawerLockMode(
            if (lockDrawer) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED,
        )
        binding.navHostFragment.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            topMargin = if (showAppBar) latestTopInset + appBarContentHeightPx else 0
            bottomMargin = if (showBottomNav) bottomBarBaseHeightPx + latestBottomInset else 0
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

    private fun setUpInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val topSafeArea = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val bottomSafeArea = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            latestTopInset = topSafeArea.top
            latestBottomInset = bottomSafeArea.bottom

            binding.appBarLayout.updatePadding(top = topSafeArea.top)
            binding.bottomNavBar.root.updatePadding(bottom = bottomSafeArea.bottom)
            binding.navView.root.updatePadding(top = topSafeArea.top, bottom = bottomSafeArea.bottom)

            val currentDestinationId = navController.currentDestination?.id
            val isFullScreen = currentDestinationId != null && currentDestinationId in fullScreenDestinationIds
            val isBackButtonMode = currentDestinationId != null && currentDestinationId in backButtonDestinationIds
            applyChromeVisibility(
                showAppBar = !isFullScreen,
                showBottomNav = !isFullScreen,
                lockDrawer = isFullScreen || isBackButtonMode,
            )

            insets
        }
    }
}
