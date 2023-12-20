package com.matejdro.weartransit.wear

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.wearable.Wearable
import com.matejdro.weartransit.common.CommPaths
import com.matejdro.weartransit.common.toLocalTime
import com.matejdro.weartransit.model.Mode
import com.matejdro.weartransit.model.TransitSteps
import com.matejdro.weartransit.util.getDataItemFlow
import com.matejdro.weartransit.wear.model.TransitStepUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WearableViewModel(application: Application) : AndroidViewModel(application) {
   private val dataClient = Wearable.getDataClient(application)

   @SuppressLint("VisibleForTests")
   val steps: Flow<List<TransitStepUi>?> = dataClient.getDataItemFlow(Uri.parse("wear://*${CommPaths.TRANSIT_STEPS}"))
      .map { rawDataItem ->
         if (rawDataItem == null) return@map null

         val steps = TransitSteps.ADAPTER.decode(rawDataItem.data!!).steps

         buildList {
            var emitWalk = false
            var walkMinutes = 0

            for (step in steps) {
               when (step.mode) {

                  Mode.MODE_WALK -> {
                     emitWalk = true
                     walkMinutes = step.minutes ?: -1
                  }

                  Mode.MODE_RIDE -> {
                     if (emitWalk) {
                        add(TransitStepUi.Walk(step.from_location.orEmpty(), step.minutes ?: 0))
                     }

                     add(
                        TransitStepUi.Ride(
                           step.line_name.orEmpty(),
                           step.line_description.orEmpty(),
                           step.from_location.orEmpty(),
                           step.to_location.orEmpty(),
                           step.from_time?.toLocalTime(),
                           step.to_time?.toLocalTime()
                        )
                     )
                  }

                  Mode.MODE_START -> {}
                  Mode.MODE_DESTINATION -> {
                     if (emitWalk) {
                        add(TransitStepUi.Walk(step.to_location.orEmpty(), walkMinutes))
                     }
                  }
               }
            }
         }
      }
}
