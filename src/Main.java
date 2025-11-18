import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Optional;

/**
 * A Java Swing application for interactively designing tube joints.
 * Version 2:
 * - Adds a JComboBox to set specific angles (0, 30, 45, 90, 135).
 * - A tube now has a persistent "selected" state.
 * - Shift-dragging only works when JComboBox is set to "Free".
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tube Joint Designer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 800);

            // --- Control Panel Setup ---
            JPanel controlPanel = new JPanel();

            // 1. Angle Dropdown
            controlPanel.add(new JLabel("Angle:"));
            String[] angleOptions = {"Free", "0°", "30°", "45°", "90°", "135°"};
            JComboBox<String> angleComboBox = new JComboBox<>(angleOptions);

            // 2. Buttons
            JButton addTubeButton = new JButton("Add Tube");
            JButton undoButton = new JButton("Undo");

            controlPanel.add(angleComboBox);
            controlPanel.add(addTubeButton);
            controlPanel.add(undoButton);

            // --- Canvas ---
            // Pass the angleComboBox to the canvas so it can read its state
            DrawingCanvas canvas = new DrawingCanvas(angleComboBox);

            // --- Add functional logic ---
            addTubeButton.addActionListener(e -> {
                canvas.addTube(new Tube(
                        canvas.getWidth() / 2,
                        canvas.getHeight() / 2,
                        200, 50, 5
                ));
            });

            undoButton.addActionListener(e -> canvas.undo());

            // Add listener to the ComboBox
            angleComboBox.addActionListener(e -> {
                Tube selectedTube = canvas.getSelectedTube();
                // Check if a tube is actually selected
                if (selectedTube != null) {
                    String selectedAngle = (String) angleComboBox.getSelectedItem();

                    if (!"Free".equals(selectedAngle)) {
                        try {
                            // Save state *before* making the change
                            canvas.saveState();

                            // Parse the angle (e.g., "45°" -> 45.0)
                            double angleDeg = Double.parseDouble(
                                    selectedAngle.replace("°", "")
                            );

                            // Set the tube's angle
                            selectedTube.rotationAngle = Math.toRadians(angleDeg);

                            // Repaint to show the change
                            canvas.repaint();
                        } catch (NumberFormatException nfe) {
                            // This might happen if "Free" is selected, just ignore
                        }
                    }
                }
            });

            // --- Layout ---
            frame.setLayout(new BorderLayout());
            frame.add(canvas, BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

/**
 * The Model class for a single tube. (No changes from before)
 */
class Tube {
    double x, y; // Center position
    double length, width, thickness;
    double rotationAngle; // In radians

    public Tube(double x, double y, double length, double width, double thickness) {
        this.x = x;
        this.y = y;
        this.length = length;
        this.width = width;
        this.thickness = thickness;
        this.rotationAngle = 0;
    }

    public Tube(Tube other) {
        this.x = other.x;
        this.y = other.y;
        this.length = other.length;
        this.width = other.width;
        this.thickness = other.thickness;
        this.rotationAngle = other.rotationAngle;
    }

    public Shape getBaseShape() {
        Area outer = new Area(new Rectangle2D.Double(-length / 2, -width / 2, length, width));
        double innerLength = length - (thickness * 2);
        double innerWidth = width - (thickness * 2);

        if (innerLength > 0 && innerWidth > 0) {
            Area inner = new Area(new Rectangle2D.Double(
                    -innerLength / 2, -innerWidth / 2, innerLength, innerWidth));
            outer.subtract(inner);
        }
        return outer;
    }

    public Shape getTransformedShape() {
        AffineTransform tx = new AffineTransform();
        tx.translate(x, y);
        tx.rotate(rotationAngle);
        return tx.createTransformedShape(getBaseShape());
    }

    public boolean contains(Point p) {
        return getTransformedShape().contains(p);
    }
}

/**
 * The View/Controller class.
 * Now manages a persistent selection and reads from the angle JComboBox.
 */
class DrawingCanvas extends JPanel {

    private List<Tube> tubes = new ArrayList<>();
    private Deque<List<Tube>> undoStack = new ArrayDeque<>();

