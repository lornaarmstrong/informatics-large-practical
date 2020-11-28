package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The Drone class represents a Drone, with a position and number of 
 * moves as attributes.
 *
 */
public class Drone {
	
	private Coordinate currentPosition;
	public Coordinate startPosition;
	private int moves = 150;
	public final double moveLength = 0.0003;
	public boolean returningToStart;
	public ArrayList<Point> route = new ArrayList<Point>();
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	public ArrayList<Sensor> checkedSensors = new ArrayList<Sensor>();
	private CampusMap map;
	public Map<Integer,String> possibleMoveTracker = new HashMap<Integer,String>(); // HashMap of droneMoves, positionMovedTo
	
	public Drone(Coordinate startPosition, CampusMap map) {
	    this.currentPosition = startPosition;
	    this.startPosition = startPosition;
	    this.returningToStart = false;
	    this.map = map;
	}
	
	// Getters and Setters
	public Coordinate getCurrentPosition() {
		return this.currentPosition;
	}
	
	public void setCurrentPosition(Coordinate currentPosition) {
		this.currentPosition = currentPosition;
	}
	
	public Coordinate getStartPosition() {
	    return this.startPosition;
	}
	
	public void setStartPosition(Coordinate startPosition) {
	    this.startPosition = startPosition;
	}
	
	public int getMoves() {
		return this.moves;
	}
	
	public void setMoves(int moves) {
	    this.moves = moves;
	}
	
	// TODO fix this to make sure it's properly passing
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
	        // Calculate the angle needed
	        var destination = getDestination();
	        var direction = this.currentPosition.getAngle(destination);
	        // Make one move
	        moveDrone(direction);
	        // Check the 'stopping' conditions
	        if (this.moves == 0) {
	            System.out.println("---- Out of moves!");
	            keepGoing = false;
	        } else if (backToStart() && returningToStart) {
	            System.out.println("---- Returned to the start!");
	            keepGoing = false;
	        }
	    }
	}
	
	/*
	 * Move the drone, update its position and add the new position coordinates to route
	 */
	public void moveDrone(int direction) throws IOException, InterruptedException {
	    var proposedNextPosition = this.currentPosition.getNextPosition(direction, moveLength);
	    var readingTaken = false;
	    
	    if (moveInterceptsNoFly(proposedNextPosition, this.currentPosition) || proposedNextPosition.isInNoFlyZone(map)) {   
	        var newDirection = getNewAngleClockwise(direction);
	        moveDrone(newDirection);
	    } else {
            var flightPath = currentPosition.longitude + "," + currentPosition.latitude + ","
                    + direction + "," + proposedNextPosition.longitude + "," 
                    + proposedNextPosition.latitude;
	        this.moves -= 1;
	        this.currentPosition = proposedNextPosition;
	        var nextPoint = Point.fromLngLat(this.currentPosition.longitude, this.currentPosition.latitude);
	        route.add(nextPoint);
	        // Check if this new position is in range of the destination sensor
	        if (sensors.size() > 0) {
	            var sensor = sensors.get(0);
	            if (withinSensorRange(sensor)) {
	                takeReading(sensor);
	                flightPath += "," + sensor.getLocation();
	                readingTaken = true;
	                sensors.remove(0);
	            }
	        }   
	        
	        if (!readingTaken) {
	            flightPath += ",null";
	        }
	        
	        // Add the flightPathInfo string
	        App.flightpathInformation.add(flightPath);
	    }
	}
	
	/*
	 * Returns the number of drone moves required to get the drone from one coordinate 
	 * to another.
	 */
	public int countNumberMoves(Coordinate startPoint, Coordinate destination, int movesTaken) throws IOException, InterruptedException {
	    //System.out.println();
	    var closeTo = 0.0002; // within range of a sensor
	    int movesCount = movesTaken + 1;
	    
	    var direction = startPoint.getAngle(destination);
	    
        var nextPosition = startPoint.getNextPosition(direction + 10, moveLength);
        
        System.out.println("Direction: " + direction);
        System.out.println("Starting: " + startPoint.toString());
        System.out.println("Destination " + destination.toString());
        System.out.println("Suggested Next: " + nextPosition.toString());
        // Check if close to destination
        
        // If the destination is the starting point, 'close to' is defined as < 0.0003
        // so update closeTo to be 0.0003
        if (destination.equals(this.startPosition)) {
            System.out.println("aiming for the start");
            closeTo = 0.0003;
        }
        
        System.out.println("Intercepts? " + (moveInterceptsNoFly(nextPosition, startPoint) || nextPosition.isInNoFlyZone(map)));
        
        int count = 0;
        while (moveInterceptsNoFly(nextPosition, startPoint) || nextPosition.isInNoFlyZone(map)) {
            //System.out.println(moveInterceps);
            count++;            System.out.println(direction + "-->" + getNewAngleClockwise(direction));
            direction = getNewAngleClockwise(direction);
            nextPosition = startPoint.getNextPosition(direction, moveLength);
        }
        
	    // Check if we are close to destination, return how many moves it took
	    if (nextPosition.getEuclideanDistance(destination) < closeTo) {
	        System.out.println("Close to destination so returning movesCount: " + nextPosition.getEuclideanDistance(destination) + "   " + nextPosition.toString() + " " + destination.toString() );
	        return movesCount;
	    } else { // else, recursively call countNumberMoves
	        movesCount = countNumberMoves(nextPosition, destination, movesTaken + 1);
	    }
	    //System.out.println(movesCount);
	    return movesCount;
	}
	
