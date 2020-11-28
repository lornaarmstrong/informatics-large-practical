package uk.ac.ed.inf.aqmaps;

import java.io.IOException;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;

/**
 * Coordinate class, as pairs of latitude and longitude values
 *
 */
public class Coordinate {
	
	public double latitude;
	public double longitude;
	
	// Constructor for Coordinate
	public Coordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	// Getters and Setters
	public double getLatitude() {
		return this.latitude;
	}
	
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	/*
	 * equals method
	 */
	private boolean equals(Coordinate coord) {
		return ((this.latitude == coord.latitude) && (this.longitude == coord.longitude));
	}
	
	/*
	 * Check if the coordinate is in the confinement area
	 */
	public boolean isInConfinementZone() {
		boolean permittedLatitude;
		boolean permittedLongitude;
		permittedLatitude = 55.942617 < latitude && latitude < 55.946233;
		permittedLongitude = -3.192473 < longitude && longitude < -3.184319;
		return (permittedLatitude && permittedLongitude);
	}
    
    /*
     * Check if the coordinate is in a forbidden zone
     */
    public boolean isInNoFlyZone(CampusMap map) {
        for (Feature feature: map.noFlyZones) {
            Polygon polygon = (Polygon) feature.geometry();
            Point point = Point.fromLngLat(this.longitude, this.latitude);
            boolean isInside = TurfJoins.inside(point, polygon);
            if (isInside) {
                return true;
            }
        }
        return false;
    }
    
    /*
     *  Calculate Euclidean distance between currentNode and sensor
     */
    public double getEuclideanDistance(Coordinate coordinate) { 
        var y1 = this.latitude;
        var x1 = this.longitude;
        var y2 = coordinate.latitude;
        var x2 = coordinate.longitude;
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }
    
    /*
     * Returns the angle from the coordinate to the passed in coordinate, rounded to
     * a multiple of 10.
     */
    public int getAngle(Coordinate destination) throws IOException, InterruptedException {
        // Calculate the angle of the line needed to get to the sensor
        var yDistance = destination.latitude - this.latitude;
        var xDistance = destination.longitude - this.longitude;
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
        var angleRoundedUp = (int) ((10 - angleFromEast % 10) + angleFromEast);
        //System.out.println(angleRoundedDown + " " + angleRoundedUp + " = " + angleDegrees);
        if ((angleRoundedUp - angleFromEast) < (angleFromEast - angleRoundedDown)) {
            return angleRoundedUp;
        } else {
            return angleRoundedDown;
        }
    }
    
    /*
     * Gets the next coordinate based on the direction and distance
     */
    public Coordinate getNextPosition(int direction, double moveLength) {
        var radians = Math.toRadians(direction);
        // Use trigonometry to calculate the longitude and latitude values
        var xValue = moveLength * Math.cos(radians);
        var yValue = moveLength * Math.sin(radians);
        var updatedPosition = new Coordinate(this.latitude + yValue, this.longitude + xValue);
        return updatedPosition;
    }
    
    /*
     * A useful function that represents the coordinate as a string
     */
    public String toString() {
        return (latitude + ", "  + longitude);
    }
}
