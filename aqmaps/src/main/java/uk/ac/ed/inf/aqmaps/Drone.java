package uk.ac.ed.inf.aqmaps;

/**
 * The Drone class represents a Drone, with a position and number of 
 * moves as attributes.
 *
 */
public class Drone {
	
	private Coordinate position;
	private int moves;
	
	// Constructor for Drone
	public Drone(Coordinate position) {
		this.position = position;
		this.moves = 150;
	}
	
	// Getters and Setters
	public Coordinate getPosition() {
		return this.position;
	}
	
	public void setPosition(Coordinate position) {
		this.position = position;
	}
	
	public int getMoves() {
		return this.moves;
	}
	
	public void setMoves(int moves) {
		this.moves = moves;
	}
	
	// TODO methods:
	// - get distance to sensor
	// - check if within range of sensor
	// - take reading
	
}
