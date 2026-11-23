package com.moneymanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneymanager.app.data.TransactionRecord
import com.moneymanager.app.data.TxType
import com.moneymanager.app.ui.*
import com.moneymanager.app.viewmodel.MoneyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MoneyViewModel = viewModel()
            val state by vm.uiState.collectAsState()

            MoneyManagerTheme(darkMode = state.darkMode, accentHex = state.accentHex) {
                var selectedTab by remember { mutableIntStateOf(0) }
                var showTxDialog by remember { mutableStateOf(false) }
                var editingTx by remember { mutableStateOf<TransactionRecord?>(null) }
                var prefillDate by remember { mutableStateOf<String?>(null) }
                var prefillType by remember { mutableStateOf<TxType?>(null) }
                var prefillFromId by remember { mutableStateOf<Long?>(null) }
                var prefillToId by remember { mutableStateOf<Long?>(null) }
                var prefillAmt by remember { mutableStateOf<Double?>(null) }
                var prefillNote by remember { mutableStateOf<String?>(null) }

                fun openNewTx(
                    date: String? = null,
                    type: TxType? = null,
                    fromId: Long? = null,
                    toId: Long? = null,
                    amt: Double? = null,
                    note: String? = null
                ) {
                    editingTx = null
                    prefillDate = date
                    prefillType = type
                    prefillFromId = fromId
                    prefillToId = toId
                    prefillAmt = amt
                    prefillNote = note
                    showTxDialog = true
                }

                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { openNewTx() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                        }
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Book, contentDescription = null) },
                                label = { Text("Trans.") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                                label = { Text("Stats") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                                label = { Text("Accounts") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                                label = { Text("More") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> TransScreen(
                                state = state,
                                vm = vm,
                                onEditTx = { tx ->
                                    editingTx = tx
                                    showTxDialog = true
                                },
                                onQuickAddOnDate = { iso -> openNewTx(date = iso) }
                            )
                            1 -> StatsScreen(state = state, vm = vm)
                            2 -> AccountsScreen(
                                state = state,
                                vm = vm,
                                onPayCreditCard = { cc, bal ->
                                    val bankId = state.accounts.firstOrNull { !it.isLiability && it.groupName == "Bank Accounts" }?.id
                                        ?: state.accounts.firstOrNull()?.id
                                    openNewTx(
                                        type = TxType.TRANSFER,
                                        fromId = bankId,
                                        toId = cc.id,
                                        amt = bal,
                                        note = "Card Settlement (${cc.name})"
                                    )
                                }
                            )
                            3 -> MoreScreen(state = state, vm = vm)
                        }
                    }
                }

                if (showTxDialog) {
                    AddEditTxDialog(
                        state = state,
                        vm = vm,
                        editing = editingTx,
                        prefillDate = prefillDate,
                        prefillType = prefillType,
                        prefillFromAccountId = prefillFromId,
                        prefillToAccountId = prefillToId,
                        prefillAmount = prefillAmt,
                        prefillNote = prefillNote,
                        onDismiss = { showTxDialog = false }
                    )
                }
            }
        }
    }
}
