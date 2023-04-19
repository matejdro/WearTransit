package com.matejdro.weartransit.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.matejdro.weartransit.JavaTimeMoshiAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.internal.Util
import logcat.logcat
import java.time.LocalTime

class TransitWatcherService : AccessibilityService() {
   private val moshi = Moshi.Builder().add(JavaTimeMoshiAdapter).build()

   private var currentSteps = emptyList<TransitStep>()

   override fun onAccessibilityEvent(event: AccessibilityEvent) {
//      logNodeHeirarchy(rootInActiveWindow, 1)
//
//      logcat {"Event ${AccessibilityEvent.eventTypeToString(event.eventType)}"}

//      val source = event.source ?: return
//      val id = source.viewIdResourceName ?: return
//      if (id.contains("details_cardlist") || id.contains("expandingscrollview_container")) {
//      }
      val source = event.source ?: return
      if (source.viewIdResourceName?.contains("details_cardlist") == true) {
         val steps = source.extractTransitSteps(log = true)
         if (steps != null && steps.isNotEmpty()) {
            val mergedSteps = currentSteps.mergeWith(steps)
            currentSteps = mergedSteps
            val adapter =
               moshi.adapter<List<TransitStep>>(Util.ParameterizedTypeImpl(null, List::class.java, TransitStep::class.java))

            logcat { "Steps: ${adapter.toJson(mergedSteps)}" }

            if (mergedSteps.firstOrNull()?.mode == Mode.START && mergedSteps.lastOrNull()?.mode == Mode.DESTINATION) {
               logcat { "COMPLETE" }
               Toast.makeText(this, "Transit Captured", Toast.LENGTH_SHORT).show()
            }
         }
      }
//      logcat { "Change ${source.viewIdResourceName} ${source.toJson()}" }
   }

   override fun onInterrupt() {}
}

fun AccessibilityNodeInfo.toJson(): String {
   val children = children().map { it.toJson() }.joinToString()
   return "{\"text\":${this.text.toJson()},\"descr\":${this.contentDescription.toJson()}," +
      "\"id\":${this.viewIdResourceName.toJson()},\"children\":[$children]}"
}

@Suppress("LongMethod", "ComplexMethod", "LoopWithTooManyJumpStatements", "MagicNumber")
private fun AccessibilityNodeInfo.extractTransitSteps(log: Boolean): List<TransitStep>? {
   return try {
      if (findChildrenById("details_non_transit_leg").isEmpty()) {
         return null
      }

      val steps = ArrayList<TransitStep>()

      if (log) logcat { "START ${this.toJson()}" }

      for ((stepIndex, step) in children().withIndex()) {
         val children = step.children().toList()
         val fullText = step.getFullText()

         if (step.viewIdResourceName?.contains("details_non_transit_leg") == true) {
            if (log) logcat { " WALKING STEP ${step.toJson()}" }
            val description = fullText
            val walkMatch = WALK_REGEX.find(description)
            if (walkMatch != null) {
               if (log) logcat { "  WALK ${walkMatch.groupValues.elementAt(1)} MINUTES" }
               steps += TransitStep(Mode.WALK, minutes = walkMatch.groupValues.elementAt(1).toInt())
            }
            continue
         }

         if (children.size >= 2 && children.lastOrNull()?.text?.let { TIME_REGEX.matches(it) } == true) {
            val location = children.elementAt(0).text
            val time = LocalTime.parse(children.last().text)
            if (log) logcat { " START/END STEP ${step.toJson()}" }
            if (log) logcat { "START/END '$location' at $time" }

            val startStep = stepIndex == 0

            steps += if (startStep) {
               TransitStep(
                  Mode.START,
                  fromTime = time,
                  fromLocation = location.toString()
               )
            } else {
               TransitStep(
                  Mode.DESTINATION,
                  toTime = time,
                  toLocation = location.toString()
               )
            }

            if (startStep) {
               continue
            } else {
               break
            }
         }

         if (children.size == 3) {
            if (log) logcat { " TRANSIT STEP ${step.toJson()}" }

            val rideNode = children.elementAt(0)
            if (rideNode.childCount < 3) {
               continue
            }

            val startLocation = rideNode.getChild(0).getFullText(includeContentDescription = false)
            val line = rideNode.getChild(1).text.trim()
            val lineName = rideNode.getChild(2).text
            val departureTime = LocalTime.parse(DEPART_TIME_REGEX.find(fullText)!!.groupValues.elementAt(1))

            val endLocation: String
            val endTime: LocalTime
            val endNode = children.elementAt(2)
            val locationTimeMatch = endNode.text?.let { LOCATION_AND_TIME_REGEX.find(it) }
            if (locationTimeMatch != null) {
               endLocation = locationTimeMatch.groupValues.elementAt(1)
               endTime = LocalTime.parse(locationTimeMatch.groupValues.elementAt(2))
            } else {
               endLocation = endNode.getChild(0).text.toString()
               endTime = LocalTime.parse(endNode.getChild(1).text)
            }

            if (log) {
               logcat {
                  "  RIDE LINE $line ($lineName) FROM $startLocation " +
                     "AT $departureTime UNTIL $endLocation at $endTime"
               }
            }

            steps += TransitStep(
               Mode.RIDE,
               fromTime = departureTime,
               toTime = endTime,
               fromLocation = startLocation,
               toLocation = endLocation,
               lineNumber = line.toString(),
               lineDescription = lineName.toString()
            )

            continue
         }

         if (children.isNotEmpty()) {
            logcat { "UNKNOWN STEP ${step.toJson()}" }
         }
      }

      steps
   } catch (e: Exception) {
      logcat { "Failed to parse ${toJson()}" }
      e.printStackTrace()
      null
   }
}

