package com.terrascope.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrascope.app.ui.theme.*
import kotlinx.coroutines.delay

private val AiStart = Color(0xFF6C4CF1)
private val AiEnd = Color(0xFF2F80ED)

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Done(val text: String) : AiState()
    data class Failed(val message: String) : AiState()
}

/** Mensaje amable según el estado que devuelve el backend. */
fun aiFailureMessage(status: String) = when (status) {
    "disabled" -> "La IA no está activada en el servidor (falta la clave en el backend)."
    "rejected" -> "La IA mencionó una cifra que no está en tus datos, así que descarté su texto. Intenta de nuevo."
    else -> "No pude generar el reporte IA ahora (puede ser alta demanda del servicio). Intenta de nuevo en un momento."
}

/** Estrella de 4 puntas dibujada a mano (sin depender de iconos extra). */
@Composable
fun Sparkle(dp: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(dp)) {
        val c = center
        val r = size.minDimension / 2f
        val k = r * 0.26f
        val p = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticBezierTo(c.x + k, c.y - k, c.x + r, c.y)
            quadraticBezierTo(c.x + k, c.y + k, c.x, c.y + r)
            quadraticBezierTo(c.x - k, c.y + k, c.x - r, c.y)
            quadraticBezierTo(c.x - k, c.y - k, c.x, c.y - r)
            close()
        }
        drawPath(p, color)
    }
}

/** Botón "Reporte IA" + su animación de carga + el resultado. */
@Composable
fun AiReportSection(
    state: AiState,
    buttonLabel: String = "Reporte IA",
    loadingSteps: List<String>,
    onGenerate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state) {
            is AiState.Idle, is AiState.Failed -> {
                AiButton(label = if (state is AiState.Failed) "Reintentar $buttonLabel" else buttonLabel, onClick = onGenerate)
                if (state is AiState.Failed) {
                    Text(state.message, fontSize = 12.sp, color = BrandTextMuted)
                } else {
                    Text("Un resumen en lenguaje sencillo, escrito por IA solo con los datos de este reporte.", fontSize = 12.sp, color = BrandTextMuted)
                }
            }
            is AiState.Loading -> AiLoadingCard(loadingSteps)
            is AiState.Done -> AiResultCard(state.text)
        }
    }
}

@Composable
private fun AiButton(label: String, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "idle")
    val pulse by t.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Brush.horizontalGradient(listOf(AiStart, AiEnd)))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Sparkle(20.dp, Color.White, Modifier.scale(pulse))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun AiLoadingCard(steps: List<String>) {
    val t = rememberInfiniteTransition(label = "load")
    val shift by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "shift"
    )
    val big by t.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "big"
    )
    val small by t.animateFloat(
        initialValue = 1.2f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "small"
    )
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(steps) {
        while (true) { delay(2600); step = (step + 1) % steps.size }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(AiStart, AiEnd, AiStart),
                        start = androidx.compose.ui.geometry.Offset(-400f + 900f * shift, 0f),
                        end = androidx.compose.ui.geometry.Offset(400f + 900f * shift, 300f)
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sparkle(14.dp, Color.White.copy(alpha = 0.75f), Modifier.scale(small))
                    Spacer(Modifier.width(6.dp))
                    Sparkle(32.dp, Color.White, Modifier.scale(big))
                    Spacer(Modifier.width(6.dp))
                    Sparkle(10.dp, Color.White.copy(alpha = 0.6f), Modifier.scale(small))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Generando tu reporte IA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(steps[step % steps.size], color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color.White, trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(Modifier.height(10.dp))
            Text("Puede tardar unos segundos; si el servicio está saturado, reintento solo.", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun AiResultCard(text: String) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    AnimatedVisibility(visible = shown, enter = fadeIn(tween(500)) + expandVertically(tween(500))) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFF3F0FF), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sparkle(18.dp, AiStart)
                    Spacer(Modifier.width(8.dp))
                    Text("Reporte IA", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AiStart)
                }
                Spacer(Modifier.height(10.dp))
                Text(text, fontSize = 14.sp, color = BrandText, lineHeight = 21.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Escrito por IA solo con los datos de este reporte; las cifras se verificaron contra ellos. Puede contener imprecisiones.",
                    fontSize = 11.sp, color = BrandTextMuted
                )
            }
        }
    }
}
