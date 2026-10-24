package com.example.aivideogenerator

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

private val Ink = Color(0xFF10101A)
private val Panel = Color(0xFF1B1B2A)
private val Purple = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF4DE1D2)
// Replace with your deployed HTTPS backend URL before releasing the app.

private const val API_BASE_URL = "http://10.0.2.2:8000"

private val httpClient = OkHttpClient()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VideoGeneratorScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGeneratorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Text to video") }
    var duration by remember { mutableStateOf("5 seconds") }
    var ratio by remember { mutableStateOf("9:16 Portrait") }
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Your next video starts with an idea.") }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var videoUrl by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImage = uri }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Purple, secondary = Cyan, background = Ink, surface = Panel
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI VIDEO STUDIO", color = Cyan, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("Make it move.", color = Color.White, fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(color = Panel, shape = RoundedCornerShape(14.dp)) {
                        Text("✦ AI", color = Cyan, modifier = Modifier.padding(14.dp),
                            fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(165.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(Brush.linearGradient(listOf(
                                Color(0xFF3B1F69), Color(0xFF182B4D), Color(0xFF103C43)
                            )), RoundedCornerShape(24.dp))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("▶", color = Color.White, fontSize = 42.sp)
                        Text("YOUR CREATIVE CANVAS", color = Color.White,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Describe a scene to get started",
                            color = Color(0xFFD7D5E7), fontSize = 12.sp)
                    }
                }

                Text("CREATE A VIDEO", color = Color.White, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Text to video", "Image to video").forEach { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = { mode = item },
                            label = { Text(item) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Purple,
                                selectedLabelColor = Color.White,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = { Text("A cinematic shot of a quiet lake at sunrise, mist drifting over the water…") },
                    label = { Text("Describe your video") },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = Color(0xFF424256),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Cyan
                    )
                )

                if (mode == "Image to video") {
                    OutlinedButton(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (selectedImage == null) "＋ Choose an image" else "✓ Image selected — change image") }
                    selectedImage?.let {
                        Text("Image selected: ${it.lastPathSegment ?: "photo"}",
                            color = Cyan, fontSize = 12.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DURATION", color = Color.LightGray, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        DropdownChoice(duration, listOf("5 seconds", "10 seconds")) { duration = it }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ASPECT RATIO", color = Color.LightGray, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        DropdownChoice(ratio, listOf("9:16 Portrait", "16:9 Landscape", "1:1 Square")) { ratio = it }
                    }
                }

                Button(
                    onClick = {
                        if (prompt.isBlank()) {
                            status = "Please describe the video you want to create."
                        } else if (mode == "Image to video" && selectedImage == null) {
                            status = "Please choose an image for image-to-video mode."
                        } else {
                            busy = true
                            progress = 0f
                            videoUrl = null
                            status = "Submitting request to the video service…"
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        submitGeneration(context, mode, prompt, duration, ratio, selectedImage)
                                    }
                                    val taskId = result.optString("id").ifBlank { result.optString("task_id") }
                                    if (taskId.isBlank()) throw IOException("Backend response did not include a task ID.")
                                    status = "Generation submitted. Task: $taskId"
                                    var finished = false
                                    repeat(90) {
                                        if (!finished) {
                                            delay(4000)
                                            val task = withContext(Dispatchers.IO) { fetchTask(taskId) }
                                            val state = task.optString("status", "PROCESSING").uppercase()
                                            progress = ((it + 1) / 90f).coerceAtMost(0.98f)
                                            status = "Generation status: $state"
                                            if (state == "SUCCEEDED" || state == "COMPLETED") {
                                                videoUrl = extractVideoUrl(task)
                                                progress = 1f
                                                status = if (videoUrl != null) "Video generated successfully." else "Generation finished, but no video URL was found in the response."
                                                finished = true
                                            } else if (state == "FAILED" || state == "CANCELED" || state == "CANCELLED") {
                                                throw IOException("Video generation $state. Check your provider account and backend logs.")
                                            }
                                        }
                                    }
                                    if (!finished) status = "Still processing. Your task ID is $taskId. Check again later in provider dashboard."
                                } catch (e: Exception) {
                                    status = "Could not generate video: ${e.message ?: "unknown error"}"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text(if (busy) "Preparing…" else "✦  Generate video",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("GENERATION STATUS", color = Cyan, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(status, color = Color.White, fontSize = 13.sp)
                    }
                }

                videoUrl?.let { url ->
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF176B62))
                    ) { Text("▶  Open generated video") }
                }

                Text("RECENT PROJECTS", color = Color.White, fontWeight = FontWeight.Bold)
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("▣", color = Purple, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Your video projects", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Generated videos will appear here after backend integration.",
                                color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
                Text("Prototype • Real AI generation requires a connected provider and secure backend.",
                    color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DropdownChoice(value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)) {
            Text(value, fontSize = 12.sp, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelect(option)
                    expanded = false
                })
            }
        }
    }
}


private suspend fun submitGeneration(
    context: Context,
    mode: String,
    prompt: String,
    duration: String,
    ratio: String,
    imageUri: Uri?
): JSONObject {
    if (API_BASE_URL.contains("YOUR_BACKEND_HOST")) {
        throw IOException("Set API_BASE_URL in MainActivity.kt to your deployed HTTPS backend URL first.")
    }
    val seconds = duration.substringBefore(" ").toIntOrNull() ?: 5
    val ratioValue = when {
        ratio.startsWith("16:9") -> "1280:720"
        ratio.startsWith("1:1") -> "960:960"
        else -> "720:1280"
    }
    val json = JSONObject()
        .put("mode", if (mode == "Image to video") "image" else "text")
        .put("prompt", prompt)
        .put("duration", seconds)
        .put("ratio", ratioValue)
    if (mode == "Image to video") {
        val uri = imageUri ?: throw IOException("Choose an image first.")
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Unable to read the selected image.")
        if (bytes.size > 5 * 1024 * 1024) throw IOException("Selected image must be 5 MB or smaller.")
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        json.put("image_base64", Base64.encodeToString(bytes, Base64.NO_WRAP))
        json.put("image_mime", mime)
    }
    val request = Request.Builder()
        .url(API_BASE_URL.trimEnd('/') + "/generate")
        .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()
    httpClient.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IOException("Backend HTTP ${response.code}: $body")
        return JSONObject(body)
    }
}

private suspend fun fetchTask(taskId: String): JSONObject {
    val request = Request.Builder()
        .url(API_BASE_URL.trimEnd('/') + "/tasks/" + taskId)
        .get().build()
    httpClient.newCall(request).execute().use { response ->
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IOException("Status check HTTP ${response.code}: $body")
        return JSONObject(body)
    }
}

private fun extractVideoUrl(task: JSONObject): String? {
    val output = task.optJSONArray("output")
    if (output != null) {
        for (i in 0 until output.length()) {
            val item = output.optString(i)
            if (item.startsWith("https://")) return item
            val obj = output.optJSONObject(i)
            val url = obj?.optString("url")
            if (!url.isNullOrBlank() && url.startsWith("https://")) return url
        }
    }
    val direct = task.optString("video_url")
    if (direct.startsWith("https://")) return direct
    return null
}
