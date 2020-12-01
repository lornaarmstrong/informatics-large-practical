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
 * CampusMap class represents a map of Sensors and No Fly Zones on a specific day
 */
public class CampusMap {
    
    private final String day;
    private final String month;
    private final String year;
    public final List<Sensor> sensors = new ArrayList<Sensor>();
    public final List<Feature> noFlyZones = new ArrayList<Feature>();
    public final double[][] distanceMatrix = new double [34][34];
    
    public CampusMap(String day, String month, String year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }
    
    /*
     * Get list of all sensors to be visited, from the server, using passed-in portNumber.
     */
    public void getSensorListFromServer(int portNumber) throws IOException, InterruptedException {
        System.out.println("Retrieving list of sensors from server.");
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("http://localhost:%d/maps/%s/%s/%s/" 
                        + "air-quality-data.json", portNumber,this.year,this.month,this.day)))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        var listType = new TypeToken<ArrayList<Sensor>>(){}.getType();
        List<Sensor> sensorsForThisDay = new Gson().fromJson(response.body(), listType);
        for (var sensor: sensorsForThisDay) {
            sensors.add(sensor);
        }
        System.out.println("Number of sensors retrieved from server: " + sensors.size());
    }
    
    /*
     * Get the list of all polygons representing the No-Fly Zone buildings
     */
    public void getNoFlyZonesFromServer(int portNumber) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + portNumber + "/buildings/no-fly-zones.geojson"))
                .build();
        var response = client.send(request, BodyHandlers.ofString());
        var featureCollection = FeatureCollection.fromJson(response.body());
        List<Feature> featureList = featureCollection.features();
        // Adds each feature to the list of No Fly Zones
        for (var feature : featureList) {
            noFlyZones.add(feature);
        }
        System.out.println("No Fly Zones retrieved from server: " + noFlyZones.size());
    }
    
    /*
     * Fills a 34 x 34 grid with the distances from all sensors to each other sensor, and all sensors
     * to the start.
     */
    public void calculateDistanceMatrix(Coordinate droneStartPosition) throws IOException, InterruptedException {
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
