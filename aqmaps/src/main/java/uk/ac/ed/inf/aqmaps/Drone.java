package uk.ac.ed.inf.aqmaps;

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
	 * Check if drone is within the range of the sensor
	 */
    public boolean withinSensorRange(Sensor sensor) {
        // Drone is within range of a sensor if within 0.0002 of sensor
        var withinRange = false;
        // if drone point to sensor point
        return withinRange;
    }
	
	// TODO methods:
	// - get distance to sensor
	// - check if within range of sensor
	// - take reading
	
}
