package com.example.splity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensesFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private val expenses = mutableListOf<Expense>()
    private lateinit var groupId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_expenses, container, false)

        groupId = arguments?.getString("GROUP_ID") ?: return view

        recyclerView = view.findViewById(R.id.recyclerViewExpenses)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ExpenseAdapter(expenses)
        recyclerView.adapter = adapter

        val buttonAddExpense: Button = view.findViewById(R.id.buttonAddExpense)
        val editTextDescription: EditText = view.findViewById(R.id.editTextDescription)
        val editTextAmount: EditText = view.findViewById(R.id.editTextAmount)

        buttonAddExpense.setOnClickListener {
            addExpense(editTextDescription, editTextAmount)
        }

        loadExpenses()

        return view
    }

    private fun addExpense(editTextDescription: EditText, editTextAmount: EditText) {
        val description = editTextDescription.text.toString()
        val amount = editTextAmount.text.toString().toDoubleOrNull()

        if (description.isNotEmpty() && amount != null) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val currentUserId = withContext(Dispatchers.IO) {
                        FirebaseManager.getCurrentUserId() ?: throw Exception("User not logged in")
                    }
                    withContext(Dispatchers.IO) {
                        FirebaseManager.addExpense(groupId, description, amount, currentUserId, listOf(currentUserId))
                    }
                    editTextDescription.text.clear()
                    editTextAmount.text.clear()
                    Toast.makeText(context, "Expense added successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(context, "Error loading expenses: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

class ExpenseAdapter(private val expenses: List<Expense>) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {
    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.textViewExpenseDescription)
        val amount: TextView = view.findViewById(R.id.textViewExpenseAmount)
        val paidBy: TextView = view.findViewById(R.id.textViewExpensePaidBy)
        val date: TextView = view.findViewById(R.id.textViewExpenseDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.description.text = expense.description
        holder.amount.text = String.format("$%.2f", expense.amount)
        holder.paidBy.text = "Paid by: ${expense.paidBy}"
        holder.date.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))
    }

    override fun getItemCount() = expenses.size
}