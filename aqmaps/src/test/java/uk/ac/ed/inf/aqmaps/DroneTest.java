package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.mapbox.geojson.Point;

public class DroneTest {

    Point point1 = Point.fromLngLat(-3.1878, 55.94444);
    Drone drone1 = new Drone(point1);
    
    /*
     * When given a degree, should convert to radians
     */
    @Test
    public void testDegreeToRadians()
    {
        assertTrue(drone1.convertToRadians(90) == Math.PI/2);
    }
    
    /*
     * When given a degree of 0, should convert to 0 radians
     */
    @Test
    public void testDegreeToRadiansZero()
    {
        assertTrue(drone1.convertToRadians(0) == 0);
    }
    
    /*
     * When given a direction (angle, multiple of 10), the current position of drone
     * should be updated using trigonometry
     */
    @Test
    public void moveDroneEast() 
    {
        // angle is 0
        assertTrue(false);
    }
    
    @Test
    public void moveDroneNorth() 
    {
        // angle is 90
        assertTrue(false);
    }
    
//    @Test
//    public void moveDroneWest() 
//    {
//        // angle is 180
//        //assertTrue(drone1.moveDrone(180));
//    }
    
    @Test
    public void moveDroneSouth() 
    {
        // angle is 
        assertTrue(false);
    }
}
