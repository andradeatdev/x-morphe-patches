package app.xpatches.patches.com.twitter.android

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_X = Compatibility(
        name = "X (Twitter)",
        packageName = "com.twitter.android",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x000000,
        targets = listOf(
            // Exact version these patches were developed and validated against.
            AppTarget(version = "12.19.1"),
            // Any other version is experimental.
            AppTarget(version = null, isExperimental = true),
        ),
    )
}