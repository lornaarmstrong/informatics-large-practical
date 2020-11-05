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
        var start = new Coordinate(startLatitude, startLongitude);
        System.out.println("Drone's starting location: " + start.getLatitude() + " " 
                + start.getLongitude());
        
        // Create drone instance
        var drone = new Drone(start);
        
        // Get the list of sensors and no-fly zones
        sensorList = getSensorList(day, month, year);
        noFlyZones = getNoFlyZoneList();    
   
        // 1. Add start node to the path
        pathCoordinates.add(start);
        var startPoint = Point.fromLngLat(start.getLongitude(), start.getLatitude());
        System.out.println("Start Node");
        System.out.println("Lng: " + start.getLongitude());
        System.out.println("Lat: " + start.getLatitude());
        directRoute.add(startPoint);
        
        // 2. Find nearest node J and build the partial tour (I, J)
        var nearestSensor = findNearestNode(startPoint);
        var nearestSensorPoint = Point.fromLngLat(nearestSensor.getCoordinates().getLongitude(), nearestSensor.getCoordinates().getLatitude());
        // Add the point of the sensor to the path
        directRoute.add(nearestSensorPoint);
        System.out.println("Nearest Sensor Lat: " + nearestSensor.getCoordinates().getLatitude());
        System.out.println("Nearest Sensor Lng: " + nearestSensor.getCoordinates().getLongitude());
        // Add sensor to list of visited nodes
        visitedSensorList.add(nearestSensor);
        
        // while the drone still has moves left ...
        // while we haven't visited all nodes ... 
        while (visitedSensorList.size() < sensorList.size()) {
            // 3. Select random node (N) that is not yet visited
            int sensorCount = 0;
            for (Sensor sensor : sensorList) {
                System.out.println("Checking sensors " + sensorCount);
                sensorCount++;
                if (!visitedSensorList.contains(sensor)) {
                    // 4. Choose the edge (i,j) from the current path with the minimum value of:
                    // distance(i, N) + distance (N, j) - distance(i, j)
                    // distanceIJ = getEuclideanDistance();
                    var minimum = 0.0;
                    int count = 0;
                    Point nodeN = Point.fromLngLat(sensor.getCoordinates().getLongitude(), sensor.getCoordinates().getLatitude());
                    Point nodeI = null;
                    Point nodeJ = null;
                    
                    // loop through all edges in path
                    System.out.println("Loop through all edges in path");
                    for (int i = 0; i < directRoute.size() - 1; i++) {
                        // Consider an i and a j node
                        System.out.println("I: " + i);
                        Point temporaryNodeI = directRoute.get(i);
                        System.out.println("temp I: " + temporaryNodeI.longitude() + " " + temporaryNodeI.latitude());
                        Point temporaryNodeJ = directRoute.get(i+1);
                        System.out.println("temp J: " + temporaryNodeJ.longitude() + " " + temporaryNodeJ.latitude());
                        double distanceIJ = getEuclideanDistance(temporaryNodeI, temporaryNodeJ);
                        double distanceIN = getEuclideanDistance(temporaryNodeI, nodeN);
                        double distanceNJ = getEuclideanDistance(nodeN, temporaryNodeJ);
                        // calculate d(I,N) + d(N,J) - d(I,J)
                        double formulaResult = distanceIN + distanceNJ - distanceIJ;
                        // if this is the first edge considered, we assume minimum
                        if (count == 0) {
                            minimum = formulaResult;
                        }
                        // if the result of the formula for this edge is less than minimum,
                        // update the nodeI and nodeJ variables to store the new (i,j) edge
                        if (formulaResult <= minimum) {
                            minimum = formulaResult;
                            nodeI = temporaryNodeI;
                            nodeJ = temporaryNodeJ;
                        }
                    }
                    
                    // Insert the sensor into the path between nodes I and J
                    // Loop through directRoute to find where to insert
                    for (int i = 0; i < directRoute.size(); i++) {
                        // when you find nodeI
                        Point node = directRoute.get(i);
                        
                        if (node.latitude() == nodeI.latitude() && node.longitude() == nodeI.longitude()) {
                            directRoute.add(i+1, nodeN);
                            System.out.println("Point added! ");
                        }
                    }
                    
                    // add to visited nodes list
                    visitedSensorList.add(sensor);
                }
            }
        }
        
        // CHECKING -- PRINTING ALL SENSORS
        var markerFeatures = createMarkers();
        // CHECKING -- PRINTING START LOCATION
        var pointStart = Point.fromLngLat(startLongitude, startLatitude);
        var startGeometry = (Geometry) pointStart;
        var startFeature = Feature.fromGeometry(startGeometry);
        startFeature.addStringProperty("rgb-string", "#000000");
        startFeature.addStringProperty("marker-color", "#000000");
        startFeature.addStringProperty("marker-symbol", "lighthouse");
        markerFeatures.add(startFeature);
        // CHECKING -- PRINTING ALL PATH SO FAR
        var pathLine = LineString.fromLngLats(directRoute);
        var pathGeometry = (Geometry) pathLine;
        var pathFeature = Feature.fromGeometry(pathGeometry);
        markerFeatures.add(pathFeature);
        var allMarkers = FeatureCollection.fromFeatures(markerFeatures);
        writeFile("sensorMap.geojson", allMarkers.toJson());
        
