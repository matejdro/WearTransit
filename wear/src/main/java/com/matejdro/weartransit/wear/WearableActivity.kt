@file:OptIn(ExperimentalComposeUiApi::class)

package com.matejdro.weartransit.wear

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeSource
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.matejdro.weartransit.R
import com.matejdro.weartransit.theme.WearAppTheme
import com.matejdro.weartransit.wear.model.TransitStepUi
import com.matejdro.weartransit.wear.util.ambient.AmbientCallbackController
import com.matejdro.weartransit.wear.util.ambient.AmbientScreen
import com.matejdro.weartransit.wear.util.ambient.AmbientState
import com.matejdro.weartransit.wear.util.ambient.LocalAmbientCallbackController
import com.matejdro.weartransit.wear.util.ambient.ProvideTestAmbientController
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class WearableActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
   private val ambientCallbackController = AmbientCallbackController()

   override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
      return ambientCallbackController
   }

   override fun onStart() {
      super.onStart()

      ambientCallbackController.onUpdateAmbient()
   }

   @OptIn(ExperimentalLifecycleComposeApi::class)
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      AmbientModeSupport.attach(this)

      setContent {
         CompositionLocalProvider(LocalAmbientCallbackController provides ambientCallbackController) {
            val viewModel by viewModels<WearableViewModel>()
            AmbientScreen(
               Modifier.fillMaxSize(),
            ) { modifier: Modifier, _, instant: Instant ->
               val localTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalTime()

               TransitScreen(
                  localTime,
                  viewModel.steps.collectAsStateWithLifecycle(null).value ?: emptyList(),
                  modifier
               )
            }
         }
      }
   }
}

@Composable
fun TransitScreen(time: LocalTime, steps: List<TransitStepUi>, modifier: Modifier = Modifier) {
   val scope = rememberCoroutineScope()
   val focusRequester = remember { FocusRequester() }

   val activeIndex = steps.indexOfFirst { it is TransitStepUi.Ride && time in it.startTime..it.endTime }.takeIf { it >= 0 }
   val state = rememberScalingLazyListState(
      initialCenterItemIndex = activeIndex?.plus(1) ?: 0,
      initialCenterItemScrollOffset = if (activeIndex != null) (with(LocalDensity.current) { -32.dp.roundToPx() }) else 0
   )

   Scaffold(
      modifier = modifier,
      timeText = {
         val timeSource = object : TimeSource {
            val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.GERMANY)
            override val currentTime: String
               @Composable get() = formatter.format(time)
         }
         TimeText(timeSource = timeSource)
      },
      content = {
         ScalingLazyColumn(
            state = state,
            modifier = Modifier
               .onRotaryScrollEvent {
                  scope.launch {
                     state.scrollBy(it.verticalScrollPixels)
                  }
                  true
               }
               .focusRequester(focusRequester)
               .focusable(),
         ) {
            item {
               Spacer(Modifier.height(32.dp))
            }

            items(steps) {
               when (it) {
                  is TransitStepUi.Ride -> {
                     RideStep(it, time in it.startTime..it.endTime)
                  }

                  is TransitStepUi.Walk -> WalkStep(it)
               }
            }

            item {
               Spacer(Modifier.height(32.dp))
            }
         }
      }
   )

   LaunchedEffect(Unit) {
      focusRequester.requestFocus()
   }
}

@Composable
private fun WalkStep(step: TransitStepUi.Walk) {
   val isAmbient = LocalAmbientCallbackController.current.ambientState is AmbientState.Ambient

   val backgroundPainter = if (isAmbient) {
      ColorPainter(Color.Transparent)
   } else {
      ColorPainter(Color.Gray.copy(alpha = 0.3f))
   }

   val context = LocalContext.current

   val outlineModifier = if (isAmbient) {
      Modifier.border(1.dp, Color.Gray, shape = MaterialTheme.shapes.large)
   } else {
      Modifier
   }

   val destination = step.to
   Card(onClick = {
      openNavigation(destination, context)
   }, backgroundPainter = backgroundPainter, modifier = outlineModifier) {
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
         Icon(painterResource(R.drawable.ic_walk), contentDescription = "Walk")
         Text("${destination} [${step.minutes} min]")
      }
   }
}

private fun openNavigation(destination: String, context: Context) {
   val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
   val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
   context.startActivity(mapIntent)
}

