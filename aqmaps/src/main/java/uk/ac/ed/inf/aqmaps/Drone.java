package uk.ac.ed.inf.aqmaps;

public class Drone {
	
	private Coordinate position;
	private int moves = 150;
	
	// Constructor for Drone
	public Drone(Coordinate position) {
		this.position = position;
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
	// -

}
