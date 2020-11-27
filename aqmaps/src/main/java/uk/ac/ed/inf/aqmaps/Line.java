package uk.ac.ed.inf.aqmaps;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * This class represents an object as Line, with two sets
 * coordinates (one for each end of the line) as attributes.
 */
public class Line {

    private Coordinate coordinateA;
    private Coordinate coordinateB;
    
    public Line(Coordinate coordinateA, Coordinate coordinateB) {
        this.coordinateA = coordinateA;
        this.coordinateB = coordinateB;
    }
    
    /* Getters and Setters */
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
     * Checks if this line (the drone's suggested movement) intersects with the passed-in boundary
     */
    public boolean isIntersecting(Line boundary) {
        /* Coordinates for the line representing the drone's suggested movement */
        var X1 = this.coordinateA.longitude;
        var Y1 = this.coordinateA.latitude;
        var X2 = this.coordinateB.longitude;
        var Y2 = this.coordinateB.latitude;
        /* Coordinates for the line representing the boundary */
        var X3 = boundary.getCoordinateA().longitude;
        var Y3 = boundary.getCoordinateA().latitude;
        var X4 = boundary.getCoordinateB().longitude;
        var Y4 = boundary.getCoordinateB().latitude;
        /* Check if there are any possible longitude values where the lines could intersect */
        if ( Math.max(X1, X2) < Math.min(X3, X4)) {
            return false;
        }
        /* If either line has an infinite gradient, check if one point of the other line 
        falls on either side. */
        if ( X1 - X2 == 0 ) {
            if ( Math.min(X3, X4) <= X1 && Math.max(X3, X4) >= X1) {
                return true;
            }
        } else if (X3 - X4 == 0) {
            if (Math.min(X1, X2) <= X3 && Math.max(X1, X2) >= X3){
                return true;
            }
        }
        /* Use the equation of a line, y = mx + c, to form the infinite lines passing through both
         * points. Check if the lines intersect at any point, then check if the point is within
         * the interval of the two line segments.
         */
        else {
            var m1 = (Y1 - Y2) / ( X1 - X2);
            var m2 = (Y3 - Y4) / (X3 - X4);
            var c1 = Y1 - (m1 * X1);
            var c2 = Y3 - (m2 * X3);
            
            /* Check if the two lines are parallel */
            if (m1 == m2) {
                return false;
            }
            
            /* A point (Xi, Yi) of intersection lying on both lines must fit both line equations */
            var Xi = (c2 - c1) / (m1 - m2);
            var Yi1 = (m1 * Xi) + c1;
            var Yi2 = (m2 * Xi) + c2;
            
            /* To account for imprecise double arithmetic, 'truncate' Yi2 and Yi1 to check if equal 
             * to 14 d.p. */
            var bigDecimalYi1 = new BigDecimal(Yi1).setScale(14, RoundingMode.DOWN);
            var bigDecimalYi2 = new BigDecimal(Yi2).setScale(14, RoundingMode.DOWN);
            
            /* If the point lies on both lines, check if it is within the X value interval of the
             * ling segments. */
            if (bigDecimalYi1.equals(bigDecimalYi2)) {
                if ((Xi < Math.max(Math.min(X1, X2), Math.min(X3, X4))) 
                        || (Xi > Math.min(Math.max(X1, X2), Math.max(X3, X4)))) {
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
        return ("line: " + coordinateB.toString() + " to " + coordinateA.toString());
    }
}
