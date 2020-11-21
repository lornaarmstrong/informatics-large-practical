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
	public final double moveLength = 0.0003;
	public ArrayList<Point> route = new ArrayList<Point>();
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	public ArrayList<Sensor> checkedSensors = new ArrayList<Sensor>();
	
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
	    
	    // Move the drone to next position
	    this.currentPosition = currentPosition.getNextPosition(direction, moveLength);
	    boolean inNoFly = currentPosition.isInNoFlyZone();
	    
	    if (currentPosition.isInNoFlyZone()) {
	        goRoundNoFlyZone(initialLatitude, initialLongitude);
	    }
	    
	    // Add the new position to the route
	    var nextPoint = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude);
	    route.add(nextPoint);
	    
	    // Check if this new position is in range of the destination sensor
	    if (sensors.size() > 0) {
	        Sensor sensor = sensors.get(0);
	        if (withinSensorRange(sensor)) {
	            takeReading(sensor);
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
	
	public void goRoundNoFlyZone(double initialLatitude, double initialLongitude) {
        // The drone has reached a no fly zone and must then go round the building and continue
	    // along its path to its destination
	    
	    // Set the drone back to previous move (where it is outside of the no fly zone)
	    currentPosition.latitude = initialLatitude;
	    currentPosition.longitude = initialLongitude;
	    
	    
        
    }

    /*
	 * Take the sensor reading of battery percentage and air quality reading
	 */
	private void takeReading(Sensor sensor) {
	    System.out.println("--- reading taken ---");
	    var sensorBatteryLevel = sensor.getBattery();
	    var sensorLocation = sensor.getLocation();
	    var sensorReading = sensor.getReading();
	    Sensor checkedSensor = new Sensor(sensorLocation, sensorBatteryLevel, sensorReading);
	    checkedSensors.add(sensor);
	}
	
	/*
	 * Returns the coordinate of the next point the drone is aiming for (either 
	 * the next sensor to visit or back to the starting point)
	 */
	public Coordinate getDestination() throws IOException, InterruptedException {
	    Coordinate destination;
        if (sensors.size() != 0) {
            Sensor destinationSensor = sensors.get(0);
            var sensorCoordinate = destinationSensor.getCoordinate();
            destination = new Coordinate (sensorCoordinate.latitude, sensorCoordinate.longitude);
        } else {
            // There are no sensors left to visit
            // The next destination is back to the drone starting position
            returningToStart = true;
            destination = new Coordinate (this.startPosition.latitude, this.startPosition.longitude);
        }
        return destination;
	}
	
	private int getDirection() throws IOException, InterruptedException {
	    // Gets the first sensor in the list left to visit (destination sensor)
//	    double destinationLatitude;
//	    double destinationLongitude;
//	    
//	    // call get destination
//	    
//	    if (sensors.size() != 0) {
//	        Sensor destination = sensors.get(0);
//	        destinationLatitude = destination.getCoordinate().latitude;
//	        destinationLongitude = destination.getCoordinate().longitude;
//	    } else {
//	        // There are no sensors left to visit
//	        // The next destination is back to the drone starting position
//	        returningToStart = true;
//	        destinationLatitude = this.startPosition.latitude;
//	        destinationLongitude = this.startPosition.longitude;
//	    }
	    var destination = getDestination();
	    
	    // Calculate the angle of the line needed to get to the sensor
	    var yDistance = destination.latitude - currentPosition.latitude;
	    var xDistance = destination.longitude - currentPosition.longitude;
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
	    if ((angleRoundedUp - angleFromEast) < (angleFromEast - angleRoundedDown)) {
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
	    return (calculateDistance(currentLatitude, currentLongitude, 
	            startLatitude, startLongitude) < 0.0003);
	}
	/*
	 * Check if drone is within the range of the sensor (<0.0002 degrees)
	 */
	public boolean withinSensorRange(Sensor sensor) throws IOException, InterruptedException {
	    var sensorLatitude = sensor.getCoordinate().latitude;
	    var sensorLongitude = sensor.getCoordinate().longitude;
	    var positionLatitude = this.currentPosition.latitude;
	    var positionLongitude = this.currentPosition.longitude;
	    return (calculateDistance(sensorLatitude, sensorLongitude, 
	            positionLatitude, positionLongitude) < 0.0002);
	}
	/*
	 * Calculate Euclidean distance between two sets of latitude and longitude
	 */
	public double calculateDistance(double fromLatitude, double fromLongitude,
	        double toLatitude, double toLongitude) { 
	    var distance = Math.sqrt(Math.pow((toLatitude - fromLatitude), 2) 
	            + Math.pow((toLongitude - fromLongitude), 2));
	    return distance;
	}
}
