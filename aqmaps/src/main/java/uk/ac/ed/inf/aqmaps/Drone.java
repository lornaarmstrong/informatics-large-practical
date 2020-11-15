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
	boolean returningToStart;
	public double moveLength = 0.0003;
	public ArrayList<Point> route = new ArrayList<Point>();
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	
	public Drone(Coordinate startPosition) {
	    this.currentPosition = startPosition;
	    this.startPosition = startPosition;
	    this.returningToStart = false;
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
	 * Move the drone to create a route visiting all sensors (if possible)
	 */
	public void visitSensors() throws IOException, InterruptedException {
	    var keepGoing = true;
	    while (keepGoing) {
	        var direction = getDirection();
	        moveDrone(direction);
	        
	        // Check the 'stopping' conditions
	        if (this.moves == 0) {
	            keepGoing = false;
	        } else if (backToStart() && returningToStart) {
	            keepGoing = false;
	        }
	    }
	}
	
	/*
	 * Move the drone, update its position and add the new position coordinates to route
	 */
	public void moveDrone(int direction) throws IOException, InterruptedException {
	    this.moves -= 1;
	    var initialLatitude = this.currentPosition.latitude;
	    var initialLongitude = this.currentPosition.longitude;
	    // Move the drone to next position and add this to the route
	    this.currentPosition = currentPosition.getNextPosition(direction, moveLength);
	    var nextPoint = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude);
	    route.add(nextPoint);
	    // Check if this new position is in range of the destination sensor
	    if (sensors.size() > 0) {
	        if (withinSensorRange(sensors.get(0))) {
	            takeReading();
	            sensors.remove(0);
	        }
	    }
	    // Convert radians to degrees
	    var radians = Math.toRadians(direction);
	    // Use trigonometry to calculate the longitude and latitude values
	    var xValue = moveLength * Math.cos(radians);
	    var yValue = moveLength * Math.sin(radians);
	    // Update the drone's position
	    var newLatitude = initialLatitude + yValue;
	    var newLongitude = initialLongitude + xValue;
	    currentPosition = new Coordinate(newLatitude, newLongitude);
	}
	
	/*
	 * Take the sensor reading of battery percentage and air quality reading
	 */
	private void takeReading() {
	    System.out.println("--- reading taken ---");
	}
    
    /*
     * 
     */
    private int getDirection() throws IOException, InterruptedException {
        // Gets the first sensor in the list left to visit (destination sensor)
        Sensor destination = new Sensor(null, 0.0, null);
        double destinationLatitude;
        double destinationLongitude;
                
        if (sensors.size() != 0) {
            destination = sensors.get(0);
            destinationLatitude = destination.getCoordinate().latitude;
            destinationLongitude = destination.getCoordinate().longitude;
        }
        else {
            // There are no sensors left to visit, so the next destination is back to the drone starting position
            returningToStart = true;
            destinationLatitude = this.startPosition.latitude;
            destinationLongitude = this.startPosition.longitude;
        }
        
        // Calculate the angle of the line needed to get to the sensor
        var yDistance = destinationLatitude - currentPosition.latitude;
        var xDistance = destinationLongitude - currentPosition.longitude;
        var angleRadians = Math.atan(yDistance / xDistance);
        var angleDegrees = Math.toDegrees(angleRadians);
        
        var angleFromEast = 0.0;
        // Calculate the angle anti-clockwise, with East = 0 degrees
        if (xDistance > 0 && yDistance > 0) {
            angleFromEast = angleDegrees;
        } else if (xDistance < 0 && yDistance > 0) {
            angleFromEast = 180 - Math.abs(angleDegrees);
        } else if (xDistance < 0 && yDistance < 0) {
            angleFromEast = 180 + angleDegrees;
        } else if (xDistance > 0 && yDistance < 0) {
            angleFromEast = 360 - (Math.abs(angleDegrees));
        }
        // Round up or down to the corresponding multiple of 10
        var angleRoundedDown = (int) (angleFromEast - angleFromEast % 10);
        var angleRoundedUp = (int) ((10 - angleDegrees % 10) + angleFromEast);
        if ( (angleRoundedUp - angleFromEast) < (angleFromEast - angleRoundedDown)) {
            return angleRoundedUp;
        } else {
            return angleRoundedDown;
        }
    }

    /*
     * Adds the starting point to the route
     */
    public void startRoute() {
        var startPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
        route.add(startPoint); 
    }
    
    /*
     * Check if the drone is close to the starting position (<0.0003)
     */
    private boolean backToStart() throws IOException, InterruptedException {
        var currentLatitude = this.currentPosition.getLatitude();
        var currentLongitude = this.currentPosition.getLongitude();
        var startLatitude = this.startPosition.getLatitude();
        var startLongitude = this.startPosition.getLongitude();
        return (calculateDistance(currentLatitude, currentLongitude, startLatitude, startLongitude) < 0.0003);
    }
    
    /*
     * Check if drone is within the range of the sensor (<0.0002 degrees)
     */
    public boolean withinSensorRange(Sensor sensor) throws IOException, InterruptedException {
        var sensorLatitude = sensor.getCoordinate().latitude;
        var sensorLongitude = sensor.getCoordinate().longitude;
        var positionLatitude = this.currentPosition.latitude;
        var positionLongitude = this.currentPosition.longitude;
        return (calculateDistance(sensorLatitude, sensorLongitude, positionLatitude, positionLongitude) < 0.0002);
    }
    
    /*
     *  Calculate Euclidean distance between two sets of latitude and longitude
     */
    public double calculateDistance(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude ) { 
        var x1 = toLatitude;
        var y1 = toLongitude;
        var x2 = fromLatitude;
        var y2 = fromLongitude;
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }
}
