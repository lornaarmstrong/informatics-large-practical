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
	private List<String> flightpathInformation = new ArrayList<String>();
	private final GeographicalArea map;
	
	public Drone(Coordinate startPosition, GeographicalArea map) {
	    this.currentPosition = startPosition;
	    this.startPosition = startPosition;
	    this.returningToStart = false;
	    this.map = map;
	}
	
	// Getters and Setters
	public Coordinate getCurrentPosition() {
		return this.currentPosition;
	}
	
	public int getMoves() {
		return this.moves;
	}
	
	public void setSensors(List<Sensor> sensors) {
	    this.sensors = sensors;
	}
	
	public List<String> getFlightpathInformation() {
	    return this.flightpathInformation;
	}
	
	/*
     * Add the starting position to the drone's route
     */
    public void startRoute() {
        route.add(startPosition.toPoint());
    }
	
	/*
	 * Move the drone to create a route of at most 150 moves, visiting all sensors (if possible).
	 */
	public void visitSensors() {
	    var continueFlight = true;
	    while (continueFlight) {
	        var destination = getDestination();	  
	        var direction = this.currentPosition.getAngle(destination);
	        moveDrone(direction, destination);
	        // Check the stopping conditions
	        if (this.moves == 0 
	                || (withinRange(this.startPosition, moveLength) && returningToStart)) {
	            continueFlight = false;
	        }
	    }
	}
	
	/*
     * Return the coordinate of the next point the drone is aiming for (either 
     * the next sensor to visit or the starting point)
     */
    private Coordinate getDestination() {
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
	private void moveDrone(int angle, Coordinate destination) {
	    var proposedNextPosition = this.currentPosition.getNextPosition(angle, moveLength);
	    var proposedNextPoint = proposedNextPosition.toPoint();
	    var currentPoint = this.currentPosition.toPoint();
	    var readingTaken = false;
	    
	    // Get confinement area
	    var topLeftCoordinate = map.topLeftConfinement;
	    var bottomLeftCoordinate = map.bottomLeftConfinement;
	    var bottomRightCoordinate = map.bottomRightConfinement;
	    
	    // Check if the move involves flying through a No-Fly Zone
	    if (moveIntersectsNoFlyZone(proposedNextPosition, this.currentPosition)) { 
	        var newDirection = getNewAngleAnticlockwise(angle);
	        moveDrone(newDirection, destination);
	    } 
	    // Check if the proposed next position is outside of the confinement zone
	    else if (!proposedNextPosition.isInConfinementZone(topLeftCoordinate, 
	            bottomLeftCoordinate, bottomRightCoordinate)) {
	        var newDirection = getNewAngleAnticlockwise(angle - 10);
            moveDrone(newDirection, destination);
	    } 
	    // Check if the suggested move has been repeated within the last 5 moves
	    else if (isRepeatedMove(proposedNextPoint, currentPoint)){
	        var newDirection = getNewAngleAnticlockwise(angle - 20);
	        moveDrone(newDirection, destination);
	    } 
	    // The move is a legal move for the drone
	    else {
	        var flightPath = currentPosition.getLongitude() + "," + currentPosition.getLatitude()
	                + "," + angle + "," + proposedNextPosition.getLongitude() + "," 
                    + proposedNextPosition.getLatitude();
	        this.moves -= 1;
	        this.currentPosition = proposedNextPosition;
	        route.add(proposedNextPoint);
	        // Check if this new position is in range of a sensor and take reading if so
	        if (this.sensors.size() > 0) {
	            for (int i = 0; i < this.sensors.size(); i++) {
	                var sensor = this.sensors.get(i);
	                if (withinRange(sensor.getCoordinate(), sensor.getRange())) {
	                    takeReading(sensor);
	                    flightPath += "," + sensor.getLocation();
	                    readingTaken = true;
	                    this.sensors.remove(i);
	                    break;
	                }
	            }
	        }
	        // If no reading is taken, append null to the flightPath line.
	        if (!readingTaken) {
	            flightPath += "," + null;
	        }
	        // Add the flightPath line to flightpathInformation.
	        this.flightpathInformation.add(flightPath);
	    }
	}
	
    /*
	 * Return true is the drone has already moved from point A to point B in the last 5 moves,
	 * false if not.
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
	 * Return the next angle in anti-clockwise direction
	 */
    private int getNewAngleAnticlockwise(int angle) {
	    return ((angle - 10) % 360);
    }

    /*
	 * Return true if the line formed by the move goes into any No-Fly Zone, false if not.
	 */
	private boolean moveIntersectsNoFlyZone(Coordinate newPosition, Coordinate initialPosition) {
	    var noFlyBoundaries = new ArrayList<Line>();
	    // Get all features from the map, and break them down into all boundary lines.
	    // Add the boundary lines to noFlyBoundaries.
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
	    // Loop through all boundaries of No-Fly Zones and check if the move intersects.
	    for (int i = 0; i < noFlyBoundaries.size(); i++) {
	        var boundary = noFlyBoundaries.get(i);
	        if (moveLine.isIntersecting(boundary)) {
	            return true;
	        }
	    } 
	    return false;
	}
    
	/*
	 * Takes the reading from the air quality sensor
	 */
	private void takeReading(Sensor sensor) {
	    var sensorBatteryLevel = sensor.getBattery();
	    var sensorLocation = sensor.getLocation();
	    var sensorReading = sensor.getReading();
	    Sensor checkedSensor = new Sensor(sensorLocation, sensorBatteryLevel, sensorReading);
	    checkedSensors.add(checkedSensor);
	}
	
	/* 
	 * Check if the drone is within range of a given Coordinate (either Sensor or start point)
	 */
	private boolean withinRange(Coordinate destination, double rangeValue) {
	    return (this.currentPosition.getEuclideanDistance(destination) < rangeValue);
	}
	
}
