plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group = "gov.census.cspro"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    // Target: WebAssembly for browsers
    wasmJs {
        moduleName = "csentry-web"
        browser {
            commonWebpackConfig {
                outputFileName = "csentry-web.js"
            }
            testTask {
                enabled = false
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }
        
        val wasmJsMain by getting {
            dependencies {
                // Kotlin/Wasm specific dependencies - provides window, document, console APIs
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
            }
        }
    }
}

// Task to copy WASM output to public directory
tasks.register<Copy>("copyWasmToPublic") {
    dependsOn("wasmJsBrowserDistribution")
    from(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
    into(layout.projectDirectory.dir("public/wasm-ui"))
}

tasks.named("build") {
    finalizedBy("copyWasmToPublic")
}
