package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/**
 * Coordinate class, as pairs of latitude and longitude values
 *
 */
public class Coordinate {
	
    private final double latitude;
	private final double longitude;
	
	// Constructor for Coordinate
	public Coordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	// Getters and Setters
	public double getLatitude() {
		return this.latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	/*
	 * Check if the coordinate is in the confinement area
	 */
	public boolean isInConfinementZone() {
		var permittedLatitude = 55.942617 < this.latitude && this.latitude < 55.946233;
		var permittedLongitude = -3.192473 < this.longitude && this.longitude < -3.184319;
		return (permittedLatitude && permittedLongitude);
	}
	
    /*
     *  Calculate Euclidean distance between currentNode and sensor
     */
    public double getEuclideanDistance(Coordinate coordinate) { 
        var y1 = this.latitude;
        var x1 = this.longitude;
        var y2 = coordinate.getLatitude();
        var x2 = coordinate.getLongitude();
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }
    
    /*
     * Calculate the angle to the passed-in coordinate.
     */
    public int getAngle(Coordinate destination) {
        var yDistance = destination.getLatitude() - this.latitude;
        var xDistance = destination.getLongitude() - this.longitude;
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
        // Round the angle up or down to the corresponding multiple of 10
        var angleRoundedDown = (int) (angleFromEast - angleFromEast % 10);
        var angleRoundedUp = (int) ((10 - angleFromEast % 10) + angleFromEast);
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
     * Convert a Coordinate into a Point with the same latitude and longitude
     */
    public Point toPoint() {
        var point = Point.fromLngLat(this.longitude, this.latitude);
        return point;
    }
    
    /*
     * Equality comparison method
     */
    public boolean equals(Coordinate coordinate) {
        return ((this.latitude == coordinate.getLatitude()) 
                && (this.longitude == coordinate.getLongitude()));
    }
    
    /*
     * A useful function that represents the coordinate as a string
     */
    public String toString() {
        return (this.latitude + ", "  + this.longitude);
    }
}
