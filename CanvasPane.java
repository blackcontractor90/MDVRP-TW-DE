import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ${user}blackcontractor@farid
 */
public class CanvasPane extends Pane {
    private final Canvas canvas;
    private final GraphicsContext gc;

    private boolean addDepotMode = false;
    private boolean addCustomerMode = false;

    private List<Depot> depots = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private List<Route> solutionRoutes = new ArrayList<>();

    private boolean showDepotLabels = true;
    private boolean showCustomerLabels = true;
    private boolean showTimeWindows = true;
    private boolean showRouteLabels = true;

    private final double offsetX = 20;
    private final double offsetY = 20;
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    public CanvasPane(double width, double height) {
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        setOnMouseClicked(this::handleMouseClick);

        canvas.widthProperty().addListener((obs, oldVal, newVal) -> draw());
        canvas.heightProperty().addListener((obs, oldVal, newVal) -> draw());
    }

    public void draw() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        computeScaling();
        drawDepots();
        drawCustomers();
        drawRoutes();
    }

    public void drawWithGenerationOverlay(List<Route> routes, int generation) {
        this.solutionRoutes = routes;
        draw();
        gc.setGlobalAlpha(0.1);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setGlobalAlpha(1.0);
    }

    private void computeScaling() {
        double maxX = 0, maxY = 0;
        for (Depot d : depots) {
            maxX = Math.max(maxX, d.x);
            maxY = Math.max(maxY, d.y);
        }
        for (Customer c : customers) {
            maxX = Math.max(maxX, c.x);
            maxY = Math.max(maxY, c.y);
        }
        if (maxX > 0 && maxY > 0) {
            scaleX = (canvas.getWidth() - 2 * offsetX) / maxX;
            scaleY = (canvas.getHeight() - 2 * offsetY) / maxY;
        } else {
            scaleX = scaleY = 1.0;
        }
    }

    private void drawDepots() {
        for (Depot depot : depots) {
            double x = depot.x * scaleX + offsetX;
            double y = depot.y * scaleY + offsetY;
            gc.setFill(Color.BLACK);
            gc.fillRect(x - 4, y - 4, 8, 8);
            if (showDepotLabels) {
                gc.setFill(Color.BLACK);
                gc.fillText(depot.name, x + 10, y - 10);
            }
        }
    }

    private void drawCustomers() {
        for (Customer customer : customers) {
            double x = customer.x * scaleX + offsetX;
            double y = customer.y * scaleY + offsetY;
            gc.setFill(Color.BLUE);
            gc.fillOval(x - 5, y - 5, 10, 10);
            if (showCustomerLabels) {
                gc.setFill(Color.BLACK);
                gc.fillText(customer.name, x + 10, y - 10);
            }
            if (showTimeWindows) {
                gc.setFill(Color.DARKGRAY);
                gc.fillText(String.format("[%.1f - %.1f]", customer.readyTime, customer.dueTime), x + 10, y + 10);
            }
        }
    }

    private void drawRoutes() {
        int colorIndex = 0;
        for (Route route : solutionRoutes) {
            if (route.customers.isEmpty()) continue;
            Color color = route.color instanceof Color ? (Color) route.color : Color.GRAY;
            gc.setStroke(color);
            gc.setLineWidth(2);

            double lastX = route.depot.x * scaleX + offsetX;
            double lastY = route.depot.y * scaleY + offsetY;
            for (Customer customer : route.customers) {
                double x = customer.x * scaleX + offsetX;
                double y = customer.y * scaleY + offsetY;
                gc.strokeLine(lastX, lastY, x, y);
                lastX = x;
                lastY = y;
            }
            gc.strokeLine(lastX, lastY, route.depot.x * scaleX + offsetX, route.depot.y * scaleY + offsetY);

            if (showRouteLabels && !route.customers.isEmpty()) {
                Customer first = route.customers.get(0);
                double labelX = first.x * scaleX + offsetX;
                double labelY = first.y * scaleY + offsetY;
                gc.setFill(Color.BLACK);
                gc.fillText("R" + (colorIndex + 1), labelX, labelY - 15);
            }
            colorIndex++;
        }
    }

    private void handleMouseClick(MouseEvent e) {
        double rawX = (e.getX() - offsetX) / scaleX;
        double rawY = (e.getY() - offsetY) / scaleY;

        if (addDepotMode) {
            Depot depot = new Depot(rawX, rawY, "D" + (depots.size() + 1));
            depots.add(depot);
            draw();
        } else if (addCustomerMode) {
            Customer customer = new Customer(rawX, rawY, "C" + (customers.size() + 1), 10, 0, 100, 10);
            customers.add(customer);
            draw();
        }
    }

    public void setAddDepotMode() {
        addDepotMode = true;
        addCustomerMode = false;
    }

    public void setAddCustomerMode() {
        addDepotMode = false;
        addCustomerMode = true;
    }

    public void setSolutionRoutes(List<Route> routes) {
        this.solutionRoutes = routes;
    }

    public void setDepots(List<Depot> depots) {
        this.depots = depots;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }

    public void setShowDepotLabels(boolean value) { this.showDepotLabels = value; }
    public void setShowCustomerLabels(boolean value) { this.showCustomerLabels = value; }
    public void setShowTimeWindows(boolean value) { this.showTimeWindows = value; }
    public void setShowRouteLabels(boolean value) { this.showRouteLabels = value; }

    public void saveSnapshot(String label) {
        Platform.runLater(() -> {
            double w = canvas.getWidth();
            double h = canvas.getHeight();
            if (w <= 0 || h <= 0) return;
            WritableImage image = new WritableImage((int) w, (int) h);
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            canvas.snapshot(snapshotResult -> {
                File dir = new File("output");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, String.format("snapshot_%s.png", label));
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(snapshotResult.getImage(), null), "png", file);
                } catch (IOException e) {
                    System.err.println("⚠ Failed to save PNG: " + e.getMessage());
                }
                return null;
            }, params, image);
        });
    }
}