package com.matejdro.weartransit.wear.model

import java.time.LocalTime

sealed class TransitStepUi {
   data class Walk(val to: String, val minutes: Int) : TransitStepUi()
   data class Ride(
      val lineIdentifier: String,
      val lineDirection: String,
      val from: String,
      val to: String,
      val startTime: LocalTime,
      val endTime: LocalTime
   ) : TransitStepUi()
}
