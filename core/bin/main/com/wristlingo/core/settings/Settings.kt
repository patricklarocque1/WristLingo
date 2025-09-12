package com.wristlingo.core.settings

/**
 * Pure settings contract kept in :core; concrete implementations live in app/wear.
 */
interface Settings {
    var defaultTargetLanguage: String
    var autoSpeak: Boolean
    var useCloudTranslate: Boolean
    var useWhisperRemote: Boolean
    var whisperModelPath: String

    var vadRmsThreshold: Int
    var vadSilenceMs: Int
    var partialWindowMs: Int
    var partialThrottleMs: Int
    var backlogCapMs: Int
    var logRms: Boolean
}
