/**
 * Mars Simulation Project
 * Settlement.java
 * @version 2.74 2002-04-13
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.structure;

import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.vehicle.*;
import org.mars_sim.msp.simulation.equipment.*;
import org.mars_sim.msp.simulation.malfunction.MalfunctionManager;
import java.util.*;

/** The Settlement class represents a settlement unit on virtual Mars.
 *  It contains information related to the state of the settlement.
 */
public class Settlement extends Structure implements LifeSupport {

    // Default population capacity for a settlement
    private static int DEFAULT_POPULATION_CAPACITY = 20;
    private static Random rand = new Random();

    // Data members
    int populationCapacity; // The population capacity of the settlement
    FacilityManager facilityManager; // The facility manager for the settlement
    protected Airlock airlock; // the settlement's airlock.

    /** Constructs a Settlement object at a given location
     *  @param name the settlement's name
     *  @param location the settlement's location
     *  @param populationCapacity the settlement's population capacity
     *  @param mars the virtual Mars
     */
    Settlement(String name, Coordinates location, int populationCapacity, Mars mars) {
        // Use Unit constructor
        super(name, location, mars);

        // Initialize data members
        if (populationCapacity == 0) this.populationCapacity = DEFAULT_POPULATION_CAPACITY;
        else this.populationCapacity = populationCapacity;
        facilityManager = new FacilityManager(this, mars);
	setProperties();
    }

    /** Constructs a Settlement object at a random location
     *  @param name the settlement's name
     *  @param populationCapacity the settlement's population capacity
     *  @param mars the virtual Mars
     */
    Settlement(String name, int populationCapacity, Mars mars) {

        // Use Unit constructor
        super(name, new Coordinates(0D, 0D), mars);

        // Determine random location of settlement, 
	// adjust so it will be less likely to be near the poles.
        double settlementPhi = (rand.nextGaussian() * (Math.PI / 7D)) + (Math.PI / 2D);
        if (settlementPhi > Math.PI) settlementPhi = Math.PI;
        if (settlementPhi < 0D) settlementPhi = 0D;
        double settlementTheta = (double)(Math.random() * (2D * Math.PI));
        setCoordinates(new Coordinates(settlementPhi, settlementTheta));

        // Initialize data members
        if (populationCapacity == 0) this.populationCapacity = DEFAULT_POPULATION_CAPACITY;
        else this.populationCapacity = populationCapacity;
        facilityManager = new FacilityManager(this, mars);
	setProperties();
    }

    /** Initialize settlement properties */
    public void setProperties() {
       
        // Add scope string to malfunction manager.
	malfunctionManager.addScopeString("Settlement");
	malfunctionManager.addScopeString("LifeSupport");
	    
	// Set inventory total mass capacity.
	inventory.setTotalCapacity(Double.MAX_VALUE);
	
        // Set inventory resource capacities.
        SimulationProperties properties = mars.getSimulationProperties();
	double fuelCap = properties.getSettlementFuelStorageCapacity();
        inventory.setResourceCapacity(Inventory.FUEL, fuelCap);
	double oxygenCap = properties.getSettlementOxygenStorageCapacity();
        inventory.setResourceCapacity(Inventory.OXYGEN, oxygenCap);
	double waterCap = properties.getSettlementWaterStorageCapacity();
        inventory.setResourceCapacity(Inventory.WATER, waterCap);
	double foodCap = properties.getSettlementFoodStorageCapacity();
        inventory.setResourceCapacity(Inventory.FOOD, foodCap);

	// Set random initial resources from 1/4 to 1/2 total capacity.
	double fuel = (fuelCap / 4D) + RandomUtil.getRandomDouble(fuelCap / 4D);
	inventory.addResource(Inventory.FUEL, fuel); 
	double oxygen = (oxygenCap / 4D) + RandomUtil.getRandomDouble(oxygenCap / 4D);
	inventory.addResource(Inventory.OXYGEN, oxygen); 
	double water = (waterCap / 4D) + RandomUtil.getRandomDouble(waterCap / 4D);
	inventory.addResource(Inventory.WATER, water); 
	double food = (foodCap / 4D) + RandomUtil.getRandomDouble(foodCap / 4D);
	inventory.addResource(Inventory.FOOD, food);

	// Set random initial rock samples from 0 to 500 kg.
	double rockSamples = RandomUtil.getRandomDouble(500D);
	inventory.addResource(Inventory.ROCK_SAMPLES, rockSamples);

	// Create airlock for settlement.
	airlock = new Airlock(this, mars, 4);

	// Adds enough EVA suits for inhabitant capacity.
	for (int x=0; x < getPopulationCapacity(); x++) 
	    inventory.addUnit(new EVASuit(location, mars));
    }
	
