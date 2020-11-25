package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

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
    public static ArrayList<Sensor> sensorsInOrder = new ArrayList<Sensor>(); // list of all sensors in the order they should be visited
    public static List<Feature> noFlyZones; // areas the drone cannot fly into
    public static int portNumber;
    public static Drone drone;
    public static List<Point> idealRoute = new ArrayList<Point>(); // the ideal route for the drone to take; connected sensor cycle.
    public static double[][] distanceMatrix = new double [9][9];
    public static List<LineString> buildingLines = new ArrayList<LineString>();
    
    // for testing
    public static List<Point> pointsInZones = new ArrayList<>();
    
    public static void main( String[] args ) throws Exception {
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
        sensorList = sensorsForTheDay.subList(0, 8); // to ensure only 33 sensors are checked        
        FeatureCollection noFlyZoneList = getNoFlyZoneList();
        
        // Get the latitude and longitude values of each sensor using the server
        for (Sensor sensor: sensorList) {
            sensor.translateWhat3Words();
//            var position = sensor.getPosition();
//            System.out.println("Sensor:" + position.getLatitude() + "," + position.getLongitude());
        }
        
        // Breaking the no fly zones into polygons
        noFlyZones = noFlyZoneList.features();
//        for (Feature noFlyZoneFeature: noFlyFeatureList) {
//            Geometry noFlyZoneGeometry = noFlyZoneFeature.geometry();
//            Polygon noFlyZonePolygon = (Polygon) noFlyZoneGeometry;
//            noFlyPolygons.add(noFlyZonePolygon);
//        }
        
        // Create the drone's starting point and drone instance
        Coordinate startPosition = new Coordinate(startLatitude, startLongitude);
        drone = new Drone(startPosition);
        Point startPoint = Point.fromLngLat(startPosition.longitude, startPosition.latitude);
        System.out.println("Drone start: " + drone.startPosition.latitude + ", " + drone.startPosition.longitude);

        calculateDistanceMatrix();
        // CHECKING -- print out distance matrix
//        for (int i = 0; i < distanceMatrix.length; i++) {
//            System.out.println(Arrays.toString(distanceMatrix[i]));
//        }
//        
       
        // Find nearest node J, move to it, and build the partial tour (I, J)
        Sensor nearestSensor = findNearestSensor(startPosition);
//        System.out.println("Nearest Node: " + nearestSensor.getPosition().toString());
        sensorsInOrder.add(nearestSensor);
       
        System.out.println("Loop through all sensors and add them to the sensorsInOrder list");
        System.out.println("-----------------------------");
        while (sensorsInOrder.size() < sensorList.size()) {
            // checked and this works!
            var nextSensorToInclude = selectNearestSensor();
            // inserting needs to be checked
            insertIntoOrder(nextSensorToInclude);
        }
        
        drone.setSensors(sensorsInOrder); // give the drone the list of sensors to visit in order
        drone.startRoute(); // starts the route by adding the start point
        drone.visitSensors();
        
        // The 'expected' route (calculated using Nearest Insertion)
        idealRoute.add(startPoint);
        for (int i = 0; i < sensorsInOrder.size(); i++) {
            Sensor sensor = sensorsInOrder.get(i);
            Point sensorCoordinate = Point.fromLngLat(sensor.getPosition().longitude, sensor.getPosition().latitude);
            idealRoute.add(sensorCoordinate);
        }
        idealRoute.add(startPoint);
        
        // CHECKING -- PRINTING ALL SENSORS
        var markerFeatures = createMarkers();
        // CHECKING -- PRINT THE NO FLY ZONES
        for (Feature feature: noFlyZones) {
            markerFeatures.add(feature);
        }
        // CHECKING -- PRINT OUT THE POSITION OF ANY POINTS IN BUILDINGS
        if (pointsInZones.size() > 0) {
            for (Point point : pointsInZones) {
                var markerGeometry = (Geometry) point;
                var markerFeature = Feature.fromGeometry(markerGeometry);
                markerFeatures.add(markerFeature);
            }   
        }
        
        // CHECKING -- PRINTING START LOCATION
        var pointStart = Point.fromLngLat(startLongitude, startLatitude);
        var startGeometry = (Geometry) pointStart;
        var startFeature = Feature.fromGeometry(startGeometry);
        startFeature.addStringProperty("rgb-string", "#000000");
        startFeature.addStringProperty("marker-color", "#000000");
        startFeature.addStringProperty("marker-symbol", "lighthouse");
        markerFeatures.add(startFeature);
//        // CHECKING -- PRINTING ALL PATH SO FAR
//        var pathLine = LineString.fromLngLats(idealRoute);
//        var pathGeometry = (Geometry) pathLine;
//        var pathFeature = Feature.fromGeometry(pathGeometry);
//        markerFeatures.add(pathFeature);
        // CHECKING -- PRINT ALL NO FLY ONE EDGES
//        for (LineString line: buildingLines) {
//            Geometry geoLine = (Geometry) line;
//            Feature featureLine = Feature.fromGeometry(geoLine);
//            markerFeatures.add(featureLine);
//        }
        // CHECKING -- PRINT ALL SENSORS
        for (Sensor sensor: sensorList) {
            Point sensorPoint = Point.fromLngLat(sensor.getPosition().longitude, sensor.getPosition().latitude);
            var markerGeometry = (Geometry) sensorPoint;
            var markerFeature = Feature.fromGeometry(markerGeometry);
            markerFeature.addStringProperty("marker-color", "#0000FF");
            markerFeatures.add(markerFeature);
        }
        // CHECKING -- PRINTING LINESTRING FOR THE DRONE
        var pathLineDrone = LineString.fromLngLats(drone.route);
        var pathLineDroneGeometry = (Geometry) pathLineDrone;
        var pathFeatureDrone = Feature.fromGeometry(pathLineDroneGeometry);
        markerFeatures.add(pathFeatureDrone);
        var allMarkers = FeatureCollection.fromFeatures(markerFeatures);
        writeFile("sensorMap.geojson", allMarkers.toJson());
        
        System.out.println("Number of moves: " + (150 - drone.moves) );
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
                        var destination = sensorList.get(j - 1).getPosition();
                        var startFrom = drone.startPosition;
                        distanceMatrix[i][j] = getEuclideanDistance(startFrom, destination);
                    } else {
                        if (j == 0) {
                            var destination = drone.startPosition;
                            var startFrom = sensorList.get(i - 1).getPosition();
                            distanceMatrix[i][j] = getEuclideanDistance(startFrom, destination);
                        } else {
                            var startFrom = sensorList.get(i - 1).getPosition();
                            var destination = sensorList.get(j - 1).getPosition();
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
        
        // Coordinates of the sensor to insert into the path
        Coordinate nodeN = nextSensorToInclude.getPosition();
        
        // Coordinates of nodes I and J already in the path
        Coordinate nodeI = null;
        Coordinate nodeJ = null;
        
//        System.out.println("Number of Sensors In Order: " + sensorsInOrder.size());
        for (int i = 0; i < sensorsInOrder.size(); i ++) {
//            System.out.println("I: " + i);
            int count = 0;

            // Consider adjacent points I and J from the sensors in order
            // We need to consider the start --> sensor1
            Coordinate temporaryNodeI;
            Coordinate temporaryNodeJ;
            
            if (i == 0) {
                temporaryNodeI = drone.startPosition;
                temporaryNodeJ = sensorsInOrder.get(i).getPosition();
            }
            // sensor1 --> sensor2, sensor2 --> sensor3 etc.
            else {
                temporaryNodeI = sensorsInOrder.get(i-1).getPosition();
                temporaryNodeJ = sensorsInOrder.get(i).getPosition();
            }

            // Calculate d(I,N) + d(N,J) - d(I,J)
            var distanceIJ = getEuclideanDistance(temporaryNodeI, temporaryNodeJ);
            var distanceIN = getEuclideanDistance(temporaryNodeI, nodeN);
            var distanceNJ = getEuclideanDistance(nodeN, temporaryNodeJ);
//            System.out.println("DistanceIJ: " + distanceIJ);
//            System.out.println("DistanceIN: " + distanceIN);
//            System.out.println("DistanceNJ: " + distanceNJ);
            var formulaResult = distanceIN + distanceNJ - distanceIJ;
//            System.out.println("-----");
//            System.out.println("TemporaryNodeI: " + temporaryNodeI.toString());
//            System.out.println("TemporaryNodeJ: " + temporaryNodeJ.toString());
//            System.out.println("Formula result: " + formulaResult);
            
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
        
        // If nodeI is the start node, we need to insert node into the first position (index 0) of the sensorsInOrderList
        if (nodeI.latitude == drone.startPosition.latitude && nodeI.longitude == drone.startPosition.longitude) {
            sensorsInOrder.add(0, nextSensorToInclude);
        } else {
            for (int j = 0; j < sensorsInOrder.size(); j++) {
                // when you find sensor node I, add the new sensor node into the next index of sensorsInOrder
                // to make it between i and j
                Coordinate node = sensorsInOrder.get(j).getPosition();
                if (node.latitude == nodeI.latitude && node.longitude == nodeI.longitude) {
//                    System.out.println("Inserting sensor into position: " + (j+1));
                    sensorsInOrder.add(j+1, nextSensorToInclude);
                    
//                    //Print all sensors in Path
//                    System.out.println("Sensors in Path:  ----------");
//                    for (Sensor sensor: sensorsInOrder) {
//                        System.out.println(sensor.getPosition().toString());
//                    }
//                    System.out.println("-------");
                    
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
                var sensorNotAddedCoordinate = currentSensor.getPosition();
                // Calculate distance to each sensor in sensorsInOrder and save the shortest
                for (int i = 0; i < sensorsInOrder.size(); i++) {
                    Sensor sensorAdded = sensorsInOrder.get(i);
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
                 }
                sensorDistancePair.put(currentSensor, shortestDistance);
            }
        }
        // Get the sensor for which the minimum distance is the minimum of all sensors
        nextSensorToInclude = Collections.min(sensorDistancePair.entrySet(), Map.Entry.comparingByValue()).getKey();
        
//        System.out.println("Next sensor to include: " + nextSensorToInclude.getPosition().toString());
        return nextSensorToInclude;
    }
    

    public static ArrayList<Feature> createMarkers() throws Exception {
        var markerFeatures = new ArrayList<Feature>();
        
        // Create a Feature for every marker in sensor list
        for (Sensor sensor: drone.checkedSensors) {
            // Create a marker for that coordinate
            Coordinate markerCoordinate = sensor.getPosition();
            Point markerPoint = Point.fromLngLat(markerCoordinate.longitude, markerCoordinate.latitude);
            var markerGeometry = (Geometry) markerPoint;
            var markerFeature = Feature.fromGeometry(markerGeometry);
            
            var rgbValue = "";
            var symbol = "";
            // If the battery level is high enough for an accurate reading..
            if (sensor.getBattery() >= 10) {
                double reading = Double.parseDouble(sensor.getReading());
                rgbValue = getRGBString(reading);
                symbol = getMarkerSymbol(reading);
            } else {
                // Report the sensor as needing a new battery and discard the reading
                rgbValue = "#000000";
                symbol = "cross";
            }
            // Create the markers with the four properties
            markerFeature.addStringProperty("location", sensor.getLocation());
            markerFeature.addStringProperty("rgb-string", rgbValue);
            markerFeature.addStringProperty("marker-color", rgbValue);
            markerFeature.addStringProperty("marker-symbol", symbol);
            markerFeatures.add(markerFeature);
        }
        
        return markerFeatures;
    }
    
    /*
     * Returns corresponding RGB value for the colour mapping based on sensor reading value
     */
    public static String getRGBString(double value) throws Exception {
        if (0 <= value && value < 32) {
            return "#00ff00";
        } else if (32 <= value && value < 64) {
            return "#40ff00";
        } else if (64 <= value && value < 96) {
            return "#80ff00";
        } else if (96 <= value && value < 128) {
            return "#c0ff00";
        } else if (128 <= value && value < 160) {
            return "#ffc000";
        } else if (160 <= value && value < 192) {
            return "#ff8000";
        } else if (192 <= value && value < 224) {
            return "#ff4000";
        } else if (224 <= value && value < 256) {
            return "#ff0000";
        } else {
            // This is the case for if the prediction value is not in range
            throw new Exception("The prediction value must be between 0 and 255 (inclusive)");
        }
    }
    
    /*
     * Returns the corresponding marker symbol for the sensor reading value
     */
    public static String getMarkerSymbol(double value) throws Exception {
        if (0 <= value && value < 128) {
            return "lighthouse";
        } else if (128 <= value && value < 256) {
            return "danger";
        } else {
            // This is the case for if the prediction value is not in range
            throw new Exception("The prediction value must be between 0 and 255 (inclusive)");
        }
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
        double y1 = currentNode.latitude;
        double x1 = currentNode.longitude;
        double y2 = nextNode.latitude;
        double x2 = nextNode.longitude;
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
        System.out.println("Writing to file " + filename);
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        try {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.close();
    }
}

