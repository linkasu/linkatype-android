package ru.ibakaidov.distypepro.data

import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import ru.ibakaidov.distypepro.structures.Statement
import ru.ibakaidov.distypepro.utils.Callback
import java.util.Date

class StatementManager(private val categoryId: String) : Manager<Statement>() {

    override fun getList(callback: Callback<Map<String, String>>) {
        getRoot()
            .orderByChild("created")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val result = snapshot.children
                        .mapNotNull { child ->
                            (child.value as? Map<*, *>)?.let(Statement::fromMap)
                        }
                        .sortedByDescending { it.created }
                        .associate { it.id to it.text }
                    callback.onDone(result)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback.onError(error.toException())
                }
            })
    }

    override fun edit(key: String, value: String, callback: Callback<Unit>) {
        getRoot().child(key).updateChildren(mapOf("text" to value))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onDone(Unit)
                } else {
                    callback.onError(task.exception)
                }
            }
    }

    override fun create(value: String, callback: Callback<Unit>) {
        val reference = getRoot().push()
        val data = mapOf(
            "text" to value,
            "id" to reference.key.orEmpty(),
            "categoryId" to categoryId,
            "created" to Date().time
        )
        reference.updateChildren(data) { error, _ ->
            if (error != null) {
                callback.onError(error.toException())
            } else {
                callback.onDone(Unit)
            }
        }
    }

    override fun getRoot(): DatabaseReference =
        Firebase.database.reference
            .child("users/${userId()}")
            .child("Category/$categoryId/statements")

    private fun userId(): String =
        Firebase.auth.currentUser?.uid ?: error("User must be authenticated")
}
