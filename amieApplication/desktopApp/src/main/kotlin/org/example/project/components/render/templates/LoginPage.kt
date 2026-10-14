package org.example.project.components.render.templates

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.auth.AuthSession
import org.example.project.auth.AuthState
import org.example.project.ui.theme.UnifiedBodyBackground
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor
import java.awt.Desktop
import java.net.URI

/**
 * Sign in with Keycloak: username and password through the token endpoint, or the hosted login page in
 * the browser. "Create account" goes to the register page; guest access needs no server.
 */
@Composable
fun LoginPage(onLoginSuccess: (String) -> Unit, onRegister: () -> Unit = {}) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // What the page is waiting on, shown on the overlay; null when idle
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun attempt(what: String, block: suspend () -> AuthSession) {
        if (busy != null) return
        error = null
        busy = what
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) { block() }
                AuthState.signIn(session)
                onLoginSuccess(session.username)
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthScaffold(title = "Welcome back", subtitle = "Sign in to continue to amie") {
            Spacer(modifier = Modifier.height(16.dp))
            AuthField(username, { username = it }, "Username or email", enabled = busy == null)
            Spacer(modifier = Modifier.height(8.dp))
            AuthField(password, { password = it }, "Password", password = true, enabled = busy == null)
            AuthError(error)

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { attempt("Signing in") { AuthState.client.passwordLogin(username, password) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                enabled = busy == null
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { attempt("Waiting for the browser") { AuthState.client.browserLogin() } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF878e9c)),
                border = BorderStroke(1.dp, Color(UnifiedBorderColor.toArgb())),
                enabled = busy == null
            ) {
                Text("SIGN IN WITH BROWSER (SSO)", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            }

            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onRegister, enabled = busy == null) {
                    Text("Create account", color = Color(0xFFc7cbd4), fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                }
                TextButton(
                    onClick = { openInBrowser(AuthState.client.config.resetPasswordUrl) { error = it } },
                    enabled = busy == null
                ) {
                    Text("Forgot password?", color = Color(0xFF878e9c), fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                }
                TextButton(onClick = { onLoginSuccess("guest-desktop-user") }, enabled = busy == null) {
                    Text("Continue as guest", color = Color(0xFF878e9c).copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                }
            }
        }

        busy?.let { LoadingOverlay(it) }
    }
}

fun openInBrowser(url: String, onError: (String) -> Unit) {
    try {
        Desktop.getDesktop().browse(URI(url))
    } catch (e: Exception) {
        onError("Couldn't open the browser: ${e.message}")
    }
}

@Composable
fun PrintErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Error", color = Color(0xFFc7cbd4), fontWeight = FontWeight.Bold) },
        text = { Text(text = message, color = Color(0xFF878e9c)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = Color(0xFFc7cbd4)) }
        },
        containerColor = Color(UnifiedBoxColor.toArgb()),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun LoadingOverlay(message: String = "Connecting...") {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(UnifiedBodyBackground.toArgb()).copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color(0xFFc7cbd4), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message, color = Color(0xFF878e9c), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
        }
    }
}
