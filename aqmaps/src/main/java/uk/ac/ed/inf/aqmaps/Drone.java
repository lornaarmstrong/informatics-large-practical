package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;

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
	public ArrayList<Point> route = new ArrayList<Point>();
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
    
    public void visitSensors() throws IOException, InterruptedException {
      while (this.moves > 0) {
          int direction = getDirection();
          moveDrone(direction);
          moves = 0;
      }
  }
    
    /*
     * Moves the drone, updates its position and adds the new coordinates to path
     */
    public void moveDrone(int direction) throws IOException, InterruptedException {
        this.moves -= 1;
        
        // CHECKING
        System.out.println("Number of moves remaining: " + moves);
        System.out.println("From A: " + currentPosition.toString());

        Double initialLatitude = this.currentPosition.latitude;
        Double initialLongitude = this.currentPosition.longitude;
       
        // Move the drone to next position
        this.currentPosition = currentPosition.getNextPosition(direction, moveLength);
        Point nextPoint = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude);
        route.add(nextPoint);
        
        // Check if this new position is in range of the destination sensor
        if (withinSensorRange(sensors.get(0))) {
           takeReading();
        }
        // Convert radians to degrees
        double radians = Math.toRadians(direction);
        
        // Use trigonometry to calculate the longitude and latitude values
        Double xValue = moveLength * Math.cos(radians);
        Double yValue = moveLength * Math.sin(radians);
        
        // Update the drone's position
        Double newLatitude = initialLatitude + yValue;
        Double newLongitude = initialLongitude + xValue;
        currentPosition = new Coordinate(newLongitude, newLatitude);
       
        System.out.println("To B: " + currentPosition.toString());
    }

    private void takeReading() {
        // TODO Auto-generated method stub
        
    }
    
    private int getDirection() throws IOException, InterruptedException {
        // Gets the first sensor in the list left to visit (destination sensor)
        Sensor destination = sensors.get(0);
        // calculate the angle of the line needed to get to the sensor
        double yDistance = destination.getCoordinate().latitude - currentPosition.latitude;
        double xDistance = destination.getCoordinate().longitude - currentPosition.longitude;
        double angleRadians = Math.atan(yDistance / xDistance);
        double angleDegrees = Math.toDegrees(angleRadians);
        int angleRounded = (int) Math.round((angleDegrees / 10.0) * 10);
        return angleRounded;
    }

    /*
     * Adds the starting point to the route
     */
    public void startRoute() {
        Point startPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
        route.add(startPoint); 
    }
   
	// TODO methods:
	// - get distance to sensor
	// - check if within range of sensor
	// - take reading
	
}
