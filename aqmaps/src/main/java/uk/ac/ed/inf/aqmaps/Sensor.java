package uk.ac.ed.inf.aqmaps;

/**
 * Sensor class to represent a sensor for air pollution levels
 *
 */
public class Sensor {
	
	private String location;
	private double batteryPercentage;
	private double reading;
	
	public Sensor(String location, double batteryPercentage, double reading) {
		this.location = location;
		this.batteryPercentage = batteryPercentage;
		this.reading = reading;
	}
	
	// Getters and Setters
	public String getLocation() {
		return this.location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public double getBatteryPercentage() {
		return this.batteryPercentage;
	}
	
	public void setBatteryPercentage(Double batteryPercentage) {
		this.batteryPercentage = batteryPercentage;
	}
	
	public double getReading() {
		return this.reading;
	}
	
	public void setReading(Double reading) {
		this.reading = reading;
	}
}
