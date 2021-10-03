package com.mmoczkowski.cyberface

import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository

class CyberWatchFaceService : WatchFaceService() {

  override suspend fun createWatchFace(
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository
  ): WatchFace {

    // Creates class that renders the watch face.
    val renderer = CyberWatchFaceRenderer(
      resources = applicationContext.resources,
      surfaceHolder = surfaceHolder,
      watchState = watchState,
      currentUserStyleRepository = currentUserStyleRepository,
      canvasType = CanvasType.HARDWARE,
      interactiveDrawModeUpdateDelayMillis = 16L
    )

    // Creates the watch face.
    return WatchFace(
      watchFaceType = WatchFaceType.DIGITAL,
      renderer = renderer
    )
  }
}
