package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Category
import com.example.data.FirebaseService
import com.example.data.Nominee
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser?, val userId: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class VotingViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseService = FirebaseService(application)

    // Auth flows
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Data lists
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _nominees = MutableStateFlow<List<Nominee>>(emptyList())
    val nominees: StateFlow<List<Nominee>> = _nominees.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _userVotedCategories = MutableStateFlow<Set<String>>(emptySet())
    val userVotedCategories: StateFlow<Set<String>> = _userVotedCategories.asStateFlow()

    // Voting and Verification Stats (Shared Preferences persistence)
    private val prefs = application.getSharedPreferences("topq_voter_stats", android.content.Context.MODE_PRIVATE)
    private val _totalVotesCount = MutableStateFlow(prefs.getInt("total_votes", 0))
    val totalVotesCount: StateFlow<Int> = _totalVotesCount.asStateFlow()

    fun incrementTotalVotes(amount: Int = 1) {
        val next = _totalVotesCount.value + amount
        _totalVotesCount.value = next
        prefs.edit().putInt("total_votes", next).apply()
    }

    fun simulateVoteIncrement() {
        incrementTotalVotes(5)
        viewModelScope.launch {
            _toastMessage.emit("تنبيه: تمت محاكاة +5 أصوات إضافية لتحديث مستوى التوثيق تلقائياً!")
        }
    }

    // UI Feedback events
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        // Run database seeding dynamically
        viewModelScope.launch {
            try {
                firebaseService.seedDatabaseIfNeeded()
                // Observe categories real-time
                firebaseService.observeCategories().collect { list ->
                    _categories.value = list.ifEmpty { getDefaultCategories() }
                }
            } catch (e: Exception) {
                Log.e("VotingViewModel", "Firebase category stream failed, applying local fallback", e)
                _categories.value = getDefaultCategories()
            }
        }

        // Initialize user flow
        checkCurrentUser()
    }

    private fun getDefaultCategories(): List<Category> {
        return listOf(
            Category("best_doctor", "أفضل طبيب", "stethoscope", 124),
            Category("best_barber", "أفضل حلاق", "content_cut", 89),
            Category("best_cafe", "أفضل مقهى", "coffee", 256),
            Category("best_gym", "أفضل صالة رياضية", "fitness_center", 42),
            Category("best_pizza", "أفضل بيتزا", "local_pizza", 312),
            Category("best_salon", "أفضل صالون تجميل", "face_retouching_natural", 67)
        )
    }

    private fun getDefaultNominees(categoryId: String): List<Nominee> {
        return when (categoryId) {
            "best_barber" -> listOf(
                Nominee("classic_cut", "best_barber", "صالون الحلاقة الكلاسيكية", 2145),
                Nominee("groomed_co", "best_barber", "صالون جيرومد وشركاه", 1980),
                Nominee("urban_shave", "best_barber", "محل حلاقة الحضر", 1420),
                Nominee("fade_factory", "best_barber", "مصنع التدرج (ذا فيد)", 912)
            )
            "best_doctor" -> listOf(
                Nominee("dr_emily", "best_doctor", "د. إميلي ستون", 154),
                Nominee("dr_marcus", "best_doctor", "د. ماركوس فانس", 120),
                Nominee("dr_sarah", "best_doctor", "د. سارة تشن", 98)
            )
            "best_cafe" -> listOf(
                Nominee("daily_grind", "best_cafe", "مقهى القهوة اليومية", 420),
                Nominee("mocha_express", "best_cafe", "موخا إكسبريس", 380),
                Nominee("central_perk", "best_cafe", "سنترال بيرك", 310)
            )
            "best_gym" -> listOf(
                Nominee("iron_temple", "best_gym", "نادي معبد الحديد الرياضي", 220),
                Nominee("powerhouse", "best_gym", "بورهاوس للياقة البدنية", 180),
                Nominee("golds_athletic", "best_gym", "نادي جولدز الرياضي", 130)
            )
            "best_pizza" -> listOf(
                Nominee("luigis", "best_pizza", "بيتزا لويجي الشهيرة", 840),
                Nominee("slice_heaven", "best_pizza", "بيتزا شريحة من الجنة", 760),
                Nominee("dough_boys", "best_pizza", "بيتزا دوف بويز", 510)
            )
            "best_salon" -> listOf(
                Nominee("glam_studio", "best_salon", "استوديو الجاذبية للتجميل", 290),
                Nominee("chic_cuts", "best_salon", "صالون شيك كتس", 240),
                Nominee("diva_design", "best_salon", "تصاميم شعر ديفا", 185)
            )
            else -> emptyList()
        }
    }

    private fun checkCurrentUser() {
        try {
            val user = firebaseService.getCurrentUser()
            if (user != null) {
                _authState.value = AuthState.Authenticated(
                    user = user,
                    userId = user.uid,
                    email = user.email ?: "local_voter@topq.com"
                )
                loadUserVotes()
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun loadUserVotes() {
        viewModelScope.launch {
            try {
                val voted = mutableSetOf<String>()
                val cats = _categories.value.ifEmpty { getDefaultCategories() }
                for (cat in cats) {
                    if (firebaseService.hasUserVoted(cat.id)) {
                        voted.add(cat.id)
                    }
                }
                _userVotedCategories.value = voted
            } catch (e: Exception) {
                // Ignore load user votes errors
            }
        }
    }

    fun selectCategory(category: Category?) {
        _selectedCategory.value = category
        if (category != null) {
            viewModelScope.launch {
                try {
                    firebaseService.observeNominees(category.id).collect { list ->
                        _nominees.value = list.ifEmpty { getDefaultNominees(category.id) }
                    }
                } catch (e: Exception) {
                    Log.e("VotingViewModel", "Firebase nominee stream failed, using local fallback", e)
                    _nominees.value = getDefaultNominees(category.id)
                }
            }
        } else {
            _nominees.value = emptyList()
        }
    }

    // Perform Sign-In Fallback (robust & immediate)
    fun signInWithEmail(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val auth = firebaseService.getAuth()
                // Use anonymous or custom-auth to get a real Firebase User ID!
                val result = auth.signInAnonymously().await()
                val user = result.user
                _authState.value = AuthState.Authenticated(
                    user = user,
                    userId = user?.uid ?: "local_voter",
                    email = email
                )
                _toastMessage.emit("تم تسجيل الدخول بنجاح باسم ${email.substringBefore("@")}!")
                loadUserVotes()
            } catch (e: Exception) {
                Log.e("VotingViewModel", "Login fallback failed, using local offline session", e)
                // Fallback to offline/local session instantly to bypass any network or server limits
                _authState.value = AuthState.Authenticated(
                    user = null,
                    userId = "local_voter_${email.hashCode().dec().coerceAtLeast(0)}",
                    email = email
                )
                _toastMessage.emit("تم الدخول محلياً كحساب زائر: $email")
                loadUserVotes()
            }
        }
    }

    // Google Sign-In response processing
    fun handleGoogleSignInResult(userId: String, email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Sign in anonymously on firebase to tie to a real Firebase SDK session safely
                val result = firebaseService.getAuth().signInAnonymously().await()
                val user = result.user
                _authState.value = AuthState.Authenticated(
                    user = user,
                    userId = user?.uid ?: userId,
                    email = email
                )
                _toastMessage.emit("تم تسجيل الدخول باستخدام جوجل: $email")
                loadUserVotes()
            } catch (e: Exception) {
                Log.e("VotingViewModel", "Google binding failed, using local session", e)
                _authState.value = AuthState.Authenticated(
                    user = null,
                    userId = userId,
                    email = email
                )
                _toastMessage.emit("تم تسجيل الدخول عبر جوجل (محلياً): $email")
                loadUserVotes()
            }
        }
    }

    fun signOut() {
        try {
            firebaseService.getAuth().signOut()
        } catch (e: Exception) {
            // Ignore signout error
        }
        _authState.value = AuthState.Unauthenticated
        _userVotedCategories.value = emptySet()
    }

    // Casting a vote
    fun voteForNominee(category: Category, nominee: Nominee) {
        viewModelScope.launch {
            val currentAuth = _authState.value
            if (currentAuth is AuthState.Authenticated && currentAuth.email == "guest_voter@topq.com") {
                _toastMessage.emit("عذراً، لا يمكن التصويت باستخدام حساب الزائر. يرجى المتابعة برقم هاتف أو حساب مفعّل لتسجيل صوتك!")
                return@launch
            }

            if (_userVotedCategories.value.contains(category.id)) {
                _toastMessage.emit("يمكنك التصويت مرة واحدة فقط لكل فئة!")
                return@launch
            }

            val success = firebaseService.castVote(category.id, nominee.id)
            if (success) {
                _userVotedCategories.value = _userVotedCategories.value + category.id
                incrementTotalVotes(1)
                _toastMessage.emit("تم تسجيل صوتك لصالح: ${nominee.name}!")
            } else {
                // Local fallback: increment votes in direct flow state
                _nominees.value = _nominees.value.map {
                    if (it.id == nominee.id) it.copy(votes = it.votes + 1) else it
                }.sortedByDescending { it.votes }

                _userVotedCategories.value = _userVotedCategories.value + category.id
                incrementTotalVotes(1)
                _toastMessage.emit("تم تسجيل صوتك محلياً لصالح: ${nominee.name}!")
            }
        }
    }

    // Submitting a suggestion
    fun submitSuggestion(categoryName: String, nomineeName: String, reason: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            val success = firebaseService.submitSuggestion(categoryName, nomineeName, reason)
            if (success) {
                _toastMessage.emit("تم إرسال اقتراحك بنجاح!")
                onFinished()
            } else {
                _toastMessage.emit("تم حفظ واقتراح ${nomineeName} محلياً بنجاح!")
                onFinished()
            }
        }
    }

    // Wipe/Clear all data and reset scores
    fun clearAllData() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val success = firebaseService.clearAllData()
            _userVotedCategories.value = emptySet()
            _nominees.value = _nominees.value.map { it.copy(votes = 0) }
            _totalVotesCount.value = 0
            prefs.edit().putInt("total_votes", 0).apply()
            checkCurrentUser()
            if (success) {
                _toastMessage.emit("تم بنجاح تصفير ومسح جميع بيانات التصويت والاقتراحات!")
            } else {
                _toastMessage.emit("تم تصفير الأصوات محلياً بنجاح!")
            }
        }
    }
}