//	/*
//	 * Returns the number of moves required to get to get to the destination
//	 * @params direction (angle) and moves (number of moves the drone has at the start of the move
//	 * and startPosition, coordinate representing where the move starts from
//	 */
//	public void possibleMoveDrone(int direction, int droneMoves, Coordinate startPosition) {
//	    var proposedNextPosition = this.currentPosition.getNextPosition(direction, moveLength);
//	    var readingTaken = false;
//	    
//	    if (moveInterceptsNoFly(proposedNextPosition, startPosition) || proposedNextPosition.isInNoFlyZone(map)) {   
//            var newDirection = getNewAngleClockwise(direction);
//            possibleMoveDrone(newDirection, droneMoves, startPosition);
//        } else {
//            droneMoves -= 1;
//            startPosition = proposedNextPosition;
//            var nextPoint = Point.fromLngLat(this.currentPosition.longitude, this.currentPosition.latitude);
//            route.add(nextPoint);
//            // Check if this new position is in range of the destination sensor
//            if (sensors.size() > 0) {
//                var sensor = sensors.get(0);
//                if (withinSensorRange(sensor)) {
//                    takeReading(sensor);
//                    readingTaken = true;
//                    sensors.remove(0);
//                }
//            }
//        }
//	    
//	}
	
	private int getNewAngleClockwise(int direction) {
	    return ((direction + 10) % 360);
    }
	
	private int getNewAngleAnticlockwise(int direction) {
	    return ((direction - 10) % 360);
	}

    /*
	 * A method to check if the line formed by the move goes into any No-Fly Zone
	 */
	private boolean moveInterceptsNoFly(Coordinate newPosition, Coordinate initialPosition) {
	    var noFlyBoundaries = new ArrayList<Line>();
	    for (Feature feature: map.noFlyZones) {
	        var polygon = (Polygon) feature.geometry();
	        var coordinateLists = polygon.coordinates();
	        var coordinateList = coordinateLists.get(0);
	        for (int i = 0; i < coordinateList.size() - 1; i++) {
	            var  pointA = coordinateList.get(i);
	            var pointB = coordinateList.get(i + 1);
	            var coordA = new Coordinate(pointA.latitude(), pointA.longitude());
                var coordB = new Coordinate(pointB.latitude(), pointB.longitude());
	            var line = new Line(coordA, coordB);
	            noFlyBoundaries.add(line);
	        }
	    }
	    var moveLine = new Line(newPosition, initialPosition);
	    for (int i = 0; i < noFlyBoundaries.size(); i++) {
	        var boundary = noFlyBoundaries.get(i);
	        var moveIntersects = moveLine.isIntersecting(boundary);
	        if (moveIntersects) {
	            return true; // since the proposed move crosses a boundary
	        }
	    }
	    return false;
	}
    
    
	private void takeReading(Sensor sensor) {
	    System.out.println("--- reading taken ---");
	    var sensorBatteryLevel = sensor.getBattery();
	    var sensorLocation = sensor.getLocation();
	    var sensorReading = sensor.getReading();
	    Sensor checkedSensor = new Sensor(sensorLocation, sensorBatteryLevel, sensorReading);
	    // TODO fix this problem as it only wokrs when you add the sensor, not checkedSensor
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
            var sensorCoordinate = destinationSensor.getPosition();
            destination = new Coordinate (sensorCoordinate.latitude, sensorCoordinate.longitude);
        } else {
            returningToStart = true;
            destination = new Coordinate (this.startPosition.latitude, this.startPosition.longitude);
        }
        return destination;
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
	    var sensorLatitude = sensor.getPosition().latitude;
	    var sensorLongitude = sensor.getPosition().longitude;
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