    // The JComboBox from the main UI
    private JComboBox<String> angleComboBox;

    // State for mouse interactions
    private Tube selectedTube = null; // Now a persistent selection
    private Point dragStartPoint = null;
    private Tube originalTubeState = null; // For undo state

    public DrawingCanvas(JComboBox<String> angleComboBox) {
        this.setBackground(Color.WHITE);
        this.angleComboBox = angleComboBox; // Store the reference
        saveState(); // Save the initial empty state

        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                // Check if we clicked on an existing tube
                Tube clickedTube = null;
                for (int i = tubes.size() - 1; i >= 0; i--) {
                    if (tubes.get(i).contains(e.getPoint())) {
                        clickedTube = tubes.get(i);
                        break;
                    }
                }

                // Set the new selected tube
                selectedTube = clickedTube;

                if (selectedTube != null) {
                    // Clicked on a tube, prepare for dragging
                    dragStartPoint = e.getPoint();
                    saveState(); // Save state *before* starting a drag
                    originalTubeState = new Tube(selectedTube);

                    // Update the combo box to match the selected tube's angle
                    // (This is advanced, we'll skip for now to avoid feedback loops)

                } else {
                    // Clicked on empty space, clear drag state
                    originalTubeState = null;
                }
                repaint(); // Show new selection
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedTube != null && originalTubeState != null) {

                    String angleMode = (String) angleComboBox.getSelectedItem();

                    // Check if SHIFT is held down for rotation
                    if (e.isShiftDown()) {
                        // --- ROTATION LOGIC ---
                        // Only allow free-rotate if ComboBox is set to "Free"
                        if ("Free".equals(angleMode)) {
                            double angle = Math.atan2(
                                    e.getY() - originalTubeState.y,
                                    e.getX() - originalTubeState.x
                            );
                            double startAngle = Math.atan2(
                                    dragStartPoint.y - originalTubeState.y,
                                    dragStartPoint.x - originalTubeState.x
                            );
                            selectedTube.rotationAngle = originalTubeState.rotationAngle + (angle - startAngle);
                        }
                        // If angleMode is not "Free", do nothing (rotation is locked)

                    } else {
                        // --- TRANSLATION (DRAG) LOGIC ---
                        double dx = e.getX() - dragStartPoint.x;
                        double dy = e.getY() - dragStartPoint.y;
                        selectedTube.x = originalTubeState.x + dx;
                        selectedTube.y = originalTubeState.y + dy;
                    }

                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Clear the *drag* state, but not the *selection*
                dragStartPoint = null;
                originalTubeState = null;
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    /**
     * Adds a new tube, selects it, and saves the state.
     */
    public void addTube(Tube tube) {
        saveState(); // Save *before* the change
        tubes.add(tube);
        selectedTube = tube; // Select the new tube
        repaint();
    }

    /**
     * Reverts to the previous state.
     */
    public void undo() {
        if (undoStack.size() > 1) {
            undoStack.pop();
            this.tubes = new ArrayList<>();
            for (Tube t : undoStack.peek()) {
                this.tubes.add(new Tube(t));
            }
            // After undo, we lose track of the selected tube, so deselect
            selectedTube = null;
            repaint();
        }
    }

    /**
     * Saves a deep copy of the current state to the undo stack.
     * Made public to be accessible by the ComboBox listener.
     */
    public void saveState() {
        List<Tube> currentState = new ArrayList<>();
        for (Tube t : tubes) {
            currentState.add(new Tube(t));
        }
        undoStack.push(currentState);
    }

    /**
     * Returns the currently selected tube.
     * Made public to be accessible by the ComboBox listener.
     */
    public Tube getSelectedTube() {
        return this.selectedTube;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw all the tubes
        for (Tube tube : tubes) {
            Shape tubeShape = tube.getTransformedShape();

            // Highlight the selected tube
            if (tube == selectedTube) {
                g2d.setColor(Color.CYAN); // Selection color
                g2d.fill(tubeShape);
            } else {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fill(tubeShape);
            }

            g2d.setColor(Color.BLACK);
            g2d.draw(tubeShape);
        }
    }
}