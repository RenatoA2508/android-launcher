# Android Launcher

Launcher para Android inspirado en Samsung One UI, con reloj y fecha sobre el fondo de pantalla, favoritas y colecciones en dos columnas y búsqueda con teclado. El usuario elige el orden de Inicio. El historial se forma al abrir aplicaciones desde el launcher.

## Qué versión instalar

La rama `main` incluye Inicio, Búsqueda, historial, colecciones, catálogo alfabético e insignias numéricas. La rama `feature/busqueda-interactiva` incluye además la preparación de Fedora, la variante **Launcher Dev**, el arrastre interactivo hacia Búsqueda y **Apariencia → Colores**. El historial de integración está en el [roadmap](docs/ROADMAP.md).

| En esta rama | Paquete | Uso |
| --- | --- | --- |
| Debug · Launcher Dev | `com.renato.launcher.dev` | Instalación de desarrollo con su firma debug y sus propios datos |
| Release · Launcher | `com.renato.launcher` | Versión normal con firma de distribución |

Ambos paquetes pueden convivir. El rol de Inicio y el acceso a las notificaciones se eligen por separado en Android. Para ver insignias en Dev hay que autorizar **Launcher Dev**, aunque **Launcher** ya tenga acceso. [Diagnóstico y solución de las insignias](docs/UX.md#incidencia-de-insignias-en-launcher-dev)

## Compilar en Fedora 44

Desde la raíz del clon, para reproducir esta revisión antes de su incorporación a main, usa `git switch feature/busqueda-interactiva`. En una Fedora 44 x86-64 nueva puedes instalar las herramientas de la distribución y Android CLI de Google con estos comandos.

```sh
sudo dnf install git gh android-tools java-25-openjdk-headless
curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh -o /tmp/android-cli-install.sh
bash /tmp/android-cli-install.sh
```

Comprueba las herramientas e instala la plataforma que el catálogo denomina `platforms/android-37.0`.

```sh
java -version
adb version
~/.local/bin/android sdk list --all
~/.local/bin/android sdk install platforms/android-37.0 build-tools/37.0.0 platform-tools
```

Configura la ruta real del SDK en `local.properties`. En esta máquina se usó la siguiente configuración para una instalación nueva.

```sh
printf 'sdk.dir=%s/Android/Sdk\n' "$HOME" > local.properties
./gradlew --version
./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest
```

El Wrapper usa Gradle 9.5.0 y selecciona JDK 17 para el daemon según [los criterios del proyecto](gradle/gradle-daemon-jvm.properties). En Fedora se comprobó con Java 25 como JVM de arranque y JDK 17 para compilar. La primera ejecución necesita acceso a los repositorios de Gradle, Java y Android. El módulo compila para SDK 37, apunta a SDK 37 y admite desde API 29. Debug en esta rama compila sin `keystore.properties`.

## Instalar y configurar el teléfono

Activa la depuración USB, conecta el teléfono desbloqueado y acepta su autorización RSA. Usa el serial del teléfono que figure como `device` en la salida de ADB.

```sh
adb devices -l
serial='SERIAL_DEL_TELEFONO'
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" shell am start -W -n com.renato.launcher.dev/com.renato.launcher.MainActivity
```

1. Si aparece **Set as default launcher**, púlsalo y elige **Launcher Dev** como app de Inicio. Puedes comprobar la elección con `adb -s "$serial" shell cmd role get-role-holders android.app.role.HOME`.
2. Para habilitar las insignias, abre el acceso a notificaciones con `adb -s "$serial" shell am start -a android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. Selecciona **Launcher Dev** y confirma en el teléfono.
3. Vuelve a Inicio. Una notificación activa válida de una favorita debe mostrar su número. Si pertenece a una colección, comprueba también el total de la tarjeta.

`install -r` actualiza el mismo paquete con una firma compatible y conserva su base de datos y preferencias. La variante Dev empieza con su propio almacenamiento al instalarse por primera vez.

## Documentación

- [Producto](docs/PRODUCT.md) describe qué guarda el launcher y cómo se forma el historial.
- [UX y pruebas en el teléfono](docs/UX.md) cubre los gestos, los colores y la verificación de insignias.
- [Arquitectura](docs/ARCHITECTURE.md) relaciona cada función con su código y explica el recorrido de una notificación.
- [Roadmap e historial de integración](docs/ROADMAP.md) distingue la base de main, esta rama y el trabajo pendiente.
