package uk.ac.ed.inf.aqmaps;

/**
 * Coordinate class, as pairs of latitude and longitude values
 *
 */
public class Coordinate {
	
	private double latitude;
	private double longitude;
	
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
	
	/**
	 * Check if the coordinate is in the confinement area
	 */
	public boolean isInConfinementZone() {
		boolean permittedLatitude;
		boolean permittedLongitude;
		
		permittedLatitude = 55.942617 < latitude && latitude < 55.946233;
		permittedLongitude = -3.192473 < longitude && longitude < -3.184319;
		
		return (permittedLatitude && permittedLongitude);
	}
	
	// TODO method:
	// - add if in disallowed building area...
}
