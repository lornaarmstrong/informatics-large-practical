package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class contains the implementation of Nearest Insertion Heuristic
 * used to determine the order that sensors should be visited, depending on 
 * their location in a geographical area.
 */
public class NearestInsertion {
    
    private List<Sensor> sensorsInOrder = new ArrayList<Sensor>();
    private Coordinate startNode;
    private GeographicalArea map;
    
    public NearestInsertion(Coordinate startNode, GeographicalArea map) {
        this.startNode = startNode;
        this.map = map;
    }
    
    /*
     * Generate the order in which the sensors should be visited.
     */
    public List<Sensor> generateSensorOrder() {     
        // Find nearest sensor to the start and insert as first sensor to visit
        var nearestSensorToStart = findNearestSensorToStart(startNode, map);
        this.sensorsInOrder.add(nearestSensorToStart);
        
        // Loop through all sensors yet to be added to the order until all have been included.
        while (sensorsInOrder.size() < map.sensors.size()) {
            var nextSensorToInclude = selectNearestSensor(map, startNode);
            insertIntoOrder(nextSensorToInclude, startNode);
        }
        return sensorsInOrder;
    }

    /*
     * Loop through all sensors and find the sensor closest to passed-in point
     */
    public static Sensor findNearestSensorToStart(Coordinate startNode, GeographicalArea map) {
        var shortestDistance = 0.0;
        Sensor nextNode = null; // default value of null
        var counter = 0; 
        for (int i = 0; i < map.distanceMatrix.length - 1; i++) {
            var distance = map.distanceMatrix[0][i + 1];
            if (distance < shortestDistance || counter == 0) {
                shortestDistance = distance;
                nextNode = map.sensors.get(i);
            }
        }
        return nextNode;
    }
    
    /* 
     * Find the sensor closest to the start position.
     */
    public Sensor selectNearestSensor(GeographicalArea map, Coordinate startPosition){
        Sensor nextSensorToInclude = null;
        var sensorDistancePair = new HashMap<Sensor, Double>();
        
        // For each row in distanceMatrix where row greater than 0, find the smallest value 
        // in the row        
        for (var currentSensor: map.sensors) {
            if (!sensorsInOrder.contains(currentSensor)) {
                var shortestDistance = 0.0;
                var distance = 0.0;
                var sensorNotAddedCoordinate = currentSensor.getCoordinate();
                // Calculate distance to each sensor in sensorsInOrder and save the shortest
                for (var i = 0; i < this.sensorsInOrder.size(); i++) {
                    var sensorAdded = this.sensorsInOrder.get(i);
                    distance = map.distanceMatrix[map.sensors.indexOf(sensorAdded) + 1]
                            [map.sensors.indexOf(currentSensor) + 1];
                    if (i == 0) {
                        shortestDistance = distance;
                    } else {
                       if (distance < shortestDistance) {
                           shortestDistance = distance;
                       }
                    } 
                }
                // Check distance to start and save if shorter than shortestDistance value
                distance = startPosition.getEuclideanDistance(sensorNotAddedCoordinate);
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                 }
                sensorDistancePair.put(currentSensor, shortestDistance);
            }
        }
        // Get the sensor for which the minimum distance to any sensor is the minimum out of 
        // all sensors
        nextSensorToInclude = Collections.min(sensorDistancePair.entrySet(), 
                Map.Entry.comparingByValue()).getKey();
        return nextSensorToInclude;
    }
    
    /*
     * Insert sensor N into sensorsInOrder such that d(I,N) + d(N,J) - d(I,J) is minimised and
     * I and J are sensors already in the sensorsInOrder list
     */
    public void insertIntoOrder(Sensor nextSensorToInclude, Coordinate startPosition) {
        var minimum = 0.0;
        
        // Coordinates of the sensor to insert into the path
        var nodeN = nextSensorToInclude.getCoordinate();
        
        // Coordinates of nodes I and J already in the path
        Coordinate nodeI = null;
        Coordinate nodeJ = null;
        
        for (var i = 0; i < this.sensorsInOrder.size(); i ++) {
            var count = 0;

            // Consider adjacent points I and J from the sensors in order
            // We need to consider the start --> sensor1
            Coordinate temporaryNodeI;
            Coordinate temporaryNodeJ;
            
            if (i == 0) {
                temporaryNodeI = startPosition;
                temporaryNodeJ = this.sensorsInOrder.get(i).getCoordinate();
            }
            // sensor1 --> sensor2, sensor2 --> sensor3 etc.
            else {
                temporaryNodeI = this.sensorsInOrder.get(i-1).getCoordinate();
                temporaryNodeJ = this.sensorsInOrder.get(i).getCoordinate();
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
        if (nodeI.equals(startPosition)) {
            sensorsInOrder.add(0, nextSensorToInclude);
        } else {
            for (var j = 0; j < this.sensorsInOrder.size(); j++) {
                var node = this.sensorsInOrder.get(j).getCoordinate();
                if (node.equals(nodeI)) {
                    this.sensorsInOrder.add(j+1, nextSensorToInclude);
                    break;
                }
            }
        }
    }
}
