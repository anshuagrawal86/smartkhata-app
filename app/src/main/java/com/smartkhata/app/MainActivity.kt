package com.smartkhata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.smartkhata.app.ui.navigation.Screen
import com.smartkhata.app.ui.screens.backup.BackupScreen
import com.smartkhata.app.ui.screens.backup.BackupViewModel
import com.smartkhata.app.ui.screens.contactledger.ContactLedgerScreen
import com.smartkhata.app.ui.screens.contactledger.ContactLedgerViewModel
import com.smartkhata.app.ui.screens.contacts.ContactsScreen
import com.smartkhata.app.ui.screens.contacts.ContactsViewModel
import com.smartkhata.app.ui.screens.dashboard.DashboardScreen
import com.smartkhata.app.ui.screens.dashboard.DashboardViewModel
import com.smartkhata.app.ui.screens.newentry.NewEntryScreen
import com.smartkhata.app.ui.screens.newentry.NewEntryViewModel
import com.smartkhata.app.ui.screens.reminders.RemindersScreen
import com.smartkhata.app.ui.screens.reminders.RemindersViewModel
import com.smartkhata.app.ui.theme.PrimaryBlue
import com.smartkhata.app.ui.theme.SmartKhataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartKhataTheme {
                MainAppScaffold()
            }
        }
    }
}

@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Contacts,
        Screen.Reminders,
        Screen.Backup
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    val app = SmartKhataApp.instance

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(app.ledgerRepository)
                )
                DashboardScreen(
                    viewModel = vm,
                    onNavigateToNewEntry = { mode ->
                        navController.navigate(Screen.NewEntry.createRoute(mode = mode))
                    },
                    onNavigateToContactLedger = { contactId ->
                        navController.navigate(Screen.ContactLedger.createRoute(contactId))
                    }
                )
            }

            composable(Screen.Contacts.route) {
                val vm: ContactsViewModel = viewModel(
                    factory = ContactsViewModel.Factory(app.ledgerRepository)
                )
                ContactsScreen(
                    viewModel = vm,
                    onNavigateToLedger = { contactId ->
                        navController.navigate(Screen.ContactLedger.createRoute(contactId))
                    }
                )
            }

            composable(Screen.Reminders.route) {
                val vm: RemindersViewModel = viewModel(
                    factory = RemindersViewModel.Factory(app, app.ledgerRepository)
                )
                RemindersScreen(viewModel = vm)
            }

            composable(Screen.Backup.route) {
                val vm: BackupViewModel = viewModel(
                    factory = BackupViewModel.Factory(app, app.backupRepository)
                )
                BackupScreen(viewModel = vm)
            }

            composable(
                route = Screen.NewEntry.route,
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "type"
                    },
                    navArgument("contactId") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "type"
                val vm: NewEntryViewModel = viewModel(
                    factory = NewEntryViewModel.Factory(app, app.ledgerRepository)
                )
                NewEntryScreen(
                    viewModel = vm,
                    initialMode = mode,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ContactLedger.route,
                arguments = listOf(
                    navArgument("contactId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val contactId = backStackEntry.arguments?.getLong("contactId") ?: 0L
                val vm: ContactLedgerViewModel = viewModel(
                    factory = ContactLedgerViewModel.Factory(contactId, app.ledgerRepository)
                )
                ContactLedgerScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onAddNewEntryForContact = { cid ->
                        navController.navigate(Screen.NewEntry.createRoute(mode = "type", contactId = cid))
                    }
                )
            }
        }
    }
}
