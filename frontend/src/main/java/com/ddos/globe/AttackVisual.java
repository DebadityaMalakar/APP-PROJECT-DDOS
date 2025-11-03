package com.ddos.globe;

import javafx.animation.*;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import javafx.geometry.Point3D;

public class AttackVisual extends Group {
    private Runnable onFinished;
    private static final int NUM_SEGMENTS = 40; // More segments for smoother curve

    public AttackVisual(Point3D sourcePos, Point3D targetPos, Color color, double intensity) {
        // Add a small offset to prevent arrows from going inside the sphere
        double sphereRadius = 200.0; // Adjust this based on your sphere's actual radius
        double offset = 5.0; // Additional offset to keep arrows outside
        
        // Calculate direction and distance
        Point3D sourceDir = sourcePos.normalize();
        Point3D targetDir = targetPos.normalize();
        
        // Offset source and target positions to be slightly outside the sphere
        Point3D adjustedSource = sourceDir.multiply(sphereRadius + offset);
        Point3D adjustedTarget = targetDir.multiply(sphereRadius + offset);
        
        Point3D direction = adjustedTarget.subtract(adjustedSource);
        double distance = direction.magnitude();
        
        if (distance < 10) {
            System.err.println("ERROR: Distance too small");
            return;
        }
        
        // Create curved arrow path segments with adjusted positions
        Group curvedArrow = createCurvedArrow(adjustedSource, adjustedTarget, color, intensity);
        
        // Create markers - half embedded in globe surface
        double markerSize = 4; // Slightly bigger so half is visible
        
        // Source marker - positioned AT surface (half inside, half outside)
        Sphere sourceMarker = new Sphere(markerSize);
        PhongMaterial sourceMat = new PhongMaterial(color.deriveColor(0, 0.7, 1.0, 0.9)); // More subtle
        sourceMat.setSpecularColor(color.brighter());
        sourceMarker.setMaterial(sourceMat);
        // Position slightly above surface
        sourceMarker.setTranslateX(adjustedSource.getX());
        sourceMarker.setTranslateY(adjustedSource.getY());
        sourceMarker.setTranslateZ(adjustedSource.getZ());
        
       Point3D sourceSurfacePos = sourcePos.normalize().multiply(200.0); // Match sphere radius
        sourceMarker.setTranslateX(sourceSurfacePos.getX());
        sourceMarker.setTranslateY(sourceSurfacePos.getY());
        sourceMarker.setTranslateZ(sourceSurfacePos.getZ());

        // Target marker - positioned on surface
        Sphere targetMarker = new Sphere(3); // Same size as source FadeTransition showTarge
        PhongMaterial targetMat = new PhongMaterial(color.deriveColor(0, 0.9, 1.2, 0.9));
        targetMat.setSpecularColor(color.brighter().brighter());
        targetMarker.setMaterial(targetMat);

        // Position on sphere surface
        Point3D targetSurfacePos = targetPos.normalize().multiply(200.0); // Match sphere radius
        targetMarker.setTranslateX(targetSurfacePos.getX());
        targetMarker.setTranslateY(targetSurfacePos.getY());
        targetMarker.setTranslateZ(targetSurfacePos.getZ());
        targetMarker.setOpacity(0); // Start hidden
        
        // Add all to this group
        this.getChildren().addAll(curvedArrow, sourceMarker, targetMarker);
        
        // Start animation
        animateCurvedArrow(curvedArrow, sourceMarker, targetMarker);
    }
    
    private Group createCurvedArrow(Point3D source, Point3D target, Color color, double intensity) {
        Group arrowPath = new Group();
        
        // Calculate the arc height (how far above the globe surface)
        double arcHeight = 60; // Slightly lower arc for smoother appearance
        
        // Use more subtle colors
        Color subtleColor = color.deriveColor(0, 0.7, 1.0, 0.75);
        
        // Create multiple small cylinders to form a curved path
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            double t = (double) i / NUM_SEGMENTS;
            double nextT = (double) (i + 1) / NUM_SEGMENTS;
            
            // Calculate points along the arc
            Point3D p1 = getArcPoint(source, target, t, arcHeight);
            Point3D p2 = getArcPoint(source, target, nextT, arcHeight);
            
            // Create segment Point3D
            Point3D segmentDir = p2.subtract(p1);
            double segmentLength = segmentDir.magnitude();
            
            if (segmentLength < 0.1) continue;
            
            double radius = Math.max(1.2, Math.min(2.5, intensity / 35.0)); // Thinner arrows
            Cylinder segment = new Cylinder(radius, segmentLength);
            
            PhongMaterial mat = new PhongMaterial(subtleColor);
            mat.setSpecularColor(subtleColor.brighter());
            mat.setSpecularPower(5);
            segment.setMaterial(mat);
            
            // Position at midpoint of segment
            Point3D midpoint = p1.midpoint(p2);
            segment.setTranslateX(midpoint.getX());
            segment.setTranslateY(midpoint.getY());
            segment.setTranslateZ(midpoint.getZ());
            
            // Align segment with direction
            alignCylinderWithVector(segment, segmentDir);
            
            // Store segment index for animation
            segment.setUserData(i);
            
            arrowPath.getChildren().add(segment);
        }
        
