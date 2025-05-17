package com.example.mapsapp.webrtc.utils

import com.google.firebase.database.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun DatabaseReference.awaitSingle(): DataSnapshot =
    suspendCoroutine { cont ->
        this.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = cont.resume(snapshot)
            override fun onCancelled(error: DatabaseError) = cont.resumeWithException(error.toException())
        })
    }

suspend fun DatabaseReference.awaitSetValue(value: Any?): Unit =
    suspendCoroutine { cont ->
        this.setValue(value)
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

suspend fun DatabaseReference.awaitRemoveValue(): Unit =
    suspendCoroutine { cont ->
        this.removeValue()
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

suspend fun <T> DatabaseReference.awaitTypedValue(clazz: Class<T>): T? {
    val snapshot = this.awaitSingle()
    return snapshot.getValue(clazz)
}
