package com.matejdro.weartransit.common

import com.matejdro.weartransit.model.LocalTimeProto
import java.time.LocalTime

fun LocalTime.toProtobuf(): LocalTimeProto {
   return LocalTimeProto(hour, minute, second)
}

fun LocalTimeProto.toLocalTime(): LocalTime {
   return LocalTime.of(hours, minutes, seconds)
}
