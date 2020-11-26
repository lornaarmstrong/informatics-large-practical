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
	public Coordinate startPosition;
	public int moves = 150;
	public final double moveLength = 0.0003;
	public boolean returningToStart;
	public ArrayList<Point> route = new ArrayList<Point>();
	private ArrayList<Sensor> sensors = new ArrayList<Sensor>();
	public ArrayList<Sensor> checkedSensors = new ArrayList<Sensor>();
	
	public Drone(Coordinate startPosition) {
	    this.currentPosition = startPosition;
	    this.startPosition = startPosition;
	    this.returningToStart = false;
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
	        var direction = getDirection();
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
	
//	public void testing() {
//	    var noFlyBoundaries = new ArrayList<Line>();
//        for (Feature feature: App.noFlyZones) {
//            var polygon = (Polygon) feature.geometry();
//            var coordinateLists = polygon.coordinates();
//            var coordinateList = coordinateLists.get(0);
//            for (int i = 0; i < coordinateList.size() - 1; i++) {
//                var  pointA = coordinateList.get(i);
//                var pointB = coordinateList.get(i + 1);
//                var coordA = new Coordinate(pointA.latitude(), pointA.longitude());
//                var coordB = new Coordinate(pointB.latitude(), pointB.longitude());
//                var line = new Line(coordA, coordB);
//                noFlyBoundaries.add(line);
//            }
//        }
//        
//	    Coordinate dronePos = new Coordinate(55.94524637980763, -3.1880390932053637);
//	    Coordinate droneNewPos = dronePos.getNextPosition(90, 0.0003);
//	    System.out.println("Drone New Pos: " + droneNewPos.toString());
//	    
//	    for (int i = 0; i < noFlyBoundaries.size(); i++) {
//            var boundary = noFlyBoundaries.get(i);
//            var intersects = intersect(droneNewPos, dronePos, boundary.getCoordinateA(), boundary.getCoordinateB());
//            if (intersects) {
//                System.out.println("Drone: " + droneNewPos.toString() + " " + dronePos);
//                System.out.println("Boundary: " + boundary.getCoordinateA().toString() + ", " + boundary.getCoordinateB().toString());
//                System.out.println("Intersect = true");
//                System.out.println("--------");
//                //System.out.println("Intersects: " + );
//                //return true; // since the proposed move crosses a forbidden line
//            } else {
//                System.out.println("Drone: " + droneNewPos.toString() + " " + dronePos);
//                System.out.println("Boundary: " + boundary.getCoordinateA().toString() + ", " + boundary.getCoordinateB().toString());
//                System.out.println("Intersect = false");
//                System.out.println("-------");
//            }
//        }
//	    
//	}
	
	/*
	 * Move the drone, update its position and add the new position coordinates to route
	 */
	public void moveDrone(int direction) throws IOException, InterruptedException {
	    var proposedNextPosition = this.currentPosition.getNextPosition(direction, moveLength);
	    if (moveInterceptsNoFly(proposedNextPosition, this.currentPosition) || proposedNextPosition.isInNoFlyZone()) {   
	        var newDirection = getNewAngleClockwise(direction);
	        //System.out.println("Direction " + direction);
	        moveDrone(newDirection);
	    } else {
	        this.moves -= 1;
	        this.currentPosition = proposedNextPosition;
	        var nextPoint = Point.fromLngLat(this.currentPosition.longitude, this.currentPosition.latitude);
	        route.add(nextPoint);
	        // Check if this new position is in range of the destination sensor
	        if (sensors.size() > 0) {
	            var sensor = sensors.get(0);
	            if (withinSensorRange(sensor)) {
	                takeReading(sensor);
	                sensors.remove(0);
	            }
	        }   
	        
	    }
	}
	
	private int getNewAngleClockwise(int direction) {
	    return (direction + 10);
    }
	
	private int getNewAngleAnticlockwise(int direction) {
	    return (direction - 10);
	}

    /*
	 * A method to check if the line formed by the move goes into any No-Fly Zone
	 */
	private boolean moveInterceptsNoFly(Coordinate newPosition, Coordinate initialPosition) {
	    var noFlyBoundaries = new ArrayList<Line>();
	    for (Feature feature: App.noFlyZones) {
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
	    // Print out all no fly lines
//	    System.out.println(noFlyBoundaries.size());
//	    System.out.println("LINES");
//	    for (Line line : noFlyBoundaries) {
//	        System.out.println(line.toString());
//	    }    
	    var moveLine = new Line(newPosition, initialPosition);
	    for (int i = 0; i < noFlyBoundaries.size(); i++) {
	        var boundary = noFlyBoundaries.get(i);
	        var intersects = intersect(newPosition, initialPosition, boundary.getCoordinateA(), boundary.getCoordinateB());
	        if (intersects) {
	            //System.out.println("Drone: " + newPosition.toString() + " " + initialPosition);
	            //System.out.println("Intersects: " + );
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
        
//        System.out.println("X1, Y1: " + X1 + "," + Y1);
//        System.out.println("X1, Y1: " + X1 + "," + Y1);
//        System.out.println("X1, Y1: " + X1 + "," + Y1);
//        System.out.println("X1, Y1: " + X1 + "," + Y1);
        
        
        boolean result = false;
       
        // Check if there's an interval
        if (Math.max(X1, X2) < Math.min(X3, X4)) {
            // if there isn't a common x interval, return false as the lines can't intersect
            //System.out.println("No common interval");
            result = false;
            //System.out.println("no possible interval");
            //return false;
        }
        
//        System.out.println("Y1 - Y2 " + (Y1 - Y2));
//        System.out.println("X1 - X2 " + (X1 - X2));
        
        // f1(x) = m1x + c1 = y
        // f2(x) = m2x + c2 = y
        // Calculate A1, A2, b1, b2
        
        if (X1 - X2 == 0 ) {
            // We know one line is a vertical line
            // Check if one point of the other line lies on either side
            // if so, the lines intersect
            if ( Math.min(X3,X4) <= X1 && Math.max(X3,X4) >= X1) {
                return true;
            }
        } else {
        
            double m1 = (Y1 - Y2) / ( X1 - X2);
            double m2 = (Y3 - Y4) / (X3 - X4);
            double c1 = Y1 - (m1 * X1);
            double c2 = Y3 - (m2 * X3);
            
            //System.out.println("m1: " + m1);
            
            // Check if the two lines are parallel
            if (m1 == m2) {
                //System.out.println("m1 == m2");
                result = false;
            }
        
            // A point (Xi, Yi) of intersection lying on both lines must fit both formulas
            //System.out.println("c2 - c1" + (c2-c1));
            double Xi = (c2 - c1) / (m1 - m2);
            double Yi1 = (m1 * Xi) + c1;
            double Yi2 = (m2 * Xi) + c2;
            if ( Yi1 == Yi2) {
                // The point of intersection lies on both lines are in mcgrath
                
                // Check that Xi is in the interval
                if ((Xi < Math.max(Math.min(X1,X2), Math.min(X3, X4))) 
                        || (Xi > Math.min(Math.max(X1, X2), Math.max(X3, X4)))) {
                    result = false;
                    //System.out.println("Not in range");
                } else {
                    //System.out.println("TRUE");
                    result = true;
                }
            }
        
//        if (Yi1 != Yi2) {
//            System.out.println("not equal!");
//            System.out.println("(x,y):  " + Xi + ", " + Yi1 );
//            System.out.println("(x,y):  " + Xi + ", " + Yi2 );
//        }
        }
        return result;
    }
    
    
    
    
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
