package com.example.splity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupsFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter
    private val groups = mutableListOf<Group>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_groups, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewGroups)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = GroupAdapter(groups)
        recyclerView.adapter = adapter

        val buttonAddGroup: Button = view.findViewById(R.id.buttonAddGroup)
        val editTextGroupName: EditText = view.findViewById(R.id.editTextGroupName)

        buttonAddGroup.setOnClickListener {
            val groupName = editTextGroupName.text.toString()
            if (groupName.isNotEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val currentUserId = FirebaseManager.getCurrentUserId() ?: throw Exception("User not logged in")
                        FirebaseManager.createGroup(groupName, listOf(currentUserId))
                        editTextGroupName.text.clear()
                    } catch (e: Exception) {
                        // Handle error (e.g., show a toast)
                    }
                }
            }
        }

        loadGroups()

        return view
    }

    private fun loadGroups() {
        val currentUserId = FirebaseManager.getCurrentUserId() ?: return
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
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}

class GroupAdapter(private val groups: List<Group>) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {
    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // TODO: Implement ViewHolder
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        // TODO: Implement onCreateViewHolder
        return GroupViewHolder(View(parent.context))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        // TODO: Implement onBindViewHolder
    }

    override fun getItemCount() = groups.size
}