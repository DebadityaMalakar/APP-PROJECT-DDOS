package com.ddos.globe;
import com.ddos.globe.AttackVisual;
import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.shape.Circle;

import java.util.Random;

public class DDosGlobe extends Application {

    private static final double GLOBE_RADIUS = 200;
    private static final int MIN_WIDTH = 1280;
    private static final int MIN_HEIGHT = 720;
    private static final int PREF_WIDTH = 1600;
    private static final int PREF_HEIGHT = 900;

    private Sphere globe;
    private Group globeGroup;
    private PerspectiveCamera camera;
    private Rotate rotateX, rotateY;
    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;

    private ObservableList<AttackData> attackData = FXCollections.observableArrayList();
    private Random random = new Random();
    private AnimationTimer attackTimer;

    // Material Design Colors
    private final Color[] ATTACK_COLORS = {
        Color.web("#FF5252"), // Red 400
        Color.web("#FF9800"), // Orange 500
        Color.web("#FFEB3B"), // Yellow 500
        Color.web("#9C27B0"), // Purple 500
        Color.web("#00BCD4")  // Cyan 500
    };

    // Material Design Color Palette
    private final String BACKGROUND_PRIMARY = "#121212";
    private final String BACKGROUND_SECONDARY = "#1E1E1E";
    private final String BACKGROUND_CARD = "#2D2D2D";
    private final String TEXT_PRIMARY = "#E0E0E0";
    private final String TEXT_SECONDARY = "#A0A0A0";
    private final String ACCENT_PRIMARY = "#BB86FC";
    private final String ACCENT_SECONDARY = "#03DAC6";
    private final String ERROR_COLOR = "#CF6679";

    @Override
    public void start(Stage primaryStage) {
        // Create main layout with Material Design background
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BACKGROUND_PRIMARY + ";");

        // Create 3D root group
        Group root3D = create3DScene();

        // Put SubScene into the UI
        StackPane sceneContainer = createSceneContainer(root3D);
        root.setCenter(sceneContainer);

        // Create control panel with Material Design - NOW WITH PROPER SCROLLING
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: " + BACKGROUND_SECONDARY + "; -fx-background-color: " + BACKGROUND_SECONDARY + ";");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPrefViewportHeight(300);
        scrollPane.setMaxHeight(350);
        
        VBox controlPanel = createControlPanel();
        scrollPane.setContent(controlPanel);
        
        root.setBottom(scrollPane);

        // Setup stage with responsive sizing
        Scene uiScene = new Scene(root, PREF_WIDTH, PREF_HEIGHT);
        primaryStage.setTitle("Global DDoS Attack Monitor");
        primaryStage.setScene(uiScene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        primaryStage.show();

        // Start attack simulation
        startAttackSimulation();
    }

    private StackPane createSceneContainer(Group root3D) {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: " + BACKGROUND_PRIMARY + ";");
        
        // Create SubScene that fills available space
        SubScene subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        
        // Bind SubScene size to container
        subScene.widthProperty().bind(container.widthProperty().multiply(0.95));
        subScene.heightProperty().bind(container.heightProperty().multiply(0.95));

        // Attach camera to SubScene
        subScene.setCamera(camera);

        // Setup mouse behavior
        setupMouseControls(subScene);

        container.getChildren().add(subScene);
        StackPane.setAlignment(subScene, Pos.CENTER);
        
        return container;
    }

