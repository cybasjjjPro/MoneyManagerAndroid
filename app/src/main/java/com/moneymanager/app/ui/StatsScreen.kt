package com.moneymanager.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.CategoryDefaults
import com.moneymanager.app.data.TxType
import com.moneymanager.app.viewmodel.MoneyUiState
import com.moneymanager.app.viewmodel.MoneyViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PieColors = listOf(
    Color(0xFFE84C3D), Color(0xFF3B82F6), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF14B8A6), Color(0xFF6366F1)
)

@Composable
fun StatsScreen(state: MoneyUiState, vm: MoneyViewModel) {
    var subTab by remember { mutableIntStateOf(0) } // 0=Stats, 1=Budget
    var statsType by remember { mutableStateOf(TxType.EXPENSE) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    val monthPrefix = state.selectedMonth.toString()
    val monthTxs = remember(state.transactions, monthPrefix) {
        state.transactions.filter { it.dateIso.startsWith(monthPrefix) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.prevMonth() }) { Icon(Icons.Default.ChevronLeft, null) }
                        Text(
                            state.selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { vm.nextMonth() }) { Icon(Icons.Default.ChevronRight, null) }
                    }
                    if (subTab == 1) {
                        FilledTonalButton(onClick = { showBudgetDialog = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Budget", fontSize = 12.sp)
                        }
                    }
                }
                TabRow(selectedTabIndex = subTab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Stats (Pie)", fontSize = 13.sp) })
                    Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Budgets", fontSize = 13.sp) })
                }
            }
        }

        if (subTab == 0) {
            val filtered = monthTxs.filter { it.type == statsType }
            val grouped = filtered.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
            val total = grouped.sumOf { it.value }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = statsType == TxType.EXPENSE,
                            onClick = { statsType = TxType.EXPENSE },
                            label = { Text("Expenses") }
                        )
                        FilterChip(
                            selected = statsType == TxType.INCOME,
                            onClick = { statsType = TxType.INCOME },
                            label = { Text("Income") }
                        )
                    }
                }

                item {
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = Stroke(width = 36f)
                                    val inset = 22f
                                    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                                    val topLeft = Offset(inset, inset)
                                    if (total <= 0.0) {
                                        drawArc(Color.LightGray, 0f, 360f, false, topLeft, arcSize, style = stroke)
                                    } else {
                                        var start = -90f
                                        grouped.forEachIndexed { idx, entry ->
                                            val sweep = (entry.value / total * 360.0).toFloat()
                                            drawArc(
                                                color = PieColors[idx % PieColors.size],
                                                startAngle = start,
                                                sweepAngle = sweep,
                                                useCenter = false,
                                                topLeft = topLeft,
                                                size = arcSize,
                                                style = stroke
                                            )
                                            start += sweep
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (statsType == TxType.EXPENSE) "Total Spent" else "Total Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text(vm.formatMoney(total), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(grouped) { idx, entry ->
                    val pct = if (total > 0) (entry.value / total * 100.0) else 0.0
                    val col = PieColors[idx % PieColors.size]
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(col)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(String.format(Locale.US, "%.1f%%", pct), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(entry.key, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Text(vm.formatMoney(entry.value), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        } else {
            // Budget View with Pace Marker
            val spentByCat = monthTxs.filter { it.type == TxType.EXPENSE }
                .groupBy { it.category }.mapValues { (_, l) -> l.sumOf { it.amount } }
            val paceFraction = (LocalDate.now().dayOfMonth.toFloat() / state.selectedMonth.lengthOfMonth().toFloat()).coerceIn(0f, 1f)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.budgets) { _, item ->
                    val spent = spentByCat[item.category] ?: 0.0
                    val ratio = if (item.monthlyLimit > 0) (spent / item.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
                    val over = spent > item.monthlyLimit
                    val barColor = if (over) ExpenseRed else if (ratio > 0.8f) BudgetAmber else BudgetGreen

                    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (over) "Over by ${vm.formatMoney(spent - item.monthlyLimit)}" else "${vm.formatMoney(item.monthlyLimit - spent)} left",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (over) ExpenseRed else IncomeBlue
                                    )
                                    IconButton(onClick = { vm.deleteBudget(item.category) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(ratio)
                                        .background(barColor)
                                )
                                // Today's pace marker line
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(paceFraction)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Spent: ${vm.formatMoney(spent)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("Limit: ${vm.formatMoney(item.monthlyLimit)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showBudgetDialog) {
        var selectedCat by remember { mutableStateOf(CategoryDefaults.expenseCategories.keys.first()) }
        var limitText by remember { mutableStateOf("") }
        var catExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box {
                        OutlinedButton(onClick = { catExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Category: $selectedCat")
                        }
                        DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                            CategoryDefaults.expenseCategories.keys.forEach { cat ->
                                DropdownMenuItem(text = { Text(cat) }, onClick = {
                                    selectedCat = cat
                                    catExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it },
                        label = { Text("Monthly Limit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = limitText.toDoubleOrNull()
                    if (amt != null && amt > 0.0) {
                        vm.saveBudget(selectedCat, amt)
                        showBudgetDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
