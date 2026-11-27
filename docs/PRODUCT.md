# Producto

Android Launcher sustituye la app de Inicio del teléfono. Prioriza un orden estable elegido por el usuario, iconos reconocibles y una búsqueda local de las aplicaciones instaladas. El catálogo se obtiene de las actividades que Android permite abrir en los perfiles accesibles.

## Funciones integradas en main

| Superficie | Comportamiento |
| --- | --- |
| Inicio | Reloj, fecha y fondo de pantalla del dispositivo. Hasta 14 favoritas y 6 colecciones, en dos columnas. Los elementos conservan su orden al reiniciar. |
| Favoritas | El editor permite buscar apps, seleccionarlas y revisar la vista previa de Inicio. Pulsar **Listo** guarda la selección. Una pulsación larga seguida de movimiento permite reordenarlas desde Inicio. |
| Colecciones | Cada colección tiene un nombre, una posición y una lista ordenada de apps. Un toque abre la hoja con sus integrantes. Desde **Editar** se cambia su contenido. Una app puede estar en Favoritas y en una colección. |
| Búsqueda | Un swipe hacia arriba abre el campo con teclado. La consulta busca por nombre, palabras, abreviaturas y alias conocidos. Con la consulta vacía se ofrece acceso a **Todas las aplicaciones**. |
| Historial | **Recientes** refleja aperturas desde el launcher. **Buscadas recientemente** refleja aplicaciones abiertas desde resultados con consulta. Cada sección muestra hasta cuatro apps y se resuelve por componente y perfil. Abrir una app por una vía distinta puede cambiarla de sección. |
| Catálogo | **Todas las aplicaciones** organiza los iconos por inicial en una cuadrícula de cuatro columnas con índice lateral. El catálogo visual se reutiliza en los selectores de favoritas y colecciones. |
| Notificaciones | Cada aplicación muestra el total de avisos activos aptos para insignia, por paquete y perfil. La tarjeta de una colección usa la suma de sus miembros. Esta rama corrige la actualización de esa suma mientras la tarjeta está visible. |

El historial procede de acciones del usuario. La ordenación manual de Inicio y las secciones de historial tienen funciones distintas. El launcher guarda referencias a componentes de Android y abre la app en el perfil seleccionado. Al confirmar Android una desinstalación, limpia las referencias de ese paquete y perfil.

## Funciones de esta rama

- **Launcher Dev** puede instalarse junto a Launcher. Sus favoritas, colecciones, historial, preferencias de color y autorización de notificaciones pertenecen a su paquete.
- **Búsqueda interactiva** sigue el desplazamiento del dedo durante un arrastre deliberado. Un swipe corto y rápido abre Búsqueda directamente.
- **Apariencia → Colores** ofrece la paleta dinámica del sistema y un modo personalizado. El acento, el color compartido de Búsqueda y Colecciones y su opacidad tienen vista previa sobre el fondo real. Los iconos conservan los colores de sus aplicaciones.

La [guía de uso y pruebas](UX.md) detalla los gestos y el acceso a notificaciones. El [roadmap](ROADMAP.md) identifica los PR de cada etapa.
