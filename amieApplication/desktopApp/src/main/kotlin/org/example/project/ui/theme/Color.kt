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
// Calmed down: the levels are told apart by their badge text and three grays; only an error carries a hue
val LogInfoColor = Color(0xFFB4BCC8)      // light gray
val LogErrorColor = Color(0xFFD9726B)     // muted red
val LogDataColor = Color(0xFF9AA8B8)      // gray-blue
val LogInternalColor = Color(0xFF7D7D7D)  // mid gray
val LogTimestampColor = Color(0xFF6E7681)
// Message text: one neutral for every ordinary line, a light tint only for errors
val LogMessageColor = Color(0xFFD8DEE6)
val LogErrorTextColor = Color(0xFFF0B8B3)
val LogDataTextColor = LogMessageColor
val LogInternalTextColor = Color(0xFFC4C8CE)
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
// The actions that end something (Terminate, Stop, Destroy): a dark red that brightens on hover
val DangerButton = Color(0xFF5E2222)
val DangerButtonHover = Color(0xFF7A2D2D)
val DangerButtonBorder = Color(0xFF7F3434)
// The one accent on the page: the actions that create, build, validate or search (the header's + blue), and
// the charts. ChartSecond is the second series next to it, checked for colour-blind separation on the panel.
val PageAccent = Color(0xFF2B5797)
val PageAccentHover = Color(0xFF356AB5)
val AccentButtonHover = PageAccentHover
// Charts: one muted slate hue in three steps (dark, mid, light) and a gray; nothing vivid, nothing warm
val ChartAccent = Color(0xFF7D93AD)
val ChartSecond = Color(0xFFA9ADB3)
val ChartMarkDark = Color(0xFF4E6480)
val ChartMarkMid = Color(0xFF7D93AD)
val ChartMarkLight = Color(0xFFC3CEDB)
val ChartGrid = Color(0xFF242424)
// Heatmap cells: one hue, dark to light, so a bright cell reads as a large value on the dark panel. Text on a
// cell switches to the dark ink once the cell is lighter than the midpoint.
val ChartHeatLow = Color(0xFF1B1F25)
val ChartHeatHigh = Color(0xFFBCC8D6)
val ChartInkDark = Color(0xFF0B0B0B)
