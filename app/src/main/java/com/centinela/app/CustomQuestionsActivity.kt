package com.centinela.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun loadCustomQuestions(context: Context): List<String> {
    val prefs = context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
    val raw = prefs.getString("custom_questions", "") ?: ""
    return if (raw.isEmpty()) emptyList() else raw.split("||").filter { it.isNotBlank() }
}

fun saveCustomQuestions(context: Context, questions: List<String>) {
    context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .edit().putString("custom_questions", questions.joinToString("||")).apply()
}

fun loadCustomPhrases(context: Context): List<String> {
    val prefs = context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
    val raw = prefs.getString("custom_phrases", "") ?: ""
    return if (raw.isEmpty()) emptyList() else raw.split("||").filter { it.isNotBlank() }
}

fun saveCustomPhrases(context: Context, phrases: List<String>) {
    context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .edit().putString("custom_phrases", phrases.joinToString("||")).apply()
}

// Defaults editables — se guardan en SharedPreferences la primera vez
fun loadAllQuestions(context: Context): List<String> {
    val prefs = context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
    if (!prefs.contains("all_questions_initialized")) {
        prefs.edit()
            .putString("editable_questions", InterruptActivity.DEFAULT_QUESTIONS.joinToString("||"))
            .putString("editable_phrases", InterruptActivity.DEFAULT_PHRASES.joinToString("||"))
            .putBoolean("all_questions_initialized", true)
            .apply()
    }
    val raw = prefs.getString("editable_questions", "") ?: ""
    return if (raw.isEmpty()) InterruptActivity.DEFAULT_QUESTIONS
    else raw.split("||").filter { it.isNotBlank() }
}

fun saveAllQuestions(context: Context, questions: List<String>) {
    context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .edit().putString("editable_questions", questions.joinToString("||")).apply()
}

fun loadAllPhrases(context: Context): List<String> {
    val prefs = context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
    val raw = prefs.getString("editable_phrases", "") ?: ""
    return if (raw.isEmpty()) InterruptActivity.DEFAULT_PHRASES
    else raw.split("||").filter { it.isNotBlank() }
}

fun saveAllPhrases(context: Context, phrases: List<String>) {
    context.getSharedPreferences("centinela", Context.MODE_PRIVATE)
        .edit().putString("editable_phrases", phrases.joinToString("||")).apply()
}

class CustomQuestionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CustomQuestionsScreen(onDone = { finish() }) }
    }
}

@Composable
fun CustomQuestionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var questions by remember { mutableStateOf(loadAllQuestions(context)) }
    var phrases by remember { mutableStateOf(loadAllPhrases(context)) }
    var newQuestion by remember { mutableStateOf("") }
    var newPhrase by remember { mutableStateOf("") }
    var editingQuestion by remember { mutableStateOf<String?>(null) }
    var editingPhrase by remember { mutableStateOf<String?>(null) }
    var editQText by remember { mutableStateOf("") }
    var editPText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080808))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── PREGUNTAS ──
            item {
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    "TUS PREGUNTAS",
                    color = Color(0xFF444444), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 6.sp
                )
            }

            items(questions) { q ->
                if (editingQuestion == q) {
                    // Modo edición inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = editQText,
                            onValueChange = { editQText = it },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color(0xFFFFFF00))
                        )
                        androidx.compose.material3.Text(
                            "✓",
                            color = Color(0xFF00CC44), fontSize = 18.sp,
                            modifier = Modifier.clickable {
                                if (editQText.isNotBlank()) {
                                    questions = questions.map { if (it == q) editQText else it }
                                    saveAllQuestions(context, questions)
                                }
                                editingQuestion = null
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            "✕",
                            color = Color(0xFFCC0000), fontSize = 18.sp,
                            modifier = Modifier.clickable { editingQuestion = null }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            q, color = Color(0xFFCCCCCC), fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            androidx.compose.material3.Text(
                                "✎",
                                color = Color(0xFF444444), fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp).clickable {
                                    editingQuestion = q
                                    editQText = q
                                }
                            )
                            androidx.compose.material3.Text(
                                "✕",
                                color = Color(0xFF444444), fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    questions = questions.filter { it != q }
                                    saveAllQuestions(context, questions)
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
            }

            // Agregar nueva pregunta
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = newQuestion,
                        onValueChange = { newQuestion = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFFFFFF00)),
                        decorationBox = { inner ->
                            if (newQuestion.isEmpty()) androidx.compose.material3.Text(
                                "Nueva pregunta...", color = Color(0xFF333333), fontSize = 14.sp
                            )
                            inner()
                        }
                    )
                    androidx.compose.material3.Text(
                        "+ AGREGAR",
                        color = if (newQuestion.length > 5) Color(0xFFFFFF00) else Color(0xFF333333),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (newQuestion.length > 5) {
                                questions = questions + newQuestion
                                saveAllQuestions(context, questions)
                                newQuestion = ""
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ── FRASES ──
            item {
                androidx.compose.material3.Text(
                    "TUS FRASES",
                    color = Color(0xFF444444), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 6.sp
                )
            }

            items(phrases) { p ->
                if (editingPhrase == p) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = editPText,
                            onValueChange = { editPText = it },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(Color(0xFFFFFF00))
                        )
                        androidx.compose.material3.Text(
                            "✓",
                            color = Color(0xFF00CC44), fontSize = 18.sp,
                            modifier = Modifier.clickable {
                                if (editPText.isNotBlank()) {
                                    phrases = phrases.map { if (it == p) editPText else it }
                                    saveAllPhrases(context, phrases)
                                }
                                editingPhrase = null
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            "✕",
                            color = Color(0xFFCC0000), fontSize = 18.sp,
                            modifier = Modifier.clickable { editingPhrase = null }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Text(
                            p, color = Color(0xFFCCCCCC), fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            androidx.compose.material3.Text(
                                "✎",
                                color = Color(0xFF444444), fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp).clickable {
                                    editingPhrase = p
                                    editPText = p
                                }
                            )
                            androidx.compose.material3.Text(
                                "✕",
                                color = Color(0xFF444444), fontSize = 16.sp,
                                modifier = Modifier.clickable {
                                    phrases = phrases.filter { it != p }
                                    saveAllPhrases(context, phrases)
                                }
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A1A1A)))
            }

            // Agregar nueva frase
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = newPhrase,
                        onValueChange = { newPhrase = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFFFFFF00)),
                        decorationBox = { inner ->
                            if (newPhrase.isEmpty()) androidx.compose.material3.Text(
                                "Nueva frase...", color = Color(0xFF333333), fontSize = 14.sp
                            )
                            inner()
                        }
                    )
                    androidx.compose.material3.Text(
                        "+ AGREGAR",
                        color = if (newPhrase.length > 5) Color(0xFFFFFF00) else Color(0xFF333333),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (newPhrase.length > 5) {
                                phrases = phrases + newPhrase
                                saveAllPhrases(context, phrases)
                                newPhrase = ""
                            }
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Botón guardar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFFCC0000))
                        .clickable { onDone() },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        "GUARDAR Y SALIR",
                        color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 4.sp
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