        // Add arrowhead at the end - smaller and subtler Sphere sourceMarker
        Point3D arrowHeadPos = getArcPoint(source, target, 0.98, arcHeight);
        Point3D arrowHeadDir = target.subtract(arrowHeadPos).normalize();
        
        Cylinder arrowHead = new Cylinder(4, 15); // Smaller arrowhead
        PhongMaterial headMat = new PhongMaterial(subtleColor.brighter());
        headMat.setSpecularColor(Color.WHITE);
        headMat.setSpecularPower(8);
        arrowHead.setMaterial(headMat);
        
        arrowHeadPos = getArcPoint(source, target, 1.0, 0);
        arrowHead.setTranslateX(arrowHeadPos.getX());
        arrowHead.setTranslateY(arrowHeadPos.getY());
        arrowHead.setTranslateZ(arrowHeadPos.getZ());
        
        Point3D finalDir = target.normalize();
        alignCylinderWithVector(arrowHead, finalDir.multiply(20));
        
        arrowHead.setUserData(-1); // Mark as arrowhead
        arrowPath.getChildren().add(arrowHead);
        
        return arrowPath;
    }
    
    /**
     * Calculate a point along an arc between source and target
     * The arc rises above the globe surface with smoother curve
     */
    private Point3D getArcPoint(Point3D source, Point3D target, double t, double arcHeight) {
    // Get normalized directions
    Point3D sourceNorm = source.normalize();
    Point3D targetNorm = target.normalize();
    
    // Calculate angle between source and target
    double angle = Math.acos(Math.max(-1, Math.min(1, sourceNorm.dotProduct(targetNorm))));
    
    // Spherical interpolation
    Point3D interpolated;
    double sinAngle = Math.sin(angle);
    if (sinAngle > 0.001) {
        double a = Math.sin((1 - t) * angle) / sinAngle;
        double b = Math.sin(t * angle) / sinAngle;
        interpolated = sourceNorm.multiply(a).add(targetNorm.multiply(b));
    } else {
        interpolated = sourceNorm.add(targetNorm.subtract(sourceNorm).multiply(t));
    }
    
    // Calculate base point on sphere surface
    double sphereRadius = 200.0; // Match your sphere radius
    Point3D basePoint = interpolated.normalize().multiply(sphereRadius);
    
    // Calculate height offset using a sine curve
    double height = arcHeight * Math.sin(t * Math.PI);
    
    // Add height offset in the direction away from the sphere center
    return basePoint.add(interpolated.normalize().multiply(height));
}
    private void alignCylinderWithVector(Cylinder cylinder, Point3D direction) {
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D normalizedDir = direction.normalize();
        Point3D rotationAxis = yAxis.crossProduct(normalizedDir);
        
        double dotProduct = yAxis.dotProduct(normalizedDir);
        dotProduct = Math.max(-1, Math.min(1, dotProduct));
        double angle = Math.toDegrees(Math.acos(dotProduct));
        
        if (rotationAxis.magnitude() > 0.0001) {
            Rotate rotate = new Rotate(angle, rotationAxis);
            cylinder.getTransforms().add(rotate);
        } else if (dotProduct < 0) {
            Rotate rotate = new Rotate(180, new Point3D(1, 0, 0));
            cylinder.getTransforms().add(rotate);
        }
    }
    
    private void animateCurvedArrow(Group arrowPath, Sphere sourceMarker, Sphere targetMarker) {
        Duration totalDuration = Duration.seconds(3.0); // Slightly longer for smoother animation
        
        // Animate each segment appearing in sequence with improved timing
        Timeline segmentAnimation = new Timeline();
        
        // Pre-calculate timing for smoother progression
        double segmentDuration = 0.1; // Base duration for each segment
        double totalSegmentTime = NUM_SEGMENTS * segmentDuration * 0.5; // 50% overlap for smoother flow
        
        for (int i = 0; i < arrowPath.getChildren().size(); i++) {
            if (arrowPath.getChildren().get(i) instanceof Cylinder) {
                Cylinder segment = (Cylinder) arrowPath.getChildren().get(i);
                Integer index = (Integer) segment.getUserData();
                
                if (index == null || index == -1) continue;
                
                // Smoother timing with easing and overlap
                double delay = (index * totalSegmentTime) / NUM_SEGMENTS;
                double duration = segmentDuration * 0.8; // Slightly faster for better flow
                
                // Fade and scale in with easing
                KeyFrame startFrame = new KeyFrame(
                    Duration.seconds(delay),
                    new KeyValue(segment.scaleYProperty(), 0.01),
                    new KeyValue(segment.opacityProperty(), 0.0)
                );
                
                KeyFrame midFrame = new KeyFrame(
                    Duration.seconds(delay + duration * 0.4),
                    new KeyValue(segment.scaleYProperty(), 1.2, Interpolator.EASE_OUT),
                    new KeyValue(segment.opacityProperty(), 0.9, Interpolator.EASE_BOTH)
                );
                
                KeyFrame endFrame = new KeyFrame(
                    Duration.seconds(delay + duration),
                    new KeyValue(segment.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                    new KeyValue(segment.opacityProperty(), 1.0, Interpolator.EASE_BOTH)
                );
                
                segmentAnimation.getKeyFrames().addAll(startFrame, midFrame, endFrame);
            }
        }
        
        // Smoother arrowhead animation with better timing and effects
        Cylinder arrowHead = (Cylinder) arrowPath.getChildren().get(arrowPath.getChildren().size() - 1);
        
        // Initial state (invisible and scaled down)
        arrowHead.setScaleX(0.01);
        arrowHead.setScaleY(0.01);
        arrowHead.setScaleZ(0.01);
        arrowHead.setOpacity(0);
        
        // Animate arrowhead with more fluid motion
        double arrowStartTime = 1.8;
        double arrowDuration = 0.5;
        
        // Scale up with bounce effect
        KeyFrame arrowHeadStart = new KeyFrame(
            Duration.seconds(arrowStartTime),
            new KeyValue(arrowHead.scaleYProperty(), 0.01, Interpolator.EASE_OUT),
            new KeyValue(arrowHead.scaleXProperty(), 0.01, Interpolator.EASE_OUT),
            new KeyValue(arrowHead.scaleZProperty(), 0.01, Interpolator.EASE_OUT),
            new KeyValue(arrowHead.opacityProperty(), 0.0, Interpolator.LINEAR)
        );
        
        KeyFrame arrowHeadMid = new KeyFrame(
            Duration.seconds(arrowStartTime + arrowDuration * 0.5),
            new KeyValue(arrowHead.scaleYProperty(), 1.4, Interpolator.EASE_OUT),
            new KeyValue(arrowHead.scaleXProperty(), 1.4, Interpolator.EASE_OUT),
            new KeyValue(arrowHead.scaleZProperty(), 1.4, Interpolator.EASE_OUT),
            new KeyValue(arrowHead.opacityProperty(), 1.0, Interpolator.EASE_IN)
        );
        
        KeyFrame arrowHeadEnd = new KeyFrame(
            Duration.seconds(arrowStartTime + arrowDuration * 0.8),
            new KeyValue(arrowHead.scaleYProperty(), 0.9, Interpolator.EASE_IN),
            new KeyValue(arrowHead.scaleXProperty(), 0.9, Interpolator.EASE_IN),
            new KeyValue(arrowHead.scaleZProperty(), 0.9, Interpolator.EASE_IN)
        );
        
        KeyFrame arrowHeadFinal = new KeyFrame(
            Duration.seconds(arrowStartTime + arrowDuration),
            new KeyValue(arrowHead.scaleYProperty(), 1.1, Interpolator.EASE_BOTH),
            new KeyValue(arrowHead.scaleXProperty(), 1.1, Interpolator.EASE_BOTH),
            new KeyValue(arrowHead.scaleZProperty(), 1.1, Interpolator.EASE_BOTH)
        );
        
        segmentAnimation.getKeyFrames().addAll(arrowHeadStart, arrowHeadMid, arrowHeadEnd, arrowHeadFinal);
        
        // Smoother fade out for the arrow
        FadeTransition fadeArrow = new FadeTransition(Duration.seconds(1.0), arrowPath);
        fadeArrow.setFromValue(1.0);
        fadeArrow.setToValue(0.0);
        fadeArrow.setInterpolator(Interpolator.EASE_IN);
        fadeArrow.setDelay(Duration.seconds(2.5)); // Slightly delayed for smoother transition
        
        // Smoother pulse for source marker with better timing
        ScaleTransition pulseSource = new ScaleTransition(Duration.seconds(0.6), sourceMarker);
        pulseSource.setFromX(1.0);
        pulseSource.setFromY(1.0);
        pulseSource.setFromZ(1.0);
        pulseSource.setToX(2.8);
        pulseSource.setToY(2.8);
        pulseSource.setToZ(2.8);
        pulseSource.setInterpolator(Interpolator.EASE_BOTH);
        pulseSource.setCycleCount(2);
        pulseSource.setAutoReverse(true);
        
        // Add subtle fade to the pulse
        FadeTransition fadePulse = new FadeTransition(Duration.seconds(0.6), sourceMarker);
        fadePulse.setFromValue(1.0);
        fadePulse.setToValue(0.7);
        fadePulse.setInterpolator(Interpolator.EASE_BOTH);
        fadePulse.setCycleCount(2);
        fadePulse.setAutoReverse(true);
        
        // Smoother fade for source marker
        FadeTransition fadeSource = new FadeTransition(Duration.seconds(1.2), sourceMarker);
        fadeSource.setFromValue(1.0);
        fadeSource.setToValue(0.0);
        fadeSource.setInterpolator(Interpolator.EASE_IN);
        fadeSource.setDelay(Duration.seconds(1.2)); // Slightly delayed for better timing
        
        // Enhanced target impact with smoother animation
        FadeTransition showTarget = new FadeTransition(Duration.seconds(0.3), targetMarker);
        showTarget.setDelay(Duration.seconds(2.0));
        showTarget.setFromValue(0.0);
        showTarget.setToValue(1.0);
        showTarget.setInterpolator(Interpolator.EASE_OUT);

        // Scale effect for impact
        ScaleTransition impactTarget = new ScaleTransition(Duration.seconds(0.6), targetMarker);
        impactTarget.setDelay(Duration.seconds(2.0));
        impactTarget.setFromX(0.1);
        impactTarget.setFromY(0.1);
        impactTarget.setFromZ(0.1);
        impactTarget.setToX(1.0);
        impactTarget.setToY(1.0);
        impactTarget.setToZ(1.0);
        impactTarget.setInterpolator(Interpolator.EASE_OUT);
        
        // Add a second impact for more dramatic effect
        ScaleTransition impactTarget2 = new ScaleTransition(Duration.seconds(0.4), targetMarker);
        impactTarget2.setDelay(Duration.seconds(2.5));
        impactTarget2.setFromX(3.0);
        impactTarget2.setFromY(3.0);
        impactTarget2.setFromZ(3.0);
        impactTarget2.setToX(2.0);
        impactTarget2.setToY(2.0);
        impactTarget2.setToZ(2.0);
        impactTarget2.setInterpolator(Interpolator.EASE_BOTH);
        
        // Smoother fade for target marker
        FadeTransition fadeTarget = new FadeTransition(Duration.seconds(1.0), targetMarker);
        fadeTarget.setDelay(Duration.seconds(2.7)); // Slightly delayed after impact
        fadeTarget.setFromValue(1.0);
        fadeTarget.setToValue(0.0);
        fadeTarget.setInterpolator(Interpolator.EASE_IN);
        
        ParallelTransition parallel = new ParallelTransition(
            segmentAnimation, 
            fadeArrow, 
            pulseSource, 
            fadePulse, // Added fade pulse
            fadeSource,
            showTarget, 
            impactTarget,
            impactTarget2, // Added second impact
            fadeTarget
        );
        
        parallel.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        
        parallel.play();
    }
    
    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }
    
    public void play() {
        // Animation auto-starts
    }
}