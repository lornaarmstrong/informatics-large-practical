package uk.ac.ed.inf.aqmaps;

/*
 * A class to contain the Word objects when the JSON
 * is parsed from the server.
 */
public class Word {
    
	private LngLat coordinates;
	
	/* Getters and Setters */
	public LngLat getCoordinates() {
		return this.coordinates;
	}
	
	public void setCoordinates(LngLat coordinates) {
		this.coordinates = coordinates;
	}
}
