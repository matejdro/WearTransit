package com.matejdro.weartransit

import android.app.Application
import logcat.AndroidLogcatLogger
import logcat.LogcatLogger

class WearTransitApplication : Application() {
   override fun onCreate() {
      super.onCreate()

      LogcatLogger.install(AndroidLogcatLogger())
   }
}
