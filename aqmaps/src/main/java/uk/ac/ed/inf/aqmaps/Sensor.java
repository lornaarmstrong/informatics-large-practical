package uk.ac.ed.inf.aqmaps;

/**
 * Sensor class to represent a sensor for air pollution levels
 *
 */
public class Sensor {
	
	private final String location;
	private final double batteryPercentage;
	private final double reading;
	
	public Sensor(String location, double batteryPercentage, double reading) {
		this.location = location;
		this.batteryPercentage = batteryPercentage;
		this.reading = reading;
	}

}
