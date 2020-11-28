package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
    public static List<Sensor> sensorList = new ArrayList<Sensor>(); // list of all sensors to be visited in the day
    public static ArrayList<Sensor> sensorsInOrder = new ArrayList<Sensor>(); // list of all sensors in the order they should be visited
    //public static List<Feature> noFlyZones; // areas the drone cannot fly into
    public static int portNumber;
    public static Drone drone;
    public static List<String> flightpathInformation = new ArrayList<String>();
//    public static List<Point> idealRoute = new ArrayList<Point>(); // the ideal route for the drone to take; connected sensor cycle.
    public static double[][] distanceMatrix = new double [34][34];
    public static List<LineString> buildingLines = new ArrayList<LineString>();
    
    public static void main( String[] args ) throws Exception {
        // Get the input 
        String day = args[0];
        String month = args[1];
        String year = args[2];
        double startLatitude = Double.parseDouble(args[3]);
        double startLongitude = Double.parseDouble(args[4]);
        int seed = Integer.parseInt(args[5]);
        portNumber = Integer.parseInt(args[6]);
        
        // Set up the map and fetch all necessary data from the server
        CampusMap map = new CampusMap(day, month, year);
        map.getSensorListFromServer(portNumber);
        sensorList = map.sensors.subList(0,33);
        map.getNoFlyZonesFromServer(portNumber);
        
        // Get the latitude and longitude values of each sensor using the server and store
        for (Sensor sensor: sensorList) {
            sensor.translateWhat3Words();
        }
        
        // Create the drone's starting point and drone instance
        Coordinate startPosition = new Coordinate(startLatitude, startLongitude);
        drone = new Drone(startPosition, map);
        
        System.out.println("Drone starting position: " + startPosition.toString());
        calculateDistanceMatrix();
        //calculateDistanceMatrix(startPosition);
               
        // Find nearest node J, move to it, and build the partial tour (I, J)
        Sensor nearestSensor = findNearestSensor(startPosition);
        sensorsInOrder.add(nearestSensor);
       
        while (sensorsInOrder.size() < sensorList.size()) {
            var nextSensorToInclude = selectNearestSensor();
            insertIntoOrder(nextSensorToInclude);
        }
        
        drone.setSensors(sensorsInOrder); // give the drone the list of sensors to visit in order
        drone.startRoute(); // starts the route by adding the start point
        drone.visitSensors();
        
        // Counting number of moves
        Coordinate coordA = new Coordinate(55.94402112658734,-3.186974744341161);
        //Coordinate destA = new Coordinate(55.94475044893357,-3.1911109369038146);
        Coordinate destA = new Coordinate(55.9447,-3.1911);
//        System.out.println("Number of moves:");
//        System.out.println(drone.countNumberMoves(coordA, destA, 0));
        
//        // The 'expected' route (calculated using Nearest Insertion)
//        idealRoute.add(startPoint);
//        for (int i = 0; i < sensorsInOrder.size(); i++) {
//            Sensor sensor = sensorsInOrder.get(i);
//            Point sensorCoordinate = Point.fromLngLat(sensor.getPosition().longitude, sensor.getPosition().latitude);
//            idealRoute.add(sensorCoordinate);
//        }
//        idealRoute.add(startPoint);
        
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
//        // CHECKING -- PRINTING ALL PATH SO FAR
//        var pathLine = LineString.fromLngLats(idealRoute);
//        var pathGeometry = (Geometry) pathLine;
//        var pathFeature = Feature.fromGeometry(pathGeometry);
//        markerFeatures.add(pathFeature);
        // CHECKING -- PRINT ALL SENSORS
//        for (Sensor sensor: sensorList) {
//            Point sensorPoint = Point.fromLngLat(sensor.getPosition().longitude, sensor.getPosition().latitude);
//            var markerGeometry = (Geometry) sensorPoint;
//            var markerFeature = Feature.fromGeometry(markerGeometry);
//            markerFeature.addStringProperty("marker-color", "#0000FF");
//            markerFeatures.add(markerFeature);
//        }
        
        // Sensors and drone path for readings file
        var features = createMarkers();
        var dronePathLine = LineString.fromLngLats(drone.route);
        var dronePathGeometry = (Geometry) dronePathLine;
        var dronePathFeature = Feature.fromGeometry(dronePathGeometry);
        features.add(dronePathFeature);
        var allFeatures = FeatureCollection.fromFeatures(features);
        
        // Output Files
        String flightpathFile = "flightpath" + "-" + day + "-" + month + "-" + year + ".txt";
        String readingsFile = "readings" + "-" + day + "-" + month + "-" + year +".geojson";
        
        FileWriter fileWriter = new FileWriter(flightpathFile);
        for (int i = 0; i < flightpathInformation.size(); i ++) {
            fileWriter.write( (i+1) + "," + flightpathInformation.get(i) + "\n");
        }
        fileWriter.close();
        
        // GeoJSON
        writeJsonFile(readingsFile, allFeatures.toJson());
      
    }
    
    /*
     * Fills a 34 x 34 grid with the distances from all sensors to each other sensor, and all sensors
     * to the start.
     */
    public static void calculateDistanceMatrix() throws IOException, InterruptedException {
        for (int i = 0; i < distanceMatrix.length; i++) {
            for (int j = 0; j < distanceMatrix.length; j++) {
                if (i == j) {
                    distanceMatrix[i][j] = 100;
                } else {
                    Coordinate destination;
                    Coordinate startFrom;
                    if (i == 0) {
                        destination = sensorList.get(j - 1).getPosition();
                        startFrom = drone.startPosition;
                    } else {
                        if (j == 0) {
                            destination = drone.startPosition;
                            startFrom = sensorList.get(i - 1).getPosition();
                        } else {
                            startFrom = sensorList.get(i - 1).getPosition();
                            destination = sensorList.get(j - 1).getPosition();
                        }
                    }
                    distanceMatrix[i][j] = startFrom.getEuclideanDistance(destination);
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
        
        //System.out.println("Number of Sensors In Order: " + sensorsInOrder.size());
        for (int i = 0; i < sensorsInOrder.size(); i ++) {
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
            var distanceIJ = temporaryNodeI.getEuclideanDistance(temporaryNodeJ);
            var distanceIN = temporaryNodeI.getEuclideanDistance(nodeN);
            var distanceNJ = nodeN.getEuclideanDistance(temporaryNodeJ);
            var formulaResult = distanceIN + distanceNJ - distanceIJ;
            
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
                distance = drone.startPosition.getEuclideanDistance(sensorNotAddedCoordinate);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                 }
                sensorDistancePair.put(currentSensor, shortestDistance);
            }
        }
        // Get the sensor for which the minimum distance is the minimum of all sensors
        nextSensorToInclude = Collections.min(sensorDistancePair.entrySet(), Map.Entry.comparingByValue()).getKey();
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
                nextNode = sensorList.get(i);
            }
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nextNode = sensorList.get(i);
            }
        }
        return nextNode;
    }
 
    /*
     * Write json to a given filename
     */
    public static void writeJsonFile(String filename, String json) throws IOException {
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

