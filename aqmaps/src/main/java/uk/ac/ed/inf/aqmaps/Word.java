package uk.ac.ed.inf.aqmaps;

/*
 * A class to contain the Word objects when the JSON
 * is parsed from the server.
 */
public class Word {
    
	private Coordinate coordinates;
	
	// Getters and Setters
	public Coordinate getCoordinates() {
		return this.coordinates;
	}
	
	public void setCoordinates(Coordinate coordinates) {
		this.coordinates = coordinates;
	}
}
