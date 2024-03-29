package uk.ac.ed.inf.aqmaps;

import com.google.gson.annotations.SerializedName;
import com.mapbox.geojson.Point;

/*
 * Coordinate class where a Coordinate is a pair of latitude and longitude values
 */
public class Coordinate {
	
    @SerializedName("lat")
    private final double latitude;
    @SerializedName("lng")
	private final double longitude;
	
	public Coordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	// Getters
	public double getLatitude() {
		return this.latitude;
	}
	
	public double getLongitude() {
		return this.longitude;
	}
	
	/*
	 * Check if the coordinate is in the confinement area
	 */
	public boolean isInConfinementZone(Coordinate topLeft, Coordinate bottomLeft, 
	        Coordinate bottomRight) {
		var permittedLatitude = bottomLeft.getLatitude() < this.latitude
		        && this.latitude < topLeft.getLatitude();
		var permittedLongitude = bottomLeft.getLongitude() < this.longitude
		        && this.longitude < bottomRight.getLatitude();
		return (permittedLatitude && permittedLongitude);
	}
	
    /*
     *  Calculate Euclidean distance between current Coordinate and passed-in Coordinate
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
     * Calculate the angle to the passed-in Coordinate.
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
     * Get the next Coordinate based on the direction and distance
     * from the current Coordinate
     */
    public Coordinate getNextPosition(int angle, double moveLength) {
        var radians = Math.toRadians(angle);
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
