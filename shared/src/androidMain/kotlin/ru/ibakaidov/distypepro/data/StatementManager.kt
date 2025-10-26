package ru.ibakaidov.distypepro.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import ru.ibakaidov.distypepro.structures.Statement
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.ProgressState

class StatementManager : Manager<Statement>() {
    
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("statements")
    
    override fun getRoot(): DatabaseReference = database
    
    override fun getList(callback: Callback<Map<String, String>>) {
        database.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result?.value as? Map<String, Any> ?: emptyMap()
                val statements = result.mapValues { (_, value) ->
                    val statement = Statement.fromMap(value as Map<*, *>)
                    statement.text
                }
                callback.onDone(ProgressState.Success(statements))
            } else {
                callback.onError(task.exception ?: Exception("Failed to load statements"))
            }
        }
    }
    
    override fun edit(key: String, value: String, callback: Callback<Unit>) {
        database.child(key).child("text").setValue(value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onDone(Unit)
                } else {
                    callback.onError(task.exception ?: Exception("Failed to edit statement"))
                }
            }
    }
    
    override fun create(value: String, callback: Callback<Unit>) {
        val key = database.push().key ?: return callback.onError(Exception("Failed to generate key"))
        val statement = Statement(key, "", value, System.currentTimeMillis())
        database.child(key).setValue(statement)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onDone(Unit)
                } else {
                    callback.onError(task.exception ?: Exception("Failed to create statement"))
                }
            }
    }
}