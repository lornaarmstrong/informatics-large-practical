package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.FeatureCollection;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

/**
 * App class for the autonomous drone, collecting sensor readings for air quality.
 *
 */
public class App 
{
	
	// Initialise Variables
	public static List<Sensor> sensorList = new ArrayList<Sensor>();
	public static FeatureCollection noFlyZones;
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {
        // Get the input
        String day = args[0];
        String month = args[1];
        String year = args[2];
        double startLatitude = Double.parseDouble(args[3]);
        double startLongitude = Double.parseDouble(args[4]);   
        int seed = Integer.parseInt(args[5]);
        int portNumber = Integer.parseInt(args[6]);
    	
        sensorList = getSensorList(day, month, year);
        noFlyZones = getNoFlyZoneList();
       
    }

    /*
     * Get the list of all sensors to be visited on the given date (from input) 
     */
	public static List<Sensor> getSensorList(String day, String month, String year) 
				throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        		.uri(URI.create("http://localhost:80/maps/" + year + "/" + month + "/" + day
        				+ "/air-quality-data.json"))
        		.build();
        var response = client.send(request, BodyHandlers.ofString());
        
        Type listType = new TypeToken<ArrayList<Sensor>>(){}.getType();
        List<Sensor> sensorsForThatDay = new Gson().fromJson(response.body(), listType);
		return sensorsForThatDay;
	}
	
    /*
     * Get the list of all polygons representing the buildings in the no-fly zone 
     */
	public static FeatureCollection getNoFlyZoneList() throws IOException, InterruptedException {
		var client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
        		.uri(URI.create("http://localhost:80/buildings/no-fly-zones.json"))
        		.build();
        var response = client.send(request, BodyHandlers.ofString());
        FeatureCollection features = new Gson().fromJson(response.body(), FeatureCollection.class);
        return features;
	}
}
