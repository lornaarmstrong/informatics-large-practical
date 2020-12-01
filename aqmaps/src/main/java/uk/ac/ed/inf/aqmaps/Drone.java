package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * The Drone class represents a Drone, with a position and number of 
 * moves as attributes.
 *
 */
public class Drone {
	
	private final Coordinate startPosition;
	private Coordinate currentPosition;
	private int moves = 150;
	public static final double moveLength = 0.0003;
	public boolean returningToStart;
	public List<Point> route = new ArrayList<Point>();	
	public List<Sensor> sensors = new ArrayList<Sensor>();
	public List<Sensor> checkedSensors = new ArrayList<Sensor>();
	private final CampusMap map;
	
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
	
	public Coordinate getStartPosition() {
	    return this.startPosition;
	}
	
	public int getMoves() {
		return this.moves;
	}
	
	public void setSensors(List<Sensor> sensors) {
	    this.sensors = sensors;
	}
	
	/*
     * Adds the starting position to the drone's route
     */
    public void startRoute() {
        route.add(startPosition.toPoint());
    }
	
	/*
	 * Move the drone to create a route visiting all sensors (if possible).
	 */
	public void visitSensors() {
	    var continueFlight = true;

	    while (continueFlight) {
	        var destination = getDestination();	  
	        var direction = this.currentPosition.getAngle(destination);
	        moveDrone(direction, destination);
	        // Check the stopping conditions
	        if (this.moves == 0 || (withinRange(this.startPosition, moveLength) && returningToStart)) {
	            continueFlight = false;
	        }
	    }
	}
	
	/*
     * Return the coordinate of the next point the drone is aiming for (either 
     * the next sensor to visit or back to the starting point)
     */
    public Coordinate getDestination() {
        Coordinate destination = null;
        if (this.sensors.size() != 0) {
            var destinationSensor = sensors.get(0);
            destination = destinationSensor.getCoordinate();
        } else {
            returningToStart = true;
            destination = this.startPosition;
        }
        return destination;
    }

    /*
	 * Move the drone, update its position and add the new position coordinates to route
	 */
	public void moveDrone(int direction, Coordinate destination) {
	    var proposedNextPosition = this.currentPosition.getNextPosition(direction, moveLength);
	    var proposedNextPoint = proposedNextPosition.toPoint();
	    var currentPoint = this.currentPosition.toPoint();
	    var readingTaken = false;
	    
	    if (moveInterceptsNoFly(proposedNextPosition, this.currentPosition)) { 
	        var newDirection = getNewAngleClockwise(direction);
	        moveDrone(newDirection, destination);
	    } else if (isRepeatedMove(proposedNextPoint, currentPoint)){
	        var newDirection = getNewAngleClockwise(direction + 20);
	        moveDrone(newDirection, destination);
	    } else {
	        var flightPath = currentPosition.getLongitude() + "," + currentPosition.getLatitude() + ","
                        + direction + "," + proposedNextPosition.getLongitude() + "," 
                        + proposedNextPosition.getLatitude();
	        this.moves -= 1;
	        this.currentPosition = proposedNextPosition;
	        route.add(proposedNextPoint);
	        // Check if this new position is in range of the destination sensor
	        if (sensors.size() > 0) {
	            for (int i = 0; i < sensors.size(); i++) {
	                var sensor = sensors.get(i);
	                if (withinRange(sensor.getCoordinate(), sensor.getRange())) {
	                    takeReading(sensor);
	                    flightPath += "," + sensor.getLocation();
	                    readingTaken = true;
	                    sensors.remove(i);
	                    break;
	                }
	            }
	        }   
	        if (!readingTaken) {
	            flightPath += "," + null;
	        }
	        // Add the flightPathInfo string
	        App.flightpathInformation.add(flightPath);
	    }
	}
	
    /*
	 * Returns true is the drone has already moved from point A to point B in the last 5 moves, false if not
	 */
	private boolean isRepeatedMove(Point proposedNext, Point current) {
        for (int i = 1; i <= 5; i ++) {
            if(this.route.size() > 5) {
                var pointA = this.route.get(this.route.size() - i);
                var pointB = this.route.get(this.route.size() - (i + 1));
                if (pointA.equals(proposedNext) && pointB.equals(current)) {
                    return true;
                }
            }
        }
        return false;
	}

	/*
	 * Returns the next angle in anti-clockwise direction
	 */
    private int getNewAngleClockwise(int angle) {
	    return ((angle + 10) % 360);
    }

    /*
	 * A method to check if the line formed by the move goes into any No-Fly Zone
	 */
	public boolean moveInterceptsNoFly(Coordinate newPosition, Coordinate initialPosition) {
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
	        if (moveLine.isIntersecting(boundary)) {
	            return true;
	        }
	    } 
	    return false;
	}
    
	/*
	 * The drone takes the reading from the air quality sensor
	 */
	private void takeReading(Sensor sensor) {
	    var sensorBatteryLevel = sensor.getBattery();
	    var sensorLocation = sensor.getLocation();
	    var sensorReading = sensor.getReading();
	    Sensor checkedSensor = new Sensor(sensorLocation, sensorBatteryLevel, sensorReading);
	    // TODO fix this problem as it only wokrs when you add the sensor, not checkedSensor
	    checkedSensors.add(sensor);
	}
	
	/* 
	 * Check if the drone is within range of a given Coordinate (either Sensor or start point)
	 */
	public boolean withinRange(Coordinate destination, double rangeValue) {
	    return (this.currentPosition.getEuclideanDistance(destination) < rangeValue);
	}
	
}
