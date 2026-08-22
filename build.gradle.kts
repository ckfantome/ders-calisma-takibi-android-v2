// Top-level build file.
plugins {
    id("com.android.application") version "8.3.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    // Room'un derleme-zamani kod uretimi (SessionDao_Impl vb.) icin.
    // Surum formati ("<kotlin-surumu>-<ksp-surumu>") Kotlin surumuyle ESLESMELI.
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}
