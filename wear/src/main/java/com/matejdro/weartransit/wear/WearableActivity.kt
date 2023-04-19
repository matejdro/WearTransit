package com.matejdro.weartransit.wear

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
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
import com.deliveryhero.whetstone.Whetstone
import com.deliveryhero.whetstone.activity.ContributesActivityInjector
import com.matejdro.weartransit.R
import com.matejdro.weartransit.theme.WearAppTheme
import com.matejdro.weartransit.wear.model.TransitStep
import com.matejdro.weartransit.wear.util.ambient.AmbientCallbackController
import com.matejdro.weartransit.wear.util.ambient.AmbientState
import com.matejdro.weartransit.wear.util.ambient.LocalAmbientCallbackController
import com.matejdro.weartransit.wear.util.ambient.ProvideTestAmbientController
import java.time.LocalTime

@ContributesActivityInjector
class WearableActivity : FragmentActivity(), AmbientModeSupport.AmbientCallbackProvider {
   private val ambientCallbackController = AmbientCallbackController()

   override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
      return ambientCallbackController
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      Whetstone.inject(this)
      super.onCreate(savedInstanceState)

      AmbientModeSupport.attach(this)

      setContent {
      }
   }
}

@Composable
fun TransitScreen(time: LocalTime, steps: List<TransitStep>) {
   val activeIndex = steps.indexOfFirst { it is TransitStep.Ride && time in it.startTime..it.endTime }.takeIf { it >= 0 }
   val state = rememberScalingLazyListState(
      initialCenterItemIndex = activeIndex?.plus(1) ?: 0,
      initialCenterItemScrollOffset = if (activeIndex != null) (with(LocalDensity.current) { -32.dp.roundToPx() }) else 0
   )

   Scaffold(
      timeText = {
         val timeSource = object : TimeSource {
            override val currentTime: String
               @Composable get() = time.toString()
         }
         TimeText(timeSource = timeSource)
      },
      content = {
         ScalingLazyColumn(state = state) {
            item {
               Spacer(Modifier.height(32.dp))
            }

            items(steps) {
               when (it) {
                  is TransitStep.Ride -> {
                     RideStep(it, time in it.startTime..it.endTime)
                  }

                  is TransitStep.Walk -> WalkStep(it)
               }
            }

            item {
               Spacer(Modifier.height(32.dp))
            }
         }
      }
   )
}

@Composable
private fun WalkStep(step: TransitStep.Walk) {
   val isAmbient = LocalAmbientCallbackController.current.ambientState is AmbientState.Ambient

   val backgroundPainter = if (isAmbient) {
      ColorPainter(Color.Transparent)
   } else {
      ColorPainter(Color.Gray.copy(alpha = 0.3f))
   }

   val outlineModifier = if (isAmbient) {
      Modifier.border(1.dp, Color.Gray, shape = MaterialTheme.shapes.large)
   } else {
      Modifier
   }

   Card(onClick = {}, backgroundPainter = backgroundPainter, modifier = outlineModifier) {
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
         Icon(painterResource(R.drawable.ic_walk), contentDescription = "Walk")
         Text(step.to)
      }
   }
}

@Composable
private fun RideStep(step: TransitStep.Ride, active: Boolean) {
   val isAmbient = LocalAmbientCallbackController.current.ambientState is AmbientState.Ambient

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

   Card(onClick = {}, backgroundPainter = ColorPainter(cardBackground), modifier = outlineModifier) {
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
      TransitStep.Walk("Picadilly"),
      TransitStep.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(7, 7),
         LocalTime.of(7, 12)
      ),
      TransitStep.Walk("Picadilly"),
      TransitStep.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(8, 7),
         LocalTime.of(8, 12)
      ),
      TransitStep.Walk("Picadilly"),
      TransitStep.Ride(
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
      TransitStep.Walk("Picadilly"),
      TransitStep.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(7, 7),
         LocalTime.of(7, 12)
      ),
      TransitStep.Walk("Picadilly"),
      TransitStep.Ride(
         "Northern",
         "Morden",
         "London Bridge",
         "Elephant & Castle",
         LocalTime.of(8, 7),
         LocalTime.of(8, 12)
      ),
      TransitStep.Walk("Picadilly"),
      TransitStep.Ride(
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
            WalkStep(TransitStep.Walk("Picadilly"))
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
            WalkStep(TransitStep.Walk("Picadilly"))
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
            WalkStep(TransitStep.Walk("Travelodge London Central Waterloo Aparthotels Farringdon"))
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
               TransitStep.Ride(
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
               TransitStep.Ride(
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
               TransitStep.Ride(
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
               TransitStep.Ride(
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