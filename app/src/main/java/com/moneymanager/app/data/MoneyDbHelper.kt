package com.moneymanager.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.YearMonth

class MoneyDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "money_manager.db"
        private const val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                group_name TEXT NOT NULL,
                initial_balance REAL NOT NULL DEFAULT 0.0,
                is_liability INTEGER NOT NULL DEFAULT 0,
                exclude_from_totals INTEGER NOT NULL DEFAULT 0,
                cutoff_day INTEGER NOT NULL DEFAULT 15,
                payment_day INTEGER NOT NULL DEFAULT 5
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date_iso TEXT NOT NULL,
                type TEXT NOT NULL,
                account_id INTEGER NOT NULL,
                to_account_id INTEGER,
                category TEXT NOT NULL,
                sub_category TEXT NOT NULL DEFAULT '',
                amount REAL NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE budgets (
                category TEXT PRIMARY KEY,
                monthly_limit REAL NOT NULL
            )
            """.trimIndent()
        )
        seedStarterData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS accounts")
        db.execSQL("DROP TABLE IF EXISTS budgets")
        onCreate(db)
    }

    fun resetToStarterData() {
        val db = writableDatabase
        db.execSQL("DELETE FROM transactions")
        db.execSQL("DELETE FROM accounts")
        db.execSQL("DELETE FROM budgets")
        seedStarterData(db)
    }

    private fun seedStarterData(db: SQLiteDatabase) {
        val cashId = insertAccountInternal(db, Account(name = "Cash Wallet", groupName = "Cash", initialBalance = 320.0))
        val maybankId = insertAccountInternal(db, Account(name = "Maybank MAE", groupName = "Bank Accounts", initialBalance = 6450.0))
        val mbsbId = insertAccountInternal(db, Account(name = "MBSB Bank", groupName = "Bank Accounts", initialBalance = 4200.0))
        val tngId = insertAccountInternal(db, Account(name = "Touch 'n Go eWallet", groupName = "eWallets", initialBalance = 485.0))
        val grabId = insertAccountInternal(db, Account(name = "GrabPay", groupName = "eWallets", initialBalance = 140.0))
        val ccId = insertAccountInternal(
            db,
            Account(
                name = "Maybank Visa Signature",
                groupName = "Credit Cards",
                initialBalance = 0.0,
                isLiability = true,
                cutoffDay = 15,
                paymentDay = 5
            )
        )
        insertAccountInternal(db, Account(name = "Emergency FD / Gold", groupName = "Savings & Investments", initialBalance = 12000.0))

        val ym = YearMonth.now()
        val today = LocalDate.now()
        fun d(day: Int): String = ym.atDay(day.coerceIn(1, ym.lengthOfMonth())).toString()

        val starterTxs = listOf(
            TransactionRecord(dateIso = d(1), type = TxType.INCOME, accountId = maybankId, category = "Coaching & Classes", subCategory = "PT Sessions", amount = 3800.0, note = "PT Package Renewals", tags = "coaching"),
            TransactionRecord(dateIso = d(3), type = TxType.INCOME, accountId = mbsbId, category = "Coaching & Classes", subCategory = "Group Classes", amount = 1450.0, note = "Studio Group Classes", tags = "classes"),
            TransactionRecord(dateIso = d(5), type = TxType.EXPENSE, accountId = maybankId, category = "Household", subCategory = "Internet", amount = 129.0, note = "Home Fibre Broadband", tags = "bills,auto"),
            TransactionRecord(dateIso = d(7), type = TxType.EXPENSE, accountId = tngId, category = "Food", subCategory = "Groceries", amount = 186.40, note = "Jaya Grocer Fresh Produce", tags = "groceries"),
            TransactionRecord(dateIso = d(9), type = TxType.TRANSFER, accountId = maybankId, toAccountId = tngId, category = "Transfer", amount = 300.0, note = "TNG eWallet Reload"),
            TransactionRecord(dateIso = d(11), type = TxType.EXPENSE, accountId = ccId, category = "Health & Fitness", subCategory = "Gym / Gear", amount = 249.0, note = "Training Shoes & Grips", tags = "fitness,tax"),
            TransactionRecord(dateIso = d(14), type = TxType.EXPENSE, accountId = ccId, category = "Food", subCategory = "Dining Out", amount = 168.0, note = "Weekend Dinner", tags = "dining"),
            TransactionRecord(dateIso = d(16), type = TxType.EXPENSE, accountId = tngId, category = "Transport", subCategory = "Fuel", amount = 95.0, note = "Petronas RON95", tags = "car"),
            TransactionRecord(dateIso = d(20), type = TxType.EXPENSE, accountId = maybankId, category = "Household", subCategory = "Utilities", amount = 218.50, note = "TNB Electricity Bill", tags = "bills"),
            TransactionRecord(dateIso = today.toString(), type = TxType.EXPENSE, accountId = grabId, category = "Food", subCategory = "Coffee & Tea", amount = 18.50, note = "Client Coffee Catch-up", tags = "work,coffee"),
            TransactionRecord(dateIso = today.toString(), type = TxType.EXPENSE, accountId = cashId, category = "Food", subCategory = "Dining Out", amount = 16.50, note = "Lunch", tags = "lunch")
        )
        starterTxs.forEach { insertTransactionInternal(db, it) }

        val starterBudgets = listOf(
            BudgetItem("Food", 1100.0),
            BudgetItem("Transport", 450.0),
            BudgetItem("Household", 600.0),
            BudgetItem("Health & Fitness", 350.0),
            BudgetItem("Shopping", 400.0)
        )
        starterBudgets.forEach { upsertBudgetInternal(db, it) }
    }

    private fun insertAccountInternal(db: SQLiteDatabase, acc: Account): Long {
        val cv = ContentValues().apply {
            put("name", acc.name)
            put("group_name", acc.groupName)
            put("initial_balance", acc.initialBalance)
            put("is_liability", if (acc.isLiability) 1 else 0)
            put("exclude_from_totals", if (acc.excludeFromTotals) 1 else 0)
            put("cutoff_day", acc.cutoffDay)
            put("payment_day", acc.paymentDay)
        }
        return db.insert("accounts", null, cv)
    }

    private fun insertTransactionInternal(db: SQLiteDatabase, tx: TransactionRecord): Long {
        val cv = ContentValues().apply {
            put("date_iso", tx.dateIso)
            put("type", tx.type.name)
            put("account_id", tx.accountId)
            if (tx.toAccountId != null) put("to_account_id", tx.toAccountId) else putNull("to_account_id")
            put("category", tx.category)
            put("sub_category", tx.subCategory)
            put("amount", tx.amount)
            put("note", tx.note)
            put("tags", tx.tags)
        }
        return db.insert("transactions", null, cv)
    }

    private fun upsertBudgetInternal(db: SQLiteDatabase, item: BudgetItem) {
        val cv = ContentValues().apply {
            put("category", item.category)
            put("monthly_limit", item.monthlyLimit)
        }
        db.insertWithOnConflict("budgets", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllAccounts(): List<Account> {
        val list = mutableListOf<Account>()
        readableDatabase.rawQuery("SELECT * FROM accounts ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Account(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        name = c.getString(c.getColumnIndexOrThrow("name")),
                        groupName = c.getString(c.getColumnIndexOrThrow("group_name")),
                        initialBalance = c.getDouble(c.getColumnIndexOrThrow("initial_balance")),
                        isLiability = c.getInt(c.getColumnIndexOrThrow("is_liability")) == 1,
                        excludeFromTotals = c.getInt(c.getColumnIndexOrThrow("exclude_from_totals")) == 1,
                        cutoffDay = c.getInt(c.getColumnIndexOrThrow("cutoff_day")),
                        paymentDay = c.getInt(c.getColumnIndexOrThrow("payment_day"))
                    )
                )
            }
        }
        return list
    }

    fun insertAccount(acc: Account): Long = insertAccountInternal(writableDatabase, acc)

    fun getAllTransactions(): List<TransactionRecord> {
        val list = mutableListOf<TransactionRecord>()
        readableDatabase.rawQuery("SELECT * FROM transactions ORDER BY date_iso DESC, id DESC", null).use { c ->
            while (c.moveToNext()) {
                val toIdx = c.getColumnIndexOrThrow("to_account_id")
                list.add(
                    TransactionRecord(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        dateIso = c.getString(c.getColumnIndexOrThrow("date_iso")),
                        type = TxType.valueOf(c.getString(c.getColumnIndexOrThrow("type"))),
                        accountId = c.getLong(c.getColumnIndexOrThrow("account_id")),
                        toAccountId = if (c.isNull(toIdx)) null else c.getLong(toIdx),
                        category = c.getString(c.getColumnIndexOrThrow("category")),
                        subCategory = c.getString(c.getColumnIndexOrThrow("sub_category")),
                        amount = c.getDouble(c.getColumnIndexOrThrow("amount")),
                        note = c.getString(c.getColumnIndexOrThrow("note")),
                        tags = c.getString(c.getColumnIndexOrThrow("tags"))
                    )
                )
            }
        }
        return list
    }

    fun saveTransaction(tx: TransactionRecord): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("date_iso", tx.dateIso)
            put("type", tx.type.name)
            put("account_id", tx.accountId)
            if (tx.toAccountId != null) put("to_account_id", tx.toAccountId) else putNull("to_account_id")
            put("category", tx.category)
            put("sub_category", tx.subCategory)
            put("amount", tx.amount)
            put("note", tx.note)
            put("tags", tx.tags)
        }
        return if (tx.id > 0L) {
            db.update("transactions", cv, "id = ?", arrayOf(tx.id.toString()))
            tx.id
        } else {
            db.insert("transactions", null, cv)
        }
    }

    fun deleteTransaction(id: Long) {
        writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
    }

    fun getAllBudgets(): List<BudgetItem> {
        val list = mutableListOf<BudgetItem>()
        readableDatabase.rawQuery("SELECT * FROM budgets ORDER BY category ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    BudgetItem(
                        category = c.getString(c.getColumnIndexOrThrow("category")),
                        monthlyLimit = c.getDouble(c.getColumnIndexOrThrow("monthly_limit"))
                    )
                )
            }
        }
        return list
    }

    fun upsertBudget(item: BudgetItem) = upsertBudgetInternal(writableDatabase, item)

    fun deleteBudget(category: String) {
        writableDatabase.delete("budgets", "category = ?", arrayOf(category))
    }
}
