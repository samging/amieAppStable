package org.example.project.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val UnifiedBodyBackground = Color(0xFF0E0E0E)
val UnifiedBoxColor = Color(0xFF171717)
val UnifiedBorderColor = Color(0xFF262626)
val AddButton = Color(0XFF4C8DF6)
val FontColor = Color(0XFFE8EAED)

// Extended palette
val ChromeBackground = Color(0xFF161616)
val SidebarBackground = Color(0xFF1E1E1E)
val SurfaceHover = Color(0xFF2C2C2C)
val MutedFontColor = Color(0xFF8E8E8E)
val AccentSoft = AddButton.copy(alpha = 0.14f)
val SuccessColor = Color(0xFF3FB950)
val DangerColor = Color(0xFFF85149)

// Editor palette: gray chrome and tabs, near-black working panels
val TabBarBackground = Color(0xFF1A1A1A)
val TabActiveBackground = Color(0xFF2A2A2A)
val TabActiveIndicator = Color(0xFF9A9A9A)
val EditorBackground = Color(0xFF080808)
val ConsoleBackground = Color(0xFF0B0B0B)
val ConsoleInputBackground = Color(0xFF161616)
val ConsoleBorderColor = Color(0xFF262626)
val PanelDividerColor = Color(0xFF333333)
val GutterBackground = Color(0xFF0D0D0D)
val LineNumberColor = Color(0xFF5A5A5A)

// Console log levels: one hue per level so the chips and badges are told apart at a glance
val LogInfoColor = Color(0xFF79B8FF)      // blue
val LogErrorColor = Color(0xFFFF7B72)     // red
val LogDataColor = Color(0xFF7EE787)      // green
val LogInternalColor = Color(0xFFD2A8FF)  // violet
val LogTimestampColor = Color(0xFF6E7681)
// Message text: neutral for ordinary lines, a light tint of the level for errors and data
val LogMessageColor = Color(0xFFD8DEE6)
val LogErrorTextColor = Color(0xFFFFB4AE)
val LogDataTextColor = Color(0xFFC6F0CF)
val LogInternalTextColor = Color(0xFFE2D6F5)
// Alternate rows get a faint stripe so wrapped lines stay grouped
val LogRowStripe = Color(0x0AFFFFFF)

// Sandbox editor: syntax colours over EditorBackground and the completion dropdown
val EditorKeywordColor = Color(0xFFC792EA)   // purple/pink: fun, val, if
val EditorApiColor = Color(0xFF56D4BC)       // teal: amie and the script API names
val EditorStringColor = Color(0xFFF2A65A)    // warm orange
val EditorCommentColor = Color(0xFF6B7280)   // muted grey
val EditorNumberColor = Color(0xFF82AAFF)    // light blue
val EditorAnnotationColor = Color(0xFFE5C07B)
val EditorPunctuationColor = Color(0xFFB4BCC8)
val CompletionBackground = Color(0xFF1C1C1E)
val CompletionBorder = Color(0xFF3A3A3C)
val CompletionSelected = Color(0xFF2E3A44)

// Models page: one quiet scheme, the same grays as the sandbox. White marks what is active, gray everything else;
// nothing on the page carries a hue of its own.
val ActiveColor = FontColor
val IdleColor = Color(0xFF4A4A4A)
// Rows and panels sit a step above the page background; a selected row one more step, with a brighter edge
val PanelSurface = Color(0xFF141414)
val PanelSurfaceSelected = Color(0xFF1E1E1E)
val PanelBorder = Color(0xFF242424)
val PanelBorderSelected = Color(0xFF444444)
// Code boxes and session logs: the console's near-black with the console's border
val CodeBackground = Color(0xFF0B0B0B)
// Small buttons: gray chips that brighten on hover instead of carrying a colour
val ChipBackground = Color(0xFF1F1F1F)
val ChipHoverBackground = Color(0xFF2C2C2C)
val ChipBorderColor = Color(0xFF343434)
// The one accent on the page: the actions that create, build, validate or search (the header's + blue), and
// the charts. ChartSecond is the second series next to it, checked for colour-blind separation on the panel.
val PageAccent = Color(0xFF2B5797)
val PageAccentHover = Color(0xFF356AB5)
val AccentButtonHover = PageAccentHover
val ChartAccent = Color(0xFF3568C4)
val ChartSecond = Color(0xFFC9742A)
val ChartGrid = Color(0xFF242424)
