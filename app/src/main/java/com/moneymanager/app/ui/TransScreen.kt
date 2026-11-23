package com.moneymanager.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.TransactionRecord
import com.moneymanager.app.data.TxType
import com.moneymanager.app.viewmodel.MoneyUiState
import com.moneymanager.app.viewmodel.MoneyViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TransScreen(
    state: MoneyUiState,
    vm: MoneyViewModel,
    onEditTx: (TransactionRecord) -> Unit,
    onQuickAddOnDate: (String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0=Daily, 1=Calendar, 2=Monthly
    var showSearch by remember { mutableStateOf(false) }

    val monthPrefix = state.selectedMonth.toString()
    val monthTxs = remember(state.transactions, monthPrefix, state.searchQuery) {
        val q = state.searchQuery.trim().lowercase(Locale.US)
        state.transactions.filter { tx ->
            if (!tx.dateIso.startsWith(monthPrefix)) return@filter false
            if (q.isEmpty()) return@filter true
            val hay = "${tx.note} ${tx.category} ${tx.subCategory} ${tx.tags} ${vm.getAccountName(tx.accountId)}".lowercase(Locale.US)
            hay.contains(q.removePrefix("#"))
        }
    }

    val totalIncome = monthTxs.filter { it.type == TxType.INCOME }.sumOf { it.amount }
    val totalExpense = monthTxs.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
    val carryOver = vm.computeCarryOver(state.selectedMonth)
    val netTotal = totalIncome - totalExpense + carryOver

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Header Bar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.prevMonth() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                        }
                        Text(
                            text = state.selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { vm.nextMonth() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                        }
                    }
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) vm.setSearchQuery("")
                    }) {
                        Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                    }
                }

                if (showSearch) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { vm.setSearchQuery(it) },
                        placeholder = { Text("Search note, category, or #tag...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                // Sub-tabs: Daily | Calendar | Monthly
                TabRow(selectedTabIndex = subTab, containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("Daily", "Calendar", "Monthly").forEachIndexed { idx, label ->
                        Tab(
                            selected = subTab == idx,
                            onClick = { subTab = idx },
                            text = { Text(label, fontSize = 13.sp, fontWeight = if (subTab == idx) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // 3-Column Summary Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryCol("Income", vm.formatMoney(totalIncome), IncomeBlue, Modifier.weight(1f))
                    SummaryCol("Expenses", vm.formatMoney(totalExpense), ExpenseRed, Modifier.weight(1f))
                    SummaryCol(
                        if (state.carryOverEnabled && carryOver != 0.0) "Total (+CO)" else "Total",
                        vm.formatMoney(netTotal),
                        MaterialTheme.colorScheme.onSurface,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        HorizontalDivider()

        when (subTab) {
            0 -> DailyListView(monthTxs, vm, onEditTx)
            1 -> CalendarGridView(state.selectedMonth, monthTxs, vm, onEditTx, onQuickAddOnDate)
            2 -> MonthlySummaryView(state.selectedMonth.year, state.transactions, vm) { chosenYm ->
                vm.selectMonth(chosenYm)
                subTab = 0
            }
        }
    }
}

@Composable
private fun SummaryCol(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DailyListView(
    txs: List<TransactionRecord>,
    vm: MoneyViewModel,
    onEditTx: (TransactionRecord) -> Unit
) {
    if (txs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }
    val grouped = remember(txs) { txs.groupBy { it.dateIso }.toSortedMap(compareByDescending { it }) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (dateIso, dayList) ->
            item(key = dateIso) {
                val parsed = runCatching { LocalDate.parse(dateIso) }.getOrNull()
                val dayInc = dayList.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                val dayExp = dayList.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = parsed?.dayOfMonth?.toString()?.padStart(2, '0') ?: dateIso.takeLast(2),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = parsed?.dayOfWeek?.name?.take(3) ?: "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = dateIso.take(7),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (dayInc > 0) Text(vm.formatMoney(dayInc), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeBlue)
                                if (dayExp > 0) Text(vm.formatMoney(dayExp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                        }

                        dayList.forEachIndexed { idx, tx ->
                            if (idx > 0) HorizontalDivider(thickness = 0.5.dp)
                            TxRowItem(tx, vm, onClick = { onEditTx(tx) })
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun TxRowItem(tx: TransactionRecord, vm: MoneyViewModel, onClick: () -> Unit) {
    val amtColor = when (tx.type) {
        TxType.INCOME -> IncomeBlue
        TxType.EXPENSE -> ExpenseRed
        TxType.TRANSFER -> TransferSlate
    }
    val subLine = if (tx.type == TxType.TRANSFER) {
        "${vm.getAccountName(tx.accountId)} -> ${vm.getAccountName(tx.toAccountId)}"
    } else {
        val tagStr = tx.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ") { "#$it" }
        listOf(vm.getAccountName(tx.accountId), tagStr).filter { it.isNotEmpty() }.joinToString("  ")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(88.dp)) {
            Text(tx.category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tx.subCategory.isNotBlank()) {
                Text(tx.subCategory, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
            Text(tx.note.ifBlank { tx.category }, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subLine, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(vm.formatMoney(tx.amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = amtColor)
    }
}

@Composable
private fun CalendarGridView(
    ym: YearMonth,
    txs: List<TransactionRecord>,
    vm: MoneyViewModel,
    onEditTx: (TransactionRecord) -> Unit,
    onQuickAddOnDate: (String) -> Unit
) {
    var selectedDate by remember(ym) { mutableStateOf(ym.atDay(1).toString()) }
    val byDay = remember(txs) { txs.groupBy { it.dateIso } }
    val firstDow = ym.atDay(1).dayOfWeek.value % 7 // Sun=0 .. Sat=6
    val totalDays = ym.lengthOfMonth()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { d ->
                            Text(
                                d,
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (d == "Sun") ExpenseRed else if (d == "Sat") IncomeBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val cells = firstDow + totalDays
                    val rows = (cells + 6) / 7
                    for (r in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val dayNum = r * 7 + c - firstDow + 1
                                if (dayNum in 1..totalDays) {
                                    val iso = ym.atDay(dayNum).toString()
                                    val list = byDay[iso].orEmpty()
                                    val inc = list.filter { it.type == TxType.INCOME }.sumOf { it.amount }
                                    val exp = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
                                    val isSel = selectedDate == iso
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent)
                                            .clickable { selectedDate = iso }
                                            .padding(3.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(dayNum.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                                            if (inc > 0) Text("+${inc.toInt()}", fontSize = 9.sp, color = IncomeBlue, maxLines = 1)
                                            if (exp > 0) Text("-${exp.toInt()}", fontSize = 9.sp, color = ExpenseRed, maxLines = 1)
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).height(56.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val dayTxs = byDay[selectedDate].orEmpty()
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedDate, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = { onQuickAddOnDate(selectedDate) }) {
                            Text("+ Add on this date", fontSize = 12.sp)
                        }
                    }
                    if (dayTxs.isEmpty()) {
                        Text("No entries on this date.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        dayTxs.forEach { tx ->
                            HorizontalDivider(thickness = 0.5.dp)
                            TxRowItem(tx, vm, onClick = { onEditTx(tx) })
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun MonthlySummaryView(
    year: Int,
    allTxs: List<TransactionRecord>,
    vm: MoneyViewModel,
    onJumpMonth: (YearMonth) -> Unit
) {
    val months = (12 downTo 1).map { YearMonth.of(year, it) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(months) { ym ->
            val prefix = ym.toString()
            val list = allTxs.filter { it.dateIso.startsWith(prefix) }
            val inc = list.filter { it.type == TxType.INCOME }.sumOf { it.amount }
            val exp = list.filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().clickable { onJumpMonth(ym) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(ym.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${list.size} records", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCol("Inc", vm.formatMoney(inc), IncomeBlue)
                        SummaryCol("Exp", vm.formatMoney(exp), ExpenseRed)
                        SummaryCol("Net", vm.formatMoney(inc - exp), MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
