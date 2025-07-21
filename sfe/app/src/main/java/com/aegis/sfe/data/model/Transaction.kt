package com.aegis.sfe.data.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transaction(
    val id: String,
    val transactionReference: String,
    val fromAccountId: String?,
    val toAccountId: String?,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: TransactionType,
    val status: TransactionStatus,
    val description: String?,
    val remarks: String?,
    val createdAt: LocalDateTime,
    val completedAt: LocalDateTime?,
    val deviceId: String?,
    val signature: String?,
    val nonce: String?
)

enum class TransactionType {
    TRANSFER,
    DEPOSIT,
    WITHDRAWAL,
    PAYMENT,
    REFUND
}

enum class TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED
}

data class TransferRequest(
    val fromAccount: String,
    val toAccount: String,
    val amount: BigDecimal,
    val currency: String = "INR",
    val description: String?,
    val remarks: String?
)

data class TransferResponse(
    val transactionReference: String,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val fromAccount: String,
    val toAccount: String,
    val timestamp: String,
    val message: String
)

data class TransactionHistoryResponse(
    val content: List<TransactionResponse>,
    val pageable: PageableResponse,
    val totalElements: Long,
    val totalPages: Int
)

data class TransactionResponse(
    val id: String,
    val transactionReference: String,
    val fromAccountId: String?,
    val toAccountId: String?,
    val amount: BigDecimal,
    val currency: String,
    val transactionType: String,
    val status: String,
    val description: String?,
    val remarks: String?,
    val createdAt: String,
    val completedAt: String?,
    val deviceId: String?,
    val signature: String?,
    val nonce: String?
) {
    fun toTransaction(): Transaction {
        return Transaction(
            id = id,
            transactionReference = transactionReference,
            fromAccountId = fromAccountId,
            toAccountId = toAccountId,
            amount = amount,
            currency = currency,
            transactionType = TransactionType.valueOf(transactionType),
            status = TransactionStatus.valueOf(status),
            description = description,
            remarks = remarks,
            createdAt = LocalDateTime.parse(createdAt),
            completedAt = completedAt?.let { LocalDateTime.parse(it) },
            deviceId = deviceId,
            signature = signature,
            nonce = nonce
        )
    }
}

data class PageableResponse(
    val pageNumber: Int,
    val pageSize: Int
)