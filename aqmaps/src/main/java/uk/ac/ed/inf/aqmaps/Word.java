package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class Word {

	// Matching the words details.json structure from the server
	private String country;
	private Square square;
	private String nearestPlace;
	private LngLat coordinates;
	private String words;
	private String language;
	private String map;
	
	// Getters and Setters
	
	public LngLat getCoordinates() {
		return this.coordinates;
	}
	
	public void setCoordinates(LngLat coordinates) {
		this.coordinates = coordinates;
	}
	
	
}
