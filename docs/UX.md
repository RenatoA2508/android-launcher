# Uso y pruebas en el teléfono

## Configurar Inicio

Abre la variante que quieres utilizar, pulsa **Set as default launcher** si aparece y confirma esa misma entrada en el selector de Android. Para las pruebas de la rama usa **Launcher Dev**. La tecla o el gesto de Inicio restablece la pantalla principal incluso desde una hoja o un editor. Guarda las ediciones que quieras conservar antes de volver.

## Organizar las aplicaciones

- **Favoritas.** Desde Inicio vacío usa **Seleccionar aplicaciones**. Con una configuración existente, mantén pulsado un espacio libre y elige **Editar favoritas**. En main la pulsación larga del espacio vacío abre directamente el editor. Selecciona las apps y confirma con **Listo**.
- **Colecciones.** Abre la gestión de colecciones desde el editor de favoritas. Crea una colección o abre una existente para cambiar su nombre y sus integrantes. En Inicio, toca la tarjeta para abrir la hoja. Las acciones de menú incluyen editar y eliminar.
- **Acciones sobre iconos.** Toca un icono para abrir la app. Mantenerlo pulsado da acceso al menú contextual. Mantener y mover inicia el arrastre en las superficies que permiten reordenar. La desinstalación pasa por la confirmación de Android.
- **Buscar y explorar.** Desde Inicio, desliza hacia arriba. Con el campo vacío aparecen las secciones de historial que tengan entradas y el botón **Todas las aplicaciones**. Al escribir aparecen los resultados. Desde el catálogo puedes saltar por inicial con el índice lateral.

## Swipe de Inicio a Búsqueda en esta rama

Un movimiento ascendente de al menos 32 dp, más vertical que horizontal y liberado antes de 180 ms, abre directamente Búsqueda. En un arrastre deliberado, el desplazamiento controla la altura del panel. Si mantienes el dedo quieto, el panel se queda en esa posición. Si retrocedes, vuelve contigo. Al soltar por debajo del 50 % vuelve a Inicio. Al llegar al 50 % o superarlo termina la apertura y se solicita el teclado. Las esquinas superiores se aplanan al llegar al borde. La pulsación larga de una favorita conserva su arrastre.

Para probarlo en el teléfono, empieza lejos del área de navegación del sistema. Haz un swipe corto y rápido, vuelve a Inicio y repite lentamente con pausa y retroceso. Por último, abre por completo y regresa a Inicio. Comprueba el foco, el teclado, la posición del panel y las insignias que ya estaban visibles.

## Apariencia → Colores en esta rama

1. Mantén pulsado un espacio vacío de Inicio y elige **Apariencia → Colores**.
2. **Seguir los colores del sistema** es el valor inicial, también al actualizar una instalación sin preferencias de apariencia. **Personalizar** habilita Acento, Paneles y Opacidad.
3. En **Acento** o **Paneles**, mueve la barra de tono. La etiqueta junto al indicador y el campo HEX se actualizan a la vez. También puedes introducir seis dígitos con `#` opcional. **Más fondo** deja ver más el papel tapiz y **Más opaco** lo cubre.
4. Revisa la barra de búsqueda y la colección de muestra en la vista previa, incluidos sus iconos originales. **Guardar** aplica los valores. **Cancelar** y Atrás descartan el borrador. **Restablecer colores del sistema** cambia el borrador al modo del sistema y se confirma con **Guardar**.

Para verificar una modificación de color, compara una paleta clara y otra oscura, observa los controles y el número de la insignia y repite un arrastre parcial. El panel ya debe mostrar el color elegido mientras el dedo sigue apoyado. Tras Guardar, cierra y vuelve a abrir Launcher Dev. La [skill de pruebas visuales](../.agents/skills/launcher-visual/SKILL.md) describe los comandos de captura y la restauración del modo del dispositivo.

## Comprobar las insignias

1. Comprueba el paquete que tiene el rol de Inicio. En **Acceso a notificaciones** de Android habilita la entrada correspondiente. Para `com.renato.launcher.dev` se llama **Launcher Dev**. El [README](../README.md) incluye los comandos de ADB.
2. Coloca una app de prueba en Favoritas y dentro de una colección que solo contenga esa app. Usa un canal con insignias permitidas y un aviso con título o texto. Mantén el aviso activo.
3. Compara el icono de Inicio, la tarjeta de la colección, el icono dentro de su hoja, un resultado de Búsqueda y la misma app en **Todas las aplicaciones**. Un aviso ordinario aporta uno. Si el emisor declara un número positivo, aporta ese valor.
4. Actualiza el mismo aviso mientras Inicio permanece visible. El icono y la suma deben cambiar en ese momento. Descarta el último aviso y observa que ambos indicadores desaparecen antes de navegar. Repite con las dos paletas y con un swipe completo y uno cancelado.

Las capturas de este diagnóstico deben quedarse en `captures/`, que Git ignora. Para compartir una prueba, usa una notificación sintética y recorta el icono de prueba.


## Incidencia de insignias en Launcher Dev

En el Galaxy S23 Ultra, al probar el APK de `c575ca5`, Inicio pertenecía a Launcher Dev y solo Launcher tenía acceso a notificaciones. Con un aviso sintético activo de valor 3, el almacén de Dev devolvía cero y su icono recibía cero. Al autorizar **Launcher Dev** en Ajustes, el listener se conectó, recuperó ese mismo aviso para el mismo perfil y tanto el icono como la colección mostraron 3.

La actualización del aviso expuso un segundo fallo. Sin salir de Inicio, el servicio, el almacén y la favorita pasaron a 7 mientras la tarjeta de colección seguía mostrando 3. Al retirar el aviso, desapareció la insignia de la favorita y la tarjeta aún mostraba 3. La suma se memorizaba usando la lista de miembros y la función de consulta como claves. Ambas podían conservar su identidad al cambiar los contadores. Ese cálculo ya estaba en `e491f4b`, anterior al swipe interactivo y a Apariencia.

La corrección calcula la suma al leer los contadores actuales durante la composición. El filtro del servicio, la identidad de paquete y perfil y el dibujo de la insignia siguen la implementación de main. La [prueba instrumentada de colección](../app/src/androidTest/java/com/renato/launcher/home/CollectionBadgeTest.kt) cambia solo los contadores, con la misma lista de miembros y la misma función de consulta.

En el APK corregido se verificó la secuencia 0 → 3 → 7 → 0 con la paleta del sistema en modo claro y una paleta personalizada en modo oscuro. Se comprobó el número visible en la favorita, el total de la tarjeta, el miembro de la hoja, Búsqueda y Todas las aplicaciones. El 7 se conservó al cancelar el arrastre y al volver de Búsqueda después de abrirla por completo. También se descartó el aviso desde la bandeja de Android con ambas paletas y se verificó la desaparición de los dos indicadores. Las vistas previas de los editores de favoritas y colección mostraron el valor 3.

La prueba automatizada utiliza dos miembros sintéticos y cambia sus valores sin salir de la composición. Puedes ejecutarla sobre Launcher Dev con el teléfono desbloqueado y el serial del README.

```sh
./gradlew --console=plain :app:assembleDebug :app:assembleDebugAndroidTest
adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$serial" shell am instrument -w -e class com.renato.launcher.home.CollectionBadgeTest com.renato.launcher.dev.test/androidx.test.runner.AndroidJUnitRunner
```
