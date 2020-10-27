package uk.ac.ed.inf.aqmaps;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.io.IOException;
import java.net.URI;

/**
 * Sensor class to represent a sensor for air pollution levels
 *
 */
public class Sensor {
	
	private String location;
	private double batteryPercentage;
	private double reading;
	private String latitude;
	private String longitude;
	
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
	
	// Find the latitude and longitude for the what3words location
	public Coordinate getCoords() throws IOException, InterruptedException {
		
		// Split the what3words into the 3 separate words
		String[] words = location.split("\\.");
		
		// Send a GET request to the server to get the what3words details
        var client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        		.uri(URI.create("http://localhost:80/words/" + words[0] + "/" + words[1] + "/" + words[2] + "/details.json"))
        		.build();
        
        var response = client.send(request, BodyHandlers.ofString());
        
        // Check the response.statusCode()
        if (response.statusCode() == 200) {
        	System.out.println("Response recieved correctly, server is working.");
        	System.out.println(response.body());
        	// Get the latitude and longitude and make it a Coordinate
        	
        	
        } else {
        	System.out.println("Error - response not found.");
        }
        
		return null;
        
	}
}
