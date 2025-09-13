package com.wristlingo.app.qs

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent

class LiveOverlayTileService : TileService() {
    override fun onStartListening() {
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.label = "Start Live Overlay"
        qsTile?.updateTile()
    }

    override fun onClick() {
        val intent = Intent(ACTION_START_LIVE_OVERLAY).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(EXTRA_START, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivityAndCollapse(intent)
        qsTile?.state = Tile.STATE_ACTIVE
        qsTile?.updateTile()
    }

    companion object {
        const val ACTION_START_LIVE_OVERLAY = "com.wristlingo.action.START_LIVE_OVERLAY"
        const val EXTRA_START = "start"
    }
}


