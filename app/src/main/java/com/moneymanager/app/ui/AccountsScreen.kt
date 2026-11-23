package com.moneymanager.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.Account
import com.moneymanager.app.data.CategoryDefaults
import com.moneymanager.app.viewmodel.MoneyUiState
import com.moneymanager.app.viewmodel.MoneyViewModel

@Composable
fun AccountsScreen(
    state: MoneyUiState,
    vm: MoneyViewModel,
    onPayCreditCard: (ccAccount: Account, balance: Double) -> Unit
) {
    val balances = remember(state.accounts, state.transactions) { vm.computeAccountBalances() }
    var showAddDialog by remember { mutableStateOf(false) }

    val totalAssets = state.accounts.filter { !it.isLiability && !it.excludeFromTotals }.sumOf { balances[it.id] ?: 0.0 }
    val totalLiab = state.accounts.filter { it.isLiability && !it.excludeFromTotals }.sumOf { (balances[it.id] ?: 0.0).coerceAtLeast(0.0) }
    val netWorth = totalAssets - totalLiab

    val grouped = remember(state.accounts) { state.accounts.groupBy { it.groupName } }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Accounts", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    FilledTonalButton(onClick = { showAddDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Assets", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(vm.formatMoney(totalAssets), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeBlue)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Liabilities", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(vm.formatMoney(totalLiab), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Net Worth", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(vm.formatMoney(netWorth), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (groupName, accs) ->
                item(key = groupName) {
                    val groupTotal = accs.filter { !it.excludeFromTotals }.sumOf { balances[it.id] ?: 0.0 }
                    val isLiabGroup = accs.all { it.isLiability }
                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(groupName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                                Text(vm.formatMoney(groupTotal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isLiabGroup) ExpenseRed else IncomeBlue)
                            }
                            accs.forEachIndexed { idx, acc ->
                                if (idx > 0) HorizontalDivider(thickness = 0.5.dp)
                                val bal = balances[acc.id] ?: 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (acc.isLiability) {
                                            Text("Cut-off: ${acc.cutoffDay}th · Pay: ${acc.paymentDay}th", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(vm.formatMoney(bal), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (acc.isLiability) ExpenseRed else IncomeBlue)
                                        if (acc.isLiability && bal > 0.0) {
                                            Button(
                                                onClick = { onPayCreditCard(acc, bal) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Pay", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var group by remember { mutableStateOf(CategoryDefaults.accountGroups[1]) }
        var initBal by remember { mutableStateOf("0") }
        var groupExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Account / Card", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        OutlinedButton(onClick = { groupExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Group: $group")
                        }
                        DropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                            CategoryDefaults.accountGroups.forEach { g ->
                                DropdownMenuItem(text = { Text(g) }, onClick = {
                                    group = g
                                    groupExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Account Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = initBal, onValueChange = { initBal = it }, label = { Text("Initial Balance") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        vm.addAccount(
                            Account(
                                name = name.trim(),
                                groupName = group,
                                initialBalance = initBal.toDoubleOrNull() ?: 0.0,
                                isLiability = group == "Credit Cards"
                            )
                        )
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
