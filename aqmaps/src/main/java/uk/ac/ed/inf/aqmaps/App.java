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
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * App class for the autonomous drone, collecting sensor readings for air quality.
 *
 */
public class App 
{
    // Initialise Variables
    public static List<Sensor> sensorList = new ArrayList<Sensor>();
    public static List<Point> visitedPointList = new ArrayList<>();
    public static FeatureCollection noFlyZones;
    public static int portNumber;
    private static Drone drone;
    public static List<Point> path = new ArrayList<Point>();
    
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
        
        // Get the list of sensors and no-fly zones
        List<Sensor> sensorsForTheDay = getSensorList(day, month, year);
        sensorList = sensorsForTheDay.subList(0, 33);
        
        // Create the drone's starting point and drone instance
        var startPoint = Point.fromLngLat(startLongitude, startLatitude);
        var drone = new Drone(startPoint);
        System.out.println("Drone start: " + startPoint.longitude() + " " + startPoint.latitude());
        
        // DEBUGGING -- Print out the coordinates of the sensors to check
//        int num = 1;
//        System.out.println("Sensors to be checked on this day:  ");
//        for (Sensor sensor : sensorList) {
//            System.out.println("Sensor " + num + "--- " + sensor.getPoint().latitude() + "," + sensor.getPoint().longitude());
//            num++;
//        }
        noFlyZones = getNoFlyZoneList();    
        
        // 1. Add start node to the path and mark as visited point
        path.add(startPoint);
        visitedPointList.add(startPoint);
        
       
        // 2. Find nearest node J, move to it, and build the partial tour (I, J)
        var nearestSensor = findNearestNode(startPoint);
        var nearestSensorPoint = nearestSensor.getPoint();
        path.add(nearestSensorPoint);
        visitedPointList.add(nearestSensor.getPoint());
//        System.out.println("Nearest Sensor Lat: " + nearestSensorPoint.latitude());
//        System.out.println("Nearest Sensor Lng: " + nearestSensorPoint.longitude() + "\n");
//        
//        System.out.println("Contents of visitedSensorList");
        
//        // Print out all the nodes in visitedPointList
//        System.out.println("VISITED POINTS:");
//        for (Point point: visitedPointList) {
//            System.out.println(point.latitude() + " " + point.longitude());
//        }
       
        
        // Until we have visited every node ...
        while (visitedPointList.size() < sensorList.size() + 1) {
            // Find the sensor (not yet visited) that is nearest to a sensor in the visited list
            Sensor nextSensorToInclude = selectNearestSensor();
            //System.out.println("Inserting into the path the sensor: " + nextSensorToInclude.getPoint().latitude() + ", " + nextSensorToInclude.getPoint().longitude() );
            insertIntoPath(nextSensorToInclude);
        }
        
        // Back to start
        path.add(startPoint);
        
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
        var pathLine = LineString.fromLngLats(path);
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
    
    /*
     * Inserts sensor N into path such that d(I,
     */
    public static void insertIntoPath(Sensor nextSensorToInclude) throws IOException, InterruptedException {
        var minimum = 0.0;
        int count = 0;
        Point nodeN = nextSensorToInclude.getPoint();
        Point nodeI = null;
        Point nodeJ = null;
        
        for (int i = 0; i < path.size() - 1; i ++) {
            // Consider a node I and J
            Point temporaryNodeI = path.get(i);
            Point temporaryNodeJ = path.get(i + 1);
            double distanceIJ = getEuclideanDistance(temporaryNodeI, temporaryNodeJ);
            double distanceIN = getEuclideanDistance(temporaryNodeI, nodeN);
            double distanceNJ = getEuclideanDistance(nodeN, temporaryNodeJ);
            // calculate d(I,N) + d(N,J) - d(I,J)
            double formulaResult = distanceIN + distanceNJ - distanceIJ;
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
        for (int j = 0; j < path.size(); j++) {
            // when you find nodeI
            Point node = path.get(j);
          
            if (node.latitude() == nodeI.latitude() && node.longitude() == nodeI.longitude()) {
                path.add(j+1, nodeN);
            }
        }
      
      // add to visited nodes list
      visitedPointList.add(nextSensorToInclude.getPoint());
    }
    
    /*
     * Find the sensor that is closest to any sensor currently in visitedSensorList
     */
    public static Sensor selectNearestSensor() throws IOException, InterruptedException {
        Sensor nextSensorToInclude = null;
        
        // Create a hashmap to store the shortest distance from each sensor (not in path)
        // to a sensor in the path (the sensor in particular doesn't matter)
        HashMap<Sensor, Double> sensorDistancePair = new HashMap<Sensor, Double>();
        int sensorNumber = 0;
        
        //System.out.println("NOT IN PATH SENSORS");
        List<Sensor> notInPathSensors = new ArrayList<Sensor>();
        // get sensors not yet incuded in path
        for (Sensor sensor: sensorList) {
            if(!(visitedPointList.contains(sensor.getPoint()))) {
                notInPathSensors.add(sensor);
                //System.out.println(sensor.getPoint().latitude() + ", " + sensor.getPoint().longitude());
            }
        }
        
        
        for (Sensor currentSensor: sensorList) {
            if (!visitedPointList.contains(currentSensor.getPoint())) {
                //System.out.println("We found a sensor not yet in the path. Let's calculate shortest distance to another node");
                double shortestDistance = 0.0;
                double distance = 0.0;
                sensorNumber++;
                // calculate distance to each node in path and save the shortest
                for (int i = 0; i < visitedPointList.size(); i++) {
                    //System.out.println(visitedPointList.size());
                    Point pointInPath = visitedPointList.get(i);
                    Point sensorPointNotInPath = currentSensor.getPoint();
                    distance = getEuclideanDistance(pointInPath, sensorPointNotInPath);
                    //System.out.println(distance + " (i = " + i + ")");
                    if (i == 0) {
                        shortestDistance = distance;
                    } else {
                       if (distance < shortestDistance) {
                           shortestDistance = distance;
                       }
                    } 
                }
                // Add the sensor we checked the distances of, and the shortest distance
                sensorDistancePair.put(currentSensor, shortestDistance);
                //System.out.println("SENSOR " + sensorNumber + " - SHORTEST DISTANCE");
                //System.out.println(currentSensor.getPoint().longitude() + ", " + currentSensor.getPoint().latitude() + " d: " + shortestDistance);
            }
        }
        
        nextSensorToInclude = Collections.min(sensorDistancePair.entrySet(), Map.Entry.comparingByValue()).getKey();
        //System.out.println("Closest sensor to any remaining node");
        //System.out.println(nextSensorToInclude.getPoint().latitude());
        //System.out.println(nextSensorToInclude.getPoint().longitude());
        return nextSensorToInclude;
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
            var markerPoint = sensor.getPoint();
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
        //System.out.println("Loop through the not-yet-visited sensors");
        for (Sensor sensor : sensorList) {
            //System.out.println("Sensor being checked!");
            Point sensorPoint = sensor.getPoint();
            var distance = getEuclideanDistance(currentNode, sensorPoint);
            if (counter == 0) {
                shortestDistance = distance;
            }
            if (distance < shortestDistance) {
                shortestDistance = distance;
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
   
    /*
     * Check if a given point is in the confinement zone
     */
    public boolean isInConfinementZone(Point point) {
        boolean permittedLatitude;
        boolean permittedLongitude;
        permittedLatitude = 55.942617 < point.longitude() && point.latitude() < 55.946233;
        permittedLongitude = -3.192473 < point.longitude() && point.longitude() < -3.184319;
        return (permittedLatitude && permittedLongitude);
    }
}