private fun AccessibilityNodeInfo.findChildrenById(targetId: String): List<AccessibilityNodeInfo> {
   val foundChildren = ArrayList<AccessibilityNodeInfo>()
   if (viewIdResourceName?.contains(targetId) == true) {
      foundChildren += this
   }

   for (i in 0 until childCount) {
      val child = getChild(i) ?: continue
      foundChildren.addAll(child.findChildrenById(targetId))
   }
   return foundChildren
}

private fun CharSequence?.toJson(): String {
   return if (this != null) {
      "\"$this\""
   } else {
      "null"
   }
}

private fun AccessibilityNodeInfo.children(): Sequence<AccessibilityNodeInfo> {
   return (0 until childCount).asSequence().mapNotNull { getChild(it) }
}

private fun AccessibilityNodeInfo.getFullText(includeContentDescription: Boolean = true): String {
   val builder = StringBuilder()
   insertFullText(builder, includeContentDescription)
   return builder.toString().trim()
}

private fun AccessibilityNodeInfo.insertFullText(builder: StringBuilder, includeContentDescription: Boolean = true) {
   text?.let {
      builder.append(it)
      builder.append(' ')
   }

   if (includeContentDescription) {
      contentDescription?.let {
         if (it != text) {
            builder.append(it)
            builder.append(' ')
         }
      }
   }

   for (child in children()) {
      child.insertFullText(builder, includeContentDescription)
   }
}

private fun List<TransitStep>.mergeWith(other: List<TransitStep>): List<TransitStep> {
   forEachIndexed { thisIndex, thisTransitStep ->
      if (thisTransitStep.fromTime == null && thisTransitStep.toTime == null) {
         return@forEachIndexed
      }

      other.forEachIndexed { otherIndex, otherTransitStep ->
         if (otherTransitStep == thisTransitStep) {
            return if (thisIndex > otherIndex) {
               this.take(thisIndex + 1) + other.drop(otherIndex + 1)
            } else {
               other.take(otherIndex + 1) + this.drop(thisIndex + 1)
            }
         }
      }
   }

   logcat { "NO MATCH" }
   return other
}

@JsonClass(generateAdapter = true)
data class TransitStep(
   val mode: Mode,
   val fromTime: LocalTime? = null,
   val toTime: LocalTime? = null,
   val minutes: Int? = null,
   val fromLocation: String? = null,
   val toLocation: String? = null,
   val lineNumber: String? = null,
   val lineDescription: String? = null,
)

enum class Mode {
   START,
   RIDE,
   WALK,
   DESTINATION
}

private val LOCATION_AND_TIME_REGEX = Regex("(.+).? [0-9]{2}:[0-9]{2}")
private val DEPART_TIME_REGEX = Regex("Departs at ([0-9]{2}:[0-9]{2})")
private val TIME_REGEX = Regex("[0-9]{2}:[0-9]{2}")
private val WALK_REGEX = Regex("Walk ([0-9]+) min", RegexOption.IGNORE_CASE)
