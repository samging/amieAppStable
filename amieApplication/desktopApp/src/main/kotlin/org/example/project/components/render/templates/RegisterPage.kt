package org.example.project.components.render.templates

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import org.example.project.auth.EmailVerificationPending
import org.example.project.auth.RegistrationForm
import org.example.project.ui.theme.SuccessColor
import org.example.project.ui.theme.UnifiedBorderColor

/**
 * Create a Keycloak account from the app (Keycloak's registration form, submitted for the user) or in the
 * browser on Keycloak's own page. Either way the new user is signed in when it succeeds.
 */
@Composable
fun RegisterPage(onRegistered: (String) -> Unit, onBackToLogin: () -> Unit) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    // The account exists and a verification mail is on its way; shown instead of an error
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun attempt(what: String, block: suspend () -> AuthSession) {
        if (busy != null) return
        error = null
        notice = null
        busy = what
        scope.launch {
            try {
                val session = withContext(Dispatchers.IO) { block() }
                AuthState.signIn(session)
                onRegistered(session.username)
            } catch (e: EmailVerificationPending) {
                notice = e.message
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            } finally {
                busy = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AuthScaffold(title = "Create account", subtitle = "Register with Keycloak to use amie") {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) { AuthField(firstName, { firstName = it }, "First name", enabled = busy == null) }
                Box(modifier = Modifier.weight(1f)) { AuthField(lastName, { lastName = it }, "Last name", enabled = busy == null) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AuthField(email, { email = it }, "Email", enabled = busy == null)
            Spacer(modifier = Modifier.height(8.dp))
            AuthField(username, { username = it }, "Username", enabled = busy == null)
            Spacer(modifier = Modifier.height(8.dp))
            AuthField(password, { password = it }, "Password", password = true, enabled = busy == null)
            Spacer(modifier = Modifier.height(8.dp))
            AuthField(confirm, { confirm = it }, "Confirm password", password = true, enabled = busy == null)
            AuthError(error)
            notice?.let {
                Text(
                    text = it,
                    color = SuccessColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (password != confirm) {
                        error = "The passwords don't match"
                    } else attempt("Creating the account") {
                        AuthState.client.register(RegistrationForm(username.trim(), email.trim(), password, firstName.trim(), lastName.trim()))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                enabled = busy == null
            ) {
                Text("Create account", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { attempt("Waiting for the browser") { AuthState.client.browserRegister() } },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF878e9c)),
                border = BorderStroke(1.dp, Color(UnifiedBorderColor.toArgb())),
                enabled = busy == null
            ) {
                Text("REGISTER IN BROWSER", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, letterSpacing = 1.sp)
            }

            TextButton(onClick = onBackToLogin, enabled = busy == null, modifier = Modifier.padding(top = 6.dp)) {
                Text("Already have an account? Sign in", color = Color(0xFFc7cbd4), fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
            }
        }

        busy?.let { LoadingOverlay(it) }
    }
}
