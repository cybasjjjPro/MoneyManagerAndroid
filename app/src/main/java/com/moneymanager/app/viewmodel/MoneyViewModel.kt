package com.moneymanager.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.moneymanager.app.data.Account
import com.moneymanager.app.data.BudgetItem
import com.moneymanager.app.data.MoneyDbHelper
import com.moneymanager.app.data.TransactionRecord
import com.moneymanager.app.data.TxType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import java.util.Locale

data class MoneyUiState(
    val accounts: List<Account> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val budgets: List<BudgetItem> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val currency: String = "RM ",
    val darkMode: Boolean = false,
    val accentHex: Long = 0xFFE84C3D,
    val carryOverEnabled: Boolean = true,
    val searchQuery: String = ""
)

class MoneyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MoneyDbHelper(application)
    private val prefs = application.getSharedPreferences("mm_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MoneyUiState(
            currency = prefs.getString("currency", "RM ") ?: "RM ",
            darkMode = prefs.getBoolean("darkMode", false),
            accentHex = prefs.getLong("accentHex", 0xFFE84C3D),
            carryOverEnabled = prefs.getBoolean("carryOver", true)
        )
    )
    val uiState: StateFlow<MoneyUiState> = _uiState.asStateFlow()

    init {
        reloadAll()
    }

    fun reloadAll() {
        _uiState.update {
            it.copy(
                accounts = db.getAllAccounts(),
                transactions = db.getAllTransactions(),
                budgets = db.getAllBudgets()
            )
        }
    }

    fun prevMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        _uiState.update { it.copy(selectedMonth = it.selectedMonth.plusMonths(1)) }
    }

    fun selectMonth(ym: YearMonth) {
        _uiState.update { it.copy(selectedMonth = ym) }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun setCurrency(symbol: String) {
        prefs.edit().putString("currency", symbol).apply()
        _uiState.update { it.copy(currency = symbol) }
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("darkMode", enabled).apply()
        _uiState.update { it.copy(darkMode = enabled) }
    }

    fun setAccentHex(hex: Long) {
        prefs.edit().putLong("accentHex", hex).apply()
        _uiState.update { it.copy(accentHex = hex) }
    }

    fun setCarryOver(enabled: Boolean) {
        prefs.edit().putBoolean("carryOver", enabled).apply()
        _uiState.update { it.copy(carryOverEnabled = enabled) }
    }

    fun saveTransaction(tx: TransactionRecord, transferFee: Double = 0.0) {
        db.saveTransaction(tx)
        if (tx.type == TxType.TRANSFER && transferFee > 0.0 && tx.id == 0L) {
            db.saveTransaction(
                TransactionRecord(
                    dateIso = tx.dateIso,
                    type = TxType.EXPENSE,
                    accountId = tx.accountId,
                    category = "Business",
                    amount = transferFee,
                    note = "Transfer Fee (${tx.note.ifBlank { "Internal" }})",
                    tags = "fee"
                )
            )
        }
        reloadAll()
    }

    fun deleteTransaction(id: Long) {
        db.deleteTransaction(id)
        reloadAll()
    }

    fun addAccount(acc: Account) {
        db.insertAccount(acc)
        reloadAll()
    }

    fun saveBudget(category: String, limit: Double) {
        db.upsertBudget(BudgetItem(category, limit))
        reloadAll()
    }

    fun deleteBudget(category: String) {
        db.deleteBudget(category)
        reloadAll()
    }

    fun resetDemoData() {
        db.resetToStarterData()
        reloadAll()
    }

    fun getAccountName(id: Long?): String {
        if (id == null) return "Unknown"
        return _uiState.value.accounts.find { it.id == id }?.name ?: "Unknown"
    }

    fun computeAccountBalances(): Map<Long, Double> {
        val state = _uiState.value
        val balances = mutableMapOf<Long, Double>()
        val accMap = state.accounts.associateBy { it.id }
        state.accounts.forEach { balances[it.id] = it.initialBalance }

        state.transactions.forEach { tx ->
            val src = accMap[tx.accountId]
            when (tx.type) {
                TxType.INCOME -> {
                    balances[tx.accountId] = (balances[tx.accountId] ?: 0.0) + tx.amount
                }
                TxType.EXPENSE -> {
                    val cur = balances[tx.accountId] ?: 0.0
                    balances[tx.accountId] = if (src?.isLiability == true) cur + tx.amount else cur - tx.amount
                }
                TxType.TRANSFER -> {
                    val curSrc = balances[tx.accountId] ?: 0.0
                    balances[tx.accountId] = if (src?.isLiability == true) curSrc + tx.amount else curSrc - tx.amount
                    tx.toAccountId?.let { toId ->
                        val dst = accMap[toId]
                        val curDst = balances[toId] ?: 0.0
                        balances[toId] = if (dst?.isLiability == true) curDst - tx.amount else curDst + tx.amount
                    }
                }
            }
        }
        return balances
    }

    fun computeCarryOver(ym: YearMonth): Double {
        if (!_uiState.value.carryOverEnabled) return 0.0
        val prefix = ym.toString() // YYYY-MM
        var net = 0.0
        _uiState.value.transactions.forEach { tx ->
            if (tx.dateIso.take(7) < prefix) {
                if (tx.type == TxType.INCOME) net += tx.amount
                else if (tx.type == TxType.EXPENSE) net -= tx.amount
            }
        }
        return net
    }

    fun formatMoney(amount: Double): String {
        val cur = _uiState.value.currency
        val sign = if (amount < 0) "-" else ""
        return String.format(Locale.US, "%s%s%,.2f", sign, cur, kotlin.math.abs(amount))
    }

    fun buildCsvExport(): String {
        val sb = StringBuilder()
        val q = '"'
        sb.appendLine("Date,Type,Account,ToAccount,Category,Subcategory,Amount,Note,Tags")
        _uiState.value.transactions.forEach { tx ->
            val safeNote = tx.note.replace(q.toString(), "$q$q")
            val toName = if (tx.toAccountId != null) getAccountName(tx.toAccountId) else ""
            sb.appendLine(
                "${tx.dateIso},${tx.type.name},$q${getAccountName(tx.accountId)}$q,$q$toName$q,$q${tx.category}$q,$q${tx.subCategory}$q,${tx.amount},$q$safeNote$q,$q${tx.tags}$q"
            )
        }
        return sb.toString()
    }
}
