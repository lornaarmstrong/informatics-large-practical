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
import java.util.Arrays;
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
    public static List<Sensor> sensorList = new ArrayList<Sensor>(); // list of all sensors to be visited in the day
    public static List<Point> visitedPointList = new ArrayList<>();
    public static ArrayList<Sensor> sensorsInOrder = new ArrayList<Sensor>(); // list of all sensors in the order they should be visited
    public static FeatureCollection noFlyZones; // areas the drone cannot fly into
    public static int portNumber;
    public static Drone drone;
    public static List<Point> path = new ArrayList<Point>(); // the coordinates of the path of the drone
    public static List<Point> idealRoute = new ArrayList<Point>(); // the ideal route for the drone to take; connected sensor cycle.
    public static List<Point> destinations = new ArrayList<Point>();
    public static double[][] distanceMatrix = new double [34][34];
    
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
        sensorList = sensorsForTheDay.subList(0, 33); // to ensure only 33 sensors are checked
        noFlyZones = getNoFlyZoneList();
        
        // Create the drone's starting point and drone instance
        Coordinate startPosition = new Coordinate(startLatitude, startLongitude);
        drone = new Drone(startPosition);
        Point startPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
        path.add(startPoint);
        System.out.println("Drone start: " + drone.startPosition.latitude + ", " + drone.startPosition.longitude);

        calculateDistanceMatrix();
//        // Print distance matrix
//        System.out.println("Distance Matrix -----------");
//        for (int i = 0; i < distanceMatrix.length; i++) {
//            System.out.println(Arrays.toString(distanceMatrix[i]));
//        }
//        System.out.println("-----------------------");
       
        // Find nearest node J, move to it, and build the partial tour (I, J)
        Sensor nearestSensor = findNearestSensor(startPosition);
        System.out.println("First sensor to visit: " + nearestSensor.getCoordinate().latitude + ", " + nearestSensor.getCoordinate().longitude);
        sensorsInOrder.add(nearestSensor);
       
        System.out.println("Loop through all sensors and add them to the sensorsInOrder list");
        System.out.println("-----------------------------");
        while (sensorsInOrder.size() < sensorList.size()) {
            System.out.println("Number of sensors already in order: " + sensorsInOrder.size());
            var nextSensorToInclude = selectNearestSensor();
            insertIntoOrder(nextSensorToInclude);
        }
        
        drone.setSensors(sensorsInOrder); // give the drone the list of sensors to visit in order
        drone.visitSensors();
        
        
        path.add(startPoint);
        
        // The 'expected' route (calculated using Nearest Insertion)
        idealRoute.add(startPoint);
        for (int i = 0; i < sensorsInOrder.size(); i++) {
            //System.out.println("Adding sensor number " + i + " to path");
            Point sensorCoordinate = Point.fromLngLat(sensorsInOrder.get(i).getCoordinate().longitude, sensorsInOrder.get(i).getCoordinate().latitude);
            idealRoute.add(sensorCoordinate);
        }
        idealRoute.add(startPoint);
        
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
        var pathLine = LineString.fromLngLats(idealRoute);
        var pathGeometry = (Geometry) pathLine;
        var pathFeature = Feature.fromGeometry(pathGeometry);
        markerFeatures.add(pathFeature);
        var allMarkers = FeatureCollection.fromFeatures(markerFeatures);
        writeFile("sensorMap.geojson", allMarkers.toJson());