@Composable
private fun RideStep(step: TransitStepUi.Ride, active: Boolean) {
   val isAmbient = LocalAmbientCallbackController.current.ambientState is AmbientState.Ambient
   val context = LocalContext.current

   val cardBackground = if (isAmbient) {
      Color.Transparent
   } else if (active) {
      Color.Red.copy(alpha = 0.30f)
   } else {
      Color.Gray.copy(alpha = 0.30f)
   }

   val outlineModifier = if (isAmbient) {
      if (active) {
         Modifier.border(1.dp, Color.Red, shape = MaterialTheme.shapes.large)
      } else {
         Modifier.border(1.dp, Color.Gray, shape = MaterialTheme.shapes.large)
      }
   } else {
      Modifier
   }

   Card(onClick = {
      openNavigation(step.to, context)
   }, backgroundPainter = ColorPainter(cardBackground), modifier = outlineModifier) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
         Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_ride), contentDescription = "Walk")
            Text("${step.lineIdentifier} to ${step.lineDirection}")
         }

         Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(step.startTime.toString())
            Text(step.from)
         }

         Icon(
            painterResource(R.drawable.ic_arrow_down),
            contentDescription = "to",
            modifier = Modifier.align(Alignment.CenterHorizontally)
         )

         Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(step.endTime.toString())
            Text(step.to)
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewScreen() {
   val steps = listOf(
      TransitStepUi.Walk("Picadilly", 4),
      TransitStepUi.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(7, 7),
         LocalTime.of(7, 12)
      ),
      TransitStepUi.Walk("Picadilly", 4),
      TransitStepUi.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(8, 7),
         LocalTime.of(8, 12)
      ),
      TransitStepUi.Walk("Picadilly", 4),
      TransitStepUi.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(9, 7),
         LocalTime.of(9, 12)
      ),
   )

   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Interactive) {
         Box(Modifier.background(Color.Black)) {
            TransitScreen(LocalTime.of(8, 10), steps)
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewScreenAmbient() {
   val steps = listOf(
      TransitStepUi.Walk("Picadilly", 4),
      TransitStepUi.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(7, 7),
         LocalTime.of(7, 12)
      ),
      TransitStepUi.Walk("Picadilly", 4),
      TransitStepUi.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(8, 7),
         LocalTime.of(8, 12)
      ),
      TransitStepUi.Walk("Picadilly", 4),
      TransitStepUi.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(9, 7),
         LocalTime.of(9, 12)
      ),
   )

   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Ambient(false, false)) {
         Box(Modifier.background(Color.Black)) {
            TransitScreen(LocalTime.of(8, 10), steps)
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewWalkStep() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Interactive) {
         Box(Modifier.background(Color.Black)) {
            WalkStep(TransitStepUi.Walk("Picadilly", 4))
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewWalkStepAmbient() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Ambient(false, false)) {
         Box(Modifier.background(Color.Black)) {
            WalkStep(TransitStepUi.Walk("Picadilly", 4))
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewWalkStepWithLongText() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Interactive) {
         Box(Modifier.background(Color.Black)) {
            WalkStep(TransitStepUi.Walk("Travelodge London Central Waterloo Aparthotels Farringdon", 7))
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewRideStep() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Interactive) {
         Box(Modifier.background(Color.Black)) {
            RideStep(
               TransitStepUi.Ride(
                  "Northern",
                  "Morden",
                  "London Bridge",
                  "Elephant & Castle",
                  LocalTime.of(7, 7),
                  LocalTime.of(7, 12)
               ),
               false
            )
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewRideStepAmbient() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Ambient(false, false)) {
         Box(Modifier.background(Color.Black)) {
            RideStep(
               TransitStepUi.Ride(
                  "Northern",
                  "Morden",
                  "London Bridge",
                  "Elephant & Castle",
                  LocalTime.of(7, 7),
                  LocalTime.of(7, 12)
               ),
               false
            )
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewActiveRideStep() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Interactive) {
         Box(Modifier.background(Color.Black)) {
            RideStep(
               TransitStepUi.Ride(
                  "Northern",
                  "Morden",
                  "London Bridge",
                  "Elephant & Castle",
                  LocalTime.of(7, 7),
                  LocalTime.of(7, 12)
               ),
               true
            )
         }
      }
   }
}

@Preview(device = "id:wearos_small_round")
@Composable
private fun PreviewRideStepWithLongText() {
   WearAppTheme {
      ProvideTestAmbientController(AmbientState.Interactive) {
         Box(Modifier.background(Color.Black)) {
            RideStep(
               TransitStepUi.Ride(
                  "Elizabeth",
                  "Chancery Lane",
                  "London Bridge And Elephant and Castle Station",
                  "Farringdon Market Smithfield Mcdonalds Italy Station",
                  LocalTime.of(7, 7),
                  LocalTime.of(7, 12),
               ),
               false
            )
         }
      }
   }
}
