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
	 * Check if drone is within the range of the sensor (<0.0002 degrees)
	 */
    public boolean withinSensorRange(Sensor sensor) throws IOException, InterruptedException {
        double sensorLatitude = sensor.getCoordinate().latitude;
        double sensorLongitude = sensor.getCoordinate().longitude;
        double positionLatitude = this.currentPosition.latitude;
        double positionLongitude = this.currentPosition.longitude;
        return (calculateDistance(sensorLatitude, sensorLongitude, positionLatitude, positionLongitude) < 0.0002);
    }
    
    /*
     *  Calculate distance between drone and sensor
     */
    public double calculateDistance(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude ) { 
        //var x1 = this.currentPosition.latitude;
        //var y1 = this.currentPosition.longitude;
        var x1 = toLatitude;
        var y1 = toLongitude;
        var x2 = fromLatitude;
        var y2 = fromLongitude;
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }
    
    public void visitSensors() throws IOException, InterruptedException {
      //var count = 0;
      // make keepGOing false if we have got back to the start
      boolean keepGoing = true;
      while (keepGoing) {
          //System.out.println(this.moves > 0 && (sensors.size() >= 0 || !backToStart()));
          System.out.println(backToStart());
          //System.out.println(returningToStart());
          System.out.println(this.moves > 0 && (!(backToStart())));
          //if (backToStart()) {
            //  break;
          //}
          var direction = getDirection();
          moveDrone(direction);
          System.out.println("Number of moves left: " + moves);
          System.out.println("Back to start: " + backToStart() + "     Returning to start: " + returningToStart);
          
          if (this.moves == 0) {
              keepGoing = false;
          } else if (backToStart() && returningToStart) {
              keepGoing = false;
          }
          System.out.println("Keep going: " + keepGoing);
//          if (count == 50) {
//              moves = 0;
//          }
//          count++;
      }
  }
    
    private boolean backToStart() throws IOException, InterruptedException {
        double currentLatitude = this.currentPosition.getLatitude();
        double currentLongitude = this.currentPosition.getLongitude();
        double startLatitude = this.startPosition.getLatitude();
        double startLongitude = this.startPosition.getLongitude();
        return (calculateDistance(currentLatitude, currentLongitude, startLatitude, startLongitude) < 0.0003);
    }

    /*
     * Moves the drone, updates its position and adds the new coordinates to path
     */
    public void moveDrone(int direction) throws IOException, InterruptedException {
        this.moves -= 1;
        
        // CHECKING
        //System.out.println("Number of moves remaining: " + moves);
        //System.out.println("From A: " + currentPosition.toString());

        Double initialLatitude = this.currentPosition.latitude;
        Double initialLongitude = this.currentPosition.longitude;
       
        // Move the drone to next position
        this.currentPosition = currentPosition.getNextPosition(direction, moveLength);
        Point nextPoint = Point.fromLngLat(currentPosition.longitude, currentPosition.latitude);
        route.add(nextPoint);
        
        //System.out.println("Aiming for sensor: " + sensors.get(0).getCoordinate().toString());
        
        // Check if this new position is in range of the destination sensor
        if (sensors.size() > 0) {
            if (withinSensorRange(sensors.get(0))) {
                takeReading();
                sensors.remove(0);
            }
        }
        // Convert radians to degrees
        double radians = Math.toRadians(direction);
        
        // Use trigonometry to calculate the longitude and latitude values
        Double xValue = moveLength * Math.cos(radians);
        Double yValue = moveLength * Math.sin(radians);
        //System.out.println(Math.pow(xValue, 2) + Math.pow(yValue, 2));
        
        // Update the drone's position
        Double newLatitude = initialLatitude + yValue;
        Double newLongitude = initialLongitude + xValue;
        currentPosition = new Coordinate(newLatitude, newLongitude);
       
        //System.out.println("To B: " + currentPosition.toString());
    }

    private void takeReading() {
        System.out.println("--- reading taken ---");
    }
    
    private int getDirection() throws IOException, InterruptedException {
        // Gets the first sensor in the list left to visit (destination sensor)
        Sensor destination = new Sensor(null, 0.0, null);
        double destinationLatitude;
        double destinationLongitude;
                
        //System.out.println("Sensors size" + sensors.size() + sensors.get(0).getCoordinate().toString());
        if (sensors.size() != 0) {
            destination = sensors.get(0);
            destinationLatitude = destination.getCoordinate().latitude;
            destinationLongitude = destination.getCoordinate().longitude;
        }
        else {
            // If we have visited all sensors, return to the start
            System.out.println("Heading back to the start");
            returningToStart = true;
            destinationLatitude = this.startPosition.latitude;
            destinationLongitude = this.startPosition.longitude;
        }
        
        // calculate the angle of the line needed to get to the sensor
        var yDistance = destinationLatitude - currentPosition.latitude;
        var xDistance = destinationLongitude - currentPosition.longitude;
        
        var angleRadians = Math.atan(yDistance / xDistance);
        var angleDegrees = Math.toDegrees(angleRadians);
        double angleFromEast = 0.0;
        if (xDistance > 0 && yDistance > 0) {
            angleFromEast = angleDegrees;
        } else if (xDistance < 0 && yDistance > 0) {
            angleFromEast = 180 - Math.abs(angleDegrees); // update to read from the East, anticlockwise
        } else if (xDistance < 0 && yDistance < 0) {
            angleFromEast = 180 + angleDegrees;
        } else if (xDistance > 0 && yDistance < 0) {
            angleFromEast = 360 - (Math.abs(angleDegrees));
        }
        
        // Round the angle to a multiple of 10 -------------------------------------------------
        int angleRounded;
        var angleRoundedDown = (int) (angleFromEast - angleFromEast % 10);
        var angleRoundedUp = (int) ((10 - angleDegrees % 10) + angleFromEast);
        if ( (angleRoundedUp - angleFromEast) < (angleFromEast - angleRoundedDown)) {
            angleRounded = angleRoundedUp;
        } else {
            angleRounded = angleRoundedDown;
        }
        // -------------------------------------------------------------------------------------
        //System.out.println("Destination: " + sensors.get(0).getCoordinate().toString());
        //System.out.println("y distance: " +yDistance + "    x distance: " + xDistance);
        //System.out.println("angle in degrees: " + angleFromEast + "     = " + angleRounded);
        return angleRounded;
    }

    /*
     * Adds the starting point to the route
     */
    public void startRoute() {
        Point startPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
        route.add(startPoint); 
    }
}
