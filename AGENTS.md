# Desarrollo en Fedora 44

Ejecuta los comandos desde la raíz del repositorio. GitHub utiliza la sesión de gh del usuario. Estas comprobaciones funcionaron en Fedora 44:

```sh
gh auth status
gh repo view --json nameWithOwner,defaultBranchRef
adb version
adb devices -l
```

Android CLI está en ~/.local/bin. El catálogo identifica la plataforma solicitada por release(37) como platforms/android-37.0. Estos comandos instalaron el SDK y configuraron su ruta local:

```sh
~/.local/bin/android sdk list --all
~/.local/bin/android sdk install platforms/android-37.0 build-tools/37.0.0 platform-tools
printf 'sdk.dir=%s/Android/Sdk\n' "$HOME" > local.properties
```

Usa el Wrapper del repositorio. En esta máquina descargó el JDK 17 para el daemon con el criterio de gradle/gradle-daemon-jvm.properties. La variante debug se llama Launcher Dev y usa el identificador com.renato.launcher.dev. Para cambios de animación o color sigue la skill en .agents/skills/launcher-visual y guarda la evidencia local en captures/.

La compilación de desarrollo funcionó sin keystore.properties:

```sh
./gradlew --console=plain :app:assembleDebug
```

El APK queda en app/build/outputs/apk/debug/app-debug.apk.
