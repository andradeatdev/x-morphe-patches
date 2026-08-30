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
            // Verified against these exact X versions. Lookups are done via fingerprints,
            // so missing/ambiguous anchors are skipped gracefully instead of failing the apply.
            AppTarget(version = "12.7.1"),
            AppTarget(version = "12.19.1"),
        ),
    )
}