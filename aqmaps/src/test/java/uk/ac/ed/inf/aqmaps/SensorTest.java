package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

/**
 * Unit test for Sensor class.
 */
public class SensorTest 
{
	// Objects used for the tests
	Sensor sensor1 = new Sensor("acid.chair.butter", 60.0, 50.0);
	
    /**
     * @throws InterruptedException 
     * @throws IOException 
     * 
     */
    @Test
    public void testGetCoordinates() throws IOException, InterruptedException
    {
        Coordinate coord1 = new Coordinate(55.944575, -3.185536);
        assertEquals( sensor1.getCoords(), coord1);
    }
}
