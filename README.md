# Tube Joint Designer (Java Swing)

An interactive desktop tool to design and visualize joints between rectangular or square tubes.  
Built with pure Java Swing — no external graphics engine required.

## Features
- Add multiple tubes dynamically
- Select and move tubes freely on canvas
- Rotate tubes using:
  - Angle presets: 0°, 30°, 45°, 90°, 135°
  - Free rotation mode (Shift + Drag)
- Persistent tube selection
- Undo support with full state rollback
- Real-time geometric rendering using Java2D

## Tech Stack
- Java 8+  
- Swing (UI)
- Java2D / AffineTransforms (geometry & rendering)

## How It Works
1. Click **Add Tube** to create a new tube centered on screen  
2. Click a tube to select it (highlighted in **cyan**)  
3. Drag freely to move  
4. Hold **SHIFT** + drag to rotate (only if angle = Free)  
5. Use the dropdown to snap to predefined angles  
6. Press **Undo** to revert last change

## Controls
| Action | How |
|--------|-----|
| Add Tube | Button |
| Select Tube | Click on tube |
| Move Tube | Drag |
| Rotate Tube | Shift + Drag *(Free mode only)* |
| Snap Angle | Select from dropdown |
| Undo | Button |

## Code Structure
```

Main.java           → Application entry, UI setup
Tube.java           → Tube model + geometry
DrawingCanvas.java  → Render + selection + input handling + undo logic

````

## Run the Project (Source)
```bash
javac Main.java
java Main
````

## Download JAR

Drive link:
**[Application](https://drive.google.com/file/d/1y5JRLsJSlO4X6MoV6k753qkofq5bOzor/view?usp=sharing)**

## JAR Usage

```bash
java -jar TubeJointDesigner.jar
```

## Release Notes (v2)

* Angle snapping added
* Persistent tube selection
* Shift-rotate only allowed in Free mode
* Undo improved (full deep-copy state stack)
* Cleaner interactive mouse handling

## License

MIT License. Use freely, modify, or extend.

## Author

Vasan S

```
```
