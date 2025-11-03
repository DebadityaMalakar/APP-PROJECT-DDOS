package com.ddos.globe;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class LoginPage extends Application {
    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    private TextField signupUsernameField;
    private PasswordField signupPasswordField;
    private PasswordField signupConfirmPasswordField;
    private Label messageLabel;
    private Stage primaryStage;
    private Random random = new Random();
    
    private static final String BACKEND_URL = "http://localhost:8080/auth";
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("DDoS Globe - Login");
        
        // Set fullscreen
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint(""); // Remove exit hint
        
        StackPane root = new StackPane();
        
        // Animated background
        Pane backgroundPane = createAnimatedBackground();
        
        // Main content
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(40));
        
        VBox loginCard = createLoginCard();
        contentBox.getChildren().add(loginCard);
        
        root.getChildren().addAll(backgroundPane, contentBox);
        
        // Get screen dimensions for responsive background
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Entrance animation
        loginCard.setTranslateY(50);
        loginCard.setOpacity(0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), loginCard);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        TranslateTransition slideUp = new TranslateTransition(Duration.seconds(0.8), loginCard);
        slideUp.setFromY(50);
        slideUp.setToY(0);
        
        ParallelTransition entrance = new ParallelTransition(fadeIn, slideUp);
        entrance.setDelay(Duration.millis(200));
        entrance.play();
    }
    
    private Pane createAnimatedBackground() {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: #0f0c29;");
        
        // Bind to scene size for responsive background
        pane.widthProperty().addListener((obs, oldVal, newVal) -> updateBackground(pane));
        pane.heightProperty().addListener((obs, oldVal, newVal) -> updateBackground(pane));
        
        return pane;
    }
    
    private void updateBackground(Pane pane) {
        pane.getChildren().clear();
        
        double width = pane.getWidth() > 0 ? pane.getWidth() : 1920;
        double height = pane.getHeight() > 0 ? pane.getHeight() : 1080;
        
        // Gradient background
        Rectangle bg = new Rectangle(width, height);
        LinearGradient gradient = new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0f0c29")),
            new Stop(0.5, Color.web("#302b63")),
            new Stop(1, Color.web("#24243e"))
        );
        bg.setFill(gradient);
        pane.getChildren().add(bg);
        
        // Animated circles - scale count with screen size
        int circleCount = (int) (width * height / 200000);
        for (int i = 0; i < circleCount; i++) {
            Circle circle = new Circle();
            circle.setRadius(random.nextInt(80) + 40);
            circle.setFill(Color.rgb(255, 69, 58, 0.03 + random.nextDouble() * 0.05));
            circle.setCenterX(random.nextDouble() * width);
            circle.setCenterY(random.nextDouble() * height);
            
            GaussianBlur blur = new GaussianBlur(20);
            circle.setEffect(blur);
            
            // Floating animation
            TranslateTransition tt = new TranslateTransition(
                Duration.seconds(8 + random.nextInt(8)), circle
            );
            tt.setByX(random.nextInt(200) - 100);
            tt.setByY(random.nextInt(200) - 100);
            tt.setAutoReverse(true);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.play();
            
            // Pulse animation
            ScaleTransition st = new ScaleTransition(
                Duration.seconds(4 + random.nextInt(4)), circle
            );
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.3);
            st.setToY(1.3);
            st.setAutoReverse(true);
            st.setCycleCount(Animation.INDEFINITE);
            st.play();
            
            pane.getChildren().add(circle);
        }
    }
    
    private VBox createLoginCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40, 60, 40, 60));
        card.setMaxWidth(450);
        
        // Glassmorphism effect
        card.setStyle(
            "-fx-background-color: rgba(40, 40, 60, 0.7);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(255, 255, 255, 0.1);" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.4), 30, 0, 0, 10);"
        );
        
        // Icon/Logo area
        Circle iconCircle = new Circle(35);
        iconCircle.setFill(Color.rgb(255, 69, 58, 0.2));
        iconCircle.setStroke(Color.rgb(255, 69, 58, 0.6));
        iconCircle.setStrokeWidth(2);
        
        Label iconLabel = new Label("🛡");
        iconLabel.setFont(Font.font(40));
        StackPane iconStack = new StackPane(iconCircle, iconLabel);
        
        // Pulse animation for icon
        ScaleTransition iconPulse = new ScaleTransition(Duration.seconds(2), iconStack);
        iconPulse.setFromX(1.0);
        iconPulse.setFromY(1.0);
        iconPulse.setToX(1.1);
        iconPulse.setToY(1.1);
        iconPulse.setAutoReverse(true);
        iconPulse.setCycleCount(Animation.INDEFINITE);
        iconPulse.play();
        
        // Title with gradient effect
        Label titleLabel = new Label("DDoS ATTACK MONITOR");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        titleLabel.setStyle(
            "-fx-text-fill: linear-gradient(to right, #ff6b6b, #ff453a);" +
            "-fx-effect: dropshadow(gaussian, rgba(255, 69, 58, 0.8), 15, 0, 0, 0);"
        );
        
        Label subtitleLabel = new Label("Global Threat Visualization System");
        subtitleLabel.setFont(Font.font("System", FontWeight.LIGHT, 13));
        subtitleLabel.setTextFill(Color.rgb(200, 200, 220, 0.8));
        
        // TabPane for Login/Signup
        TabPane tabPane = createTabPane();
        
        // Message label
        messageLabel = new Label(" ");
        messageLabel.setFont(Font.font("System", FontWeight.NORMAL, 12));
        messageLabel.setTextFill(Color.web("#ff453a"));
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);
        messageLabel.setMinHeight(20);
        
        // Footer
        HBox footerBox = new HBox(5);
        footerBox.setAlignment(Pos.CENTER);
        
        Label infoLabel = new Label("Backend: ");
        infoLabel.setFont(Font.font("System", 11));
        infoLabel.setTextFill(Color.rgb(150, 150, 170, 0.6));
        
        Label credLabel = new Label("localhost:8000");
        credLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        credLabel.setTextFill(Color.rgb(255, 69, 58, 0.7));
        
        footerBox.getChildren().addAll(infoLabel, credLabel);
        
        // Add all components
        card.getChildren().addAll(
            iconStack,
            createSpacer(5),
            titleLabel,
            subtitleLabel,
            createSpacer(10),
            tabPane,
            messageLabel,
            createSpacer(5),
            footerBox
        );
        
        return card;
    }
    
    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Simple styling without complex lookups
        tabPane.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-border-color: transparent;" +
            "-fx-padding: 0;" +
            "-fx-tab-min-width: 150px;" +
            "-fx-tab-min-height: 40px;"
        );
        
        // Login Tab
        Tab loginTab = new Tab("LOGIN");
        loginTab.setContent(createLoginForm());
        loginTab.setClosable(false);
        loginTab.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Signup Tab
        Tab signupTab = new Tab("SIGN UP");
        signupTab.setContent(createSignupForm());
        signupTab.setClosable(false);
        signupTab.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold;");
        
        tabPane.getTabs().addAll(loginTab, signupTab);
        
        // Simple selection styling
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                newTab.setStyle("-fx-background-color: rgba(255, 69, 58, 0.3); -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
            }
            if (oldTab != null) {
                oldTab.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        });
        
        // Set initial selection style
        if (!tabPane.getTabs().isEmpty()) {
            tabPane.getSelectionModel().getSelectedItem().setStyle("-fx-background-color: rgba(255, 69, 58, 0.3); -fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
        }
        
        return tabPane;
    }
    
    private VBox createLoginForm() {
        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20, 0, 0, 0));
        
        // Input fields with icons
        HBox usernameBox = createModernFieldBox("👤", loginUsernameField = createModernTextField("Username"));
        HBox passwordBox = createModernFieldBox("🔒", loginPasswordField = createModernPasswordField("Password"));
        
        // Login button
        Button loginButton = createModernButton("LOGIN", true);
        loginButton.setOnAction(e -> handleLogin());
        
        loginPasswordField.setOnAction(e -> handleLogin());
        
        form.getChildren().addAll(usernameBox, passwordBox, loginButton);
        return form;
    }
    
    private VBox createSignupForm() {
        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(20, 0, 0, 0));
        
        // Input fields with icons
        HBox usernameBox = createModernFieldBox("👤", signupUsernameField = createModernTextField("Username"));
        HBox passwordBox = createModernFieldBox("🔒", signupPasswordField = createModernPasswordField("Password"));
        HBox confirmPasswordBox = createModernFieldBox("🔒", signupConfirmPasswordField = createModernPasswordField("Confirm Password"));
        
        // Signup button
        Button signupButton = createModernButton("CREATE ACCOUNT", true);
        signupButton.setOnAction(e -> handleSignup());
        
        form.getChildren().addAll(usernameBox, passwordBox, confirmPasswordBox, signupButton);
        return form;
    }
    
    private HBox createModernFieldBox(String icon, Control field) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(350);
        box.setPrefHeight(55);
        box.setPadding(new Insets(0, 15, 0, 15));
        box.setStyle(
            "-fx-background-color: rgba(50, 50, 70, 0.4);" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: rgba(255, 255, 255, 0.1);" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;"
        );
        
        Label iconLabel = new Label(icon);
        iconLabel.setFont(Font.font(20));
        iconLabel.setTextFill(Color.rgb(255, 69, 58, 0.7));
        
        field.setPrefWidth(280);
        
        box.getChildren().addAll(iconLabel, field);
        
        // Glow on focus
        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                box.setStyle(
                    "-fx-background-color: rgba(60, 60, 80, 0.5);" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: rgba(255, 69, 58, 0.6);" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 12;" +
                    "-fx-effect: dropshadow(gaussian, rgba(255, 69, 58, 0.4), 10, 0, 0, 0);"
                );
            } else {
                box.setStyle(
                    "-fx-background-color: rgba(50, 50, 70, 0.4);" +
                    "-fx-background-radius: 12;" +
                    "-fx-border-color: rgba(255, 255, 255, 0.1);" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 12;"
                );
            }
        });
        
        return box;
    }
    
    private TextField createModernTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(200, 200, 220, 0.5);" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: normal;" +
            "-fx-border-width: 0;" +
            "-fx-background-insets: 0;" +
            "-fx-padding: 0;"
        );
        return field;
    }
    
    private PasswordField createModernPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(200, 200, 220, 0.5);" +
            "-fx-font-size: 15px;" +
            "-fx-font-weight: normal;" +
            "-fx-border-width: 0;" +
            "-fx-background-insets: 0;" +
            "-fx-padding: 0;"
        );
        return field;
    }
    
    private Button createModernButton(String text, boolean isPrimary) {
        Button button = new Button(text);
        button.setPrefWidth(350);
        button.setPrefHeight(52);
        button.setFont(Font.font("System", FontWeight.BOLD, 14));
        button.setTextFill(Color.WHITE);
        
        if (isPrimary) {
            button.setStyle(
                "-fx-background-color: linear-gradient(to right, #ff6b6b, #ff453a);" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(255, 69, 58, 0.4), 10, 0, 0, 4);"
            );
        } else {
            button.setStyle(
                "-fx-background-color: rgba(60, 60, 80, 0.5);" +
                "-fx-background-radius: 12;" +
                "-fx-cursor: hand;" +
                "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 12;"
            );
        }
        
        button.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
            
            if (isPrimary) {
                button.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ff7b7b, #ff554a);" +
                    "-fx-background-radius: 12;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(255, 69, 58, 0.6), 15, 0, 0, 5);"
                );
            } else {
                button.setStyle(
                    "-fx-background-color: rgba(70, 70, 90, 0.6);" +
                    "-fx-background-radius: 12;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: rgba(255, 255, 255, 0.25);" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 12;"
                );
            }
        });
        
        button.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            
            if (isPrimary) {
                button.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ff6b6b, #ff453a);" +
                    "-fx-background-radius: 12;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(255, 69, 58, 0.4), 10, 0, 0, 4);"
                );
            } else {
                button.setStyle(
                    "-fx-background-color: rgba(60, 60, 80, 0.5);" +
                    "-fx-background-radius: 12;" +
                    "-fx-cursor: hand;" +
                    "-fx-border-color: rgba(255, 255, 255, 0.15);" +
                    "-fx-border-width: 1.5;" +
                    "-fx-border-radius: 12;"
                );
            }
        });
        
        return button;
    }
    
    private Region createSpacer(double height) {
        Region spacer = new Region();
        spacer.setPrefHeight(height);
        return spacer;
    }
    
    private void handleLogin() {
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Please enter both username and password", "#ff453a");
            shakeAnimation(loginUsernameField.getParent());
            return;
        }
        
        // Show loading state
        showMessage("Connecting to server...", "#ffaa00");
        
        // Call backend API
        new Thread(() -> {
            try {
                String response = callBackendAPI(BACKEND_URL + "/login", username, password);
                
                // Use javafx.application.Platform.runLater directly
                javafx.application.Platform.runLater(() -> {
                    if (!response.contains("Invalid credentials")) {
                        showMessage("Login successful! Launching...", "#4ade80");
                        
                        loginUsernameField.setDisable(true);
                        loginPasswordField.setDisable(true);
                        
                        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.2), e -> {
                            launchMainApplication();
                        }));
                        timeline.play();
                    } else {
                        showMessage("Invalid username or password", "#ff453a");
                        loginPasswordField.clear();
                        shakeAnimation(loginUsernameField.getParent());
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showMessage("Server connection failed: " + e.getMessage(), "#ff453a");
                    shakeAnimation(loginUsernameField.getParent());
                });
            }
        }).start();
    }
    
    private void handleSignup() {
        String username = signupUsernameField.getText().trim();
        String password = signupPasswordField.getText();
        String confirmPassword = signupConfirmPasswordField.getText();
        
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Please fill all fields", "#ff453a");
            shakeAnimation(signupUsernameField.getParent());
            return;
        }
        
        if (username.length() < 3) {
            showMessage("Username must be at least 3 characters", "#ff453a");
            shakeAnimation(signupUsernameField.getParent());
            return;
        }
        
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters", "#ff453a");
            shakeAnimation(signupPasswordField.getParent());
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showMessage("Passwords do not match", "#ff453a");
            shakeAnimation(signupConfirmPasswordField.getParent());
            signupConfirmPasswordField.clear();
            return;
        }
        
        // Show loading state
        showMessage("Creating account...", "#ffaa00");
        
        // Call backend API
        new Thread(() -> {
            try {
                String response = callBackendAPI(BACKEND_URL + "/signup", username, password);
                
                javafx.application.Platform.runLater(() -> {
                    if (!response.contains("already exists")) {
                        showMessage("Registration successful! Please login", "#4ade80");
                        signupUsernameField.clear();
                        signupPasswordField.clear();
                        signupConfirmPasswordField.clear();
                    } else {
                        showMessage("Username already exists", "#ff453a");
                        shakeAnimation(signupUsernameField.getParent());
                    }
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    showMessage("Server connection failed: " + e.getMessage(), "#ff453a");
                    shakeAnimation(signupUsernameField.getParent());
                });
            }
        }).start();
    }
    
    private String callBackendAPI(String urlString, String username, String password) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            String postData = "username=" + username + "&password=" + password;
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream()).useDelimiter("\\A")) {
                    return s.hasNext() ? s.next() : "";
                }
            } else {
                return "Error: " + responseCode;
            }
            
        } catch (Exception e) {
            throw new RuntimeException("API call failed: " + e.getMessage());
        }
    }
    
    private void shakeAnimation(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }
    
    private void showMessage(String message, String color) {
        messageLabel.setText(message);
        messageLabel.setTextFill(Color.web(color));
        
        FadeTransition ft = new FadeTransition(Duration.millis(300), messageLabel);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        
        ScaleTransition st = new ScaleTransition(Duration.millis(300), messageLabel);
        st.setFromX(0.8);
        st.setFromY(0.8);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
    
    private void launchMainApplication() {
        try {
            Stage globeStage = new Stage();
            DDosGlobe globe = new DDosGlobe();
            globe.start(globeStage);
            
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), primaryStage.getScene().getRoot());
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> primaryStage.close());
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Error launching application: " + e.getMessage(), "#ff453a");
            loginUsernameField.setDisable(false);
            loginPasswordField.setDisable(false);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}