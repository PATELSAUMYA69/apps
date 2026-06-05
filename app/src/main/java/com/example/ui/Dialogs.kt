package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BudgetGoal
import com.example.data.Transaction

@Composable
fun AddBudgetGoalDialog(
    goal: BudgetGoal? = null,
    onDismiss: () -> Unit,
    onConfirm: (BudgetGoal) -> Unit,
    onDelete: ((BudgetGoal) -> Unit)? = null
) {
    var category by remember { mutableStateOf(goal?.category ?: "") }
    var amountStr by remember { mutableStateOf(goal?.targetAmount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "Add Budget Goal" else "Edit Budget Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") }
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Target Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null) {
                        val cat = if (category.isNotBlank()) category else "New Goal"
                        onConfirm(
                            goal?.copy(category = cat, targetAmount = amount)
                                ?: BudgetGoal(category = cat, targetAmount = amount)
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goal != null && onDelete != null) {
                    TextButton(onClick = { onDelete(goal) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun TransactionDialog(
    transaction: Transaction? = null,
    defaultIsExpense: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
    onDelete: ((Transaction) -> Unit)? = null
) {
    var category by remember { mutableStateOf(transaction?.category ?: "") }
    var amountStr by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(transaction?.description ?: "") }
    var isExpense by remember { mutableStateOf(transaction?.isExpense ?: defaultIsExpense) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Add Transaction" else "Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("Income") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("Expense") }
                    )
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") }
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (amount != null) {
                        onConfirm(
                            transaction?.copy(
                                amount = amount,
                                category = if (category.isNotBlank()) category else "General",
                                description = description,
                                isExpense = isExpense
                            ) ?: Transaction(
                                amount = amount,
                                category = if (category.isNotBlank()) category else "General",
                                description = description,
                                timestamp = System.currentTimeMillis(),
                                isExpense = isExpense
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (transaction != null && onDelete != null) {
                    TextButton(onClick = { onDelete(transaction) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
