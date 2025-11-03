package com.ddos.globe;
import com.ddos.globe.AttackVisual;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.scene.shape.Circle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private ScheduledExecutorService apiScheduler;
    
    // API Configuration
    private static final String API_URL = "http://localhost:8080/simulate-ddos";
    
    // Statistics
    private int totalAttacks = 0;
    private int activeAttacks = 0;
    private int countriesAffected = 0;
    private double avgIntensity = 0;

    // Material Design Colors
    private final Color[] ATTACK_COLORS = {
        Color.web("#FF5252"), // Red 400 - HTTP
        Color.web("#FF9800"), // Orange 500 - HTTPS
        Color.web("#FFEB3B"), // Yellow 500 - WebSocket
        Color.web("#9C27B0"), // Purple 500 - DNS
        Color.web("#00BCD4")  // Cyan 500 - ICMP
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

    // UI Components for dynamic updates
    private Label totalAttacksLabel;
    private Label activeAttacksLabel;
    private Label countriesLabel;
    private Label intensityLabel;
    private Label countLabel;
    
    // Attack log components
    private TextArea attackLogArea;
    private final int MAX_LOG_ENTRIES = 50;
    private ObservableList<String> attackLog = FXCollections.observableArrayList();

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

        // Create control panel with Material Design
        VBox controlPanel = createControlPanel();
        
        // Wrap control panel in a container with manual positioning
        StackPane controlContainer = new StackPane();
        controlContainer.setStyle("-fx-background-color: " + BACKGROUND_SECONDARY + ";");
        
        // Manual positioning container for the control panel
        Pane positioningPane = new Pane();
        controlPanel.setLayoutX(0);
        controlPanel.setLayoutY(0);
        
        // Bind control panel width to positioning pane width
        controlPanel.prefWidthProperty().bind(positioningPane.widthProperty());
        controlPanel.maxWidthProperty().bind(positioningPane.widthProperty());
        
        positioningPane.getChildren().add(controlPanel);
        controlContainer.getChildren().add(positioningPane);
        
        // Set fixed height for control container
        controlContainer.setPrefHeight(450); // Increased height to accommodate log
        controlContainer.setMaxHeight(450);
        
        root.setBottom(controlContainer);

        // Setup stage with responsive sizing
        Scene uiScene = new Scene(root, PREF_WIDTH, PREF_HEIGHT);
        primaryStage.setTitle("Global DDoS Attack Monitor - Real Time");
        primaryStage.setScene(uiScene);
        primaryStage.setMinWidth(MIN_WIDTH);
        primaryStage.setMinHeight(MIN_HEIGHT);
        
        // Stop scheduler when window closes
        primaryStage.setOnCloseRequest(e -> stopApiScheduler());
        
        primaryStage.show();

        // Start API data fetching
        startApiDataFetching();
    }

    private void startApiDataFetching() {
        apiScheduler = Executors.newScheduledThreadPool(1);
        apiScheduler.scheduleAtFixedRate(this::fetchApiData, 0, 3, TimeUnit.SECONDS);
    }

    private void stopApiScheduler() {
        if (apiScheduler != null && !apiScheduler.isShutdown()) {
            apiScheduler.shutdown();
        }
        if (attackTimer != null) {
            attackTimer.stop();
        }
    }

    private void fetchApiData() {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                conn.disconnect();

                // Parse JSON response manually
                List<Map<String, String>> attacks = parseJsonManually(content.toString());
                
                // Update on JavaFX Application Thread
                Platform.runLater(() -> {
                    processApiData(attacks);
                });
            } else {
                System.err.println("API request failed with code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching API data: " + e.getMessage());
        }
    }

    private List<Map<String, String>> parseJsonManually(String json) {
        List<Map<String, String>> attacks = new ArrayList<>();
        
        // Remove outer brackets and split into individual attack objects
        String cleanJson = json.trim();
        if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
            cleanJson = cleanJson.substring(1, cleanJson.length() - 1);
        }
        
        // Split into individual attack objects
        String[] attackStrings = cleanJson.split("\\},\\s*\\{");
        
        for (String attackStr : attackStrings) {
            // Clean up the string
            attackStr = attackStr.trim();
            if (attackStr.startsWith("{")) {
                attackStr = attackStr.substring(1);
            }
            if (attackStr.endsWith("}")) {
                attackStr = attackStr.substring(0, attackStr.length() - 1);
            }
            
            Map<String, String> attack = new HashMap<>();
            
            // Split into key-value pairs
            String[] pairs = attackStr.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    
                    // Remove any trailing commas from value
                    if (value.endsWith(",")) {
                        value = value.substring(0, value.length() - 1);
                    }
                    
                    attack.put(key, value);
                }
            }
            
            attacks.add(attack);
        }
        
        return attacks;
    }

    private void processApiData(List<Map<String, String>> attacks) {
        // Clear previous attacks
        attackData.clear();
        globeGroup.getChildren().removeIf(node -> node instanceof AttackVisual);
        
        // Reset statistics
        totalAttacks = attacks.size();
        activeAttacks = attacks.size();
        countriesAffected = 0;
        avgIntensity = 0;
        
        // Track unique countries
        Set<String> uniqueCountries = new HashSet<>();

        // Clear previous log entries for new data
        attackLog.clear();

        for (Map<String, String> attack : attacks) {
            String country = attack.getOrDefault("country", "Unknown");
            String protocol = attack.getOrDefault("protocol", "HTTP");
            String ip = attack.getOrDefault("ip", "0.0.0.0");
            String timestamp = attack.getOrDefault("timestamp", "Unknown");
            
            // Add to unique countries
            uniqueCountries.add(country);
            
            // Determine attack type and color based on protocol
            String attackType = getAttackTypeFromProtocol(protocol);
            Color color = getColorFromProtocol(protocol);
            
            // Generate random intensity based on protocol
            int intensity = generateIntensityFromProtocol(protocol);
            avgIntensity += intensity;
            
            // Create attack data - using "Unknown" as source since API doesn't provide it
            AttackData attackDataItem = new AttackData("Unknown", country, attackType, intensity, color);
            attackData.add(attackDataItem);
            
            // Add visual representation
            addAttackVisual(attackDataItem);
            
            // Add to attack log
            addAttackLogEntry(country, protocol, intensity, timestamp);
        }
        
        // Calculate statistics
        countriesAffected = uniqueCountries.size();
        avgIntensity = totalAttacks > 0 ? avgIntensity / totalAttacks : 0;
        
        // Update UI
        updateStatistics();
    }

    private void addAttackLogEntry(String country, String protocol, int intensity, String timestamp) {
        String logEntry = String.format("[%s] %s attack on %s - %d Gbps", 
            timestamp, protocol, country, intensity);
        
        attackLog.add(0, logEntry); // Add to beginning for newest first
        
        // Limit log size
        if (attackLog.size() > MAX_LOG_ENTRIES) {
            attackLog.remove(attackLog.size() - 1);
        }
        
        // Update log display
        updateAttackLog();
    }

    private void updateAttackLog() {
        if (attackLogArea != null) {
            StringBuilder logText = new StringBuilder();
            for (String entry : attackLog) {
                logText.append(entry).append("\n");
            }
            attackLogArea.setText(logText.toString());
        }
    }

    private String getAttackTypeFromProtocol(String protocol) {
        switch (protocol.toUpperCase()) {
            case "HTTP": return "HTTP Flood";
            case "HTTPS": return "HTTPS Flood";
            case "WEBSOCKET": return "WebSocket Flood";
            case "DNS": return "DNS Amplification";
            default: return protocol + " Attack";
        }
    }

    private Color getColorFromProtocol(String protocol) {
        switch (protocol.toUpperCase()) {
            case "HTTP": return ATTACK_COLORS[0]; // Red
            case "HTTPS": return ATTACK_COLORS[1]; // Orange
            case "WEBSOCKET": return ATTACK_COLORS[2]; // Yellow
            case "DNS": return ATTACK_COLORS[3]; // Purple
            default: return ATTACK_COLORS[4]; // Cyan
        }
    }

    private int generateIntensityFromProtocol(String protocol) {
        switch (protocol.toUpperCase()) {
            case "HTTP": return random.nextInt(50) + 30; // 30-80 Gbps
            case "HTTPS": return random.nextInt(40) + 40; // 40-80 Gbps
            case "WEBSOCKET": return random.nextInt(60) + 20; // 20-80 Gbps
            case "DNS": return random.nextInt(70) + 10; // 10-80 Gbps
            default: return random.nextInt(50) + 10; // 10-60 Gbps
        }
    }

    private void updateStatistics() {
        if (totalAttacksLabel != null) {
            totalAttacksLabel.setText(String.valueOf(totalAttacks));
        }
        if (activeAttacksLabel != null) {
            activeAttacksLabel.setText(String.valueOf(activeAttacks));
        }
        if (countriesLabel != null) {
            countriesLabel.setText(String.valueOf(countriesAffected));
        }
        if (intensityLabel != null) {
            intensityLabel.setText(String.format("%.1f Gbps", avgIntensity));
        }
        if (countLabel != null) {
            countLabel.setText("Last " + totalAttacks + " attacks");
        }
    }

    private StackPane createSceneContainer(Group root3D) {
        StackPane container = new StackPane();
        container.setStyle("-fx-background-color: " + BACKGROUND_PRIMARY + ";");
        
        // Create SubScene that fills available space
        SubScene subScene = new SubScene(root3D, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        
        // Bind SubScene size to container with proper aspect ratio (16:9)
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
            mouseOldY = event.getSceneY();
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
        VBox controlPanel = new VBox(15); // Reduced spacing
        controlPanel.setPadding(new Insets(15));
        controlPanel.setStyle("-fx-background-color: " + BACKGROUND_SECONDARY + ";");
        controlPanel.setMaxWidth(Double.MAX_VALUE);

        // Header with icon and title
        HBox headerBox = createHeader();
        
        // Stats panel with cards - now responsive
        HBox statsContainer = new HBox(15);
        statsContainer.setAlignment(Pos.CENTER);
        HBox statsPanel = createStatsPanel();
        statsContainer.getChildren().add(statsPanel);
        
        // Control buttons with proper spacing - CENTERED
        HBox buttonPanel = createButtonPanel();

        // Attack log panel
        VBox attackLogPanel = createAttackLogPanel();

        // Attack list with card design - FULL WIDTH
        VBox attackListPanel = createAttackListPanel();

        controlPanel.getChildren().addAll(headerBox, statsContainer, buttonPanel, attackLogPanel, attackListPanel);
        return controlPanel;
    }

    private HBox createHeader() {
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 5, 0));
        
        // Icon
        Label icon = new Label("🌐");
        icon.setStyle("-fx-font-size: 28px;");
        
        // Title
        VBox titleBox = new VBox(2);
        titleBox.setAlignment(Pos.CENTER);
        Label title = new Label("DDoS ATTACK MONITOR - LIVE");
        title.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Label subtitle = new Label("Real-time Global Threat Visualization from API");
        subtitle.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px;");
        
        titleBox.getChildren().addAll(title, subtitle);
        
        headerBox.getChildren().addAll(icon, titleBox);
        return headerBox;
    }

    private HBox createStatsPanel() {
        HBox statsPanel = new HBox(12);
        statsPanel.setAlignment(Pos.CENTER);
        statsPanel.setPadding(new Insets(0, 0, 5, 0));

        // Create stat cards
        VBox totalAttacksCard = createStatCard("Total Attacks", "0", ATTACK_COLORS[0], "📊");
        VBox activeAttacksCard = createStatCard("Active Attacks", "0", ATTACK_COLORS[1], "⚡");
        VBox countriesCard = createStatCard("Countries Affected", "0", ATTACK_COLORS[4], "🌍");
        VBox intensityCard = createStatCard("Avg Intensity", "0 Gbps", ATTACK_COLORS[2], "📈");

        statsPanel.getChildren().addAll(totalAttacksCard, activeAttacksCard, countriesCard, intensityCard);

        return statsPanel;
    }

    private VBox createStatCard(String title, String value, Color color, String emoji) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + BACKGROUND_CARD + "; -fx-background-radius: 10;");
        card.setPrefWidth(140);
        card.setMinWidth(130);
        card.setMaxWidth(160);
        card.setPrefHeight(70);
        card.setAlignment(Pos.CENTER);
        card.setEffect(new javafx.scene.effect.DropShadow(4, Color.rgb(0, 0, 0, 0.3)));

        // Header with emoji
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER);
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 14px;");
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 12px; -fx-font-weight: 500;");
        
        header.getChildren().addAll(emojiLabel, titleLabel);

        // Value - store reference for dynamic updates
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: " + toHex(color) + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Store references for dynamic updates
        switch (title) {
            case "Total Attacks": totalAttacksLabel = valueLabel; break;
            case "Active Attacks": activeAttacksLabel = valueLabel; break;
            case "Countries Affected": countriesLabel = valueLabel; break;
            case "Avg Intensity": intensityLabel = valueLabel; break;
        }

        card.getChildren().addAll(header, valueLabel);
        return card;
    }

    private HBox createButtonPanel() {
        HBox buttonPanel = new HBox(10);
        buttonPanel.setPadding(new Insets(5, 0, 5, 0));
        buttonPanel.setAlignment(Pos.CENTER);

        Button refreshBtn = createMaterialButton("Refresh", "#4CAF50");
        Button stopBtn = createMaterialButton("Stop", "#F44336");
        Button resetBtn = createMaterialButton("Reset", "#2196F3");
        Button exitBtn = createMaterialButton("Exit", "#F44336");

        refreshBtn.setOnAction(e -> fetchApiData());
        stopBtn.setOnAction(e -> stopApiScheduler());
        resetBtn.setOnAction(e -> resetSimulation());
        exitBtn.setOnAction(e -> {
            stopApiScheduler();
            System.exit(0);
        });

        buttonPanel.getChildren().addAll(refreshBtn, stopBtn, resetBtn, exitBtn);
        return buttonPanel;
    }

    private VBox createAttackLogPanel() {
        VBox logPanel = new VBox(8);
        logPanel.setPadding(new Insets(8));
        logPanel.setStyle("-fx-background-color: " + BACKGROUND_CARD + "; -fx-background-radius: 8;");
        logPanel.setPrefHeight(100);
        logPanel.setMaxHeight(100);
        logPanel.setMaxWidth(Double.MAX_VALUE);

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 5, 0));
        
        Label logTitle = new Label("Attack Log");
        logTitle.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        header.getChildren().add(logTitle);

        // Text area for log
        attackLogArea = new TextArea();
        attackLogArea.setEditable(false);
        attackLogArea.setWrapText(true);
        attackLogArea.setStyle(
            "-fx-control-inner-background: " + BACKGROUND_CARD + ";" +
            "-fx-background-color: transparent;" +
            "-fx-text-fill: " + TEXT_SECONDARY + ";" +
            "-fx-font-size: 11px;" +
            "-fx-font-family: 'Monospaced', 'Courier New', monospace;" +
            "-fx-border-color: " + BACKGROUND_SECONDARY + ";" +
            "-fx-border-radius: 5;" +
            "-fx-padding: 5;"
        );
        
        // Bind size
        attackLogArea.prefHeightProperty().bind(logPanel.heightProperty().subtract(30));
        attackLogArea.maxHeightProperty().bind(logPanel.heightProperty().subtract(30));

        logPanel.getChildren().addAll(header, attackLogArea);
        return logPanel;
    }

    private VBox createAttackListPanel() {
        VBox attackPanel = new VBox(8);
        attackPanel.setPadding(new Insets(12));
        attackPanel.setStyle("-fx-background-color: " + BACKGROUND_CARD + "; -fx-background-radius: 8;");
        attackPanel.setPrefHeight(120);
        attackPanel.setMaxHeight(120);
        attackPanel.setMaxWidth(Double.MAX_VALUE);
        attackPanel.setEffect(new javafx.scene.effect.DropShadow(4, Color.rgb(0, 0, 0, 0.3)));

        // Manual positioning for header
        Pane headerPane = new Pane();
        headerPane.setPrefHeight(25);
        headerPane.setMaxWidth(Double.MAX_VALUE);
        
        Label attackTitle = new Label("Live Attacks");
        attackTitle.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        countLabel = new Label("Last 0 attacks");
        countLabel.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 11px;");
        
        // Manual positioning calculations
        attackTitle.setLayoutX(5);
        attackTitle.setLayoutY(5);
        
        // Position count label on the right side
        countLabel.layoutXProperty().bind(headerPane.widthProperty().subtract(countLabel.widthProperty()).subtract(5));
        countLabel.setLayoutY(6);
        
        headerPane.getChildren().addAll(attackTitle, countLabel);

        // Manual positioning for list view
        Pane listContainer = new Pane();
        listContainer.setPrefHeight(90);
        listContainer.setMaxWidth(Double.MAX_VALUE);
        
        ListView<AttackData> attackListView = new ListView<>(attackData);
        attackListView.setStyle(
            "-fx-control-inner-background: " + BACKGROUND_CARD + ";" +
            "-fx-background-color: transparent;" +
            "-fx-border-color: " + BACKGROUND_SECONDARY + ";" +
            "-fx-border-radius: 6;" +
            "-fx-padding: 5;" +
            "-fx-font-size: 12px;"
        );
        
        // Manual list view sizing and positioning
        attackListView.setLayoutX(0);
        attackListView.setLayoutY(0);
        attackListView.prefWidthProperty().bind(listContainer.widthProperty());
        attackListView.prefHeightProperty().bind(listContainer.heightProperty());
        
        attackListView.setCellFactory(param -> new ListCell<AttackData>() {
            @Override
            protected void updateItem(AttackData item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setGraphic(null);
                } else {
                    HBox cellContent = new HBox(10);
                    cellContent.setAlignment(Pos.CENTER_LEFT);
                    cellContent.setPadding(new Insets(4));
                    
                    // Color indicator
                    Circle indicator = new Circle(3, item.getColor());
                    
                    // Attack info
                    VBox infoBox = new VBox(2);
                    Label mainText = new Label(
                        String.format("%s → %s", item.getSourceCountry(), item.getTargetCountry())
                    );
                    mainText.setStyle("-fx-text-fill: " + TEXT_PRIMARY + "; -fx-font-size: 12px; -fx-font-weight: 500;");
                    
                    Label subText = new Label(
                        String.format("%s • %d Gbps", item.getAttackType(), item.getIntensity())
                    );
                    subText.setStyle("-fx-text-fill: " + TEXT_SECONDARY + "; -fx-font-size: 10px;");
                    
                    infoBox.getChildren().addAll(mainText, subText);
                    
                    cellContent.getChildren().addAll(indicator, infoBox);
                    setGraphic(cellContent);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        listContainer.getChildren().add(attackListView);
        attackPanel.getChildren().addAll(headerPane, listContainer);
        return attackPanel;
    }

    private Button createMaterialButton(String text, String color) {
        Button button = new Button(text);
        button.setPrefHeight(36);
        button.setPrefWidth(100);
        button.setMinWidth(90);
        button.setPadding(new Insets(0, 15, 0, 15));
        button.setStyle(
            "-fx-background-color: " + color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-font-weight: 600;" +
            "-fx-background-radius: 18;" +
            "-fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, " + color + "44, 8, 0, 0, 2);"
        );

        // Hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: " + darkenColor(color) + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;" +
                "-fx-background-radius: 18;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + color + "66, 12, 0, 0, 3);"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;" +
                "-fx-background-radius: 18;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, " + color + "44, 8, 0, 0, 2);"
            );
        });

        return button;
    }

    private void resetSimulation() {
        attackData.clear();
        attackLog.clear();
        globeGroup.getChildren().removeIf(node -> node instanceof AttackVisual);
        totalAttacks = 0;
        activeAttacks = 0;
        countriesAffected = 0;
        avgIntensity = 0;
        updateStatistics();
        updateAttackLog();
    }

    private void addAttackVisual(AttackData attack) {
        Point3D sourceNorm = getCountryCoordinates("Unknown"); // Source is unknown in API data
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
            case "Canada": return new Point3D(-0.7, 0.3, 0.65);
            case "Mexico": return new Point3D(-0.8, 0.1, 0.6);
            case "France": return new Point3D(0.0, 0.6, 0.8);
            case "Italy": return new Point3D(0.1, 0.5, 0.85);
            case "Spain": return new Point3D(-0.1, 0.4, 0.9);
            case "Netherlands": return new Point3D(0.0, 0.6, 0.8);
            case "South Korea": return new Point3D(0.85, 0.2, 0.5);
            case "Indonesia": return new Point3D(0.7, -0.3, 0.65);
            case "Thailand": return new Point3D(0.6, 0.0, 0.8);
            case "Vietnam": return new Point3D(0.65, 0.1, 0.75);
            case "Philippines": return new Point3D(0.75, 0.0, 0.65);
            case "Malaysia": return new Point3D(0.6, -0.1, 0.8);
            case "Singapore": return new Point3D(0.55, -0.1, 0.85);
            case "Egypt": return new Point3D(0.2, 0.3, 0.93);
            case "Nigeria": return new Point3D(0.0, 0.1, 0.99);
            case "Kenya": return new Point3D(0.3, -0.1, 0.95);
            case "Ethiopia": return new Point3D(0.4, 0.0, 0.92);
            case "Ghana": return new Point3D(-0.1, 0.1, 0.99);
            case "Morocco": return new Point3D(-0.2, 0.3, 0.93);
            case "Argentina": return new Point3D(-0.4, -0.6, 0.7);
            case "Chile": return new Point3D(-0.5, -0.7, 0.5);
            case "Colombia": return new Point3D(-0.6, -0.1, 0.8);
            case "Peru": return new Point3D(-0.6, -0.4, 0.7);
            case "Venezuela": return new Point3D(-0.5, -0.1, 0.85);
            case "Panama": return new Point3D(-0.7, -0.1, 0.7);
            case "Cuba": return new Point3D(-0.6, 0.2, 0.75);
            case "Guatemala": return new Point3D(-0.8, 0.0, 0.6);
            case "Honduras": return new Point3D(-0.75, 0.0, 0.65);
            case "New Zealand": return new Point3D(0.9, -0.7, 0.1);
            case "Fiji": return new Point3D(0.95, -0.5, 0.3);
            case "Pakistan": return new Point3D(0.4, 0.2, 0.9);
            case "Bangladesh": return new Point3D(0.5, 0.1, 0.85);
            case "Sri Lanka": return new Point3D(0.45, -0.1, 0.88);
            case "Afghanistan": return new Point3D(0.3, 0.3, 0.9);
            case "Iran": return new Point3D(0.4, 0.3, 0.87);
            case "Iraq": return new Point3D(0.3, 0.3, 0.9);
            case "Saudi Arabia": return new Point3D(0.3, 0.2, 0.93);
            case "Turkey": return new Point3D(0.2, 0.4, 0.89);
            case "Ukraine": return new Point3D(0.2, 0.7, 0.7);
            case "Poland": return new Point3D(0.1, 0.6, 0.8);
            case "Sweden": return new Point3D(0.1, 0.8, 0.6);
            case "Norway": return new Point3D(0.1, 0.9, 0.4);
            case "Finland": return new Point3D(0.2, 0.9, 0.4);
            case "Denmark": return new Point3D(0.0, 0.7, 0.7);
            case "Belgium": return new Point3D(0.0, 0.6, 0.8);
            case "Switzerland": return new Point3D(0.0, 0.6, 0.8);
            case "Austria": return new Point3D(0.1, 0.6, 0.8);
            case "Portugal": return new Point3D(-0.2, 0.4, 0.9);
            case "Greece": return new Point3D(0.1, 0.4, 0.91);
            case "Romania": return new Point3D(0.2, 0.5, 0.84);
            case "Hungary": return new Point3D(0.1, 0.6, 0.8);
            case "Czech Republic": return new Point3D(0.1, 0.6, 0.8);
            case "Ireland": return new Point3D(-0.2, 0.7, 0.7);
            case "Scotland": return new Point3D(-0.1, 0.7, 0.7);
            case "Wales": return new Point3D(-0.1, 0.6, 0.8);
            case "Northern Ireland": return new Point3D(-0.1, 0.7, 0.7);
            case "Unknown": 
            default: 
                // Random position for unknown countries
                return new Point3D(
                    random.nextDouble() * 2 - 1,
                    random.nextDouble() * 2 - 1,
                    random.nextDouble()
                ).normalize();
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