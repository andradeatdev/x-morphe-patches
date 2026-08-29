# 🧩 X Patches for Morphe

Patches for Twitter / X that fix sensitive media handling, to be used with the
[Morphe patcher](https://github.com/MorpheApp/morphe-patcher).

## ❓ About

These patches were originally developed for the ReVanced patcher and migrated to Morphe.
They target X 12.19.1 (com.twitter.android) and are known to work when the app is patched
without signature spoofing, split bundles, or the "Clear old X lib" / splitting options.

### What's included

- **Bypass sensitive media blur** — skips the blur/age-gate interstitial in the Compose
  (x-lite) timeline, post detail, and profile views.
- **Disable legacy sensitive media blur** — forces the legacy `needs blur` predicate to
  always return false (tweetview detail, metrics charts, conversations in legacy mode).
- **Always display sensitive media** — forces `AccountSettings.getDisplaySensitiveMedia()`
  to return true.

### How to use these patches

Click here to add these patches to Morphe:
https://morphe.software/add-source?github=andradeatdev/x-morphe-patches

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->
> **[v1.0.1](https://github.com/andradeatdev/x-morphe-patches/releases/tag/v1.0.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`v1.0.1`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 X (Twitter)&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 12.19.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Always display sensitive media](#always-display-sensitive-media) | Forces AccountSettings.getDisplaySensitiveMedia to always return true. |  |
| [Bypass sensitive media blur](#bypass-sensitive-media-blur) | Skips the blur/age-gate interstitial in Compose media, showing sensitive media directly. |  |
| [Disable legacy sensitive media blur](#disable-legacy-sensitive-media-blur) | Forces the legacy 'needs blur' predicate to always return false. |  |

</details>

<!-- PATCHES_END -->

## 🚀 Release process

Releases are tag-based. Create a tag `vX.Y.Z` and push it; the release workflow will build
the patches, generate `patches-list.json` / `patches-bundle.json`, update this README and
attach the `.mpp` bundle to the GitHub release.

```sh
git tag v1.0.0
git push origin v1.0.0
```

## 🧑‍💻 Dev usage

- Run `./gradlew buildAndroid` to build the mpp file in `patches/build/libs/patches-*.mpp`.
- Patch the mpp file using [Morphe-Desktop](https://github.com/MorpheApp/morphe-desktop)
  like any other patch bundle.
- See the [Morphe documentation](https://github.com/MorpheApp/morphe-documentation)
  and the migration guide
  [MorpheApp/morphe-patcher#22](https://github.com/MorpheApp/morphe-patcher/issues/22)
  for creating patches with `app.morphe.patcher`.

## 📜 License

X Patches are licensed under the [GNU General Public License v3.0](LICENSE)