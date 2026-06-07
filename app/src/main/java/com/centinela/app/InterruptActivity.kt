package com.centinela.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class InterruptActivity : ComponentActivity() {

    private val QUESTIONS = listOf(
        "¿Esto te acerca a quien quieres ser?",
        "¿Tu yo de mañana te agradecerá esto?",
        "¿Qué deberías estar haciendo AHORA MISMO?",
        "¿Cuántas horas más vas a regalar hoy?",
        "¿Esto es lo que elegiste para tu vida?",
        "Si te viera tu yo de hace 5 años, ¿qué pensaría?",
        "¿Estás construyendo o destruyendo?",
        "¿Qué excusa te estás contando ahora mismo?",
        "El tiempo que pierdes hoy, ¿quién lo paga mañana?",
        "¿Esto es urgente o solo cómodo?"
    )

    private val FALLBACKS_LIST = listOf(
        "La disciplina es elegir entre lo que quieres ahora y lo que quieres más.",
        "No hay versión exitosa de ti que haga lo que estás haciendo ahora.",
        "Cada vez que cedes, le enseñas a tu cerebro que puede cederse.",
        "El dolor de la disciplina pesa menos que el peso del arrepentimiento.",
        "Nadie va a venir a salvarte. O lo haces tú o no lo hace nadie.",
        "Lo que haces cuando nadie te ve define quién eres en realidad."
    )

    private val httpClient = OkHttpClient()

    private var videoUri by mutableStateOf<Uri?>(null)

    private val mediaPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            videoUri = it
            getSharedPreferences("centinela", MODE_PRIVATE)
                .edit()
                .putString("video_uri", it.toString())
                .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("centinela", MODE_PRIVATE)
        val savedUri = prefs.getString("video_uri", null)
        if (savedUri != null) videoUri = Uri.parse(savedUri)
        val mediaIsVideo = prefs.getBoolean("media_is_video", true)
        val videoHasSound = prefs.getBoolean("video_has_sound", true)

        val timeMs = intent.getLongExtra("time_ms", 0L)
        val minutes = timeMs / 60000

        val customQ = loadCustomQuestions(this)
        val customP = loadCustomPhrases(this)
        val allQuestions = QUESTIONS + customQ
        val allPhrases = FALLBACKS_LIST + customP

        setContent {
            InterruptScreen(
                questions = allQuestions,
                minutes = minutes,
                videoUri = videoUri,
                mediaIsVideo = prefs.getBoolean("media_is_video", true),
                videoHasSound = prefs.getBoolean("video_has_sound", true),
                apiKey = prefs.getString("api_key", "") ?: "",
                httpClient = httpClient,
                onPickVideo = { mediaPicker.launch(arrayOf("image/*", "video/*")) },
                onContinue = { finish() },
                onReturn = { finish() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpClient.dispatcher.executorService.shutdown()
    }
}

@Composable
fun InterruptScreen(
    questions: List<String>,
    minutes: Long,
    videoUri: Uri?,
    mediaIsVideo: Boolean = true,
    videoHasSound: Boolean = true,
    apiKey: String,
    httpClient: OkHttpClient,
    onPickVideo: () -> Unit,
    onContinue: () -> Unit,
    onReturn: () -> Unit
) {
    val currentQuestion = remember { questions.random() }
    val aiResponse = remember { mutableStateOf<String?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(videoHasSound) }

    LaunchedEffect(Unit) {
        isLoading.value = true
        aiResponse.value = fetchAIResponse(currentQuestion, minutes, apiKey, httpClient)
        isLoading.value = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        if (videoUri != null) {
            VideoBackground(uri = videoUri)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⚔", fontSize = 48.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "CENTINELA",
                color = Color.White,
                fontSize = 14.sp,
                letterSpacing = 8.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Llevas $minutes minutos en esta app",
                color = Color(0xFFCC0000),
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = currentQuestion,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF222222))
                    .background(Color(0xFF0D0D0D))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        color = Color(0xFFCC0000),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = aiResponse.value ?: "...",
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            SamuraiInterruptButton(
                text = "VOLVER AL TRABAJO",
                color = Color(0xFFCC0000),
                onClick = onReturn
            )

            Spacer(modifier = Modifier.height(12.dp))

            SamuraiInterruptButton(
                text = "CONTINUAR DE TODAS FORMAS",
                color = Color(0xFF1A1A1A),
                onClick = onContinue
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (videoUri == null) "＋ Elegir video o imagen de fondo" else "↺ Cambiar video o imagen",
                color = Color(0xFF444444),
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.clickable { onPickVideo() }
            )
        }
    }
}

@Composable
fun VideoBackground(uri: Uri, hasSound: Boolean = true) {
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            volume = if (hasSound) 1f else 0f
            prepare()
            play()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ImageBackground(uri: Uri) {
    androidx.compose.foundation.Image(
        painter = coil.compose.rememberAsyncImagePainter(uri),
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun SamuraiInterruptButton(text: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isPressed) color.copy(alpha = 0.8f) else color)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            fontWeight = FontWeight.Black
        )
    }
}

suspend fun fetchAIResponse(
    question: String,
    minutes: Long,
    apiKey: String,
    client: OkHttpClient
): String {
    return withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 150)
                put("messages", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", """
                        Eres CENTINELA, un coach de enfoque brutal y directo.
                        Sin rodeos, sin condescendencia, sin emojis.
                        El usuario lleva $minutes minutos en una app de distracción.
                        La pregunta que se le mostró fue: "$question"
                        Dale una respuesta de máximo 2 oraciones.
                        Directa. Que golpee. Que lo haga reflexionar.
                    """.trimIndent())
                }))
            }.toString()

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            json.getJSONArray("content").getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "El tiempo no vuelve. Cada minuto aquí es un minuto robado a quien quieres ser."
        }
    }
}
