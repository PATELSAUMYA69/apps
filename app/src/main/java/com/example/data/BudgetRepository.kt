package com.example.data

import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val transactionDao: TransactionDao,
    private val budgetGoalDao: BudgetGoalDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBudgetGoals: Flow<List<BudgetGoal>> = budgetGoalDao.getAllBudgetGoals()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal) {
        budgetGoalDao.insertBudgetGoal(budgetGoal)
    }

    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal) {
        budgetGoalDao.updateBudgetGoal(budgetGoal)
    }

    suspend fun deleteBudgetGoal(id: Int) {
        budgetGoalDao.deleteBudgetGoalById(id)
    }
}
