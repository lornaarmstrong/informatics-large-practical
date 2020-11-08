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
    @Test
    public void testGetPoint() throws IOException, InterruptedException
    {
        Point point1 = Point.fromLngLat(-3.185236, 55.944575);
        assertTrue(sensor1.getPoint().equals(point1));
    }
    
    /*
	 * A sensor with a what3words value that is NOT on the server should return
	 * null
	 */
    @Test
    public void testNoPoints() throws IOException, InterruptedException
    {
        assertTrue(sensor2.getPoint() == null);
    }
    
    // TODO further tests
    // what happens if the what3words isn't in the server?
}