    /** Returns the facility manager for the settlement
     *  @return the settlement's facility manager
     */
    public FacilityManager getFacilityManager() {
        return facilityManager;
    }

    /** Returns the malfunction manager for the settlement.
     *  @return the malfunction manager
     */
    public MalfunctionManager getMalfunctionManager() {
        return malfunctionManager;
    }
    
    /** Gets the population capacity of the settlement
     *  @return the population capacity
     */
    public int getPopulationCapacity() {
        return populationCapacity;
    }

    /** Gets the current population number of the settlement
     *  @return the number of inhabitants
     */
    public int getCurrentPopulationNum() {
        return getInhabitants().size();
    }

    /** Gets a collection of the inhabitants of the settlement.
     *  @return PersonCollection of inhabitants
     */
    public PersonCollection getInhabitants() {
	return inventory.getContainedUnits().getPeople();
    }
	
    /** Gets the current available population capacity
     *  of the settlement
     *  @return the available population capacity
     */
    public int getAvailablePopulationCapacity() {
        return getPopulationCapacity() - getCurrentPopulationNum();
    }

    /** Gets an array of current inhabitants of the settlement
     *  @return array of inhabitants
     */
    public Person[] getInhabitantArray() {
	PersonCollection people = getInhabitants();
        Person[] personArray = new Person[people.size()];
	PersonIterator i = people.iterator();
	int count = 0;
	while (i.hasNext()) {
	    personArray[count] = i.next();
	    count++;
	}
        return personArray;
    }    

    /** Gets a collection of vehicles parked at the settlement.
     *  @return VehicleCollection of parked vehicles
     */
    public VehicleCollection getParkedVehicles() {
        return inventory.getContainedUnits().getVehicles();
    }

    /** Gets the number of vehicles parked at the settlement.
     *  @return parked vehicles number
     */
    public int getParkedVehicleNum() {
        return getParkedVehicles().size();
    }
    
    /** Returns true if life support is working properly and is not out
     *  of oxygen or water.
     *  @return true if life support is OK
     */
    public boolean lifeSupportCheck() {
        boolean result = true;

	if (inventory.getResourceMass(Inventory.OXYGEN) <= 0D) result = false;
	if (inventory.getResourceMass(Inventory.WATER) <= 0D) result = false;

	// need to also check for temp and air pressure
	
	return result;
    }

    /** Gets the number of people the life support can provide for.
     *  @return the capacity of the life support system
     */
    public int getLifeSupportCapacity() {
        return getPopulationCapacity();
    }
    
    /** Gets oxygen from system.
     *  @param amountRequested the amount of oxygen requested from system (kg)
     *  @return the amount of oxgyen actually received from system (kg)
     */
    public double provideOxygen(double amountRequested) {
         return inventory.removeResource(Inventory.OXYGEN, amountRequested);
    }
    
    /** Gets water from system.
     *  @param amountRequested the amount of water requested from system (kg)
     *  @return the amount of water actually received from system (kg)
     */
    public double provideWater(double amountRequested) {
        return inventory.removeResource(Inventory.WATER, amountRequested);
    }

    /** Gets the air pressure of the life support system.
     *  @return air pressure (atm)
     */
    public double getAirPressure() {
        // Return 1 atm for now
        return 1D;
    }

    /** Gets the temperature of the life support system.
     *  @return temperature (degrees C)
     */
    public double getTemperature() {
        // Return 25 degrees celsius for now
	return 25D;
    }

    /**
     * Gets the settlement's airlock.
     * @return settlement's airlock
     */
    public Airlock getAirlock() {
        return airlock;
    }
    
    /** Perform time-related processes
     *  @param time the amount of time passing (in millisols)
     */
    public void timePassing(double time) {
        facilityManager.timePassing(time);
	airlock.timePassing(time);
    }
}
