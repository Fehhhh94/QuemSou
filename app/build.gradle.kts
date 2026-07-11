import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.quemsou.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quemsou.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // Exporta o schema do Room para app/schemas — histórico versionado para
    // futuras migrações.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    // Navigation 2.8+ usa kotlinx-serialization-core como 'implementation' internamente,
    // então precisa ser declarada aqui para toRoute<T>/hasRoute<T> compilarem.
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    // Cliente HTTP leve, usado exclusivamente pela tela de catálogo (5A):
    // a partida segue 100% offline.
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Fábrica interna (5B parte 1) — validador de catálogo como ferramenta de
// linha de comando, reusando o domínio existente sem duplicar regra
// nenhuma. Os pontos de entrada vivem em
// `src/test/kotlin/.../ferramentas/catalogo/` (não em `main`) de propósito:
// reusam o classpath já resolvido de `testDebugUnitTest` (JVM pura — o
// domínio + kotlinx.serialization, sem Android em tempo de execução) e
// nunca vão parar no APK.
tasks.register<JavaExec>("validarBaralho") {
    group = "catalogo"
    description = "Valida um JSON de baralho do catálogo. Uso: -Parquivo=<caminho>"
    dependsOn("compileDebugUnitTestKotlin", "processDebugUnitTestJavaRes", "transformDebugUnitTestClassesWithAsm")
    mainClass.set("com.quemsou.app.ferramentas.catalogo.ValidarBaralhoKt")
    // Sem isto, o processo filho usa a codificação padrão do console do
    // Windows e imprime "á"/"✓" como lixo — as mensagens são em português.
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dfile.encoding=UTF-8")
    doFirst {
        val arquivo = project.findProperty("arquivo") as String?
            ?: throw GradleException("Uso: ./gradlew validarBaralho -Parquivo=<caminho do JSON>")
        args = listOf(arquivo)
        classpath = tasks.named<Test>("testDebugUnitTest").get().classpath
    }
}

tasks.register<JavaExec>("validarCatalogo") {
    group = "catalogo"
    description = "Valida indice.json + baralhos/ + consistência cruzada. Uso: -Ppasta=<raiz>"
    dependsOn("compileDebugUnitTestKotlin", "processDebugUnitTestJavaRes", "transformDebugUnitTestClassesWithAsm")
    mainClass.set("com.quemsou.app.ferramentas.catalogo.ValidarCatalogoKt")
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Dfile.encoding=UTF-8")
    doFirst {
        val pasta = project.findProperty("pasta") as String?
            ?: throw GradleException("Uso: ./gradlew validarCatalogo -Ppasta=<raiz do catálogo>")
        args = listOf(pasta)
        classpath = tasks.named<Test>("testDebugUnitTest").get().classpath
    }
}
