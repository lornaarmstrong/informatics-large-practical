package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.*;

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
	public boolean equals(Coordinate coord) {
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
	public boolean isInNoFlyZone() {
	    for (Feature feature: App.noFlyZones) {
	        Polygon polygon = (Polygon) feature.geometry();
	        Point point = Point.fromLngLat(this.longitude, this.latitude);
	        boolean isInside = TurfJoins.inside(point, polygon);
	        if (isInside) {
	            System.out.println("Point is in no fly zone, adding to poinInZone");
	            App.pointsInZones.add(point);
	            return true;
	        }
	    }
	    return false;
	}
	
	/*
	 * A useful function that represents the coordinate as a string
	 */
	public String toString() {
	    return (latitude + ", "  + longitude);
	}

	/*
	 * Gets the next coordinate based on the direction and distance
	 */
    public Coordinate getNextPosition(int direction, double moveLength) {
        double radians = Math.toRadians(direction);
        Coordinate updatedPosition;
        // Use trigonometry to calculate the longitude and latitude values
        Double xValue = moveLength * Math.cos(radians);
        Double yValue = moveLength * Math.sin(radians);
        updatedPosition = new Coordinate(this.latitude + yValue, this.longitude + xValue);
        return updatedPosition;
    }
}
