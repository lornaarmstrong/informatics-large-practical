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
     * Moves the drone, updates its position and adds coordinate to path
     */
    public void moveDrone(int direction) {
        // Direction is a multiple of ten, telling degrees to travel in
        this.moves -= 1;
        Double initialLatitude = this.position.latitude();
        Double initialLongitude = this.position.longitude();
        
        //convert radians to degrees
        double radians = convertToRadians(direction);
        
        // Use trig to calculate the longitude and latitude values
        Double xValue = 0.0003 * Math.cos(radians);
        Double yValue = 0.0003 * Math.sin(radians);
        
        Double newLatitude = initialLatitude + yValue;
        Double newLongitude = initialLongitude + xValue;
        position = Point.fromLngLat(newLongitude, newLatitude);
    }

    public double convertToRadians(int direction) {
        return (direction * Math.PI / 180);
    }
    
    
	
	// TODO methods:
	// - get distance to sensor
	// - check if within range of sensor
	// - take reading
	
}
