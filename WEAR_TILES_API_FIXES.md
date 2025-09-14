# Wear Tiles API Issues and Required Fixes

## Overview

The Wear module currently has multiple compilation errors related to the Wear Tiles API implementation. This document outlines all identified issues and provides detailed solutions for fixing the Wear Tiles functionality.

## Current Build Errors

### 1. Compilation Errors in TranslateTileService.kt

**Error Messages:**

```
e: file:///home/boypa/projects/WristLingo/wear/src/main/java/com/wristlingo/wear/tiles/TranslateTileService.kt:113:14 Unresolved reference 'setTileTimeline'.
e: file:///home/boypa/projects/WristLingo/wear/src/main/java/com/wristlingo/wear/tiles/TranslateTileService.kt:114:30 Unresolved reference 'Timeline'.
e: file:///home/boypa/projects/WristLingo/wear/src/main/java/com/wristlingo/wear/tiles/TranslateTileService.kt:116:38 Unresolved reference 'TimelineEntry'.
e: file:///home/boypa/projects/WristLingo/wear/src/main/java/com/wristlingo/wear/tiles/TranslateTileService.kt:125:87 Return type of 'onResourcesRequest' is not a subtype of the return type of the overridden member
```

**Root Cause:** The current implementation is using outdated Wear Tiles API methods and incorrect return types for the TileService interface.

### 2. Dependency Version Issues

**Current Dependencies (wear/build.gradle.kts):**

```kotlin
implementation("androidx.wear.tiles:tiles:1.1.0")
implementation("androidx.wear.protolayout:protolayout:1.0.0")
implementation("androidx.wear.protolayout:protolayout-material:1.0.0")
```

**Issue:** Missing required dependencies for ListenableFuture support and using outdated API versions.

### 3. AndroidManifest.xml Configuration Issues

**Current Configuration:**

```xml
<intent-filter>
    <action android:name="android.service.quicksettings.action.QS_TILE_PREFERENCES" />
</intent-filter>
```

**Issue:** Incorrect intent filter action for Wear Tiles API. Should use `androidx.wear.tiles.action.BIND_TILE_PROVIDER`.

## Detailed Analysis of Issues

### Issue 1: API Method Name Changes

- **Problem:** `setTileTimeline()` method doesn't exist in current API
- **Solution:** Use `setTimeline()` method instead
- **Impact:** Core tile building functionality broken

### Issue 2: Missing ListenableFuture Support

- **Problem:** TileService methods now return `ListenableFuture<T>` instead of direct objects
- **Solution:** Add Guava dependency and wrap returns with `Futures.immediateFuture()`
- **Impact:** All tile service methods fail to compile

### Issue 3: Incorrect Import Packages

- **Problem:** Mixing `androidx.wear.tiles.DeviceParametersBuilders` with `androidx.wear.protolayout.*`
- **Solution:** Use consistent package imports from `androidx.wear.protolayout.*`
- **Impact:** Type mismatches and unresolved references

### Issue 4: Deprecated API Usage

- **Problem:** Using deprecated methods like `putExtra()` in AndroidActivity.Builder
- **Solution:** Use modern intent handling approaches
- **Impact:** Launch functionality may not work correctly

### Issue 5: Resource Icon Configuration

- **Problem:** Using system drawable that may not exist on all devices
- **Solution:** Create custom drawable resource for tile preview
- **Impact:** Tile may not display properly in system UI

## Required Fixes

### 1. Update Dependencies

**File:** `wear/build.gradle.kts`

**Current:**

```kotlin
// Wear Tiles and ProtoLayout dependencies (using stable 1.1.0)
implementation("androidx.wear.tiles:tiles:1.1.0")
implementation("androidx.wear.protolayout:protolayout:1.0.0")
implementation("androidx.wear.protolayout:protolayout-material:1.0.0")
```

**Required Fix:**

