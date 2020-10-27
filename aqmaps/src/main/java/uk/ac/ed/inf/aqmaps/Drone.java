package uk.ac.ed.inf.aqmaps;

/**
 * The Drone class represents a Drone, with a position and number of 
 * moves as attributes.
 *
 */
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
	
	/**
	 * Calculate the distance to the next sensor from the drone, 
	 * using the Euclidean formula
	 */
	public double calculateDistance(Sensor sensor) {
		double valueX = Math.pow((position.getLatitude() - sensor.getLatitude()), 2);
		double valueY = Math.pow((position.getLongitude() - sensor.getLongitude()), 2);
	    return Math.sqrt(valueX + valueY);
	}

}
