package uk.ac.ed.inf.aqmaps;

/**
 * This class represents objects as Lines, with two sets
 * coordinates
 */
public class Line {

    private Coordinate coordinateA;
    private Coordinate coordinateB;
    
    // Constructor
    public Line(Coordinate coordinateA, Coordinate coordinateB) {
        this.coordinateA = coordinateA;
        this.coordinateB = coordinateB;
    }
    
    // Getters and Setters
    public Coordinate getCoordinateA() {
        return coordinateA;
    }
  
    public void setCoordinateA(Coordinate coordinateA) {
        this.coordinateA = coordinateA;
    }
    
    public Coordinate getCoordinateB() {
        return coordinateB;
    }
    
    public void setCoordinateB(Coordinate coordinateB) {
        this.coordinateB = coordinateB;
    }
    
    /*
     * Useful method for use with testing and debugging
     */
    public String toString() {
        return ("line: " + coordinateB.toString() + " to " + coordinateA.toString());
    }
}
