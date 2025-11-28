# Simulador de Bolas - MVC

Simulador de física de bolas en Java con arquitectura MVC (Modelo-Vista-Controlador).

##  Controles

### Controles de Panel
- **Añadir Bola**: Crea una nueva bola con configuración actual
- **Auto ON/OFF**: Genera bolas automáticamente según el intervalo configurado
- **Intervalo ms**: Tiempo entre generaciones automáticas (10-10000 ms)
- **Tamaño**: Elige entre tamaño aleatorio o rango fijo (Min/Max)
- **Pausa ON/OFF**: Pausa/reanuda la simulación
- **Borrar bolas**: Elimina todas las bolas del visor

### Controles de Teclado (con bola seleccionada)

#### Selección
- **Click izquierdo**: Selecciona una bola (se visualiza como triángulo)
- **Escape**: Deselecciona la bola actual
- **Flecha Izquierda**: Selecciona la bola anterior
- **Flecha Derecha**: Selecciona la bola siguiente

#### Control de Movimiento
- **A**: Rotar a la izquierda (mantener presionado para rotación continua)
- **D**: Rotar a la derecha (mantener presionado para rotación continua)
- **W**: Acelerar (+10% velocidad)
- **S**: Decelerar (-10% velocidad)

##  Características

### Física
- **Velocidades**: Las bolas tienen velocidad aleatoria inicial (±150 px/s)
- **Aceleración**: Cada bola puede tener aceleración configurable en X e Y
- **Integración**: Física basada en ticks de 10ms con integración vel→pos
- **Rebotes**: Las bolas rebotan en los bordes del visor
- **Habitación especial**: Área central exclusiva donde solo puede entrar una bola a la vez

### Visualización
- **Bolas normales**: Se muestran como círculos de colores aleatorios
- **Bola seleccionada**: Se visualiza como triángulo apuntando en la dirección del movimiento
- **FPS**: Muestra los frames por segundo y tiempo de renderizado
- **Rotación suave**: Rotación continua a 180°/s sin lag

### Concurrencia
- Cada bola corre en su propio thread daemon
- Sincronización segura con locks para evitar condiciones de carrera
- Sistema de generación automática mediante ScheduledExecutorService

##  Arquitectura

```
src/
├── Animation.java          # Punto de entrada
├── controller/
│   ├── Controller.java     # Controlador principal
│   └── BallController.java # Controlador de parámetros físicos
├── model/
│   ├── Model.java          # Modelo de datos
│   ├── Ball.java           # Entidad bola con física
│   └── Habitacion.java     # Área de exclusión mutua
└── view/
    ├── View.java           # Ventana principal
    ├── ControlPanel.java   # Panel de controles
    └── Viewer.java         # Canvas de visualización
```

##  Compilación y Ejecución

### Compilar
```bash
javac -d bin -sourcepath src src/Animation.java
```

### Ejecutar
```bash
java -cp bin Animation
```

### Requisitos
- Java JDK 21 o superior
- Sistema operativo: Windows/Linux/macOS

##  Notas Técnicas

- **Unidades internas**: Velocidad en px/ms, aceleración en px/ms²
- **Tick de física**: 10 milisegundos
- **Framerate objetivo**: ~60 FPS
- **Thread model**: Un thread por bola + thread de viewer + EDT de Swing

##  Personalización

El código está diseñado para ser fácilmente modificable:
- Cambia los rangos de velocidad inicial en `Controller.addBallWithControlSettings()`
- Ajusta la tasa de rotación en `Viewer.ROTATION_RAD_PER_SEC`
- Modifica el factor de aceleración/deceleración en los key bindings de W/S
- Personaliza las dimensiones del triángulo de selección en `Viewer.paintBall()`
