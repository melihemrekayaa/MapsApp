package com.example.mapsapp.webrtc.utils

import com.google.firebase.database.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun DatabaseReference.awaitSingle(): DataSnapshot =
    suspendCancellableCoroutine { cont ->
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (cont.isActive) cont.resume(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                if (cont.isActive) cont.resumeWithException(error.toException())
            }
        }

        this.addListenerForSingleValueEvent(listener)

        // Coroutine iptal edilirse listener'ı da kaldır
        cont.invokeOnCancellation {
            this.removeEventListener(listener)
        }
    }

suspend fun DatabaseReference.awaitSetValue(value: Any?): Unit =
    suspendCancellableCoroutine { cont ->
        this.setValue(value)
            .addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resumeWithException(it)
            }
    }

suspend fun DatabaseReference.awaitRemoveValue(): Unit =
    suspendCancellableCoroutine { cont ->
        this.removeValue()
            .addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resumeWithException(it)
            }
    }

suspend fun <T> DatabaseReference.awaitTypedValue(clazz: Class<T>): T? {
    val snapshot = this.awaitSingle()
    return snapshot.getValue(clazz)
}
