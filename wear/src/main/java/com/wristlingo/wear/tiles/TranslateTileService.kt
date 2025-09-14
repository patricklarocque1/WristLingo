package com.wristlingo.wear.tiles

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class TranslateTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val deviceParams = requestParams.deviceParameters

        // Create launch intent for MainActivity
        val launchIntent = Intent(this, com.wristlingo.wear.MainActivity::class.java).apply {
            action = ACTION_START_LIVE
            putExtra(EXTRA_START_LIVE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val launchAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(packageName)
                    .setClassName("com.wristlingo.wear.MainActivity")
                    .build()
            ).build()

        val clickModifier = ModifiersBuilders.Clickable.Builder()
            .setOnClick(launchAction)
            .setId("launch_translate")
            .build()

        // Create main content with proper Material Design colors
        val colors = Colors.DEFAULT
        
        val micIcon = Text.Builder(this, "🎤")
            .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
            .setColor(ColorBuilders.argb(colors.onSurface))
            .build()
        
        val titleText = Text.Builder(this, "Translate")
            .setTypography(Typography.TYPOGRAPHY_TITLE2)
            .setColor(ColorBuilders.argb(colors.onSurface))
            .build()
        
        val contentColumn = LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
            .setHeight(DimensionBuilders.wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(micIcon)
            .addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setHeight(DimensionBuilders.dp(4f))
                    .build()
            )
            .addContent(titleText)
            .build()

        // Add live status if active
        val isLive = isLiveActive()
        val mainContent = if (isLive) {
            val liveBadge = Text.Builder(this, "LIVE")
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(colors.primary))
                .build()
            
            LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(contentColumn)
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setHeight(DimensionBuilders.dp(8f))
                        .build()
                )
                .addContent(liveBadge)
                .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickModifier).build())
                .build()
        } else {
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(clickModifier).build())
                .addContent(
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                        .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
                        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                        .addContent(contentColumn)
                        .build()
                )
                .build()
        }

        val layout = LayoutElementBuilders.Layout.Builder().setRoot(mainContent).build()
        val tile = TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(30000)
            .setTileTimeline(
                TileBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TileBuilders.TimelineEntry.Builder()
                            .setLayout(layout)
                            .build()
                    ).build()
            ).build()
        
        return Futures.immediateFuture(tile)
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        val resources = ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
        
        return Futures.immediateFuture(resources)
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