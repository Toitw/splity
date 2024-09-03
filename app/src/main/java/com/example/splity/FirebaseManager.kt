package com.example.splity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

data class Group(
    val id: String = "",
    val name: String = "",
    val members: List<String> = listOf()
)

data class Expense(
    val id: String = "",
    val groupId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val splitBetween: List<String> = listOf(),
    val date: Long = 0
)

object FirebaseManager {
    val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun createUser(name: String, email: String, password: String): User {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = authResult.user?.uid ?: throw Exception("Failed to create user")
        val user = User(userId, name, email)
        database.child("users").child(userId).setValue(user).await()
        return user
    }

    suspend fun createGroup(name: String, members: List<String>): Group {
        val groupId = database.child("groups").push().key ?: throw Exception("Failed to generate group ID")
        val group = Group(groupId, name, members)
        database.child("groups").child(groupId).setValue(group).await()
        return group
    }

    suspend fun addExpense(groupId: String, description: String, amount: Double, paidBy: String, splitBetween: List<String>): Expense {
        val expenseId = database.child("expenses").push().key ?: throw Exception("Failed to generate expense ID")
        val expense = Expense(expenseId, groupId, description, amount, paidBy, splitBetween, System.currentTimeMillis())
        database.child("expenses").child(expenseId).setValue(expense).await()
        return expense
    }

    fun getGroupExpenses(groupId: String): Query {
        return database.child("expenses").orderByChild("groupId").equalTo(groupId)
    }

    fun calculateBalances(groupId: String, callback: (Map<String, Double>) -> Unit) {
        getGroupExpenses(groupId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val balances = mutableMapOf<String, Double>()
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java) ?: continue
                    val splitAmount = expense.amount / expense.splitBetween.size
                    expense.splitBetween.forEach { userId ->
                        balances[userId] = (balances[userId] ?: 0.0) - splitAmount
                    }
                    balances[expense.paidBy] = (balances[expense.paidBy] ?: 0.0) + expense.amount
                }
                callback(balances)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}