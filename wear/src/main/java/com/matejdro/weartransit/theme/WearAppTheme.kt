package com.matejdro.weartransit.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

@Composable
fun WearAppTheme(
   content: @Composable () -> Unit
) {
   val defaultTextStyle = Typography().body1.copy(
      textAlign = TextAlign.Center
   )

   MaterialTheme(
      colors = colorPalette,
      content = content,
      typography = Typography().copy(body1 = defaultTextStyle)
   )
}

object DarkAppColors {
   val primary = Color(0xfffabd00)
   val secondary = Color(0xffd8c4a0)
   val tertiary = Color(0xffb0cfaa)
}

private val colorPalette = Colors(
   primary = DarkAppColors.primary,
   secondary = DarkAppColors.secondary,
)
