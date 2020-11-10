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
     * When given a direction of east, the current position of drone
     * should update as the drone moves horizontally East by 0.0003
     */
    @Test
    public void moveDroneEast() 
    {
        Point before = drone1.getPosition();
        drone1.moveDrone(0);
        assertTrue(drone1.getPosition().latitude() == before.latitude()
                && drone1.getPosition().longitude() == before.longitude() + 0.0003);
    }
    
    /*
     * When given a direction of north, the current position of drone
     * should update as the drone moves vertically north by 0.0003
     */
    @Test
    public void moveDroneNorth() 
    {
        // angle is 90
        Point before = drone1.getPosition();
        drone1.moveDrone(90);
        assertTrue(drone1.getPosition().latitude() == before.latitude() + 0.0003
                && drone1.getPosition().longitude() == before.longitude());
    }
    
    /*
     * When given a direction of west, the current position of drone
     * should update as the drone moves horizontally west by 0.0003
     */
    @Test
    public void moveDroneWest() 
    {
        Point before = drone1.getPosition();
        drone1.moveDrone(180);
        assertTrue(drone1.getPosition().latitude() == before.latitude()
                && drone1.getPosition().longitude() == before.longitude() - 0.0003);
    }
    
    /*
     * When given a direction of south, the current position of drone
     * should update as the drone moves vertically south by 0.0003
     */
    @Test
    public void moveDroneSouth() 
    {
        Point before = drone1.getPosition();
        drone1.moveDrone(270);
        assertTrue(drone1.getPosition().latitude() == before.latitude() - 0.0003
                && drone1.getPosition().longitude() == before.longitude());
    }
}