```kotlin
// Wear Tiles and ProtoLayout dependencies (updated for compatibility)
implementation("androidx.wear.tiles:tiles:1.1.0")
implementation("androidx.wear.protolayout:protolayout:1.0.0")
implementation("androidx.wear.protolayout:protolayout-material:1.0.0")

// Required for ListenableFuture support
implementation("com.google.guava:guava:31.1-android")
```

### 2. Fix AndroidManifest.xml

**File:** `wear/src/main/AndroidManifest.xml`

**Current:**

```xml
<intent-filter>
    <action android:name="android.service.quicksettings.action.QS_TILE_PREFERENCES" />
</intent-filter>
```

**Required Fix:**

```xml
<intent-filter>
    <action android:name="androidx.wear.tiles.action.BIND_TILE_PROVIDER" />
</intent-filter>
```

### 3. Complete TranslateTileService.kt Rewrite

**File:** `wear/src/main/java/com/wristlingo/wear/tiles/TranslateTileService.kt`

**Issues to Fix:**

- Add proper imports for ListenableFuture and Futures
- Change method return types to ListenableFuture<T>
- Use correct API method names (`setTimeline` instead of `setTileTimeline`)
- Fix device parameters handling
- Implement proper launch action without deprecated methods
- Remove unsupported layout properties (setVerticalAlignment, WrapDimensionProp)
- Simplify resource management

### 4. Create Custom Tile Icon

**File:** `wear/src/main/res/drawable/ic_translate_tile.xml`

**Required:** Create a custom microphone icon drawable that works across all devices instead of relying on system drawables.

## Migration Strategy

### Phase 1: Core API Compatibility

1. Update dependencies to include Guava
2. Fix AndroidManifest.xml intent filter
3. Update import statements in TranslateTileService.kt
4. Change method signatures to return ListenableFuture<T>

### Phase 2: Implementation Fixes

1. Replace deprecated API calls
2. Simplify layout structure to avoid unsupported properties
3. Implement proper launch action handling
4. Add custom drawable resources

### Phase 3: Testing and Validation

1. Verify compilation succeeds
2. Test tile appears in Wear OS tile selector
3. Test tile launches MainActivity correctly
4. Test live status badge functionality

## Expected Behavior After Fixes

1. **Tile Compilation:** All compilation errors resolved
2. **Tile Display:** Custom translate tile appears in Wear OS tiles
3. **Tile Interaction:** Tapping tile launches MainActivity with START_LIVE action
4. **Live Status:** Tile shows "LIVE" badge when translation is active
5. **Visual Design:** Material Design 3 styling with proper colors and typography

## Testing Checklist

- [ ] `./gradlew :wear:assembleOfflineDebug` completes successfully
- [ ] Tile appears in Wear OS emulator tile selector
- [ ] Tile displays microphone icon and "Translate" text
- [ ] Tapping tile launches WristLingo Wear app
- [ ] Live status badge appears when translation is active
- [ ] Tile refresh works correctly (30-second intervals)

## Risk Assessment

**Low Risk:**

- Dependency updates (backward compatible)
- AndroidManifest.xml changes (standard API usage)
- Custom drawable creation (isolated change)

**Medium Risk:**

- TranslateTileService.kt rewrite (extensive changes but well-documented API)

**Mitigation:**

- All changes follow official Android Wear Tiles API documentation
- Changes are isolated to tile functionality only
- Existing MainActivity and core app functionality unchanged

## References

- [Android Wear Tiles API Documentation](https://developer.android.com/jetpack/androidx/releases/wear-tiles)
- [ProtoLayout Migration Guide](https://developer.android.com/training/wearables/tiles/protolayout)
- [Wear Tiles Design Principles](https://developer.android.com/design/ui/wear/guides/surfaces/tiles-principles)

---

**Status:** Ready for implementation
**Estimated Effort:** 2-3 hours
**Priority:** High (blocking Wear module compilation)
