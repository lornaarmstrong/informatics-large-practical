package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

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
	    Coordinate initialPosition = new Coordinate(initialLatitude, initialLongitude);
	    
	    // Move the drone to next position
	    this.currentPosition = currentPosition.getNextPosition(direction, moveLength);
	    boolean inNoFly = currentPosition.isInNoFlyZone();
	    
	    if (moveInterceptsNoFly(currentPosition, initialPosition)) {
	        //goRoundNoFlyZone(initialLatitude, initialLongitude);
	        System.out.println("Move Intercepts!");
	    } else {
	        System.out.println("Move doesn't Intercept");
	    }
	    //if (currentPosition.isInNoFlyZone()) {
	    //    goRoundNoFlyZone(initialLatitude, initialLongitude);
	    //}
	    
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
	
	/*
	 * A method to check if the line formed by the move goes into any No-Fly Zone
	 */
	private boolean moveInterceptsNoFly(Coordinate newPosition, Coordinate initialPosition) {
	    // Get every line of a no-fly zone.
	    for (Feature feature: App.noFlyZones) {
	        Polygon polygon = (Polygon) feature.geometry();
	        // Creates a list of all polygons
	        List<List<Point>> coordinates = polygon.coordinates();
	        List<Point> coordinateList = coordinates.get(0);
	        
	        for (Point point: coordinateList) {
	            App.pointsInZones.add(point);
	        }
	        	        
	        // Loop through and get each pair of coordinates
	        for (int i = 0; i < coordinateList.size() - 1; i++) {
	            Point line1A = coordinateList.get(i);
	            Point line1B = coordinateList.get(i+1);
	            Coordinate coordLine1A = new Coordinate(line1A.latitude(), line1A.longitude());
	            Coordinate coordLine1B = new Coordinate(line1B.latitude(), line1B.longitude());
	            
	            // Pass the line and the drone line to the intersect function
	            boolean lineIntersect = intersect(initialPosition, currentPosition, coordLine1A, coordLine1B);
	            if (lineIntersect) {
	                return true; // since the move involves intersecting a no fly zone
	            }
	            if (lineIntersect) {
    	            System.out.println("Line Intersects: " + lineIntersect);
    	            System.out.println("Building Line -----");
    	            System.out.println(coordLine1A.latitude + "," + coordLine1A.longitude);
    	            System.out.println(coordLine1B.latitude + "," + coordLine1B.longitude);
    	            System.out.println("Drone Line ------");
    	            System.out.println(initialPosition.latitude + "," + initialPosition.longitude);
    	            System.out.println(currentPosition.latitude + "," + currentPosition.longitude);
	            }
	            //System.out.println(lineIntersect);
//	            List<Point> points = new ArrayList<Point>();
//	            points.add(line1A);
//	            points.add(line1B);
//	            LineString line = LineString.fromLngLats(points);
//	            App.buildingLines.add(line);
	        }
	    }
	    // None of the lines of the confinement zones intersected
	    return false;
    }

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
       
        // Check if there's an interval
        if (Math.max(X1, X2) < Math.min(X3, X4)) {
            // if there isn't a common x interval, return false as the lines can't intersect
            return false;
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
            return false; // the lines are parallel so don't intersect
        }
        
        // A point (Xi, Yi) of intersection lying on both lines must fit both formulas
        double Xi = (c2 - c1) / (m1 - m2);
        double Yi1 = (m1 * Xi) + c1;
        double Yi2 = (m2 * Xi) + c2;
        if ( Yi1 == Yi2) {
            // The point of intersection lies on both lines
            
            // Check that Xi is in the interval
            if ( (Xi < Math.max(Math.min(X1,X2), Math.min(X3, X4))) 
                    || (Xi > Math.min(Math.max(X1, X2), Math.max(X3, X4)))) {
                return false;
            } else {
                return true;
            }
        }
        
        return false;
    }

    public void goRoundNoFlyZone(double initialLatitude, double initialLongitude) throws IOException, InterruptedException {
        // The drone has reached a no fly zone and must then go round the building and continue
	    // along its path to its destination
	    
	    // Set the drone back to previous move (where it is outside of the no fly zone)
	    currentPosition.latitude = initialLatitude;
	    currentPosition.longitude = initialLongitude;
	    
	    // Get the coordinates of the destination the drone is aiming for
	    //var destination = getDestination();
	    
	    // get the angle to the destination
	    // move the angle 10 degrees anticlockwise
	    // check if the point after one move in that direction is still in building
	    // move the angle another 10 degrees anticlockwise
	    // repeat until the point isn't in a building.
        
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
            var sensorCoordinate = destinationSensor.getPosition();
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
