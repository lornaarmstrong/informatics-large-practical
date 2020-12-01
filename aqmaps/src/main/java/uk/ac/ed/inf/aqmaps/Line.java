package uk.ac.ed.inf.aqmaps;

/**
 * This class represents an object as Line, with two sets of coordinates (one for each end of the 
 * line) as attributes and a threshold to determine tolerance when comparing double values.
 */
public class Line {

    private final Coordinate coordinateA;
    private final Coordinate coordinateB;
    private static final double THRESHOLD = 0.0000000000001;
    
    public Line(Coordinate coordinateA, Coordinate coordinateB) {
        this.coordinateA = coordinateA;
        this.coordinateB = coordinateB;
    }
    
    // Getters and Setters
    public Coordinate getCoordinateA() {
        return coordinateA;
    }
    
    public Coordinate getCoordinateB() {
        return coordinateB;
    }

    /*
     * Checks if the line intersects with a passed-in line
     */
    public boolean isIntersecting(Line boundary) {
        
        var X1 = this.coordinateA.getLongitude();
        var Y1 = this.coordinateA.getLatitude();
        var X2 = this.coordinateB.getLongitude();
        var Y2 = this.coordinateB.getLatitude();
      
        var X3 = boundary.getCoordinateA().getLongitude();
        var Y3 = boundary.getCoordinateA().getLatitude();
        var X4 = boundary.getCoordinateB().getLongitude();
        var Y4 = boundary.getCoordinateB().getLatitude();
        
        // Check if there is any overlap in the X values of the two lines
        if ( Math.max(X1, X2) < Math.min(X3, X4)) {
            return false;
        }
        // Check if either line has an infinite gradient
        // If so, check if the points of the other line lie either side of the line.
        else if ( X1 - X2 == 0 ) {
            if ( Math.min(X3, X4) <= X1 && Math.max(X3, X4) >= X1) {
                return true;
            }
        } 
        else if (X3 - X4 == 0) {
            if (Math.min(X1, X2) <= X3 && Math.max(X1, X2) >= X3){
                return true;
            }
        }
        // Use y = mx + c to form the infinite lines passing through the line segments.
        else {
            var m1 = (Y1 - Y2) / ( X1 - X2);
            var m2 = (Y3 - Y4) / (X3 - X4);
            var c1 = Y1 - (m1 * X1);
            var c2 = Y3 - (m2 * X3);
            
            // Check if the two lines have the same gradient
            if (m1 == m2) {
                return false;
            }
            
            // Check if (X,Y) lies on both lines and is the intersection point.
            var intersectX = (c2 - c1) / (m1 - m2);
            var possibleY = (m1 * intersectX) + c1;
            var possibleY2 = (m2 * intersectX) + c2;
            
            // Check if (X,Y) lies in the interval of the line segments
            if (Math.abs(possibleY - possibleY2) < THRESHOLD) {
              if ( intersectX < Math.max(Math.min(X1, X2), Math.min(X3, X4)) 
                      || intersectX > Math.min(Math.max(X1, X2), Math.max(X3, X4))) {
                  return false;
              } else {
                  return true;
              }
            }
        }
        return false;
    }

    /*
     * Useful method for use with testing and debugging
     */
    public String toString() {
        return (coordinateB.toString() + " -> " + coordinateA.toString());
    }
}
