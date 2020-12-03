package uk.ac.ed.inf.aqmaps;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

/*
 * GeographicalArea class represents a map of Sensors and No Fly Zones on a specific day
 */
public class GeographicalArea {
    
    private final String day;
    private final String month;
    private final String year;
    private final List<Sensor> sensors = new ArrayList<Sensor>();
    private final List<Feature> noFlyZones = new ArrayList<Feature>();
    public final double[][] distanceMatrix = new double [34][34];
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    
    // Confinement Zone Coordinates
    private final Coordinate topLeftConfinement = new Coordinate(55.946233, -3.192473);
    private final Coordinate topRightConfinement = new Coordinate(55.946233, -3.184319);
    private final Coordinate bottomLeftConfinement = new Coordinate(55.942617, -3.192473);
    private final Coordinate bottomRightConfinement = new Coordinate(55.942617, -3.184319);
    
    public GeographicalArea(String day, String month, String year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }
    
    // Getters
    public List<Sensor> getSensors() {
        var sensors = new ArrayList<Sensor>(this.sensors);
        return sensors;
    }
    
    public List<Feature> getNoFlyZones() {
        var noFlyZones = new ArrayList<Feature>(this.noFlyZones);
        return noFlyZones;
    }
    
    public Coordinate getTopLeftConfinement() {
        return this.topLeftConfinement;
    }
    
    public Coordinate getBottomLeftConfinement() {
        return this.bottomLeftConfinement;
    }
    
    public Coordinate getBottomRightConfinement() {
        return this.bottomRightConfinement;
    }
    
    /*
     * Call all methods within GeographicalArea needed to set up the map.
     */
    public void setUp(int portNumber, Coordinate startPosition) 
            throws IOException, InterruptedException {
        // Get sensor and no fly zone information from server
        getSensorListFromServer(portNumber);
        getNoFlyZonesFromServer(portNumber);
        // Get the latitude and longitude values of each sensor using the server and store
        for (var sensor: sensors) {
            sensor.translateLocation();
        }
        calculateDistanceMatrix(startPosition);
    }
    
    /*
     * Get the list of all sensors to be visited, from the server, and add each sensor to sensors
     */
    public void getSensorListFromServer(int portNumber) throws IOException, InterruptedException {
        System.out.println("Retrieving list of sensors from server.");
        var request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://localhost:%d/maps/%s/%s/%s/" 
                        + "air-quality-data.json", portNumber,this.year,this.month,this.day)))
                .build();
        var response = httpClient.send(request, BodyHandlers.ofString());
        var listType = new TypeToken<ArrayList<Sensor>>(){}.getType();
        List<Sensor> sensorsForThisDay = new Gson().fromJson(response.body(), listType);
        for (var sensor: sensorsForThisDay) {
            sensors.add(sensor);
        }
        System.out.println("Number of sensors retrieved from server: " + sensors.size());
    }
    
    /*
     * Get the list of all polygons representing the No-Fly Zone buildings, from the server.
     */
    
    public void getNoFlyZonesFromServer(int portNumber) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + portNumber
                        + "/buildings/no-fly-zones.geojson"))
                .build();
        var response = httpClient.send(request, BodyHandlers.ofString());
        var featureCollection = FeatureCollection.fromJson(response.body());
        var featureList = featureCollection.features();
        // Adds each feature to the list of No Fly Zones
        for (var feature : featureList) {
            noFlyZones.add(feature);
        }
        System.out.println("No Fly Zones retrieved from server: " + noFlyZones.size());
    }
    
    /*
     * Fill a 34 x 34 grid with the distances from all sensors to each other sensor, and 
     * all sensors to the start.
     */
    public void calculateDistanceMatrix(Coordinate droneStartPosition) {
        for (int i = 0; i < this.distanceMatrix.length; i++) {
            for (int j = 0; j < this.distanceMatrix.length; j++) {
                if (i == j) {
                    /* Set distance from i to itself to be a large number to avoid using in 
                      minimum distance algorithm */
                    this.distanceMatrix[i][j] = 1000;
                } else {
                    Coordinate destination;
                    Coordinate startFrom;
                    if (i == 0) {
                        destination = sensors.get(j - 1).getCoordinate();
                        startFrom = droneStartPosition;
                    } else {
                        if (j == 0) {
                            destination = droneStartPosition;
                            startFrom = sensors.get(i - 1).getCoordinate();
                        } else {
                            startFrom = sensors.get(i - 1).getCoordinate();
                            destination = sensors.get(j - 1).getCoordinate();
                        }
                    }
                    this.distanceMatrix[i][j] = startFrom.getEuclideanDistance(destination);
                }
            }
        }
    }
}