//        // Create output files
//        String flightpathFile = "flightpath" + "-" + day + "-" + month + "-" + year + ".txt";
//        PrintWriter writer = new PrintWriter(flightpathFile, "UTF-8");
//        String readingsFile = "readings" + "-" + day + "-" + month + "-" + year +".geojson";
//        PrintWriter geoWriter = new PrintWriter(readingsFile, "UTF-8");
    }
    
    public static void writeFile(String filename, String json) throws IOException {
        System.out.println("Writing to file!");
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
            var coords = sensor.getCoordinates();
            var markerPoint = Point.fromLngLat(coords.getLongitude(), coords.getLatitude());
            var markerGeometry = (Geometry) markerPoint;
            var markerFeature = Feature.fromGeometry(markerGeometry);
            
            // Add features  (TODO make this a separate function when needed!)
            markerFeature.addStringProperty("rgb-string", "#00ff00");
            markerFeature.addStringProperty("marker-color", "#00ff00");
            markerFeature.addStringProperty("marker-symbol", "lighthouse");
            markerFeatures.add(markerFeature);
        }
        
        return markerFeatures;
    }

    public static Sensor findNearestNode(Point currentNode) throws IOException, InterruptedException {
        // Check through whole list of not yet added coordinates
        var shortestDistance = 0.0;
        Sensor nextNode = null; // default to null
        var counter = 0;
        
        // Loop through the sensors not yet visited and find the closest to the currentNode
        for (Sensor sensor : sensorList) {
            // Calculate the distance between the sensor and the start point
            Point sensorPoint = Point.fromLngLat(sensor.getCoordinates().getLongitude(), sensor.getCoordinates().getLatitude());
            var distance = getEuclideanDistance(currentNode, sensorPoint);
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
    public static double getEuclideanDistance(Point currentNode, Point nextNode) { 
        double x1 = currentNode.latitude();
        double y1 = currentNode.longitude();
        double x2 = nextNode.latitude();
        double y2 = nextNode.longitude();
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }

    /*
     * Get the list of all sensors to be visited on the given date (from input) 
     */
    public static List<Sensor> getSensorList(String day, String month, String year) 
            throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://localhost:%d/maps/%s/%s/%s/air-quality-data.json", 
                        portNumber,year,month,day)))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        
        var listType = new TypeToken<ArrayList<Sensor>>(){}.getType();
        List<Sensor> sensorsForThatDay = new Gson().fromJson(response.body(), listType);
        return sensorsForThatDay;
    }
    
    /*
     * Get the list of all polygons representing the buildings in the no-fly zone 
     */
    public static FeatureCollection getNoFlyZoneList() throws IOException, InterruptedException {

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + portNumber + "/buildings/no-fly-zones.geojson"))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        FeatureCollection features = FeatureCollection.fromJson(response.body());
        return features;
    }
   
    
}
