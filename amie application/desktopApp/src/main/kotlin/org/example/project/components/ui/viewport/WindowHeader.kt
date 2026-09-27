package org.example.project.components.ui.viewport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.AddButton
import org.example.project.ui.theme.FontColor
import org.example.project.ui.theme.UnifiedBodyBackground

/**
 * A standard top application bar/header window component that provides uniform structural navigation,
 * including an optional back button and a conditional action button to append nested sub-components.
 */
@Composable
fun WindowHeader(name:String,
                 showUser: Boolean = false,
                 user: String = "",
                 modifier: Modifier = Modifier,
                 showOnBack: Boolean = true,
                 onBack: () -> Unit,
                 addComponent: Boolean = false,
                 addComponentNav: () -> Unit = {}) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().offset(y = (-3).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (showOnBack) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = Color(FontColor.toArgb())
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }

            if (showUser && user.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(FontColor.toArgb()).copy(alpha = 0.1f))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User icon",
                        tint = Color(0xFFc7cbd4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = user,
                        color = Color(FontColor.toArgb()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
            }

            // Title
            Text(
                text = name,
                color = Color(FontColor.toArgb()),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.weight(1f))

            // Add Component Button
            if (addComponent) {
                IconButton(
                    onClick = { addComponentNav() },
                    modifier = Modifier.size(15.dp)
                        .background(Color(AddButton.toArgb()), CircleShape).alpha(0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create component",
                        tint = Color(FontColor.toArgb()),
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
        
        // Bottom subtle border
        /*Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(0.3f))
                .align(Alignment.BottomCenter)
        )*/
    }
}
