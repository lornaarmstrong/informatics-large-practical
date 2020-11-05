package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.FeatureCollection;
import com.sun.tools.classfile.TypeAnnotation.Position;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

/*
 * App class for the autonomous drone, collecting sensor readings for air quality.
 *
 */
public class App 
{
    // Initialise Variables
    public static List<Sensor> sensorList = new ArrayList<Sensor>();
    public static List<Sensor> visitedSensorList = new ArrayList<>();
    public static FeatureCollection noFlyZones;
    public static int portNumber;
    private static Drone drone;
    public static List<Coordinate> pathCoordinates = new ArrayList<Coordinate>();
    
    public static void main( String[] args ) throws IOException, InterruptedException {
        // Get the input 
        String day = args[0];
        String month = args[1];
        String year = args[2];
        double startLatitude = Double.parseDouble(args[3]);
        double startLongitude = Double.parseDouble(args[4]);
        int seed = Integer.parseInt(args[5]);
        portNumber = Integer.parseInt(args[6]);
        
        // TODO add input validation / checks
        
        // Create a coordinate for the drone start position
        Coordinate startPoint = new Coordinate(startLatitude, startLongitude);
        System.out.println("Drone's starting location: " + startPoint.getLatitude() + " " 
                + startPoint.getLongitude());
        
        // Create drone instance
        var drone = new Drone(startPoint);
        
        // Add the start point to the list of path coordinates
        pathCoordinates.add(startPoint);
        
        // Get the list of sensors and no-fly zones
        sensorList = getSensorList(day, month, year);
        noFlyZones = getNoFlyZoneList();    
        
        
        // get start node
        // find nearest node to that
        Coordinate currentNode = startPoint;
        //Sensor nextSensor = findNearestNode(currentNode);
        
        //System.out.println("Next sensor: " + nextSensor.getCoordinates());
        
        
        // Print all sensors
        for (Sensor sensor: sensorList) {
            
        }
        
        
        // Create output files
        String flightpathFile = "flightpath" + "-" + day + "-" + month + "-" + year + ".txt";
        PrintWriter writer = new PrintWriter(flightpathFile, "UTF-8");
        String readingsFile = "readings" + "-" + day + "-" + month + "-" + year +".geojson";
        PrintWriter geoWriter = new PrintWriter(readingsFile, "UTF-8");
    }
    
    public static Sensor findNearestNode(Coordinate currentNode) throws IOException, InterruptedException {
        // Check through whole list of not yet added coordinates
        // for every sensor in the list of not yet visited sensors,
        double shortestDistance = 0;
        Sensor nextNode = null;
        
        int counter = 0;
        
        // Loop through the sensors not yet visited and find the closest to the currentNode
        for (Sensor sensor : sensorList) {
            double distance = getEuclideanDistance(currentNode, sensor.getCoordinates());
            if (counter == 0) {
                shortestDistance = distance;
            }
            if ( distance < shortestDistance) {
                shortestDistance = distance;
                
            }
            counter++;
        }
        return nextNode;
    }

    /*
     *  Calculate Euclidean distance between currentNode and sensor
     */
    public static double getEuclideanDistance(Coordinate currentNode, Coordinate nextNode) { 
        double x1 = currentNode.getLatitude();
        double y1 = currentNode.getLongitude();
        double x2 = nextNode.getLatitude();
        double y2 = nextNode.getLongitude();
        double distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }

    /*
     * Get the list of all sensors to be visited on the given date (from input) 
     */
    public static List<Sensor> getSensorList(String day, String month, String year) 
            throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://localhost:%d/maps/%s/%s/%s/air-quality-data.json", 
                        portNumber,year,month,day)))
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
                .uri(URI.create("http://localhost:" + portNumber + "/buildings/no-fly-zones.geojson"))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        FeatureCollection features = FeatureCollection.fromJson(response.body());
        return features;
    }
}
