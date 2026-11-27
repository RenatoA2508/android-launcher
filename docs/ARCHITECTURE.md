# Arquitectura

## Aplicación y datos

`:app` contiene una actividad de Inicio en Kotlin y Jetpack Compose. [MainActivity](../app/src/main/java/com/renato/launcher/MainActivity.kt) comprueba `RoleManager.ROLE_HOME` al volver a primer plano y atiende las nuevas intenciones HOME en la actividad `singleTask`. La navegación entre Inicio, Búsqueda, catálogo, editores y colores se mantiene en el estado de la actividad.

[AppRepository](../app/src/main/java/com/renato/launcher/apps/AppRepository.kt) obtiene las actividades lanzables por perfil con `LauncherApps`, excluye el propio paquete y abre cada componente con su `UserHandle`. Escucha los cambios de paquetes. Las favoritas y los miembros de colecciones se resuelven independientemente del catálogo completo para poder pintar Inicio mientras este se carga.

| Datos | Almacenamiento e identidad |
| --- | --- |
| Favoritas | Room, tabla `favorites`. Componente, paquete, serial del usuario y posición. |
| Colecciones | Room, tablas `collections` y `collection_apps`. ID y posición de la colección, componente, serial y posición del miembro. Una clave externa elimina las filas hijas al eliminar la colección. |
| Historial | Room, tablas `recent_apps` y `recent_searches`. Componente, serial y hora de apertura o búsqueda. Las consultas se ordenan por la marca de tiempo más reciente. |
| Apariencia (rama interactiva) | `SharedPreferences` del paquete, archivo `appearance`. Se guardan el modo, dos colores ARGB y el porcentaje de opacidad al confirmar el borrador. |
| Insignias | Mapa en memoria por paquete y `UserHandle`. Android entrega las notificaciones activas al listener de cada variante autorizada. |

La base `launcher.db` usa el esquema Room 3 con migraciones desde las versiones 1 y 2. Una cola de mutaciones del proceso conserva el orden de las escrituras al recrear la actividad. Las operaciones que cambian varias filas relacionadas usan transacciones. [Base de datos](../app/src/main/java/com/renato/launcher/data/database/LauncherDatabase.kt) · [Cola](../app/src/main/java/com/renato/launcher/data/LauncherMutationQueue.kt)

## Inicio, catálogo e historial

[HomeScreen](../app/src/main/java/com/renato/launcher/home/HomeScreen.kt) compone la cuadrícula de favoritas, las tarjetas de colección y la hoja con los miembros. Los repositorios de favoritas y colecciones conservan el orden elegido. [LauncherAppCatalog](../app/src/main/java/com/renato/launcher/ui/components/LauncherAppCatalog.kt) implementa la cuadrícula alfabética y su índice para Todas las aplicaciones y los editores. Los bitmaps se reutilizan desde la [caché de iconos](../app/src/main/java/com/renato/launcher/ui/icons/LauncherAppIconCache.kt).

[AppSearchEngine](../app/src/main/java/com/renato/launcher/search/AppSearchEngine.kt) normaliza nombres y consulta. Prioriza coincidencias exactas, alias o siglas, prefijos y coincidencias parciales. [RecentDao](../app/src/main/java/com/renato/launcher/data/database/recent/RecentDao.kt) actualiza ambas secciones en transacciones. Una apertura ordinaria promueve la app a Recientes. Una apertura desde una consulta escrita registra una búsqueda, salvo que la app ya esté entre las cuatro recientes visibles, en cuyo caso refresca su uso. [RecentRepository](../app/src/main/java/com/renato/launcher/recents/RecentRepository.kt) resuelve las filas contra apps aún instaladas y evita duplicarlas entre secciones, también con datos de RC1.

## Del aviso al número

1. Android conecta [LauncherNotificationListenerService](../app/src/main/java/com/renato/launcher/notifications/LauncherNotificationListenerService.kt) cuando el usuario autoriza ese componente en Acceso a notificaciones. Al conectar, el servicio reconstruye el estado desde `activeNotifications`. Un nuevo envío con la misma clave sustituye el aviso anterior. Una retirada elimina esa clave. Al desconectar, se vacía el estado.
2. En cada publicación se exige un ranking disponible con `canShowBadge()`. Se excluyen resúmenes de grupo, avisos sin título ni texto y avisos persistentes del canal por defecto. Cada aviso aceptado contribuye `max(1, notification.number)`. Los cambios de ranking también provocan un recálculo.
3. [NotificationBadgeStore](../app/src/main/java/com/renato/launcher/notifications/NotificationBadgeStore.kt) publica el mapa completo en un `StateFlow`. La consulta de un icono usa paquete y perfil, con cero si la clave está ausente.
4. Inicio, Búsqueda y Todas las aplicaciones observan ese flujo. [LauncherAppIconWithBadge](../app/src/main/java/com/renato/launcher/ui/components/LauncherAppIconWithBadge.kt) dibuja la imagen y, por encima, un círculo rojo con texto blanco si el valor es positivo. A partir de 100 se muestra `99+`. La tarjeta de colección usa un indicador propio para la suma de miembros. La vista previa de los editores consume los contadores. Las celdas de selección del catálogo de los editores pasan cero de forma explícita.

Para diagnosticar una insignia ausente, verifica en este orden el acceso del paquete activo, la conexión del servicio, la aceptación del aviso, la clave de paquete y perfil y el valor entregado a la UI. Con un valor positivo, comprueba la superficie real, el recorte y la posición en el fotograma. La [prueba física](UX.md#comprobar-las-insignias) distingue el valor de entrada del resultado visible.

## Gesto y colores de la rama

[SearchSwipe](../app/src/main/java/com/renato/launcher/home/SearchSwipe.kt) decide entre atajo rápido y arrastre, y respeta los gestos que ya tomó un hijo al mantener pulsado. MainActivity dibuja Búsqueda sobre Inicio. Durante el arrastre usa la fracción de altura recorrida para trasladar el panel, atenuar Inicio y reducir el radio de las esquinas al acercarse al borde. Al soltar, elige el destino y anima el tramo restante. [SearchScreen](../app/src/main/java/com/renato/launcher/search/SearchScreen.kt) pide foco y teclado cuando Búsqueda queda activa.

[ColorSettingsScreen](../app/src/main/java/com/renato/launcher/appearance/ColorSettingsScreen.kt) mantiene el borrador y sincroniza la barra de tono con la entrada HEX. La ventana de Inicio muestra el fondo real detrás de la vista previa. [LauncherColors](../app/src/main/java/com/renato/launcher/appearance/LauncherColors.kt) resuelve la superficie y el contraste a partir del color, la opacidad y el color principal del fondo. El tema compartido se aplica al panel durante el gesto y una vez abierto, y también a las hojas y los editores de colecciones. El color de las insignias y los bitmaps de las apps tienen sus propios dibujos.

## Construcción

El [módulo app](../app/build.gradle.kts) define las identidades de instalación y las opciones de firma. El [catálogo de versiones](../gradle/libs.versions.toml) fija AGP 9.3.2 y Room 2.8.4. El [módulo de perfiles](../baselineprofile/build.gradle.kts) permite generar perfiles de arranque en un dispositivo conectado. Los perfiles existentes están en [generated/baselineProfiles](../app/src/main/generated/baselineProfiles). La compilación debug ordinaria usa el [Gradle Wrapper](../gradle/wrapper/gradle-wrapper.properties).
