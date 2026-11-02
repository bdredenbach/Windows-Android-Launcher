# Third-party build tooling

The Gradle wrapper scripts and `gradle/wrapper/gradle-wrapper.jar` come from the official Gradle 8.13 distribution. Gradle is distributed under the Apache License, Version 2.0; a copy is in `third_party/gradle/LICENSE.txt`. Gradle's wrapper script headers preserve their upstream copyright notices. The Gradle distribution is downloaded separately by the wrapper and verified using its official SHA-256 checksum.

Android SDK/build tools and the Android Gradle Plugin are downloaded as development dependencies under their respective licenses; they are not source assets from the inspiration project. The Internet browser uses AndroidX WebKit 1.13.0 and its transitive AndroidX support/annotation dependencies, distributed under the Apache License, Version 2.0. The license is included in `third_party/androidx/LICENSE.txt`. Source: https://android.googlesource.com/platform/frameworks/support/ . WebKit supplies compatibility APIs; the installed Android WebView provider supplies its own Chromium engine under its provider licenses.

The Nth Desktop code, wallpaper and vector/primitives icon artwork were created for this project. No Microsoft assets or jovanovski/windowslauncher code are included. Android app icons and labels shown at runtime come from apps installed on the user's device.

## Selawik typeface

The unmodified Selawik Regular (`selawk.ttf`) and Semibold (`selawksb.ttf`) fonts are copyright 2015 Microsoft Corporation, with Reserved Font Name Selawik. They are redistributed under the SIL Open Font License 1.1; the complete license is in `third_party/selawik/LICENSE.txt`. Source: https://github.com/microsoft/Selawik/releases/tag/1.01 . Selawik is an open-source Segoe UI replacement; no proprietary Segoe UI font files are included.

## Weather data

Weather forecasts are provided by the Norwegian Meteorological Institute (MET Norway), under CC BY 4.0. This application formats temperatures and condition descriptions from the original data, caches results until the provider's expiry time, and identifies itself in the User-Agent header. Attribution and a licensing link are available in Weather settings. No API key is required. Forecast requests are made only while the configured Weather gadget and launcher are visible. Location search uses Android's Geocoder service; approximate device location is optional and requested only after the user chooses that action.

- https://api.met.no/
- https://docs.api.met.no/doc/TermsOfService
- https://creativecommons.org/licenses/by/4.0/
