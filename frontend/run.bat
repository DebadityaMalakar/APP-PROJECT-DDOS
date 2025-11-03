@echo off
REM Run DDoS Globe JavaFX Application with SQLite JDBC

REM Set path to your JavaFX SDK lib folder
set JAVAFX_LIB=".\javafx-sdk-20.0.2\lib"

REM Set path to SQLite JDBC JAR
set SQLITE_JAR=".\lib\sqlite-jdbc-3.50.3.0.jar"

REM Clean previous class files
del /s /q target\classes\com\ddos\globe\*.class

REM Compile Java files with JavaFX modules
javac -cp %SQLITE_JAR% -d target\classes --add-modules javafx.controls,javafx.fxml src/main/java/com/ddos/globe/*.java

REM Run the app with JavaFX and SQLite JDBC in classpath
java --module-path %JAVAFX_LIB% --add-modules javafx.controls,javafx.fxml -cp %SQLITE_JAR%;target\classes com.ddos.globe.Main

@REM pause