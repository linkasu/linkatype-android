package ru.ibakaidov.distypepro.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import ru.ibakaidov.distypepro.structures.Category
import ru.ibakaidov.distypepro.utils.Callback
import ru.ibakaidov.distypepro.utils.ProgressState
import java.util.*

class CategoryManager : Manager<Category>() {
    
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("categories")
    
    override fun getRoot(): DatabaseReference = database
    
    override fun getList(callback: Callback<Map<String, String>>) {
        database.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result?.value as? Map<String, Any> ?: emptyMap()
                val categories = result.mapValues { (_, value) ->
                    val category = Category.fromMap(value as Map<*, *>)
                    category.label
                }
                callback.onDone(ProgressState.Success(categories))
            } else {
                callback.onError(task.exception ?: Exception("Failed to load categories"))
            }
        }
    }
    
    override fun edit(key: String, value: String, callback: Callback<Unit>) {
        database.child(key).child("label").setValue(value)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onDone(Unit)
                } else {
                    callback.onError(task.exception ?: Exception("Failed to edit category"))
                }
            }
    }
    
    override fun create(value: String, callback: Callback<Unit>) {
        val key = database.push().key ?: return callback.onError(Exception("Failed to generate key"))
        val category = Category(key, value, System.currentTimeMillis())
        database.child(key).setValue(category)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback.onDone(Unit)
                } else {
                    callback.onError(task.exception ?: Exception("Failed to create category"))
                }
            }
    }
}