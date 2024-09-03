package com.example.splity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.*

class BalanceFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BalanceAdapter
    private val balances = mutableListOf<Pair<String, Double>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_balance, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewBalances)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BalanceAdapter(balances)
        recyclerView.adapter = adapter

        loadBalances()

        return view
    }

    private fun loadBalances() {
        val currentUserId = FirebaseManager.getCurrentUserId() ?: return
        FirebaseManager.database.child("groups")
            .orderByChild("members/$currentUserId")
            .equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (groupSnapshot in snapshot.children) {
                        val group = groupSnapshot.getValue(Group::class.java) ?: continue
                        FirebaseManager.calculateBalances(group.id) { groupBalances ->
                            balances.clear()
                            for ((userId, balance) in groupBalances) {
                                if (userId != currentUserId) {
                                    balances.add(Pair(userId, balance))
                                }
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}

class BalanceAdapter(private val balances: List<Pair<String, Double>>) : RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder>() {
    class BalanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewUser: TextView = view.findViewById(R.id.textViewUser)
        val textViewBalance: TextView = view.findViewById(R.id.textViewBalance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_balance, parent, false)
        return BalanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        val (userId, balance) = balances[position]
        FirebaseManager.database.child("users").child(userId).get().addOnSuccessListener { snapshot ->
            val user = snapshot.getValue(User::class.java)
            holder.textViewUser.text = user?.name ?: "Unknown User"
        }

        val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val formattedBalance = numberFormat.format(Math.abs(balance))

        if (balance > 0) {
            holder.textViewBalance.text = "You are owed $formattedBalance"
            holder.textViewBalance.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        } else if (balance < 0) {
            holder.textViewBalance.text = "You owe $formattedBalance"
            holder.textViewBalance.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        } else {
            holder.textViewBalance.text = "Settled up"
            holder.textViewBalance.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
        }
    }

    override fun getItemCount() = balances.size
}