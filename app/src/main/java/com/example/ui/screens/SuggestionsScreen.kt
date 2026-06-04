package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.VotingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(
    viewModel: VotingViewModel,
    onSuccess: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var nomineeName by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الاقتراحات",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Text("💡", fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header Text
            Text(
                text = "تقديم اقتراح جديد",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ساعد في تشكيل مستقبل لوحة صدارة مجتمعنا المحلي عبر ترشيح فئات أو مرشحين جدد.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // White Form Card Card shadow
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Name Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "اسم الفئة / التصنيف",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it },
                            placeholder = { Text("مثال: بطل محلي، أفضل مقهى جديد") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggestion_category_input"),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("📁", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true
                        )
                    }

                    // Nominee Recommendation Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "اسم الجهة أو الشخص المرشح",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = nomineeName,
                            onValueChange = { nomineeName = it },
                            placeholder = { Text("أدخل اسم المرشح المقترح") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suggestion_nominee_input"),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("👤", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            singleLine = true
                        )
                    }

                    // Reason for Suggestion Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "سبب الترشيح / مبررات الاقتراح",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            placeholder = { Text("لماذا يستحق هذا المرشح التواجد في المسابقة؟") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("suggestion_reason_input"),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Box(modifier = Modifier.padding(bottom = 60.dp, start = 8.dp)) { Text("📝", fontSize = 16.sp) } },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            maxLines = 4
                        )
                    }



                    // Submit Button
                    Button(
                        onClick = {
                            if (categoryName.isNotBlank() && nomineeName.isNotBlank() && reasonText.isNotBlank()) {
                                isSubmitting = true
                                focusManager.clearFocus()
                                viewModel.submitSuggestion(
                                    categoryName = categoryName,
                                    nomineeName = nomineeName,
                                    reason = reasonText
                                ) {
                                    isSubmitting = false
                                    categoryName = ""
                                    nomineeName = ""
                                    reasonText = ""
                                    onSuccess()
                                }
                            }
                        },
                        enabled = !isSubmitting && categoryName.isNotBlank() && nomineeName.isNotBlank() && reasonText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_suggestion_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "إرسال الاقتراح والترشيح",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text("📤", fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submission Guidelines Tip box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("ℹ️", fontSize = 24.sp)
                    Column {
                        Text(
                            text = "إرشادات تقديم الترشيحات",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تأكد من توافق الاقتراح مع مصداقية ومعايير مجتمعاتنا المحلية. يتم مراجعة جميع الطلبات خلال 24 ساعة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
