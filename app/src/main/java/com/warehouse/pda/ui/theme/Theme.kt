package com.warehouse.pda.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
  primary = PdaPrimary,
  onPrimary = PdaOnPrimary,
  primaryContainer = PdaPrimaryContainer,
  onPrimaryContainer = PdaOnPrimaryContainer,
  background = PdaBackground,
  surface = PdaSurface
)

private val DarkColors = darkColorScheme(
  primary = PdaPrimaryContainer,
  onPrimary = PdaOnPrimaryContainer
)

@Composable
fun WarehousePdaTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = AppTypography,
    content = content
  )
}
