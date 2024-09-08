package com.example.splity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GroupsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter
    private val groups = mutableListOf<Group>()
    private lateinit var auth: FirebaseAuth
    private lateinit var emptyStateTextView: TextView

    companion object {
        private const val TAG = "GroupsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_groups, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewGroups)
        emptyStateTextView = view.findViewById(R.id.textViewEmptyState)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter(groups) { group ->
            Toast.makeText(context, "Clicked on group: ${group.name}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        val buttonAddGroup: Button = view.findViewById(R.id.buttonAddGroup)
        val editTextGroupName: EditText = view.findViewById(R.id.editTextGroupName)

        buttonAddGroup.setOnClickListener {
            val groupName = editTextGroupName.text.toString()
            if (groupName.isNotEmpty()) {
                if (auth.currentUser != null) {
                    createGroup(groupName)
                } else {
                    Toast.makeText(context, "Please log in to create a group", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(activity, LoginActivity::class.java))
                }
            } else {
                Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
            }
        }

        loadGroups()

        return view
    }

    private fun createGroup(groupName: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Attempting to create group: $groupName")
                val currentUser = auth.currentUser
                Log.d(TAG, "Current user: ${currentUser?.uid ?: "No user logged in"}")
                val currentUserId = currentUser?.uid ?: throw Exception("User not logged in")
                withContext(Dispatchers.IO) {
                    FirebaseManager.createGroup(groupName, listOf(currentUserId))
                }
                withContext(Dispatchers.Main) {
                    view?.findViewById<EditText>(R.id.editTextGroupName)?.text?.clear()
                    Toast.makeText(context, "Group created successfully", Toast.LENGTH_SHORT).show()
                    loadGroups() // Reload groups after creating a new one
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating group", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error creating group: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadGroups() {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to view groups", Toast.LENGTH_LONG).show()
            startActivity(Intent(activity, LoginActivity::class.java))
            return
        }

        lifecycleScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                FirebaseManager.database.child("groups")
                    .orderByChild("members/$currentUserId")
                    .equalTo(true)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            groups.clear()
                            for (groupSnapshot in snapshot.children) {
                                val group = groupSnapshot.getValue(Group::class.java)
                                group?.let { groups.add(it) }
                            }
                            updateUI()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error loading groups", error.toException())
                            Toast.makeText(context, "Error loading groups: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadGroups", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (groups.isEmpty()) {
                emptyStateTextView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.notifyDataSetChanged()
            }
            Log.d(TAG, "UI updated. Group count: ${groups.size}")
        }
    }
}

class GroupAdapter(
    private val groups: List<Group>,
    private val onGroupClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewGroupName: TextView = view.findViewById(R.id.textViewGroupName)
        val textViewMemberCount: TextView = view.findViewById(R.id.textViewMemberCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.textViewGroupName.text = group.name
        holder.textViewMemberCount.text = "${group.members.size} members"

        holder.itemView.setOnClickListener {
            onGroupClick(group)
        }
    }

    override fun getItemCount() = groups.size
}