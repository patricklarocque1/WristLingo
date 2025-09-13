package com.wristlingo.wear.tiles

import android.content.Intent
import android.content.SharedPreferences
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.Text
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService

class TranslateTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val deviceParams: DeviceParametersBuilders.DeviceParameters = requestParams.deviceConfiguration

        val intent = Intent(ACTION_START_LIVE).apply {
            setPackage(packageName)
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(EXTRA_START_LIVE, true)
        }
        val click = ModifiersBuilders.Clickable.Builder()
            .setOnClick(ActionBuilders.LoadAction.Builder().build())
            .setId("click-root")
            .build()

        val launch = ModifiersBuilders.Clickable.Builder()
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName("com.wristlingo.wear.MainActivity")
                .setAction(ACTION_START_LIVE)
                .addKeyToExtraMapping(EXTRA_START_LIVE, ActionBuilders.StringProp.Builder("true").build())
                .build()).build())
            .setId("launch")
            .build()

        val root = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .addContent(
                Text.Builder(this, "🎤")
                    .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_DISPLAY1)
                    .setColor(ColorBuilders.ColorProp.Builder(MaterialColors.onBackground(this)).build())
                    .build()
            )
            .addContent(
                Text.Builder(this, "Translate")
                    .setTypography(androidx.wear.protolayout.material.Typography.TYPOGRAPHY_TITLE2)
                    .build()
            )
            .build()

        val live = isLiveActive()
        val liveBadge = if (live) Chip.Builder(this, ModifiersBuilders.Modifiers.Builder().build(), deviceParams)
            .setTextContent("LIVE")
            .setPrimaryColors(androidx.wear.protolayout.material.Colors.PRIMARY)
            .build() else null

        val box = LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(launch).build())
            .addContent(root)
        if (liveBadge != null) {
            box.addContent(liveBadge)
        }

        val layout = LayoutElementBuilders.Layout.Builder().setRoot(box.build()).build()
        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TileBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TileBuilders.TimelineEntry.Builder()
                            .setLayout(layout)
                            .build()
                    ).build()
            ).build()
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder().setVersion("1").build()
    }

    private fun isLiveActive(): Boolean {
        val prefs: SharedPreferences = getSharedPreferences("wristlingo_prefs", MODE_PRIVATE)
        return prefs.getBoolean("live_active", false)
    }

    companion object {
        const val ACTION_START_LIVE = "com.wristlingo.action.START_LIVE"
        const val EXTRA_START_LIVE = "start_live"
    }
}

// Simple helper to get Material color
private object MaterialColors {
    fun onBackground(ctx: android.content.Context): Int = androidx.core.content.ContextCompat.getColor(ctx, android.R.color.white)
}


