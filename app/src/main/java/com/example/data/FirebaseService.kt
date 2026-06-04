package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Domain Data Models
data class Category(
    val id: String = "",
    val name: String = "",
    val iconName: String = "",
    val nominationCount: Int = 0
)

data class Nominee(
    val id: String = "",
    val categoryId: String = "",
    val name: String = "",
    val votes: Int = 0,
    val imageUrl: String = ""
)

data class Vote(
    val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val nomineeId: String = "",
    val votedAt: Long = 0
)

data class Suggestion(
    val id: String = "",
    val categoryName: String = "",
    val nomineeName: String = "",
    val reason: String = "",
    val userId: String = "",
    val submittedAt: Long = 0
)

class FirebaseService(private val context: Context) {

    private val dbId = "ai-studio-0b5b756a-9ce6-4ddc-973d-7c2acd4a51da"
    private val firestore: FirebaseFirestore
    private val auth: FirebaseAuth

    init {
        // Build properties from custom config safely
        val options = FirebaseOptions.Builder()
            .setApiKey("AIzaSyAxETgfep4B9wTuLCZz5nFvokyZIKTV4gM")
            .setApplicationId("1:733025846169:android:com.aistudio.topq.kxmpzq")
            .setProjectId("gen-lang-client-0840478351")
            .build()

        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context, options)
        }

        // Target the specific dataset ID directly
        firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
        auth = FirebaseAuth.getInstance()
    }

    fun getAuth(): FirebaseAuth = auth

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getUserId(): String {
        return auth.currentUser?.uid ?: "guest_user_${auth.hashCode()}"
    }

    // List categories flow
    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val subscription = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val cat = doc.toObject(Category::class.java)
                    cat?.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    // List nominees for a category flow (ordered by votes descending)
    fun observeNominees(categoryId: String): Flow<List<Nominee>> = callbackFlow {
        val subscription = firestore.collection("nominees")
            .whereEqualTo("categoryId", categoryId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val nom = doc.toObject(Nominee::class.java)
                    nom?.copy(id = doc.id)
                }?.sortedByDescending { it.votes } ?: emptyList()
                trySend(list)
            }
        awaitClose { subscription.remove() }
    }

    // Check if the user has already voted in a specific category
    suspend fun hasUserVoted(categoryId: String): Boolean {
        val userId = getUserId()
        val docId = "${userId}_$categoryId"
        return try {
            val doc = firestore.collection("votes").document(docId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    // Perform the Vote operation
    suspend fun castVote(categoryId: String, nomineeId: String): Boolean {
        val userId = getUserId()
        val docId = "${userId}_$categoryId"

        if (hasUserVoted(categoryId)) {
            Log.d("FirebaseService", "User already voted in $categoryId")
            return false
        }

        return try {
            val batch = firestore.batch()

            // 1. Create the vote record
            val voteDoc = firestore.collection("votes").document(docId)
            val voteData = mapOf(
                "id" to docId,
                "userId" to userId,
                "categoryId" to categoryId,
                "nomineeId" to nomineeId,
                "votedAt" to System.currentTimeMillis()
            )
            batch.set(voteDoc, voteData)

            // 2. Increment nominee votes
            val nomineeDoc = firestore.collection("nominees").document(nomineeId)
            // Retrieve current nominee votes dynamically or use Increment operator
            batch.update(nomineeDoc, "votes", com.google.firebase.firestore.FieldValue.increment(1))

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseService", "Vote failed", e)
            false
        }
    }

    // Submit a new nominee/category suggestion
    suspend fun submitSuggestion(categoryName: String, nomineeName: String, reason: String): Boolean {
        return try {
            val suggestionDoc = firestore.collection("suggestions").document()
            val data = Suggestion(
                id = suggestionDoc.id,
                categoryName = categoryName,
                nomineeName = nomineeName,
                reason = reason,
                userId = getUserId(),
                submittedAt = System.currentTimeMillis()
            )
            suggestionDoc.set(data).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseService", "Suggestion submit failed", e)
            false
        }
    }

    // Dynamic Seeder to populate the remote Firestore if it is empty!
    suspend fun seedDatabaseIfNeeded() {
        try {
            val categoriesSnapshot = firestore.collection("categories").get().await()
            if (categoriesSnapshot.isEmpty) {
                Log.d("FirebaseService", "Seeding database with default categories & nominees...")
                val rawCategories = listOf(
                    Category("best_doctor", "أفضل طبيب", "stethoscope", 124),
                    Category("best_barber", "أفضل حلاق", "content_cut", 89),
                    Category("best_cafe", "أفضل مقهى", "coffee", 256),
                    Category("best_gym", "أفضل صالة رياضية", "fitness_center", 42),
                    Category("best_pizza", "أفضل بيتزا", "local_pizza", 312),
                    Category("best_salon", "أفضل صالون تجميل", "face_retouching_natural", 67)
                )

                // Add Categories
                for (cat in rawCategories) {
                    firestore.collection("categories").document(cat.id).set(cat).await()
                }

                // Add Barbers
                val barbers = listOf(
                    Nominee("classic_cut", "best_barber", "صالون الحلاقة الكلاسيكية", 2145),
                    Nominee("groomed_co", "best_barber", "صالون جيرومد وشركاه", 1980),
                    Nominee("urban_shave", "best_barber", "محل حلاقة الحضر", 1420),
                    Nominee("fade_factory", "best_barber", "مصنع التدرج (ذا فيد)", 912)
                )
                for (barber in barbers) {
                    firestore.collection("nominees").document(barber.id).set(barber).await()
                }

                // Add Doctors
                val doctors = listOf(
                    Nominee("dr_emily", "best_doctor", "د. إميلي ستون", 154),
                    Nominee("dr_marcus", "best_doctor", "د. ماركوس فانس", 120),
                    Nominee("dr_sarah", "best_doctor", "د. سارة تشن", 98)
                )
                for (doc in doctors) {
                    firestore.collection("nominees").document(doc.id).set(doc).await()
                }

                // Add Cafes
                val cafes = listOf(
                    Nominee("daily_grind", "best_cafe", "مقهى القهوة اليومية", 420),
                    Nominee("mocha_express", "best_cafe", "موخا إكسبريس", 380),
                    Nominee("central_perk", "best_cafe", "سنترال بيرك", 310)
                )
                for (cafe in cafes) {
                    firestore.collection("nominees").document(cafe.id).set(cafe).await()
                }

                // Add Gyms
                val gyms = listOf(
                    Nominee("iron_temple", "best_gym", "نادي معبد الحديد الرياضي", 220),
                    Nominee("powerhouse", "best_gym", "بورهاوس للياقة البدنية", 180),
                    Nominee("golds_athletic", "best_gym", "نادي جولدز الرياضي", 130)
                )
                for (gym in gyms) {
                    firestore.collection("nominees").document(gym.id).set(gym).await()
                }

                // Add Pizza
                val pizzas = listOf(
                    Nominee("luigis", "best_pizza", "بيتزا لويجي الشهيرة", 840),
                    Nominee("slice_heaven", "best_pizza", "بيتزا شريحة من الجنة", 760),
                    Nominee("dough_boys", "best_pizza", "بيتزا دوف بويز", 510)
                )
                for (pizza in pizzas) {
                    firestore.collection("nominees").document(pizza.id).set(pizza).await()
                }

                // Add Salon
                val salons = listOf(
                    Nominee("glam_studio", "best_salon", "استوديو الجاذبية للتجميل", 290),
                    Nominee("chic_cuts", "best_salon", "صالون شيك كتس", 240),
                    Nominee("diva_design", "best_salon", "تصاميم شعر ديفا", 185)
                )
                for (salon in salons) {
                    firestore.collection("nominees").document(salon.id).set(salon).await()
                }

                Log.d("FirebaseService", "Database seeding finished successfully!")
            }
        } catch (e: Exception) {
            Log.e("FirebaseService", "Seeding failed", e)
        }
    }

    // Clear all data (votes, suggestions and reset nominee scores to 0)
    suspend fun clearAllData(): Boolean {
        return try {
            // 1. Delete all votes
            val votesSnapshot = firestore.collection("votes").get().await()
            for (doc in votesSnapshot.documents) {
                doc.reference.delete().await()
            }

            // 2. Delete all suggestions
            val suggestionsSnapshot = firestore.collection("suggestions").get().await()
            for (doc in suggestionsSnapshot.documents) {
                doc.reference.delete().await()
            }

            // 3. Reset nominee votes to 0
            val nomineesSnapshot = firestore.collection("nominees").get().await()
            for (doc in nomineesSnapshot.documents) {
                doc.reference.update("votes", 0).await()
            }
            true
        } catch (e: java.lang.Exception) {
            Log.e("FirebaseService", "Wipe database failed", e)
            false
        }
    }
}
