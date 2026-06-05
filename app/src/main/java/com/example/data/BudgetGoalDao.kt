package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetGoalDao {
    @Query("SELECT * FROM budget_goals")
    fun getAllBudgetGoals(): Flow<List<BudgetGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetGoal(budgetGoal: BudgetGoal)

    @Update
    suspend fun updateBudgetGoal(budgetGoal: BudgetGoal)

    @Query("DELETE FROM budget_goals WHERE id = :id")
    suspend fun deleteBudgetGoalById(id: Int)
}
