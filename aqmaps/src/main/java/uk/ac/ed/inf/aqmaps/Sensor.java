package uk.ac.ed.inf.aqmaps;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import com.google.gson.Gson;
import com.mapbox.geojson.Point;

import java.io.IOException;
import java.net.URI;

/**
 * Sensor class to represent a sensor for air pollution levels
 *
 */
public class Sensor {
	
	private String location;
	private Coordinate position;
	private double battery;
	private String reading;
	
	public Sensor(String location, double battery, String reading) {
		this.location = location;
		this.battery = battery;
		this.reading = reading;
		this.position = null;
	}
	
	// Getters and Setters
	public String getLocation() {
		return this.location;
	}
	
	public void setLocation(String location) {
		this.location = location;
	}
	
    public Coordinate getPosition() {
        return this.position;
    }
    
    public void setPosition(double latitude, double longitude) {
        this.position = new Coordinate(latitude, longitude);
    }
	
	public double getBattery() {
		return this.battery;
	}
	
	public void setBattery(Double battery) {
		this.battery = battery;
	}
	
	public String getReading() {
		return this.reading;
	}
	
	public void setReading(String reading) {
		this.reading = reading;
	}
	
	/*
	 *  Find the latitude and longitude for the what3words location, returned as a Coordinate
	 *  using the server.
	 */
	public void translateWhat3Words() throws IOException, InterruptedException {
	        var words = location.split("\\.");
	    
	        var client = HttpClient.newHttpClient();
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create("http://localhost:" + App.portNumber + "/words/" + words[0] + "/" 
	                     + words[1] + "/" + words[2] + "/details.json"))
        		    .build();
	        var response = client.send(request, BodyHandlers.ofString());
	        // Check the response.statusCode()
	        int statusCode = response.statusCode();
	        if (statusCode == 200) {
	            // Get the latitude and longitude and make it a LngLat
	            var word = new Gson().fromJson(response.body(), Word.class);
	            // Get the coordinates of the location
	            var longitude = word.getCoordinates().getLng();
                var latitude = word.getCoordinates().getLat();
                
                // Add the latitude and longitude to the sensor
                setPosition(latitude, longitude);

	        } else if (statusCode == 404){
	            // There is an error with the request
	            System.out.println("The server cannot find the requested resource [error 404]");
	        } else {
	            System.out.println("The status code is " + statusCode);
	    }
	}
}
