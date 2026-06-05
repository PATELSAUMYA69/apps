package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.example.data.BudgetGoal
import com.example.data.BudgetRepository
import com.example.data.Transaction
import com.example.ui.theme.*
import com.example.ui.AddBudgetGoalDialog
import com.example.ui.TransactionDialog
import com.example.ui.SettingsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "budget-database"
        ).fallbackToDestructiveMigration().build()
        val repository = BudgetRepository(db.transactionDao(), db.budgetGoalDao())
        val settingsManager = com.example.data.SettingsManager(applicationContext)

        setContent {
            MyApplicationTheme {
                val viewModel: BudgetViewModel = viewModel(
                    factory = BudgetViewModelFactory(repository)
                )
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(settingsManager)
                )
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "home"
                val appName by settingsViewModel.appName.collectAsStateWithLifecycle()

                Scaffold(
                    bottomBar = { 
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        ) 
                    },
                    floatingActionButton = {
                        var showDialog by remember { mutableStateOf(false) }
                        if (showDialog) {
                            TransactionDialog(
                                onDismiss = { showDialog = false },
                                onConfirm = { transaction ->
                                    viewModel.addTransaction(transaction)
                                    showDialog = false
                                }
                            )
                        }
                        FloatingActionButton(
                            onClick = { showDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.offset(y = 28.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
                        }
                    },
                    floatingActionButtonPosition = FabPosition.End,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            BudgetDashboard(
                                viewModel = viewModel,
                                appName = appName,
                                onNavigateToTransactions = {
                                    navController.navigate("transactions") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToInsights = {
                                    navController.navigate("insights") {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable("insights") {
                            InsightsScreen(viewModel = viewModel, appName = appName)
                        }
                        composable("transactions") {
                            TransactionsScreen(viewModel = viewModel, appName = appName)
                        }
                        composable("settings") {
                            SettingsScreen(settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetDashboard(
    viewModel: BudgetViewModel, 
    appName: String,
    onNavigateToTransactions: () -> Unit = {}, 
    onNavigateToInsights: () -> Unit = {}, 
    modifier: Modifier = Modifier
) {
    val goals by viewModel.budgetGoals.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    val income = transactions.filter { !it.isExpense }.sumOf { it.amount }
    val spent = transactions.filter { it.isExpense }.sumOf { it.amount }
    val balance = income - spent

    var goalToEdit by remember { mutableStateOf<BudgetGoal?>(null) }
    var showGoalAddDialog by remember { mutableStateOf(false) }

    if (showGoalAddDialog || goalToEdit != null) {
        val targetGoal = goalToEdit
        AddBudgetGoalDialog(
            goal = targetGoal,
            onDismiss = { showGoalAddDialog = false; goalToEdit = null },
            onConfirm = { goal ->
                if (targetGoal == null) viewModel.addBudgetGoal(goal) else viewModel.updateBudgetGoal(goal)
                showGoalAddDialog = false; goalToEdit = null
            },
            onDelete = { goal ->
                viewModel.deleteBudgetGoal(goal)
                showGoalAddDialog = false; goalToEdit = null
            }
        )
    }

    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    
    if (transactionToEdit != null) {
        TransactionDialog(
            transaction = transactionToEdit,
            onDismiss = { transactionToEdit = null },
            onConfirm = { transaction ->
                viewModel.updateTransaction(transaction)
                transactionToEdit = null
            },
            onDelete = { transaction ->
                viewModel.deleteTransaction(transaction)
                transactionToEdit = null
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        TopBar(appName = appName)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { 
                HeroCard(
                    balance = balance, 
                    income = income, 
                    spent = spent, 
                    onIncomeClick = onNavigateToTransactions, 
                    onSpentClick = onNavigateToTransactions
                ) 
            }
            item { SpendingAnalysis(onClick = onNavigateToInsights) }
            
            // Monthly Budget Goals Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monthly Goals", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { showGoalAddDialog = true }) {
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            if (goals.isEmpty()) {
                item {
                    Text("No goals set.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                items(goals) { goal ->
                    BudgetGoalItem(goal = goal, onClick = { goalToEdit = goal })
                }
            }

            item { RecentActivity(transactions, onTransactionClick = { transactionToEdit = it }, onViewAllClick = onNavigateToTransactions) }
        }
    }
}

@Composable
fun TopBar(appName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = "Wallet", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(appName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.CloudDone, contentDescription = "Cloud", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text("SYNCED", fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
        IconButton(onClick = { }) {
            Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun HeroCard(balance: Double, income: Double, spent: Double, onIncomeClick: () -> Unit = {}, onSpentClick: () -> Unit = {}) {
    val balanceStr = String.format("%,.0f", Math.floor(balance))
    val balanceCents = String.format(".%02d", ((balance - Math.floor(balance)) * 100).toInt())
    
    val incomeStr = String.format("₹%,.2f", income)
    val spentStr = String.format("₹%,.2f", spent)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(32.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text("TOTAL BALANCE", fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("USD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("₹$balanceStr", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.alignByBaseline())
            Text(balanceCents, fontSize = 14.sp, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.alignByBaseline())
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoBox(
                title = "INCOME",
                amount = incomeStr,
                icon = Icons.Filled.ArrowDownward,
                iconColor = IncomeBlue,
                onClick = onIncomeClick,
                modifier = Modifier.weight(1f)
            )
            InfoBox(
                title = "SPENT",
                amount = spentStr,
                icon = Icons.Filled.ArrowUpward,
                iconColor = SpendRed,
                onClick = onSpentClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoBox(title: String, amount: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = iconColor)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(amount, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun SpendingAnalysis(onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Spending Analysis", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("This Week", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            ChartBar("M", 0.40f)
            ChartBar("T", 0.65f)
            ChartBar("W", 1.0f, active = true)
            ChartBar("T", 0.30f)
            ChartBar("F", 0.55f)
            ChartBar("S", 0.85f)
            ChartBar("S", 0.20f)
        }
    }
}

@Composable
fun ChartBar(label: String, heightFraction: Float, active: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(72.dp * heightFraction)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer)
        )
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}

@Composable
fun BudgetGoalItem(goal: BudgetGoal, onClick: () -> Unit) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val safeProgress = progress.coerceIn(0f, 1f)
    val isExceeded = progress >= 1f
    val isWarning = progress >= 0.8f && !isExceeded

    val indicatorColor = when {
        isExceeded -> MaterialTheme.colorScheme.error
        isWarning -> Color(0xFFFFB300)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(goal.category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("₹${goal.currentAmount} / ₹${goal.targetAmount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { safeProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        if (isExceeded) {
            Text("Limit exceeded!", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        } else if (isWarning) {
            Text("Approaching limit.", color = Color(0xFFFFB300), fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun AddBudgetGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var category by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget Goal") },
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
                    label = { Text("Target Amount") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (category.isNotBlank() && amount != null) {
                        onConfirm(category, amount)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecentActivity(transactions: List<Transaction>, onTransactionClick: (Transaction) -> Unit, onViewAllClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Activity", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.clickable { onViewAllClick() })
        }
        
        if (transactions.isEmpty()) {
            Text("No transactions yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                transactions.sortedByDescending { it.timestamp }.take(5).forEach { transaction ->
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(transaction.timestamp))
                    val amountStr = if (transaction.isExpense) "-₹${String.format("%,.2f", transaction.amount)}" else "+₹${String.format("%,.2f", transaction.amount)}"
                    val amountColor = if (transaction.isExpense) SpendRed else SuccessGreen
                    val icon = if (transaction.isExpense) Icons.Filled.ShoppingBag else Icons.Filled.Work
                    
                    TransactionItem(
                        title = transaction.category,
                        subtitle = "${if(transaction.description.isNotBlank()) transaction.description else "General"} • $dateFormatted",
                        amount = amountStr,
                        amountColor = amountColor,
                        icon = icon,
                        highlight = !transaction.isExpense,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(title: String, subtitle: String, amount: String, amountColor: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, highlight: Boolean, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (highlight) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF0F4FA), 
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Text(amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = amountColor)
    }
}

@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem("Home", Icons.Filled.Home, active = currentRoute == "home") { onNavigate("home") }
                NavItem("Insights", Icons.Filled.BarChart, active = currentRoute == "insights") { onNavigate("insights") }
                NavItem("Transactions", Icons.Filled.ReceiptLong, active = currentRoute == "transactions") { onNavigate("transactions") }
                NavItem("Settings", Icons.Filled.Settings, active = currentRoute == "settings") { onNavigate("settings") }
            }
        }
    }
}

@Composable
fun NavItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, 
                    CircleShape
                )
                .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = label, 
                tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            label, 
            fontSize = 10.sp, 
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, 
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InsightsScreen(viewModel: BudgetViewModel, appName: String) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(appName)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Detailed Insights", 
            fontSize = 24.sp, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        SpendingAnalysis()
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Recent Transactions",
            modifier = Modifier.align(Alignment.Start),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                val sorted = transactions.sortedByDescending { it.timestamp }
                items(sorted) { transaction ->
                    val dateFormatted = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))
                    val amountStr = if (transaction.isExpense) "-₹${String.format("%,.2f", transaction.amount)}" else "+₹${String.format("%,.2f", transaction.amount)}"
                    val amountColor = if (transaction.isExpense) SpendRed else SuccessGreen
                    val icon = if (transaction.isExpense) Icons.Filled.ShoppingBag else Icons.Filled.Work
                    
                    TransactionItem(
                        title = transaction.category,
                        subtitle = "${if (transaction.description.isNotBlank()) transaction.description else "General"} • $dateFormatted",
                        amount = amountStr,
                        amountColor = amountColor,
                        icon = icon,
                        highlight = !transaction.isExpense,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionsScreen(viewModel: BudgetViewModel, appName: String) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var defaultIsExpense by remember { mutableStateOf(true) }
    
    if (showAddDialog || transactionToEdit != null) {
        TransactionDialog(
            transaction = transactionToEdit,
            defaultIsExpense = defaultIsExpense,
            onDismiss = { showAddDialog = false; transactionToEdit = null },
            onConfirm = { transaction ->
                if (transactionToEdit == null) viewModel.addTransaction(transaction) else viewModel.updateTransaction(transaction)
                showAddDialog = false; transactionToEdit = null
            },
            onDelete = { transaction ->
                viewModel.deleteTransaction(transaction)
                showAddDialog = false; transactionToEdit = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        TopBar(appName)
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "All Transactions", 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { defaultIsExpense = false; showAddDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Income")
            }
            Button(
                onClick = { defaultIsExpense = true; showAddDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SpendRed)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Expense")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                val sorted = transactions.sortedByDescending { it.timestamp }
                items(sorted) { transaction ->
                    val dateFormatted = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))
                    val amountStr = if (transaction.isExpense) "-₹${String.format("%,.2f", transaction.amount)}" else "+₹${String.format("%,.2f", transaction.amount)}"
                    val amountColor = if (transaction.isExpense) SpendRed else SuccessGreen
                    val icon = if (transaction.isExpense) Icons.Filled.ShoppingBag else Icons.Filled.Work
                    
                    TransactionItem(
                        title = transaction.category,
                        subtitle = "${if (transaction.description.isNotBlank()) transaction.description else "General"} • $dateFormatted",
                        amount = amountStr,
                        amountColor = amountColor,
                        icon = icon,
                        highlight = !transaction.isExpense,
                        onClick = { transactionToEdit = transaction }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
        // Preview dummy
    }
}
