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

    companion object {
        val DEFAULT_QUESTIONS = listOf(
            "¿Esto te acerca a quien quieres ser?",
            "¿Tu yo de mañana te agradecerá esto?",
            "¿Qué deberías estar haciendo AHORA MISMO?",
            "¿Cuántas horas más vas a regalar hoy?",
            "¿Esto es lo que elegiste para tu vida?",
            "¿Si te viera tu yo de hace 5 años, qué pensaría?",
            "¿Estás construyendo o destruyendo?",
            "¿Qué excusa te estás contando ahora mismo?",
            "El tiempo que pierdes hoy, ¿quién lo paga mañana?",
            "¿Esto es urgente o solo cómodo?"
        )
        val DEFAULT_PHRASES = listOf(
            "La disciplina es elegir entre lo que quieres ahora y lo que quieres más.",
            "No hay versión exitosa de ti que haga lo que estás haciendo ahora.",
            "Cada vez que cedes, le enseñas a tu cerebro que puede cederse.",
            "El dolor de la disciplina pesa menos que el peso del arrepentimiento.",
            "Nadie va a venir a salvarte. O lo haces tú o no lo hace nadie.",
            "Lo que haces cuando nadie te ve define quién eres en realidad."
        )
    }

    private val httpClient = OkHttpClient()
    private var videoUri by mutableStateOf<Uri?>(null)

    private val mediaPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // FIX: persistir permiso para sobrevivir reinicios
            contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
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

        // Cargar preguntas: defaults + custom combinados
        val customQ = loadCustomQuestions(this)
        val customP = loadCustomPhrases(this)
        val allQuestions = DEFAULT_QUESTIONS + customQ
        val allPhrases = DEFAULT_PHRASES + customP

        setContent {
            InterruptScreen(
                questions = allQuestions,
                phrases = allPhrases,
                minutes = minutes,
                videoUri = videoUri,
                mediaIsVideo = mediaIsVideo,
                videoHasSound = videoHasSound,
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
    phrases: List<String>,
    minutes: Long,
    videoUri: Uri?,
    mediaIsVideo: Boolean,
    videoHasSound: Boolean,
    apiKey: String,
    httpClient: OkHttpClient,
    onPickVideo: () -> Unit,
    onContinue: () -> Unit,
    onReturn: () -> Unit
) {
    val context = LocalContext.current
    val question = remember { questions.random() }
    val phrase = remember { phrases.random() }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (apiKey.isNotBlank()) {
            isLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val body = JSONObject().apply {
                        put("model", "claude-haiku-20240307")
                        put("max_tokens", 120)
                        put("messages", JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("content", "Eres CENTINELA. En 2 oraciones máximo, responde esto con brutalidad directa sin condescendencia: $question")
                        }))
                    }.toString().toRequestBody("application/json".toMediaType())
                    val req = Request.Builder()
                        .url("https://api.anthropic.com/v1/messages")
                        .addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .post(body).build()
                    val resp = httpClient.newCall(req).execute()
                    val json = JSONObject(resp.body?.string() ?: "")
                    aiResponse = json.getJSONArray("content").getJSONObject(0).getString("text")
                } catch (e: Exception) {
                    aiResponse = null
                }
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080808))) {
        // Fondo: video o imagen
        videoUri?.let { uri ->
            if (mediaIsVideo) {
                val player = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(uri))
                        repeatMode = ExoPlayer.REPEAT_MODE_ALL
                        volume = if (videoHasSound) 1f else 0f
                        prepare()
                        play()
                    }
                }
                DisposableEffect(Unit) { onDispose { player.release() } }
                AndroidView(
                    factory = { PlayerView(it).apply { this.player = player; useController = false } },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }

        // Overlay oscuro
        Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)))

        // Contenido
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                "⚠ PAUSA OBLIGATORIA",
                color = Color(0xFFCC0000),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            if (minutes > 0) Text(
                "$minutes MIN",
                color = Color(0xFF444444),
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )

            Text(
                question,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF222222))
                    .background(Color(0xFF0D0D0D))
                    .padding(20.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFFCC0000),
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                } else {
                    Text(
                        aiResponse ?: phrase,
                        color = Color(0xFFAAAAAA),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Botón continuar
            val interactionContinue = remember { MutableInteractionSource() }
            val pressedContinue by interactionContinue.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(if (pressedContinue) Color(0xFF990000) else Color(0xFFCC0000))
                    .clickable(interactionSource = interactionContinue, indication = null) { onContinue() },
                contentAlignment = Alignment.Center
            ) {
                Text("CONTINUAR DE TODAS FORMAS", color = Color.White, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }

            // Botón volver
            val interactionReturn = remember { MutableInteractionSource() }
            val pressedReturn by interactionReturn.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color(0xFF00CC44))
                    .background(if (pressedReturn) Color(0xFF004422) else Color.Transparent)
                    .clickable(interactionSource = interactionReturn, indication = null) { onReturn() },
                contentAlignment = Alignment.Center
            ) {
                Text("← VOLVER", color = Color(0xFF00CC44), fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
            }

            // Botón cambiar fondo
            Text(
                "cambiar fondo",
                color = Color(0xFF333333),
                fontSize = 11.sp,
                modifier = Modifier.clickable { onPickVideo() }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
