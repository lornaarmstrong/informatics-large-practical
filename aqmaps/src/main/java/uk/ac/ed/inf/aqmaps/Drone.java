package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
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
	public int moves = 150;
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
	        var direction = getDirection();
	        
	        // Check if the move involves flying through a no-fly zone
//	        Coordinate proposedNextPosition = this.currentPosition.getNextPosition(direction, moveLength);
	        
//	        // The proposed move involves flying through a no-fly zone and needs to be dealt with
//	        if (moveInterceptsNoFly(proposedNextPosition, this.currentPosition)) {
//	            // repeatedly try to
//	        } else { // the drone can make the move
//	            this.currentPosition = proposedNextPosition;
//	            moves = moves - 1;
//	            // track the point visited
//	            var nextPoint = Point.fromLngLat(this.currentPosition.longitude, this.currentPosition.latitude);
//	            route.add(nextPoint);
//	            // Check if this new position is in range of the destination sensor
//	            if (sensors.size() > 0) {
//	                Sensor sensor = sensors.get(0);
//	                if (withinSensorRange(sensor)) {
//	                    takeReading(sensor);
//	                    sensors.remove(0);
//	                }
//	            }  
//	        }
	        
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
        //System.out.println("Move Drone. Direction: " + direction);
	    // Check if the move involves flying through a no-fly zone
	    Coordinate proposedNextPosition = this.currentPosition.getNextPosition(direction, moveLength);
	    //System.out.println("Got proposedNextPosition.");
	    //System.out.println("--------");
	    //System.out.println("Next prop: " + proposedNextPosition);

	    // Check if the move involves flying through a no-fly zone
	    //|| proposedNextPosition.isInNoFlyZone()
	    if (moveInterceptsNoFly(proposedNextPosition, this.currentPosition) || proposedNextPosition.isInNoFlyZone()) {   
	        //System.out.println("oh no! Move interceps no fly zone. We need a new angle!");
	        int newDirection = goAroundNoFlyZone(direction);
	        //System.out.println("Got new direction. Let's try: " + newDirection);
	        moveDrone(newDirection);
	    } else {
	        //System.out.println("Finally - an angle that works!");
	        this.moves -= 1;
	        this.currentPosition = proposedNextPosition;
	        var nextPoint = Point.fromLngLat(this.currentPosition.longitude, this.currentPosition.latitude);
	        route.add(nextPoint);
	        // Check if this new position is in range of the destination sensor
	        if (sensors.size() > 0) {
	            Sensor sensor = sensors.get(0);
	            if (withinSensorRange(sensor)) {
	                takeReading(sensor);
	                sensors.remove(0);
	            }
	        }   
	        
	    }
	    
//	    // Add the new position to the route
//	    var nextPoint = Point.fromLngLat(this.currentPosition.longitude, this.currentPosition.latitude);
//	    route.add(nextPoint); 
	    
	    // Check if this new position is in range of the destination sensor
