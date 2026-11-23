package com.moneymanager.app.data

enum class TxType {
    INCOME, EXPENSE, TRANSFER
}

data class Account(
    val id: Long = 0L,
    val name: String,
    val groupName: String,
    val initialBalance: Double = 0.0,
    val isLiability: Boolean = false,
    val excludeFromTotals: Boolean = false,
    val cutoffDay: Int = 15,
    val paymentDay: Int = 5
)

data class TransactionRecord(
    val id: Long = 0L,
    val dateIso: String, // YYYY-MM-DD
    val type: TxType,
    val accountId: Long,
    val toAccountId: Long? = null,
    val category: String,
    val subCategory: String = "",
    val amount: Double,
    val note: String = "",
    val tags: String = "" // comma-separated without #
)

data class BudgetItem(
    val category: String,
    val monthlyLimit: Double
)

object CategoryDefaults {
    val expenseCategories: Map<String, List<String>> = linkedMapOf(
        "Food" to listOf("Dining Out", "Groceries", "Coffee & Tea"),
        "Transport" to listOf("Fuel", "Tolls / RFID", "Grab / Parking"),
        "Household" to listOf("Utilities", "Internet", "Supplies"),
        "Health & Fitness" to listOf("Supplements", "Gym / Gear", "Medical"),
        "Shopping" to listOf("Apparel", "Tech & Gadgets", "Personal Care"),
        "Social & Gifts" to listOf("Family", "Friends", "Treat"),
        "Business" to listOf("Marketing", "Tools & Software", "Client Coffee")
    )

    val incomeCategories: Map<String, List<String>> = linkedMapOf(
        "Salary" to listOf("Base Pay", "Commission"),
        "Coaching & Classes" to listOf("PT Sessions", "Group Classes"),
        "Business" to listOf("Product Sales", "Bonus"),
        "Other" to listOf("Cashback / Rebate", "Dividends")
    )

    val accountGroups: List<String> = listOf(
        "Cash",
        "Bank Accounts",
        "eWallets",
        "Credit Cards",
        "Savings & Investments"
    )
}
