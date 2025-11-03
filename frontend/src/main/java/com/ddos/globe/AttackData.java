package com.ddos.globe;

import javafx.scene.paint.Color;

public class AttackData {
    private final String sourceCountry;
    private final String targetCountry;
    private final String attackType;
    private final int intensity; // in Gbps
    private final Color color;

    public AttackData(String sourceCountry, String targetCountry, String attackType, int intensity, Color color) {
        this.sourceCountry = sourceCountry;
        this.targetCountry = targetCountry;
        this.attackType = attackType;
        this.intensity = intensity;
        this.color = color;
    }

    // Getters
    public String getSourceCountry() { return sourceCountry; }
    public String getTargetCountry() { return targetCountry; }
    public String getAttackType() { return attackType; }
    public int getIntensity() { return intensity; }
    public Color getColor() { return color; }

    @Override
    public String toString() {
        return String.format("%s → %s: %s (%d Gbps)", sourceCountry, targetCountry, attackType, intensity);
    }
}