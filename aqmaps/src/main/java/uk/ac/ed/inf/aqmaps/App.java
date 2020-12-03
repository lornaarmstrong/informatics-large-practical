package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/*
 * App class for the autonomous drone, collecting sensor readings for air quality.
 */
public class App 
{
    public static int portNumber;
    private static Drone drone;
    private static GeographicalArea map;
    
    public static void main( String[] args ) throws Exception {
        
        // Get the input data
        String day = args[0];
        String month = args[1];
        String year = args[2];
        var startLatitude = Double.parseDouble(args[3]);
        var startLongitude = Double.parseDouble(args[4]);
        var seed = Integer.parseInt(args[5]);
        portNumber = Integer.parseInt(args[6]);
        
        // Create starting Coordinate
        var startPosition = new Coordinate(startLatitude, startLongitude);
        
        // Create instance of GeographicalArea and call the map setUp method
        map = new GeographicalArea(day, month, year);
        map.setUp(portNumber, startPosition);
        
        // Create instance of Drone
        drone = new Drone(startPosition, map);
        
        // Create instance of NearestInsertion and get the list of sensors to visit, in order 
        var nearestInsertion = new NearestInsertion(startPosition, map);
        var sensorOrder = nearestInsertion.generateSensorOrder();

        // Give the drone the list of sensors to visit in order and then set the drone off moving
        drone.setSensors(sensorOrder);
        drone.startRoute();
        drone.visitSensors();
        
        // Print out the end state of the drone after flight
        System.out.println("Total number of moves: " + (150 - drone.getMoves()));
        System.out.println("End location of drone: "  + drone.getCurrentPosition());
        
        // Create all Features to be written to the Geo-JSON file
        var allFeatures = getFeaturesForGeoJson();
        
        // Output Files
        var flightpathFile = "flightpath" + "-" + day + "-" + month + "-" + year + ".txt";
        var readingsFile = "readings" + "-" + day + "-" + month + "-" + year +".geojson";
        writeJsonFile(readingsFile, allFeatures.toJson());
        writeFlightpathFile(flightpathFile);
    }
    
    /*
     * Create the FeatureCollection containing all sensor marker Features and the LineString
     * representing the drone's flight path.
     */
    private static FeatureCollection getFeaturesForGeoJson() throws Exception {
        // Get the sensor marker Features
        var features = createMarkers(map);
        // Create Drone Path LineString and add to features
        var dronePathLine = LineString.fromLngLats(drone.route);
        var dronePathGeometry = (Geometry) dronePathLine;
        var dronePathFeature = Feature.fromGeometry(dronePathGeometry);
        features.add(dronePathFeature);   
        return(FeatureCollection.fromFeatures(features));
    }

    /*
     * Create a marker for each sensor in the passed-in map, with the four properties: rgb-string,
     * location, marker-color and marker-symbol, and return all markers in an ArrayList. 
     */
    private static ArrayList<Feature> createMarkers(GeographicalArea map) throws Exception {
        var markerFeatures = new ArrayList<Feature>();
        
        for (var sensor: map.sensors) {
            // Create the marker feature
            var markerCoordinate = sensor.getCoordinate();
            var markerPoint = markerCoordinate.toPoint();
            var markerGeometry = (Geometry) markerPoint;
            var markerFeature = Feature.fromGeometry(markerGeometry);
            
            var rgbValue = "";
            var symbol = "";
            
            if (!drone.hasCheckedSensor(sensor)) {
                rgbValue = "#aaaaaa";
            } else {
                // Check if the battery level is high enough for an accurate reading
                if (sensor.getBattery() >= 10) {
                    var reading = Double.parseDouble(sensor.getReading());
                    rgbValue = getRGBString(reading);
                    symbol = getMarkerSymbol(reading);
                } else {
                    // Report the sensor as needing a new battery and discard the reading
                    // Update the rgbValue and symbol to match this case.
                    rgbValue = "#000000";
                    symbol = "cross";
                }
                markerFeature.addStringProperty("marker-symbol", symbol);
            }
            // Create the markers with the required properties
            markerFeature.addStringProperty("location", sensor.getLocation());
            markerFeature.addStringProperty("rgb-string", rgbValue);
            markerFeature.addStringProperty("marker-color", rgbValue);
            markerFeatures.add(markerFeature);
        }
        return markerFeatures;
    }
    
    /*
     * Return corresponding RGB value for the colour mapping based on sensor reading value
     */
    private static String getRGBString(double value) throws Exception {
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
     * Return the corresponding marker symbol for the sensor reading value
     */
    private static String getMarkerSymbol(double value) throws Exception {
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
     * Write JSON to a given filename
     */
    private static void writeJsonFile(String filename, String json) throws IOException {
        var writer = new BufferedWriter(new FileWriter(filename));
        try {
            writer.write(json);
            System.out.println("Written to file " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        writer.close();
    }
    
    /*
     * Write the flightpath text file to the given filename
     */
    private static void writeFlightpathFile(String filename) throws IOException {
        var fileWriter = new FileWriter(filename);
        var flightpath = drone.flightpathData;
        for (int i = 0; i < flightpath.size(); i++) {
            fileWriter.write((i + 1) + "," + flightpath.get(i) + "\n");
        }
        System.out.println("Written to file " + filename);
        fileWriter.close();
    }
}

