# Estado del proyecto

## Integrado en main

La base revisada es `bca6543`, merge de RC2. La etiqueta `v1.0.0-rc1` apunta al merge anterior `55b7aea`. El nombre de versión declarado por el APK de esta rama sigue siendo `1.0.0-rc1` con código 1. Para identificar una instalación de desarrollo comprueba también el paquete y la revisión compilada.

| Etapa | Entrega integrada |
| --- | --- |
| Inicio y descubrimiento | Rol HOME, apps instaladas y distribución de Inicio. [PR 1](https://github.com/RenatoA2508/android-launcher/pull/1), [PR 2](https://github.com/RenatoA2508/android-launcher/pull/2), [PR 3](https://github.com/RenatoA2508/android-launcher/pull/3) |
| Búsqueda e interacción | Campo con teclado, historial, ranking compartido y acciones de Inicio. [PR 4](https://github.com/RenatoA2508/android-launcher/pull/4), [PR 5](https://github.com/RenatoA2508/android-launcher/pull/5), [PR 6](https://github.com/RenatoA2508/android-launcher/pull/6) |
| Insignias, colecciones y catálogo | Conteo de avisos por perfil, agrupación ordenada y Todas las aplicaciones. [PR 7](https://github.com/RenatoA2508/android-launcher/pull/7), [PR 8](https://github.com/RenatoA2508/android-launcher/pull/8), [PR 9](https://github.com/RenatoA2508/android-launcher/pull/9) |
| RC1 y cambios posteriores | Persistencia y ciclo de vida, limpieza al desinstalar, 14 favoritas y 6 colecciones, separación de historiales y catálogo visual compartido. [PR 10](https://github.com/RenatoA2508/android-launcher/pull/10), [PR 11](https://github.com/RenatoA2508/android-launcher/pull/11) |

## Trabajo de esta rama hacia main

- `e3af5fc` prepara Fedora y el paquete de desarrollo instalable junto a release. El [PR 12](https://github.com/RenatoA2508/android-launcher/pull/12) contiene ese subconjunto. La rama interactiva lo incluye en su historial.
- `84b21fd` y `583b72c` añaden arrastre continuo, pausa, retroceso, resolución por la mitad del recorrido y atajo de swipe rápido. En la base de main el gesto abría Búsqueda al soltar tras superar 72 dp.
- `26a57fd` y `c575ca5` añaden Apariencia, vista previa, colores persistentes, barra de tono con HEX y contraste de paneles.
- Esta rama corrige el total de colección que quedaba congelado al cambiar un aviso y añade una prueba Compose que reproduce el fallo. El diagnóstico físico también confirmó que la autorización de Launcher se debe conceder por separado a Launcher Dev. [Incidencia y prueba de regresión](UX.md#incidencia-de-insignias-en-launcher-dev)

## Pendiente

La incorporación de estas funciones a main depende de la revisión del PR. Como cobertura futura queda repetir la prueba de contadores con una misma app en los perfiles personal y de trabajo y con otros fabricantes. La guía de arquitectura describe la identidad por perfil que esas pruebas deben conservar.
