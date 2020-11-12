package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drone class represents a Drone, with a position and number of 
 * moves as attributes.
 *
 */
public class Drone {
	
	private Coordinate currentPosition;
	public int moves;
	public Coordinate startPosition;
	public double moveLength = 0.0003;
	private ArrayList<Coordinate> route = new ArrayList<Coordinate>();
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();

    public Drone(Coordinate startPosition) {
        this.currentPosition = startPosition;
        this.startPosition = startPosition;
        this.moves = 150;
    }

    // Getters and Setters
	public Coordinate getPosition() {
		return this.currentPosition;
	}
	
	public void setPosition(Coordinate currentPosition) {
		this.currentPosition = currentPosition;
	}
	
	public int getMoves() {
		return this.moves;
	}
	
	public void setMoves(int moves) {
		this.moves = moves;
	}
	
	public void setSensors(ArrayList<Sensor> sensors) {
	    this.sensors = sensors;
	}
	
    public ArrayList<Sensor> getSensors() {
        return this.sensors;
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
        double x1 = this.currentPosition.latitude;
        double y1 = this.currentPosition.longitude;
        double x2 = sensor.getCoordinate().latitude;
        double y2 = sensor.getCoordinate().longitude;
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }
    
    /*
     * Moves the drone, updates its position and adds the new coordinates to path
     */
    public void moveDrone(int direction) throws IOException, InterruptedException {
        // Direction is a multiple of ten, telling degrees to travel in
        this.moves -= 1;
        Double initialLatitude = this.currentPosition.latitude;
        Double initialLongitude = this.currentPosition.longitude;
       
        // Move the drone to next position
        this.currentPosition = currentPosition.getNextPosition(direction, moveLength);
        route.add(this.currentPosition);
        
        // Check if this new position is in range of the destination sensor
        if (withinSensorRange(sensors.get(0))) {
           takeReading();
        }
        // Convert radians to degrees
        double radians = convertToRadians(direction);
        
        // Use trigonometry to calculate the longitude and latitude values
        Double xValue = moveLength * Math.cos(radians);
        Double yValue = moveLength * Math.sin(radians);
        
        // Update the drone's position
        Double newLatitude = initialLatitude + yValue;
        Double newLongitude = initialLongitude + xValue;
        currentPosition = new Coordinate(newLongitude, newLatitude);
    }

    private void takeReading() {
        // TODO Auto-generated method stub
        
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
    public boolean isInConfinementZone(Coordinate coordinate) {
        boolean permittedLatitude;
        boolean permittedLongitude;
        permittedLatitude = 55.942617 < coordinate.latitude && coordinate.latitude < 55.946233;
        permittedLongitude = -3.192473 < coordinate.longitude && coordinate.longitude < -3.184319;
        return (permittedLatitude && permittedLongitude);
    }

    public void visitSensors() {
        while (this.moves > 0) {
            
        }   
    }
    
    
	
	// TODO methods:
	// - get distance to sensor
	// - check if within range of sensor
	// - take reading
	
}
