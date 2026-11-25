package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CircuitDiagnosticRecord
import com.example.data.model.McuProjectRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class McuWorkspaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("کاربر باید با حساب گوگل وارد شده باشد.")
    }

    fun observeProjects(userId: String): Flow<List<McuProjectRecord>> = flow {
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        McuProjectRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDiagnostics(userId: String): Flow<List<CircuitDiagnosticRecord>> = flow {
        val path = "users/$userId/diagnostics"
        emitAll(
            db.collection("users").document(userId).collection("diagnostics")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CircuitDiagnosticRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProject(
        title: String,
        mcuFamily: String,
        mcuModel: String,
        category: String,
        promptText: String,
        codeOrResult: String,
        notes: String = ""
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "عنوان پروژه نمی‌تواند خالی باشد." }
            val cleanModel = mcuModel.trim().take(60)
            require(cleanModel.isNotEmpty()) { "مدل میکروکنترلر نمی‌تواند خالی باشد." }
            val cleanResult = codeOrResult.trim().take(30000)
            require(cleanResult.isNotEmpty()) { "محتوای کد یا تحلیل نمی‌تواند خالی باشد." }

            val docRef = db.collection("users").document(uid).collection("projects").document()
            docPath = docRef.path

            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to cleanTitle,
                "mcuFamily" to mcuFamily,
                "mcuModel" to cleanModel,
                "category" to category,
                "promptText" to promptText.trim().take(4000),
                "codeOrResult" to cleanResult,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanNotes = notes.trim().take(2000)
            if (cleanNotes.isNotEmpty()) {
                payload["notes"] = cleanNotes
            }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/projects/$projectId"
        return try {
            val uid = requireUserId()
            require(projectId.isNotBlank())
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveDiagnostic(
        title: String,
        mcuFamily: String,
        diagnosticType: String,
        queryText: String,
        analysisResult: String
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/diagnostics"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "عنوان گزارش نمی‌تواند خالی باشد." }
            val cleanQuery = queryText.trim().take(4000)
            require(cleanQuery.isNotEmpty()) { "متن پرسش نمی‌تواند خالی باشد." }
            val cleanResult = analysisResult.trim().take(30000)
            require(cleanResult.isNotEmpty()) { "متن گزارش نمی‌تواند خالی باشد." }

            val docRef = db.collection("users").document(uid).collection("diagnostics").document()
            docPath = docRef.path

            val payload = mapOf<String, Any>(
                "userId" to uid,
                "title" to cleanTitle,
                "mcuFamily" to mcuFamily,
                "diagnosticType" to diagnosticType,
                "queryText" to cleanQuery,
                "analysisResult" to cleanResult,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDiagnostic(diagnosticId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/diagnostics/$diagnosticId"
        return try {
            val uid = requireUserId()
            require(diagnosticId.isNotBlank())
            val docRef = db.collection("users").document(uid).collection("diagnostics").document(diagnosticId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
