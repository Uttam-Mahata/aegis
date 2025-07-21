package com.aegis.sfe.data.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Account(
    val accountNumber: String,
    val accountHolderName: String,
    val balance: BigDecimal,
    val currency: String = "INR",
    val accountType: AccountType,
    val status: AccountStatus,
    val createdAt: LocalDateTime
)

enum class AccountType {
    SAVINGS,
    CURRENT,
    FIXED_DEPOSIT,
    RECURRING_DEPOSIT
}

enum class AccountStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    CLOSED
}

data class AccountResponse(
    val accountNumber: String,
    val accountHolderName: String,
    val balance: BigDecimal,
    val currency: String,
    val accountType: String,
    val status: String,
    val createdAt: String
) {
    fun toAccount(): Account {
        return Account(
            accountNumber = accountNumber,
            accountHolderName = accountHolderName,
            balance = balance,
            currency = currency,
            accountType = AccountType.valueOf(accountType),
            status = AccountStatus.valueOf(status),
            createdAt = LocalDateTime.parse(createdAt)
        )
    }
}