package uk.ac.ed.inf.aqmaps;

import java.io.IOException;

import com.mapbox.geojson.Point;

/**
 * The Drone class represents a Drone, with a position and number of 
 * moves as attributes.
 *
 */
public class Drone {
	
	private Point position;
	private int moves;
	
	// Constructor for Drone
	public Drone(Point position) {
		this.position = position;
		this.moves = 150;
	}
	
	// Getters and Setters
	public Point getPosition() {
		return this.position;
	}
	
	public void setPosition(Point position) {
		this.position = position;
	}
	
	public int getMoves() {
		return this.moves;
	}
	
	public void setMoves(int moves) {
		this.moves = moves;
	}

	/*
	 * Check if drone is within the range of the sensor (<0.0002 degrees)
	 */
    public boolean withinSensorRange(Sensor sensor) throws IOException, InterruptedException {
        return (calculateDistance(sensor) < 0.0002);
    }
    
    /*
     *  Calculate distance between drone and sensor
     */
    public double calculateDistance(Sensor sensor) throws IOException, InterruptedException { 
        double x1 = this.position.latitude();
        double y1 = this.position.longitude();
        double x2 = sensor.getPoint().latitude();
        double y2 = sensor.getPoint().longitude();
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }
    
    /*
     * Moves the drone, updates its position and adds the new coordinates to path
     */
    public void moveDrone(int direction) {
        // Direction is a multiple of ten, telling degrees to travel in
        this.moves -= 1;
        Double initialLatitude = this.position.latitude();
        Double initialLongitude = this.position.longitude();
        
        // Convert radians to degrees
        double radians = convertToRadians(direction);
        
        // Use trigonometry to calculate the longitude and latitude values
        Double xValue = 0.0003 * Math.cos(radians);
        Double yValue = 0.0003 * Math.sin(radians);
        
        // Update the drone's position
        Double newLatitude = initialLatitude + yValue;
        Double newLongitude = initialLongitude + xValue;
        position = Point.fromLngLat(newLongitude, newLatitude);
    }

    /*
     * Converts from degrees to radians
     */
    public double convertToRadians(int direction) {
        return (direction * Math.PI / 180);
    }
    
    /*
     * Check if a given point is in the confinement zone
     */
    public boolean isInConfinementZone(Point point) {
        boolean permittedLatitude;
        boolean permittedLongitude;
        permittedLatitude = 55.942617 < point.longitude() && point.latitude() < 55.946233;
        permittedLongitude = -3.192473 < point.longitude() && point.longitude() < -3.184319;
        return (permittedLatitude && permittedLongitude);
    }
    
    
	
	// TODO methods:
	// - get distance to sensor
	// - check if within range of sensor
	// - take reading
	
}
