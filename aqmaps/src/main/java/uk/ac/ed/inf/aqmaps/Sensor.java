package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;

/*
 * Sensor class to represent a sensor for air pollution levels
 */
public class Sensor {
	
	private final String location;
	private Coordinate coordinate;
	private final double battery;
	private final String reading;
	private static final double RANGE = 0.0002;
	
	public Sensor(String location, double battery, String reading) {
		this.location = location;
		this.battery = battery;
		this.reading = reading;
		this.coordinate = null;
	}
	
	// Getters and Setters
	public String getLocation() {
		return this.location;
	}
	
    public Coordinate getCoordinate() {
        return this.coordinate;
    }
	
    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }
    
	public double getBattery() {
		return this.battery;
	}
	
	public String getReading() {
		return this.reading;
	}
	
	public double getRange() {
	    return RANGE;
	}
	
	/*
	 * Translate the What3Words location to the corresponding Coordinate for its 
	 * latitude and longitude
	 */
	public void translateLocation() {
	    var words = location.split("\\.");
	    var client = HttpClient.newHttpClient();
	    var request = HttpRequest.newBuilder()
	            .uri(URI.create("http://localhost:" + App.portNumber + "/words/" + words[0]
	                    + "/" + words[1] + "/" + words[2] + "/details.json"))
	            .build();
	    HttpResponse<String> response;
	    try {
            response = client.send(request, BodyHandlers.ofString());
            var statusCode = response.statusCode();
            if (statusCode == 200) {
                // Get the coordinates of the location and set the sensor's position to this.
                var word = new Gson().fromJson(response.body(), Word.class);
                var coordinate = word.getCoordinates();
                setCoordinate(coordinate);
            } else if (statusCode == 404){
                System.out.println("The server cannot find the requested resource [error 404]");
            } else {
                System.out.println("The status code is " + statusCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
}