//        
////        // Create output files
////        String flightpathFile = "flightpath" + "-" + day + "-" + month + "-" + year + ".txt";
////        PrintWriter writer = new PrintWriter(flightpathFile, "UTF-8");
////        String readingsFile = "readings" + "-" + day + "-" + month + "-" + year +".geojson";
////        PrintWriter geoWriter = new PrintWriter(readingsFile, "UTF-8");
    }
    
    public static void calculateDistanceMatrix() throws IOException, InterruptedException {
        // Fills a 34 x 34 grid with the distances to all other nodes
        for (int i = 0; i < distanceMatrix.length; i++) {
            for (int j = 0; j < distanceMatrix.length; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 100;
                } else {
                    if (i == 0) {
                        var destination = sensorList.get(j - 1).getCoordinate();
                        var startFrom = drone.startPosition;
                        distanceMatrix[i][j] = getEuclideanDistance(startFrom, destination);
                    } else {
                        if (j == 0) {
                            var destination = drone.startPosition;
                            var startFrom = sensorList.get(i - 1).getCoordinate();
                            distanceMatrix[i][j] = getEuclideanDistance(startFrom, destination);
                        } else {
                            var startFrom = sensorList.get(i - 1).getCoordinate();
                            var destination = sensorList.get(j - 1).getCoordinate();
                            distanceMatrix[i][j] = getEuclideanDistance(startFrom, destination);
                        }
                    }
                }
            }
        }
    }

    /*
     * Inserts sensor N into sensorsInOrder such that d(I,N) + d(N,J) - d(I,J) is minimised and I and J are sensors
     * already in the sensorsInOrder list
     */
    public static void insertIntoOrder(Sensor nextSensorToInclude) throws IOException, InterruptedException {
        var minimum = 0.0;
        //int count = 0;
        Coordinate nodeN = nextSensorToInclude.getCoordinate();
        Coordinate nodeI = null;
        Coordinate nodeJ = null;
        
        System.out.println(sensorsInOrder.size());
        for (int i = 0; i < sensorsInOrder.size(); i ++) {
            int count = 0;

            // Consider adjacent points I and J from the sensors in order
            // We need to consider the start --> sensor1
            Coordinate temporaryNodeI;
            Coordinate temporaryNodeJ;
            
            if (i == 0) {
                //System.out.println("Setting temporary nodes to start and sensor1");
                temporaryNodeI = drone.startPosition;
                temporaryNodeJ = sensorsInOrder.get(i).getCoordinate();
            }
            // sensor1 --> sensor2, sensor2 --> sensor3 etc.
            else {
                //System.out.println("Setting temporary nodes to sensorx and sensory, x = y - 1");
                temporaryNodeI = sensorsInOrder.get(i-1).getCoordinate();
                temporaryNodeJ = sensorsInOrder.get(i).getCoordinate();
            }

            // Calculate d(I,N) + d(N,J) - d(I,J)
            //System.out.println("Calculating distance formula for temp nodes I, J and nodeN");
            var distanceIJ = getEuclideanDistance(temporaryNodeI, temporaryNodeJ);
            var distanceIN = getEuclideanDistance(temporaryNodeI, nodeN);
            var distanceNJ = getEuclideanDistance(nodeN, temporaryNodeJ);
            var formulaResult = distanceIN + distanceNJ - distanceIJ;
            // System.out.println("Formula result: " + formulaResult);
            
            // If this is the first distance we have checked, update the minimum
            // because it's the only distance so must be the smallest checked yet distance
            if (count == 0) {
                minimum = formulaResult;
                nodeI = temporaryNodeI;
                nodeJ = temporaryNodeJ;
            }
            
            // If the result of the formula for this edge is less than minimum,
            // update the nodeI and nodeJ variables to store the new (i,j) edge
            else if (formulaResult <= minimum) {
                minimum = formulaResult;
                nodeI = temporaryNodeI;
                nodeJ = temporaryNodeJ;
            }
            count++;
        }
        
        // Insert the sensor into the sensorsInOrder list between nodes I and J 
        
        // If nodeI is the start node, we need to insert node into the first position of the sensorsInOrderList
        if (nodeI.latitude == drone.startPosition.latitude && nodeI.longitude == drone.startPosition.longitude) {
            sensorsInOrder.add(0, nextSensorToInclude);
        } else {
            for (int j = 0; j < sensorsInOrder.size(); j++) {
                // when you find sensor node I, add the new sensor node into the next index of sensorsInOrder
                Coordinate node = sensorsInOrder.get(j).getCoordinate();
                double nodeLatitude = node.latitude;
                double nodeILatitude = nodeI.latitude;
                //System.out.println(nodeLatitude == nodeILatitude);
                if (node.latitude == nodeI.latitude && node.longitude == nodeI.longitude) {
                    sensorsInOrder.add(j+1, nextSensorToInclude);
                    break;
                }
            }
        }
    }
    
    /*
     * Find the sensor that is closest to any sensor that is already in the sensorsInOrder list (has already been added)
     * or to the start
     */
    public static Sensor selectNearestSensor() throws IOException, InterruptedException {
        Sensor nextSensorToInclude = null;
        
        // Create a HashMap to store the shortest distance from each sensor (not in path)
        // to a sensor in the path (the sensor in particular doesn't matter)
        var sensorDistancePair = new HashMap<Sensor, Double>();
        
        // For each row in distanceMatrix where row greater than 0
        // Find the smallest value in the row
        
        for (Sensor currentSensor: sensorList) {
            if (!sensorsInOrder.contains(currentSensor)) {
                var shortestDistance = 0.0;
                var distance = 0.0;
                var sensorNotAddedCoordinate = currentSensor.getCoordinate();
                // Calculate distance to each sensor in sensorsInOrder and save the shortest
                for (int i = 0; i < sensorsInOrder.size(); i++) {
                    Sensor sensorAdded = sensorsInOrder.get(i);
                    var coordinateAdded = sensorAdded.getCoordinate();
                    //distance = getEuclideanDistance(pointAdded, sensorPointNotAdded);
                    distance = distanceMatrix[sensorList.indexOf(sensorAdded) + 1][ sensorList.indexOf(currentSensor) + 1];
                    if (i == 0) {
                        shortestDistance = distance;
                    } else {
                       if (distance < shortestDistance) {
                           shortestDistance = distance;
                       }
                    } 
                }
                // Check distance to start and save if shorter than shortestDistance
                distance = getEuclideanDistance(drone.startPosition, sensorNotAddedCoordinate);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                   //System.out.println("closest to start");
                 }
                //System.out.println("Sensor: " + currentSensor.getCoordinate().latitude + ", " + currentSensor.getCoordinate().longitude + " has shortest distance: " + shortestDistance);
                sensorDistancePair.put(currentSensor, shortestDistance);
            }
        }
        // Get the sensor for which the minimum distance is the minimum of all sensors
        nextSensorToInclude = Collections.min(sensorDistancePair.entrySet(), Map.Entry.comparingByValue()).getKey();
        //System.out.println("Next sensor to add into the path: " + nextSensorToInclude.getCoordinate().latitude + ", " + nextSensorToInclude.getCoordinate().longitude);
        return nextSensorToInclude;
    }
    

    public static ArrayList<Feature> createMarkers() throws IOException, InterruptedException {
        var markerFeatures = new ArrayList<Feature>();
        
        // Create a Feature for every marker in sensor list
        for (Sensor sensor: sensorList) {
            // Create a marker for that coordinate
            Coordinate markerCoordinate = sensor.getCoordinate();
            Point markerPoint = Point.fromLngLat(markerCoordinate.longitude, markerCoordinate.latitude);
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

    
    /*
     * Loops through all sensors and finds the sensor closest to passed-in point
     */
    public static Sensor findNearestSensor(Coordinate currentNode) throws IOException, InterruptedException {
        var shortestDistance = 0.0;
        Sensor nextNode = null; // default to null
        var counter = 0;
        
        for (int i = 0; i < distanceMatrix.length - 1; i++) {
            var distance = distanceMatrix[0][i + 1];
            if (counter == 0) {
                shortestDistance = distance;
            }
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nextNode = sensorList.get(i);
            }
            counter++;
        }
        return nextNode;
    }

    /*
     *  Calculate Euclidean distance between currentNode and sensor
     */
    public static double getEuclideanDistance(Coordinate currentNode, Coordinate nextNode) { 
        double x1 = currentNode.latitude;
        double y1 = currentNode.longitude;
        double x2 = nextNode.latitude;
        double y2 = nextNode.longitude;
        var distance = Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
        return distance;
    }

    /*
     * Get the list of all sensors to be visited on the given date (from input) 
     */
    public static List<Sensor> getSensorList(String day, String month, String year) 
            throws IOException, InterruptedException {
        System.out.println("Getting sensor list from server");
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
     * Write json to a given filename
     */
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
}

