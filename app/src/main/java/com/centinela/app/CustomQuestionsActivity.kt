package com.centinela.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CustomQuestionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomQuestionsScreen(onDone = { finish() })
        }
    }
}

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

@Composable
fun CustomQuestionsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var questions by remember { mutableStateOf(loadCustomQuestions(context)) }
    var phrases by remember { mutableStateOf(loadCustomPhrases(context)) }
    var newQuestion by remember { mutableStateOf("") }
    var newPhrase by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF080808))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                androidx.compose.material3.Text(
                    "TUS PREGUNTAS",
                    color = Color(0xFF444444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                )
            }

            items(questions) { q ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Text(
                        q, color = Color.White, fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.Text(
                        "✕",
                        color = Color(0xFF444444),
                        fontSize = 16.sp,
                        modifier = androidx.compose.ui.Modifier.clickable {
                            questions = questions.filter { it != q }
                            saveCustomQuestions(context, questions)
                        }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = newQuestion,
                        onValueChange = { newQuestion = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFFFF0000)),
                        decorationBox = { inner ->
                            if (newQuestion.isEmpty()) androidx.compose.material3.Text(
                                "Escribe tu pregunta...", color = Color(0xFF333333), fontSize = 14.sp
                            )
                            inner()
                        }
                    )
                    androidx.compose.material3.Text(
                        "+ AGREGAR",
                        color = if (newQuestion.length > 5) Color(0xFFFF0000) else Color(0xFF333333),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = androidx.compose.ui.Modifier.clickable {
                            if (newQuestion.length > 5) {
                                questions = questions + newQuestion
                                saveCustomQuestions(context, questions)
                                newQuestion = ""
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                androidx.compose.material3.Text(
                    "TUS FRASES",
                    color = Color(0xFF444444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp
                )
            }

            items(phrases) { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Text(
                        p, color = Color.White, fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.Text(
                        "✕",
                        color = Color(0xFF444444),
                        fontSize = 16.sp,
                        modifier = androidx.compose.ui.Modifier.clickable {
                            phrases = phrases.filter { it != p }
                            saveCustomPhrases(context, phrases)
                        }
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = newPhrase,
                        onValueChange = { newPhrase = it },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFFFF0000)),
                        decorationBox = { inner ->
                            if (newPhrase.isEmpty()) androidx.compose.material3.Text(
                                "Escribe tu frase...", color = Color(0xFF333333), fontSize = 14.sp
                            )
                            inner()
                        }
                    )
                    androidx.compose.material3.Text(
                        "+ AGREGAR",
                        color = if (newPhrase.length > 5) Color(0xFFFF0000) else Color(0xFF333333),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = androidx.compose.ui.Modifier.clickable {
                            if (newPhrase.length > 5) {
                                phrases = phrases + newPhrase
                                saveCustomPhrases(context, phrases)
                                newPhrase = ""
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                androidx.compose.material3.Button(
                    onClick = onDone,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0000)
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    androidx.compose.material3.Text(
                        "GUARDAR Y SALIR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                }
            }
        }
    }
}
