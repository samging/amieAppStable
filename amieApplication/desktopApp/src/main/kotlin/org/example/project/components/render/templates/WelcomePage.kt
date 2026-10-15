package org.example.project.components.render.templates

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.WatchEvent
import androidx.navigation.NavHostController
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.UnifiedBodyBackground
import org.jetbrains.compose.resources.painterResource
import amiemultiplatform.shared.generated.resources.Res
import androidx.compose.foundation.layout.wrapContentSize
import amiemultiplatform.shared.generated.resources.ascii_amie_welcome

@Composable
fun WelcomePage(navController: NavHostController, onStart: () -> Unit = { navController.navigate("login") }) {
    val session by org.example.project.auth.AuthState.session.collectAsState()
    // The start route only comes here without a session; should one exist (or appear) while this is shown, the
    // page moves on to the dashboard by itself, so a signed-in user never sees this screen or a "Continue as" button
    val restoring by org.example.project.auth.AuthState.restoring.collectAsState()
    LaunchedEffect(session, restoring) {
        if (!restoring && session != null) navController.navigate("home1/") { popUpTo("welcome") { inclusive = true } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(UnifiedBodyBackground.toArgb()), Color.Black)
                )
            )
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(Res.drawable.ascii_amie_welcome),
            contentDescription = "Amie Logo",
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Device tooling, scripting and terminal in one place.",
            color = MutedFontColor,
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            enabled = !restoring,
            modifier = Modifier.fillMaxWidth(0.4f).height(48.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
                disabledContainerColor = Color.White.copy(alpha = 0.6f),
                disabledContentColor = Color.Black.copy(alpha = 0.6f),
            ),
        ) {
            Text(
                if (restoring) "Restoring session…" else "Start",
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 0.5.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No account needed. The sandbox, the models and the metrics work on this machine.",
            color = MutedFontColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(18.dp))
        androidx.compose.material3.TextButton(onClick = { navController.navigate("login") }, enabled = !restoring) {
            Text(
                "Sign in or create an account (only for publishing and devices)",
                color = MutedFontColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
