package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.mapbox.geojson.Point;

/**
 * Unit test for Sensor class.
 */
public class SensorTest 
{
	// Objects used for the tests
	Sensor sensor1 = new Sensor("acid.chair.butter", 60.0, "50.0");
	Sensor sensor2 = new Sensor("acid.high.butter", 60.0, "50.0");
	
	/*
	 * A sensor with a what3words value that is on the server should return
	 * the point object containing the corresponding latitude and longitude.
	 */
//    @Test
//    public void testGetCoordinate() throws IOException, InterruptedException
//    {
//        Coordinate point1 = new Coordinate(55.944575, -3.185236);
//        System.out.println(sensor1.getCoordinate().latitude);
//        System.out.println(sensor1.getCoordinate().longitude);
//        assertTrue(sensor1.getCoordinate().equals(point1));
//    }
    
    /*
	 * A sensor with a what3words value that is NOT on the server should return
	 * null
	 */
//    @Test
//    public void testNoCoordinates() throws IOException, InterruptedException
//    {
//        assertTrue(sensor2.getCoordinate() == null);
//    }
    
    // TODO further tests
    // what happens if the what3words isn't in the server?
}