    private Group create3DScene() {
        globe = new Sphere(GLOBE_RADIUS);

        PhongMaterial earthMaterial = new PhongMaterial();
        
        // Load earth texture
        Image earthImage = new Image("file:src/main/resources/textures/earth.jpg");
        if (earthImage.isError()) {
            System.err.println("Failed to load earth texture: " + earthImage.getException());
            earthMaterial.setDiffuseColor(Color.web("#2196F3")); // Material Blue
        } else {
            earthMaterial.setDiffuseMap(earthImage);
            System.out.println("Earth texture loaded successfully!");
        }
        
        globe.setMaterial(earthMaterial);

        // Globe group for rotation and centering
        globeGroup = new Group(globe);
        rotateX = new Rotate(0, Rotate.X_AXIS);
        rotateY = new Rotate(0, Rotate.Y_AXIS);
        
        // Center the globe
        globeGroup.setTranslateX(0);
        globeGroup.setTranslateY(0);
        globeGroup.getTransforms().addAll(rotateX, rotateY);

        // Camera setup
        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-650);
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);

        // Enhanced lighting
        Group root3D = new Group(globeGroup);
        
        // Ambient light for general illumination
        AmbientLight ambientLight = new AmbientLight(Color.WHITE);
        ambientLight.setOpacity(0.4);
        
        // Key light for highlights
        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateZ(-800);
        keyLight.setTranslateX(400);
        keyLight.setTranslateY(-300);
        
        // Fill light for softer shadows
        PointLight fillLight = new PointLight(Color.web("#E3F2FD")); // Light blue fill
        fillLight.setTranslateZ(-700);
        fillLight.setTranslateX(-500);
        fillLight.setTranslateY(400);
        fillLight.setOpacity(0.3);

        root3D.getChildren().addAll(ambientLight, keyLight, fillLight);

        return root3D;
    }

    private void setupMouseControls(SubScene subScene) {
        subScene.setOnMousePressed(event -> {
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();

            rotateY.setAngle(rotateY.getAngle() + (mousePosX - mouseOldX) * 0.5 * -1);
            rotateX.setAngle(rotateX.getAngle() - (mousePosY - mouseOldY) * 0.5 * -1);

            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
        });

        subScene.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double newZ = camera.getTranslateZ() + delta * 2;
            if (newZ > -1200 && newZ < -400) {
                camera.setTranslateZ(newZ);
            }
        });
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(20);
        controlPanel.setPadding(new Insets(20));
        controlPanel.setStyle("-fx-background-color: " + BACKGROUND_SECONDARY + ";");

        // Header with icon and title
        HBox headerBox = createHeader();
        
        // Stats panel with cards - now properly using FlowPane for wrapping
        FlowPane statsPanel = createStatsPanel();
        
        // Control buttons with proper spacing - CENTERED
        HBox buttonPanel = createButtonPanel();

        // Attack list with card design - FULL WIDTH
        VBox attackListPanel = createAttackListPanel();

        controlPanel.getChildren().addAll(headerBox, statsPanel, buttonPanel, attackListPanel);
        return controlPanel;
    }

    private HBox createHeader() {
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        // Icon
        Label icon = new Label("🌐");
        icon.setStyle("-fx-font-size: 32px;");
        
        // Title
        VBox titleBox = new VBox(4);
        titleBox.setAlignment(Pos.CENTER);
        Label title = new Label("DDoS ATTACK MONITOR");
        title.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 22px; -fx-font-weight: bold;");
        
        Label subtitle = new Label("Real-time Global Threat Visualization");
        subtitle.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px;");
        
        titleBox.getChildren().addAll(title, subtitle);
        
        headerBox.getChildren().addAll(icon, titleBox);
        return headerBox;
    }

    private FlowPane createStatsPanel() {
        FlowPane statsPanel = new FlowPane();
        statsPanel.setAlignment(Pos.CENTER);
        statsPanel.setHgap(15);
        statsPanel.setVgap(15);
        statsPanel.setPadding(new Insets(0, 0, 10, 0));

        // Create stat cards
        VBox totalAttacksCard = createStatCard("Total Attacks", "0", ATTACK_COLORS[0], "📊");
        VBox activeAttacksCard = createStatCard("Active Attacks", "0", ATTACK_COLORS[1], "⚡");
        VBox countriesCard = createStatCard("Countries Affected", "0", ATTACK_COLORS[4], "🌍");
        VBox intensityCard = createStatCard("Avg Intensity", "0 Gbps", ATTACK_COLORS[2], "📈");

        statsPanel.getChildren().addAll(totalAttacksCard, activeAttacksCard, countriesCard, intensityCard);

        return statsPanel;
    }

    private VBox createStatCard(String title, String value, Color color, String emoji) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: " + BACKGROUND_CARD + "; -fx-background-radius: 12;");
        card.setPrefWidth(180);
        card.setMinWidth(160);
        card.setMaxWidth(220);
        card.setPrefHeight(90);
        card.setAlignment(Pos.CENTER);
        card.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.3)));

        // Header with emoji
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER);
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 16px;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 13px; -fx-font-weight: 500;");
        
        header.getChildren().addAll(emojiLabel, titleLabel);

        // Value
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + toHex(color) + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        card.getChildren().addAll(header, valueLabel);
        return card;
    }

    private HBox createButtonPanel() {
        HBox buttonPanel = new HBox(15);
        buttonPanel.setPadding(new Insets(10, 0, 10, 0));
        buttonPanel.setAlignment(Pos.CENTER);

        Button startBtn = createMaterialButton("Start", "#4CAF50");
        Button stopBtn = createMaterialButton("Stop", "#F44336");
        Button resetBtn = createMaterialButton("Reset", "#2196F3");
        Button exitBtn = createMaterialButton("Exit", "#F44336");

        startBtn.setOnAction(e -> startAttackSimulation());
        stopBtn.setOnAction(e -> stopAttackSimulation());
        resetBtn.setOnAction(e -> resetSimulation());
        exitBtn.setOnAction(e -> System.exit(0));

        buttonPanel.getChildren().addAll(startBtn, stopBtn, resetBtn, exitBtn);
        return buttonPanel;
    }

    private Button createMaterialButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefHeight(42);
        button.setPrefWidth(140);
        button.setMinWidth(120);
        button.setPadding(new Insets(0, 20, 0, 20));
        button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-radius: 21;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + color + "44, 10, 0, 0, 2);"
        );

        // Hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: " + darkenColor(color) + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 600;" +
                "-fx-background-radius: 21;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + color + "66, 15, 0, 0, 3);"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: 600;" +
                "-fx-background-radius: 21;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + color + "44, 10, 0, 0, 2);"
            );
        });

        return button;
    }

    private VBox createAttackListPanel() {
        VBox attackPanel = new VBox(12);
        attackPanel.setPadding(new Insets(18));
        attackPanel.setStyle("-fx-background-color: " + BACKGROUND_CARD + "; -fx-background-radius: 12;");
        attackPanel.setPrefHeight(200);
        attackPanel.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.3)));

        // Header with proper layout
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        Label attackTitle = new Label("Recent Attacks");
        attackTitle.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label countLabel = new Label("Last 30 attacks");
        countLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");
        
        headerBox.getChildren().addAll(attackTitle, spacer, countLabel);

        // List view with proper sizing
        ListView<AttackData> attackListView = new ListView<>(attackData);
        attackListView.setPrefHeight(140);
        attackListView.setStyle(
            "-fx-control-inner-background: " + BACKGROUND_CARD + ";" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: " + BACKGROUND_SECONDARY + ";" +
            "-fx-border-radius: 8;" +
            "-fx-padding: 8;" +
            "-fx-font-size: 13px;"
        );
        VBox.setVgrow(attackListView, Priority.ALWAYS);
        
        attackListView.setCellFactory(param -> new ListCell<AttackData>() {
            @Override
            protected void updateItem(AttackData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setGraphic(null);
                } else {
                    HBox cellContent = new HBox(12);
                    cellContent.setAlignment(Pos.CENTER_LEFT);
                    cellContent.setPadding(new Insets(8));
                    
                    // Color indicator
                    Circle indicator = new Circle(4, item.getColor());
                    
                    // Attack info
                    VBox infoBox = new VBox(3);
                    Label mainText = new Label(
                        String.format("%s → %s", item.getSourceCountry(), item.getTargetCountry())
                    );
                    mainText.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 13px; -fx-font-weight: 500;");
                    
                    Label subText = new Label(
                        String.format("%s • %d Gbps", item.getAttackType(), item.getIntensity())
                    );
                    subText.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11px;");
                    
                    infoBox.getChildren().addAll(mainText, subText);
                    
                    cellContent.getChildren().addAll(indicator, infoBox);
                    setGraphic(cellContent);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        attackPanel.getChildren().addAll(headerBox, attackListView);
        return attackPanel;
    }

    private void startAttackSimulation() {
        if (attackTimer != null) {
            attackTimer.stop();
        }

        attackTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 1_000_000_000) { // 1 second
                    simulateAttack();
                    lastUpdate = now;
                }
            }
        };
        attackTimer.start();
    }

    private void stopAttackSimulation() {
        if (attackTimer != null) {
            attackTimer.stop();
        }
    }

    private void resetSimulation() {
        stopAttackSimulation();
        attackData.clear();
        globeGroup.getChildren().removeIf(node -> node instanceof AttackVisual);
    }

    private void simulateAttack() {
        String[] countries = {"USA", "China", "Russia", "Germany", "UK", "Japan", "Brazil", "India", "Australia", "South Africa"};
        String[] attackTypes = {"HTTP Flood", "DNS Amplification", "SYN Flood", "UDP Flood", "ICMP Flood"};

        String source = countries[random.nextInt(countries.length)];
        String target = countries[random.nextInt(countries.length)];
        while (target.equals(source)) {
            target = countries[random.nextInt(countries.length)];
        }

        String type = attackTypes[random.nextInt(attackTypes.length)];
        int intensity = random.nextInt(100) + 1;
        Color color = ATTACK_COLORS[random.nextInt(ATTACK_COLORS.length)];

        AttackData attack = new AttackData(source, target, type, intensity, color);
        attackData.add(0, attack);

        // Keep only recent attacks
        if (attackData.size() > 30) {
            attackData.remove(attackData.size() - 1);
        }

        // Add visual representation
        addAttackVisual(attack);
    }

    private void addAttackVisual(AttackData attack) {
        Point3D sourceNorm = getCountryCoordinates(attack.getSourceCountry());
        Point3D targetNorm = getCountryCoordinates(attack.getTargetCountry());

        Point3D sourcePos = new Point3D(sourceNorm.getX() * GLOBE_RADIUS, sourceNorm.getY() * GLOBE_RADIUS, sourceNorm.getZ() * GLOBE_RADIUS);
        Point3D targetPos = new Point3D(targetNorm.getX() * GLOBE_RADIUS, targetNorm.getY() * GLOBE_RADIUS, targetNorm.getZ() * GLOBE_RADIUS);

        AttackVisual attackVisual = new AttackVisual(sourcePos, targetPos, attack.getColor(), attack.getIntensity());
        
        globeGroup.getChildren().add(attackVisual);

        attackVisual.setOnFinished(() -> {
            globeGroup.getChildren().remove(attackVisual);
        });
    }

    private Point3D getCountryCoordinates(String country) {
        switch (country) {
            case "USA": return new Point3D(-0.5, 0.1, 0.85);
            case "China": return new Point3D(0.8, 0.2, 0.5);
            case "Russia": return new Point3D(0.3, 0.7, 0.6);
            case "Germany": return new Point3D(0.1, 0.6, 0.7);
            case "UK": return new Point3D(-0.1, 0.6, 0.78);
            case "Japan": return new Point3D(0.9, 0.3, 0.4);
            case "Brazil": return new Point3D(-0.3, -0.6, 0.7);
            case "India": return new Point3D(0.6, 0.1, 0.8);
            case "Australia": return new Point3D(0.8, -0.7, 0.2);
            case "South Africa": return new Point3D(0.2, -0.5, 0.85);
            default: return new Point3D(0, 0, 1);
        }
    }

    // Utility methods for Material Design
    private String toHex(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }

    private String darkenColor(String hex) {
        // Simple darkening for hover effects
        return hex; // In production, implement proper color manipulation
    }

    public static void main(String[] args) {
        launch(args);
    }
}