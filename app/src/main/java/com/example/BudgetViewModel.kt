package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BudgetGoal
import com.example.data.BudgetRepository
import com.example.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(private val repository: BudgetRepository) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val budgetGoals: StateFlow<List<BudgetGoal>> = repository.allBudgetGoals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            updateGoalForTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            // Note: Recalculating goals perfectly on transaction edit requires 
            // more complex logic to revert the old amount and apply the new.
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
        }
    }

    fun addBudgetGoal(goal: BudgetGoal) {
        viewModelScope.launch {
            repository.insertBudgetGoal(goal)
        }
    }

    fun updateBudgetGoal(goal: BudgetGoal) {
        viewModelScope.launch {
            repository.updateBudgetGoal(goal)
        }
    }

    fun deleteBudgetGoal(goal: BudgetGoal) {
        viewModelScope.launch {
            repository.deleteBudgetGoal(goal.id)
        }
    }

    private suspend fun updateGoalForTransaction(transaction: Transaction) {
        val goals = budgetGoals.value
        val match = goals.find { it.category == transaction.category }
        if (match != null && transaction.isExpense) {
            repository.updateBudgetGoal(match.copy(currentAmount = match.currentAmount + transaction.amount))
        }
    }
}

class BudgetViewModelFactory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
