package com.example.splity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class GroupDetailActivity : AppCompatActivity() {
    private lateinit var groupId: String
    private lateinit var groupName: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private val expenses = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        groupId = intent.getStringExtra("GROUP_ID") ?: return finish()
        groupName = intent.getStringExtra("GROUP_NAME") ?: return finish()

        val textViewGroupName: TextView = findViewById(R.id.textViewGroupName)
        textViewGroupName.text = groupName

        recyclerView = findViewById(R.id.recyclerViewExpenses)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(expenses)
        recyclerView.adapter = adapter

        val buttonAddExpense: Button = findViewById(R.id.buttonAddExpense)
        buttonAddExpense.setOnClickListener {
            val intent = Intent(this, ExpensesFragment::class.java)
            intent.putExtra("GROUP_ID", groupId)
            startActivity(intent)
        }

        loadExpenses()
    }

    private fun loadExpenses() {
        FirebaseManager.getGroupExpenses(groupId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expenses.clear()
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java)
                    expense?.let { expenses.add(it) }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}