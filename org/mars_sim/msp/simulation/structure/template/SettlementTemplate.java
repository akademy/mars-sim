/**
 * Mars Simulation Project
 * SettlementTemplate.java
 * @version 2.75 2003-04-24
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.structure.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.mars_sim.msp.simulation.Coordinates;
import org.mars_sim.msp.simulation.Mars;
import org.mars_sim.msp.simulation.UnitManager;
import org.mars_sim.msp.simulation.equipment.EVASuit;
import org.mars_sim.msp.simulation.person.Person;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.building.BuildingManager;

/** 
 * The SettlementTemplate class represents a template from 
 * which settlements can be constructed from.
 */
public class SettlementTemplate {
    
    private String name;
    private Collection buildings;
    private Collection vehicles;
    
    /**
     * Constructor
     */
    public SettlementTemplate(String name) {
        this.name = name;
        buildings = new ArrayList();
        vehicles = new ArrayList();
    }
    
    /**
     * Gets the template's name.
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Adds a new building template.
     *
     * @param buildingName the name of the building to add.
     */
    public void addBuilding(String buildingName) {
        buildings.add(new BuildingTemplate(buildingName));
    }
    
    /**
     * Adds a new vehicle template.
     *
     * @param vehicleName the name of the vehicle to add.
     */
    public void addVehicle(String vehicleName) {
        vehicles.add(new VehicleTemplate(vehicleName));
    }
    
    /**
     * Creates a settlement instance from this template.
     *
     * @param name the name of the settlement.
     * @param location coordinate location of the settlement.
     * @param Mars the virtual Mars
     * @return constructed settlement
     */
    public Settlement constructSettlement(String name, Coordinates location, Mars mars) {
        
        UnitManager unitManager = mars.getUnitManager();
        
        // If name is "random", get a new name.
        if (name.equals("random")) name = unitManager.getNewName(UnitManager.SETTLEMENT);
        
        // Construct settlement
        Settlement settlement = new Settlement(name, location, mars);
        
        // Add buildings to settlement.
        Iterator buildingIter = buildings.iterator();
        while (buildingIter.hasNext()) {
            BuildingTemplate buildingTemplate = (BuildingTemplate) buildingIter.next();
            BuildingManager manager = settlement.getBuildingManager();
            try {
                manager.addBuilding(buildingTemplate.constructBuilding(manager));
            }
            catch (Exception e) {
                System.out.println("Error while constructing building: " + buildingTemplate.getName() + ": " + e.getMessage());
            }
        }
        
        // Add people to settlement.
        int popNum = settlement.getPopulationCapacity();
        for (int x=0; x < popNum; x++) {
            try {
                unitManager.addUnit(new Person(unitManager.getNewName(UnitManager.PERSON), settlement, mars));
            }
            catch (Exception e) {
                System.out.println("Error while constructing person: " + e.getMessage());
            }
        }
        
        // Add vehicles to settlement.
        Iterator vehicleIter = vehicles.iterator();
        while (vehicleIter.hasNext()) {
            VehicleTemplate vehicleTemplate = (VehicleTemplate) vehicleIter.next();
            try {
                unitManager.addUnit(vehicleTemplate.constructVehicle(unitManager.getNewName(UnitManager.VEHICLE), vehicleTemplate.getName(), settlement, mars));
            }
            catch (Exception e) {
                System.out.println("Error while constructing vehicle: " + vehicleTemplate.getName() + ": " + e.getMessage());
            }
        }
        
        // Add equipment to settlement.
        for (int x=0; x < settlement.getPopulationCapacity(); x++) {
            EVASuit evaSuit = new EVASuit(location, mars);
            unitManager.addUnit(evaSuit);
            settlement.getInventory().addUnit(evaSuit);
        }
        
        return settlement;
    }
    
    /**
     * Creates a settlement instance from this template.
     *
     * @param location coordinate location of the settlement.
     * @param Mars the virtual Mars
     * @return constructed settlement
     */
    public Settlement constructSettlement(Coordinates location, Mars mars) {
        UnitManager unitManager = mars.getUnitManager();
        return this.constructSettlement(unitManager.getNewName(UnitManager.SETTLEMENT), location, mars);
    }
}
