package uk.ac.ed.inf.aqmaps;

import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
	
	//Initialise Variables
	public static List<Sensor> sensorList = new ArrayList<Sensor>();
	
    public static void main( String[] args )
    {
        // Get the input
        String day = args[0];
        String month = args[1];
        String year = args[2];
        double startLatitude = Double.parseDouble(args[3]);
        double startLongitude = Double.parseDouble(args[4]);
        int seed = Integer.parseInt(args[5]);
        int portNumber = Integer.parseInt(args[6]);
       
    }
}
