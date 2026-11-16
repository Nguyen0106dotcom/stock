package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.StockQuote
import com.example.ui.components.AddTickerDialog
import com.example.ui.components.PriceAlertDialog
import com.example.ui.screens.AlertsAndSettingsScreen
import com.example.ui.screens.ChartComparisonScreen
import com.example.ui.screens.StockDetailContent
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.VnPulseTheme
import com.example.ui.viewmodel.MarketViewModel

private object AppRoutes {
    const val ROUTE_WATCHLIST = "watchlist"
    const val ROUTE_CHARTS = "charts"
    const val ROUTE_ALERTS = "alerts"
    const val ROUTE_DETAIL = "detail/{symbol}"
    fun detailRoute(symbol: String) = "detail/$symbol"
}

private data class BottomNavTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MarketViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            VnPulseTheme(darkTheme = uiState.isDarkTheme) {
                VnPulseRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VnPulseRootApp(viewModel: MarketViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var showAddTickerDialog by remember { mutableStateOf(false) }
    var alertDialogQuote by remember { mutableStateOf<StockQuote?>(null) }

    val tabs = remember {
        listOf(
            BottomNavTab(
                route = AppRoutes.ROUTE_WATCHLIST,
                labelResId = R.string.tab_watchlist,
                selectedIcon = Icons.Filled.ShowChart,
                unselectedIcon = Icons.Outlined.ShowChart,
                testTag = "nav_tab_watchlist",
            ),
            BottomNavTab(
                route = AppRoutes.ROUTE_CHARTS,
                labelResId = R.string.tab_chart,
                selectedIcon = Icons.Filled.CandlestickChart,
                unselectedIcon = Icons.Outlined.CandlestickChart,
                testTag = "nav_tab_charts",
            ),
            BottomNavTab(
                route = AppRoutes.ROUTE_ALERTS,
                labelResId = R.string.tab_alerts,
                selectedIcon = Icons.Filled.Notifications,
                unselectedIcon = Icons.Outlined.Notifications,
                testTag = "nav_tab_alerts",
            ),
        )
    }

    val isDetailRoute = currentDestination?.route?.startsWith("detail/") == true

    fun cycleInterval() {
        val options = listOf(3, 5, 10, 30, 0)
        val idx = options.indexOf(uiState.refreshIntervalSeconds)
        val next = options[(idx + 1) % options.size]
        viewModel.setRefreshIntervalSeconds(next)
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.statusBars,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!isExpandedWidth && !isDetailRoute) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val label = stringResource(tab.labelResId)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isExpandedWidth) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            val label = stringResource(tab.labelResId)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.ROUTE_WATCHLIST,
                    ) {
                        composable(AppRoutes.ROUTE_WATCHLIST) {
                            if (isExpandedWidth) {
                                // Adaptive List-Detail layout on tablets / foldables
                                Row(modifier = Modifier.fillMaxSize()) {
                                    WatchlistScreen(
                                        uiState = uiState,
                                        isSplitLayout = true,
                                        onSelectTicker = { sym -> viewModel.selectSymbol(sym) },
                                        onManualRefresh = { viewModel.manualRefresh() },
                                        onCycleRefreshInterval = { cycleInterval() },
                                        onToggleTheme = { viewModel.toggleDarkTheme() },
                                        onFilterChange = { viewModel.setFilter(it) },
                                        onOpenAddTicker = { showAddTickerDialog = true },
                                        onOpenAlertForQuote = { alertDialogQuote = it },
                                        onDismissAlertBanner = { viewModel.dismissAlertBanner() },
                                        modifier = Modifier
                                            .width(420.dp)
                                            .fillMaxHeight(),
                                    )
                                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    StockDetailContent(
                                        quote = uiState.selectedQuote,
                                        uiState = uiState,
                                        showBackButton = false,
                                        onBack = {},
                                        onSelectTimeframe = { viewModel.selectTimeframe(it) },
                                        onSelectChartStyle = { viewModel.setChartStyle(it) },
                                        onRetryChart = { viewModel.loadChart() },
                                        onOpenAlertDialog = { alertDialogQuote = it },
                                        onRemoveCustomSymbol = { viewModel.removeSymbol(it) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                    )
                                }
                            } else {
                                WatchlistScreen(
                                    uiState = uiState,
                                    isSplitLayout = false,
                                    onSelectTicker = { sym ->
                                        viewModel.selectSymbol(sym)
                                        navController.navigate(AppRoutes.detailRoute(sym))
                                    },
                                    onManualRefresh = { viewModel.manualRefresh() },
                                    onCycleRefreshInterval = { cycleInterval() },
                                    onToggleTheme = { viewModel.toggleDarkTheme() },
                                    onFilterChange = { viewModel.setFilter(it) },
                                    onOpenAddTicker = { showAddTickerDialog = true },
                                    onOpenAlertForQuote = { alertDialogQuote = it },
                                    onDismissAlertBanner = { viewModel.dismissAlertBanner() },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        composable(AppRoutes.ROUTE_CHARTS) {
                            ChartComparisonScreen(
                                uiState = uiState,
                                onSelectSymbol = { viewModel.selectSymbol(it) },
                                onSelectTimeframe = { viewModel.selectTimeframe(it) },
                                onSelectChartStyle = { viewModel.setChartStyle(it) },
                                onLoadComparison = { viewModel.loadComparisonCharts(it) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        composable(AppRoutes.ROUTE_ALERTS) {
                            AlertsAndSettingsScreen(
                                uiState = uiState,
                                onSelectInterval = { viewModel.setRefreshIntervalSeconds(it) },
                                onToggleTheme = { viewModel.toggleDarkTheme() },
                                onOpenAlertDialog = { alertDialogQuote = it },
                                onResetDefaults = { viewModel.resetDefaultSymbols() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        composable(
                            route = AppRoutes.ROUTE_DETAIL,
                            arguments = listOf(navArgument("symbol") { type = NavType.StringType }),
                        ) { backStack ->
                            val sym = backStack.arguments?.getString("symbol") ?: uiState.selectedSymbol
                            val quote = uiState.quotes.firstOrNull { it.symbol.equals(sym, ignoreCase = true) }
                                ?: uiState.selectedQuote
                            StockDetailContent(
                                quote = quote,
                                uiState = uiState,
                                showBackButton = true,
                                onBack = { navController.popBackStack() },
                                onSelectTimeframe = { viewModel.selectTimeframe(it) },
                                onSelectChartStyle = { viewModel.setChartStyle(it) },
                                onRetryChart = { viewModel.loadChart() },
                                onOpenAlertDialog = { alertDialogQuote = it },
                                onRemoveCustomSymbol = { viewModel.removeSymbol(it) },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddTickerDialog) {
        AddTickerDialog(
            existingSymbols = uiState.symbols,
            onDismiss = { showAddTickerDialog = false },
            onAddTicker = { viewModel.addSymbol(it) },
        )
    }

    alertDialogQuote?.let { targetQuote ->
        val existingAlert = uiState.priceAlerts.firstOrNull {
            it.symbol.equals(targetQuote.symbol, ignoreCase = true)
        }
        PriceAlertDialog(
            symbol = targetQuote.symbol,
            currentPrice = targetQuote.displayPrice,
            existingAlert = existingAlert,
            onDismiss = { alertDialogQuote = null },
            onSave = { above, below ->
                viewModel.savePriceAlert(targetQuote.symbol, above, below, enabled = true)
            },
            onDelete = {
                viewModel.removePriceAlert(targetQuote.symbol)
            },
        )
    }
}
