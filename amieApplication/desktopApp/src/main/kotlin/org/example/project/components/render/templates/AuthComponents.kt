package org.example.project.components.render.templates

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.DangerColor
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.MutedFontColor
import org.example.project.ui.theme.UnifiedBodyBackground
import org.example.project.ui.theme.UnifiedBorderColor
import org.example.project.ui.theme.UnifiedBoxColor

/** The dark gradient page with a centered column that the login and register pages share */
@Composable
fun AuthScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(UnifiedBodyBackground.toArgb()), Color.Black)))) {
        Column(
            modifier = Modifier.align(Alignment.Center).widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = Color(FontColor.toArgb()),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = subtitle, color = MutedFontColor, fontSize = 14.sp, fontFamily = FontFamily.SansSerif)
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                content()
            }
        }
    }
}

@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    password: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RectangleShape,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Text),
        trailingIcon = if (!password) null else {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = if (focused) Color(FontColor.toArgb()) else Color(0xFF878e9c)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(FontColor.toArgb()),
            unfocusedTextColor = Color(FontColor.toArgb()),
            disabledTextColor = MutedFontColor,
            focusedContainerColor = Color(UnifiedBoxColor.toArgb()),
            unfocusedContainerColor = Color(UnifiedBoxColor.toArgb()),
            disabledContainerColor = Color(UnifiedBoxColor.toArgb()),
            cursorColor = AddButton,
            focusedBorderColor = AddButton,
            unfocusedBorderColor = Color(UnifiedBorderColor.toArgb()),
            disabledBorderColor = Color(UnifiedBorderColor.toArgb()),
        ),
        label = { Text(label, color = if (focused) Color(FontColor.toArgb()) else Color(0xFF878e9c), fontSize = 14.sp) }
    )
}

/** An error under the form, in place so the user keeps their input */
@Composable
fun AuthError(message: String?) {
    if (message == null) return
    Text(
        text = message,
        color = DangerColor,
        fontSize = 12.sp,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    )
}
