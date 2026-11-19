---
name: launcher-visual
description: Prueba cambios de animación y color de Launcher Dev en el teléfono físico y guarda evidencia local.
---

Selecciona el teléfono autorizado con `adb devices -l`. Compila con `./gradlew :app:assembleDebug` e instala con `adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk`. Abre Inicio con `adb -s "$serial" shell am start -W -n com.renato.launcher.dev/com.renato.launcher.MainActivity`. Si aparece el selector de Inicio, avisa al usuario y espera su elección.

Para animaciones, graba la transición con screenrecord. Ajusta las coordenadas al tamaño de pantalla. Desde Inicio, realiza un deslizamiento completo hacia arriba y verifica el campo de Búsqueda. Vuelve a Inicio, arrastra parcialmente y retrocede antes de soltar. Comprueba que vuelve a Inicio. Repite desde Inicio con un deslizamiento rápido y verifica Búsqueda. Revisa la grabación para detectar saltos o estados intermedios bloqueados.

Para colores, consulta el modo original con `adb -s "$serial" shell cmd uimode night`. Captura la misma pantalla en claro y oscuro con `cmd uimode night no` y `cmd uimode night yes`. Compara texto, iconos, fondo y contraste. Restaura el modo original y compruébalo.

Guarda la evidencia en `captures/` con nombres que identifiquen el gesto o tema. Para una captura usa `adb -s "$serial" exec-out screencap -p > captures/inicio.png`. Mantén la evidencia en la máquina y comunica el resultado observado de cada prueba.
