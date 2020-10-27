package uk.ac.ed.inf.aqmaps;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.Gson;

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
	
	/*
	 *  Find the latitude and longitude for the what3words location, returned as a Coordinate
	 */
	public Coordinate getCoords() throws IOException, InterruptedException {
		
		// Split the what3words into the 3 separate words
		var words = location.split("\\.");
		
		// Send a GET request to the server to get the what3words details
        var client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        		.uri(URI.create("http://localhost:80/words/" + words[0] + "/" + words[1] + "/" + words[2] + "/details.json"))
        		.build();
        var response = client.send(request, BodyHandlers.ofString());
        
        // Check the response.statusCode()
        if (response.statusCode() == 200) {
        	System.out.println("Response recieved correctly, server is working.");
        	// Get the latitude and longitude and make it a Coordinate
        	var word = new Gson().fromJson(response.body(), Word.class);
        	// Get the coordinates of the location
        	var longitude = word.getCoordinates().getLng();
        	var latitude = word.getCoordinates().getLat();
        	Coordinate coordinate = new Coordinate(latitude, longitude);
        	return coordinate;
        } else {
        	System.out.println("Error - response not found.");
        }
        
		return null;
	}
}
