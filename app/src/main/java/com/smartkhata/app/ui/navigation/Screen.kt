package com.smartkhata.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Contacts : Screen("contacts", "Contacts", Icons.AutoMirrored.Filled.MenuBook)
    object Reminders : Screen("reminders", "Reminders", Icons.Default.Alarm)
    object Backup : Screen("backup", "Backup & Restore", Icons.Default.CloudSync)

    object NewEntry : Screen("new_entry?mode={mode}&contactId={contactId}", "New Entry") {
        fun createRoute(mode: String = "type", contactId: Long = 0L): String {
            return "new_entry?mode=$mode&contactId=$contactId"
        }
    }

    object ContactLedger : Screen("contact_ledger/{contactId}", "Ledger") {
        fun createRoute(contactId: Long): String {
            return "contact_ledger/$contactId"
        }
    }
}