//	    if (sensors.size() > 0) {
//	        Sensor sensor = sensors.get(0);
//	        if (withinSensorRange(sensor)) {
//	            takeReading(sensor);
//	            sensors.remove(0);
//	        }
//	    }	    
	}
	
	private int goAroundNoFlyZone(int direction) {
        // We know that the current direction doesn't work
	    int newDirection = direction + 10;
	    return newDirection;
    }

    /*
	 * A method to check if the line formed by the move goes into any No-Fly Zone
	 */
	private boolean moveInterceptsNoFly(Coordinate newPosition, Coordinate initialPosition) {
	    //System.out.println("--------------- moveInterceptsNoFly function -----------");
	    // Get every line of a no-fly zone.
	    List<Line> noFlyBoundaries = new ArrayList<>();
	    
	    // Adds every line that the drone cannot cross over
	    for (Feature feature: App.noFlyZones) {
	        Polygon polygon = (Polygon) feature.geometry();
	        List<List<Point>> coordinateLists = polygon.coordinates();
	        List<Point> coordinateList = coordinateLists.get(0);
	        for (int i = 0; i < coordinateList.size() - 1; i++) {
	            Point pointA = coordinateList.get(i);
	            Point pointB = coordinateList.get(i + 1);
	            Coordinate coordA = new Coordinate(pointA.latitude(), pointA.longitude());
                Coordinate coordB = new Coordinate(pointB.latitude(), pointB.longitude());
	        
	            Line line = new Line(coordA, coordB);
	            noFlyBoundaries.add(line);
	        }
	    }
	    
	    // Print out all no fly lines
//	    System.out.println(noFlyBoundaries.size());
//	    System.out.println("LINES");
//	    for (Line line : noFlyBoundaries) {
//	        System.out.println(line.toString());
//	    }
	    
	    //System.out.println("Number of No Fly Boundaries: " + noFlyBoundaries.size());
	    
	    // Check whether the two lines intersect for each one
	    Line moveLine = new Line(newPosition, initialPosition);
	    
	    //boolean noIntersection = true;
	    
	    for (int i = 0; i < noFlyBoundaries.size(); i++) {
	        Line boundary = noFlyBoundaries.get(i);
	        boolean intersects = intersect(newPosition, initialPosition, boundary.getCoordinateA(), boundary.getCoordinateB());
	        if (intersects) {
	            return true; // since the proposed move crosses a forbidden line
	        }
	    }
	    return false;
    }

	/*
	 * Returns true if the line between initialPosition and newPosition intersects the line between coordLine1A
	 * and coordLine1B, else false
	 */
    private boolean intersect(Coordinate initialPosition, Coordinate newPosition, Coordinate coordLine1A,
            Coordinate coordLine1B) {
        
        // Coordinates for the line representing the drone's suggested movement
        double X1 = initialPosition.longitude;
        double Y1 = initialPosition.latitude;
        double X2 = newPosition.longitude;
        double Y2 = newPosition.latitude;
        // Coordinates for the line representing the no fly zone
        double X3 = coordLine1A.longitude;
        double Y3 = coordLine1A.latitude;
        double X4 = coordLine1B.longitude;
        double Y4 = coordLine1B.latitude;
        
        boolean result = false;
       
        // Check if there's an interval
        if (Math.max(X1, X2) < Math.min(X3, X4)) {
            // if there isn't a common x interval, return false as the lines can't intersect
            //System.out.println("No common interval");
            result = false;
            //return false;
        }
        
        // f1(x) = m1x + c1 = y
        // f2(x) = m2x + c2 = y
        // Calculate A1, A2, b1, b2
        double m1 = (Y1 - Y2) / ( X1 - X2);
        double m2 = (Y3 - Y4) / (X3 - X4);
        double c1 = Y1 - (m1 * X1);
        double c2 = Y3 - (m2 * X3);
        
        // Check if the two lines are parallel
        if (m1 == m2) {
            //System.out.println("Parallel Lines");
            //return false; // the lines are parallel so don't intersect
            result = false;
        }
        
        // A point (Xi, Yi) of intersection lying on both lines must fit both formulas
        double Xi = (c2 - c1) / (m1 - m2);
        double Yi1 = (m1 * Xi) + c1;
        double Yi2 = (m2 * Xi) + c2;
        if ( Yi1 == Yi2) {
            // The point of intersection lies on both lines are in mcgrath
            
            // Check that Xi is in the interval
            if ((Xi < Math.max(Math.min(X1,X2), Math.min(X3, X4))) 
                    || (Xi > Math.min(Math.max(X1, X2), Math.max(X3, X4)))) {
                result = false;
            } else {
                result = true;
            }
        }
        
        //return false;
        return result;
    }

    /*
     * Return the coordinate that the drone has now moved to, where the coordinate is not in a noFlyZone
     */
//    public Coordinate goRoundNoFlyZone(int moveAngle) throws IOException, InterruptedException {
//	    
//	    // Get the coordinates of the destination the drone is aiming for
//        var destination = getDestination();
//        System.out.println("Drone's Position: " + this.currentPosition.latitude + ", " + this.currentPosition.longitude);
//        System.out.println("Destination: " + destination.getLatitude() + "," + destination.getLongitude());
//        System.out.println("Previous Angle: " + moveAngle);
//        System.out.println("--- Going Round No Fly Zone ---");
//        boolean go = true;
//        while(go) {
//            moveAngle -= 10;
//            System.out.println(moveAngle);
//            // find the next position using this angle
//            Coordinate nextPosition = this.currentPosition.getNextPosition(moveAngle, moveLength);
//            System.out.println("Next Position: " + nextPosition.toString());
//            if (!moveInterceptsNoFly(nextPosition, this.currentPosition)) {
//                // Break the while loop
//                System.out.println("The move now doesn't intersect at position: " + nextPosition.getLatitude() + ", " + nextPosition.getLongitude());
//                return (nextPosition);
//            }
//        }
//        return null;
//    }

    /*
	 * Take the sensor reading of battery percentage and air quality reading
	 */
	private void takeReading(Sensor sensor) {
	    System.out.println("--- reading taken ---");
	    var sensorBatteryLevel = sensor.getBattery();
	    var sensorLocation = sensor.getLocation();
	    var sensorReading = sensor.getReading();
	    Sensor checkedSensor = new Sensor(sensorLocation, sensorBatteryLevel, sensorReading);
	    //System.out.println("Reading Taken: " + sensorLocation + " " + sensorBatteryLevel + " " + sensorReading);
	    
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
	 * Returns the most optimal angle of travel for the drone from it's current position
	 * to the position of the next coordinate to visit.
	 */
	private int getDirection() throws IOException, InterruptedException {
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
