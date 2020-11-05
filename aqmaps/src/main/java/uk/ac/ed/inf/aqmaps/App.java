package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
    public static List<Point> directRoute = new ArrayList<Point>();
    
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
        
        // Get the list of sensors and no-fly zones
        sensorList = getSensorList(day, month, year);
        noFlyZones = getNoFlyZoneList();    
   
        // 1. Add start node to the path
        pathCoordinates.add(startPoint);
        Point start = Point.fromLngLat(startPoint.getLongitude(), startPoint.getLatitude());
        System.out.println("Start Node");
        System.out.println("Lng: " + startPoint.getLongitude());
        System.out.println("Lat: " + startPoint.getLatitude());
        directRoute.add(start);
        
        // 2. Find nearest node J and build the partial tour (I, J)
        Sensor nearestSensor = findNearestNode(startPoint);
        Point nearestSensorPoint = Point.fromLngLat(nearestSensor.getCoordinates().getLongitude(), nearestSensor.getCoordinates().getLatitude());
        directRoute.add(nearestSensorPoint);
        System.out.println("Nearest Sensor Lat: " + nearestSensor.getCoordinates().getLatitude());
        System.out.println("Nearest Sensor Lng: " + nearestSensor.getCoordinates().getLongitude());
       
        
        // CHECKING -- PRINTING ALL SENSORS
        ArrayList<Feature> markerFeatures = createMarkers();
        // CHECKING -- PRINTING START LOCATION
        Point pointStart = Point.fromLngLat(startLongitude, startLatitude);
        Geometry startGeometry = (Geometry) pointStart;
        Feature startFeature = Feature.fromGeometry(startGeometry);
        startFeature.addStringProperty("rgb-string", "#000000");
        startFeature.addStringProperty("marker-color", "#000000");
        startFeature.addStringProperty("marker-symbol", "lighthouse");
        markerFeatures.add(startFeature);
        // CHECKING -- PRINTING ALL PATH SO FAR
        LineString pathLine = LineString.fromLngLats(directRoute);
        Geometry pathGeometry = (Geometry) pathLine;
        Feature pathFeature = Feature.fromGeometry(pathGeometry);
        markerFeatures.add(pathFeature);
        FeatureCollection allMarkers = FeatureCollection.fromFeatures(markerFeatures);
        writeFile("sensorMap.geojson", allMarkers.toJson());
        
//        // Create output files
//        String flightpathFile = "flightpath" + "-" + day + "-" + month + "-" + year + ".txt";
//        PrintWriter writer = new PrintWriter(flightpathFile, "UTF-8");
//        String readingsFile = "readings" + "-" + day + "-" + month + "-" + year +".geojson";
//        PrintWriter geoWriter = new PrintWriter(readingsFile, "UTF-8");
    }
    
    public static void writeFile(String filename, String json) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        try {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.close();
    }

    public static ArrayList<Feature> createMarkers() throws IOException, InterruptedException {
        var markerFeatures = new ArrayList<Feature>();
        
        // Create a Feature for every marker in sensor list
        for (Sensor sensor: sensorList) {
            // Create a marker for that coordinate
            Coordinate coords = sensor.getCoordinates();
            Point markerPoint = Point.fromLngLat(coords.getLongitude(), coords.getLatitude());
            Geometry markerGeometry = (Geometry) markerPoint;
            Feature markerFeature = Feature.fromGeometry(markerGeometry);
            
            // Add features  (TODO make this a separate function when needed!)
            markerFeature.addStringProperty("rgb-string", "#00ff00");
            markerFeature.addStringProperty("marker-color", "#00ff00");
            markerFeature.addStringProperty("marker-symbol", "lighthouse");
            markerFeatures.add(markerFeature);
        }
        
        return markerFeatures;
    }

    public static Sensor findNearestNode(Coordinate currentNode) throws IOException, InterruptedException {
        // Check through whole list of not yet added coordinates
        double shortestDistance = 0;
        Sensor nextNode = null; // default to null
        int counter = 0;
        
        // Loop through the sensors not yet visited and find the closest to the currentNode
        for (Sensor sensor : sensorList) {
            // Calculate the distance between the sensor and the start point
            double distance = getEuclideanDistance(currentNode, sensor.getCoordinates());
            if (counter == 0) {
                shortestDistance = distance;
            }
            if ( distance < shortestDistance) {
                // update distance
                shortestDistance = distance;
                // set nextNode
                nextNode = sensor;
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
