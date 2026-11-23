package com.moneymanager.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneymanager.app.data.CategoryDefaults
import com.moneymanager.app.data.TransactionRecord
import com.moneymanager.app.data.TxType
import com.moneymanager.app.viewmodel.MoneyUiState
import com.moneymanager.app.viewmodel.MoneyViewModel
import java.time.LocalDate
import java.util.Locale

@Composable
fun AddEditTxDialog(
    state: MoneyUiState,
    vm: MoneyViewModel,
    editing: TransactionRecord?,
    prefillDate: String?,
    prefillType: TxType?,
    prefillFromAccountId: Long?,
    prefillToAccountId: Long?,
    prefillAmount: Double?,
    prefillNote: String?,
    onDismiss: () -> Unit
) {
    var txType by remember { mutableStateOf(editing?.type ?: prefillType ?: TxType.EXPENSE) }
    var dateIso by remember { mutableStateOf(editing?.dateIso ?: prefillDate ?: LocalDate.now().toString()) }
    var accountId by remember {
        mutableLongStateOf(editing?.accountId ?: prefillFromAccountId ?: state.accounts.firstOrNull()?.id ?: 1L)
    }
    var toAccountId by remember {
        mutableLongStateOf(editing?.toAccountId ?: prefillToAccountId ?: state.accounts.getOrNull(1)?.id ?: accountId)
    }
    var category by remember {
        mutableStateOf(
            editing?.category ?: if (txType == TxType.INCOME) CategoryDefaults.incomeCategories.keys.first()
            else CategoryDefaults.expenseCategories.keys.first()
        )
    }
    var subCategory by remember { mutableStateOf(editing?.subCategory ?: "") }
    var amountText by remember {
        mutableStateOf(
            editing?.amount?.toString() ?: prefillAmount?.let { String.format(Locale.US, "%.2f", it) } ?: ""
        )
    }
    var feeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(editing?.note ?: prefillNote ?: "") }
    var tags by remember { mutableStateOf(editing?.tags ?: "") }
    var quickText by remember { mutableStateOf("") }
    var showCalc by remember { mutableStateOf(false) }

    var accMenu by remember { mutableStateOf(false) }
    var toAccMenu by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf(false) }
    var subMenu by remember { mutableStateOf(false) }

    val activeCatMap = if (txType == TxType.INCOME) CategoryDefaults.incomeCategories else CategoryDefaults.expenseCategories
    val subList = activeCatMap[category].orEmpty()

    fun evalExpr(expr: String): Double? {
        val cleaned = expr.replace(" ", "")
        if (cleaned.isEmpty()) return null
        cleaned.toDoubleOrNull()?.let { return it }
        val ops = listOf('+', '-', '*', '/')
        for (op in ops) {
            val idx = cleaned.indexOf(op, startIndex = 1)
            if (idx > 0) {
                val left = cleaned.substring(0, idx).toDoubleOrNull() ?: return null
                val right = cleaned.substring(idx + 1).toDoubleOrNull() ?: return null
                return when (op) {
                    '+' -> left + right
                    '-' -> left - right
                    '*' -> left * right
                    '/' -> if (right != 0.0) left / right else null
                    else -> null
                }
            }
        }
        return null
    }

    fun applyQuickParse() {
        val raw = quickText.trim()
        if (raw.isEmpty()) return
        val lower = raw.lowercase(Locale.US)
        Regex("\b\d+(\.\d{1,2})?\b").find(raw)?.let { amountText = it.value }
        val foundTags = Regex("#(\w+)").findAll(raw).map { it.groupValues[1] }.toList()
        if (foundTags.isNotEmpty()) tags = foundTags.joinToString(",")

        state.accounts.find { acc ->
            lower.contains(acc.name.lowercase(Locale.US).split(" ").first()) ||
                (lower.contains("tng") && acc.name.lowercase(Locale.US).contains("touch")) ||
                (lower.contains("visa") && acc.isLiability)
        }?.let { accountId = it.id }

        when {
            Regex("coffee|latte|lunch|dinner|rice|grocer|food|makan").containsMatchIn(lower) -> {
                txType = TxType.EXPENSE
                category = "Food"
                subCategory = if (lower.contains("coffee") || lower.contains("latte")) "Coffee & Tea" else if (lower.contains("grocer")) "Groceries" else "Dining Out"
            }
            Regex("fuel|petrol|toll|rfid|grab|park").containsMatchIn(lower) -> {
                txType = TxType.EXPENSE
                category = "Transport"
                subCategory = if (lower.contains("toll") || lower.contains("rfid")) "Tolls / RFID" else "Fuel"
            }
            Regex("salary|pt|class|bonus|income").containsMatchIn(lower) -> {
                txType = TxType.INCOME
                category = "Coaching & Classes"
                subCategory = "PT Sessions"
            }
        }
        val cleanedNote = raw.replace(Regex("\b\d+(\.\d{1,2})?\b"), "").replace(Regex("#\w+"), "").trim()
        if (cleanedNote.isNotEmpty()) note = cleanedNote.replaceFirstChar { it.uppercase() }
    }

    fun commit(keepOpen: Boolean) {
        val amt = evalExpr(amountText) ?: return
        if (amt <= 0.0) return
        if (txType == TxType.TRANSFER && accountId == toAccountId) return
        val record = TransactionRecord(
            id = editing?.id ?: 0L,
            dateIso = dateIso.ifBlank { LocalDate.now().toString() },
            type = txType,
            accountId = accountId,
            toAccountId = if (txType == TxType.TRANSFER) toAccountId else null,
            category = if (txType == TxType.TRANSFER) "Transfer" else category,
            subCategory = if (txType == TxType.TRANSFER) "" else subCategory,
            amount = amt,
            note = note.trim(),
            tags = tags.replace("#", "").trim()
        )
        vm.saveTransaction(record, feeText.toDoubleOrNull() ?: 0.0)
        if (keepOpen) {
            amountText = ""
            note = ""
            feeText = ""
            quickText = ""
        } else {
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editing != null) "Edit Transaction" else "Add Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row {
                        if (editing != null) {
                            IconButton(onClick = {
                                vm.deleteTransaction(editing.id)
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                            }
                        }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }

                // 3-Way Type Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TxType.INCOME to "Income", TxType.EXPENSE to "Expense", TxType.TRANSFER to "Transfer").forEach { (tp, lbl) ->
                        FilterChip(
                            selected = txType == tp,
                            onClick = {
                                txType = tp
                                if (tp == TxType.INCOME) category = CategoryDefaults.incomeCategories.keys.first()
                                else if (tp == TxType.EXPENSE) category = CategoryDefaults.expenseCategories.keys.first()
                                subCategory = ""
                            },
                            label = { Text(lbl, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Smart Quick-Parse Bar
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = quickText,
                            onValueChange = { quickText = it },
                            placeholder = { Text("Quick: 18.50 coffee tng #work", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = { applyQuickParse() }) { Text("Fill", fontSize = 12.sp) }
                    }

                    OutlinedTextField(
                        value = dateIso,
                        onValueChange = { dateIso = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Account / From
                    Box {
                        OutlinedButton(onClick = { accMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("${if (txType == TxType.TRANSFER) "From" else "Account"}: ${vm.getAccountName(accountId)}")
                        }
                        DropdownMenu(expanded = accMenu, onDismissRequest = { accMenu = false }) {
                            state.accounts.forEach { acc ->
                                DropdownMenuItem(text = { Text(acc.name) }, onClick = {
                                    accountId = acc.id
                                    accMenu = false
                                })
                            }
                        }
                    }

                    if (txType == TxType.TRANSFER) {
                        Box {
                            OutlinedButton(onClick = { toAccMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("To: ${vm.getAccountName(toAccountId)}")
                            }
                            DropdownMenu(expanded = toAccMenu, onDismissRequest = { toAccMenu = false }) {
                                state.accounts.forEach { acc ->
                                    DropdownMenuItem(text = { Text(acc.name) }, onClick = {
                                        toAccountId = acc.id
                                        toAccMenu = false
                                    })
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { catMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(category, maxLines = 1)
                                }
                                DropdownMenu(expanded = catMenu, onDismissRequest = { catMenu = false }) {
                                    activeCatMap.keys.forEach { c ->
                                        DropdownMenuItem(text = { Text(c) }, onClick = {
                                            category = c
                                            subCategory = ""
                                            catMenu = false
                                        })
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(onClick = { subMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(subCategory.ifBlank { "Subcategory" }, maxLines = 1)
                                }
                                DropdownMenu(expanded = subMenu, onDismissRequest = { subMenu = false }) {
                                    subList.forEach { s ->
                                        DropdownMenuItem(text = { Text(s) }, onClick = {
                                            subCategory = s
                                            subMenu = false
                                        })
                                    }
                                }
                            }
                        }
                    }

                    // Amount + Built-in Calculator Toggle
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount (${state.currency.trim()})") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showCalc = !showCalc }) {
                            Icon(Icons.Default.Calculate, contentDescription = "Calculator")
                        }
                    }

                    if (showCalc) {
                        val rows = listOf(
                            listOf("7", "8", "9", "/"),
                            listOf("4", "5", "6", "*"),
                            listOf("1", "2", "3", "-"),
                            listOf("C", "0", ".", "+"),
                            listOf("=", "⌫")
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            rows.forEach { r ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    r.forEach { k ->
                                        FilledTonalButton(
                                            onClick = {
                                                when (k) {
                                                    "C" -> amountText = ""
                                                    "⌫" -> amountText = amountText.dropLast(1)
                                                    "=" -> evalExpr(amountText)?.let { amountText = String.format(Locale.US, "%.2f", it) }
                                                    else -> amountText += k
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text(k, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                    }

                    if (txType == TxType.TRANSFER) {
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it },
                            label = { Text("Transfer Fee (optional expense)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note / Merchant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma separated, e.g. work,meal)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { commit(keepOpen = false) }, modifier = Modifier.weight(2f)) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { commit(keepOpen = true) }, modifier = Modifier.weight(1f)) {
                        Text("Continue", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
