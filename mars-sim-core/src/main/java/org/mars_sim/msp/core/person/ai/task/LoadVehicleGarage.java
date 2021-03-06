/**
 * Mars Simulation Project
 * LoadVehicleGarage.java
 * @version 3.1.0 2017-03-03
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.equipment.EVASuit;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.equipment.EquipmentFactory;
import org.mars_sim.msp.core.person.NaturalAttributeType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RoboticAttributeType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The LoadVehicleGarage class is a task for loading a vehicle with fuel and supplies
 * in a vehicle maintenance garage.
 */
public class LoadVehicleGarage
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(LoadVehicleGarage.class.getName());

    private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1, logger.getName().length());

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.loadVehicleGarage"); //$NON-NLS-1$

	/** Comparison to indicate a small but non-zero amount. */
	private static final double SMALL_AMOUNT_COMPARISON = .0000001D;

	/** Task phases. */
    private static final TaskPhase LOADING = new TaskPhase(Msg.getString(
            "Task.phase.loading")); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .1D;

	/** The amount of resources (kg) one person of average strength can load per millisol. */
	private static double LOAD_RATE = 20D;

	/** The duration of the loading task (millisols). */
	private static double DURATION = RandomUtil.getRandomDouble(50D) + 10D;

	// Data members
	/** The vehicle that needs to be loaded. */
	private Vehicle vehicle;
	/** The person's settlement. */
	private Settlement settlement;

	private static PersonConfig personConfig;// = SimulationConfig.instance().getPersonConfiguration();

	/** Resources required to load. */
	private Map<Integer, Number> requiredResources;
	/** Resources desired to load but not required. */
	private Map<Integer, Number> optionalResources;
	/** Equipment required to load. */
	private Map<Integer, Integer> requiredEquipment;
	/** Equipment desired to load but not required. */
	private Map<Integer, Integer> optionalEquipment;

	public static AmountResource [] availableDesserts = PreparingDessert.getArrayOfDessertsAR();

	private static int oxygenID = ResourceUtil.oxygenID;
	private static int waterID = ResourceUtil.waterID;
	private static int foodID = ResourceUtil.foodID;
	private static int methaneID = ResourceUtil.methaneID;
	

	/**
	 * Constructor.
	 * @param person the person performing the task.
	 */
	public LoadVehicleGarage(Person person) {
    	// Use Task constructor
    	super(NAME, person, true, false, STRESS_MODIFIER, true, DURATION);

    	if (personConfig == null)
    		personConfig = SimulationConfig.instance().getPersonConfiguration();
    	
    	VehicleMission mission = getMissionNeedingLoading();
    	if (mission != null) {
    		vehicle = mission.getVehicle();
    		setDescription(Msg.getString("Task.description.loadVehicleGarage.detail",
    		        vehicle.getName())); //$NON-NLS-1$
    		requiredResources = mission.getRequiredResourcesToLoad();
    		optionalResources = mission.getOptionalResourcesToLoad();
    		requiredEquipment = mission.getRequiredEquipmentToLoad();
    		optionalEquipment = mission.getOptionalEquipmentToLoad();
    		
    		settlement = person.getSettlement();
    		if (settlement == null) {
    		    endTask();
    		}

    		// If vehicle is in a garage, add person to garage.
            Building garageBuilding = BuildingManager.getBuilding(vehicle);
            if (garageBuilding != null) {

                // Walk to garage.
                walkToActivitySpotInBuilding(garageBuilding, false);
            }

            // End task if vehicle or garage not available.
            if ((vehicle == null) || (garageBuilding == null)) {
                endTask();
            }

    		// Initialize task phase
            addPhase(LOADING);
            setPhase(LOADING);
    	}
    	else {
    		endTask();
    	}
    }
	public LoadVehicleGarage(Robot robot) {
    	// Use Task constructor
    	super(NAME, robot, true, false, STRESS_MODIFIER, true, DURATION);

    	VehicleMission mission = getMissionNeedingLoading();
    	if (mission != null) {
    		vehicle = mission.getVehicle();
    		setDescription(Msg.getString("Task.description.loadVehicleGarage.detail",
    		        vehicle.getName())); //$NON-NLS-1$
    		requiredResources = mission.getRequiredResourcesToLoad();
    		// TODO: add extra food/dessert as optionalResources
    		optionalResources = mission.getOptionalResourcesToLoad();
    		requiredEquipment = mission.getRequiredEquipmentToLoad();
    		optionalEquipment = mission.getOptionalEquipmentToLoad();
    		settlement = robot.getSettlement();
    		if (settlement == null) {
    		    endTask();
    		}

    		// If vehicle is in a garage, add robot to garage.
            Building garageBuilding = BuildingManager.getBuilding(vehicle);
            if (garageBuilding != null) {

                // Walk to garage.
                walkToActivitySpotInBuilding(garageBuilding, false);
            }

            // End task if vehicle or garage not available.
            if ((vehicle == null) || (garageBuilding == null)) {
                endTask();
            }

    		// Initialize task phase
            addPhase(LOADING);
            setPhase(LOADING);
    	}
    	else {
    		endTask();
    	}
    }

    /**
     * Constructor
     * @param person the person performing the task.
     * @param vehicle the vehicle to be loaded.
     * @param requiredResources a map of required resources to be loaded.
     * @param optionalResources a map of optional resources to be loaded.
     * @param requiredEquipment a map of required equipment to be loaded.
     * @param optionalEquipment a map of optional equipment to be loaded.
     */
    public LoadVehicleGarage(Person person, Vehicle vehicle, Map<Integer, Number> requiredResources,
            Map<Integer, Number> optionalResources, Map<Integer, Integer> requiredEquipment,
            Map<Integer, Integer> optionalEquipment) {
    	// Use Task constructor.
    	super("Loading vehicle", person, true, false, STRESS_MODIFIER, true, DURATION);

    	setDescription(Msg.getString("Task.description.loadVehicleGarage.detail",
                vehicle.getName())); //$NON-NLS-1$
        this.vehicle = vehicle;

        if (requiredResources != null) {
            this.requiredResources = new HashMap<Integer, Number>(requiredResources);
        }
        if (optionalResources != null) {
            this.optionalResources = new HashMap<Integer, Number>(optionalResources);
        }
        if (requiredEquipment != null) {
            this.requiredEquipment = new HashMap<>(requiredEquipment);
        }
        if (optionalEquipment != null) {
            this.optionalEquipment = new HashMap<>(optionalEquipment);
        }

        settlement = person.getSettlement();

        // If vehicle is in a garage, add person to garage.
        Building garage = BuildingManager.getBuilding(vehicle);
        if (garage != null) {

            // Walk to garage.
            walkToActivitySpotInBuilding(garage, false);
        }

        // Initialize task phase
        addPhase(LOADING);
        setPhase(LOADING);
    }
    public LoadVehicleGarage(Robot robot, Vehicle vehicle, Map<Integer, Number> requiredResources,
            Map<Integer, Number> optionalResources, Map<Integer, Integer> requiredEquipment,
            Map<Integer, Integer> optionalEquipment) {
    	// Use Task constructor.
    	super("Loading vehicle", robot, true, false, STRESS_MODIFIER, true, DURATION);

    	setDescription(Msg.getString("Task.description.loadVehicleGarage.detail",
                vehicle.getName())); //$NON-NLS-1$
        this.vehicle = vehicle;

        if (requiredResources != null) {
            this.requiredResources = new HashMap<Integer, Number>(requiredResources);
        }
        if (optionalResources != null) {
            this.optionalResources = new HashMap<Integer, Number>(optionalResources);
        }
        if (requiredEquipment != null) {
            this.requiredEquipment = new HashMap<>(requiredEquipment);
        }
        if (optionalEquipment != null) {
            this.optionalEquipment = new HashMap<>(optionalEquipment);
        }

        settlement = robot.getSettlement();

        // If vehicle is in a garage, add robot to garage.
        Building garage = BuildingManager.getBuilding(vehicle);
        if (garage != null) {

            // Walk to garage.
            walkToActivitySpotInBuilding(garage, false);
        }

        // Initialize task phase
        addPhase(LOADING);
        setPhase(LOADING);
    }

    @Override
    protected FunctionType getLivingFunction() {
        return FunctionType.GROUND_VEHICLE_MAINTENANCE;
    }

    @Override
    protected FunctionType getRoboticFunction() {
        return FunctionType.GROUND_VEHICLE_MAINTENANCE;
    }

    /**
     * Gets a list of all embarking vehicle missions at a settlement with vehicle
     * currently in a garage.
     * @param settlement the settlement.
     * @return list of vehicle missions.
     * @throws Exception if error finding missions.
     */
    public static List<Mission> getAllMissionsNeedingLoading(Settlement settlement) {

    	List<Mission> result = new ArrayList<Mission>();

    	MissionManager manager = Simulation.instance().getMissionManager();
    	Iterator<Mission> i = manager.getMissions().iterator();
    	while (i.hasNext()) {
    		Mission mission = i.next();
    		if (mission instanceof VehicleMission) {
    			if (VehicleMission.EMBARKING.equals(mission.getPhase())) {
    				VehicleMission vehicleMission = (VehicleMission) mission;
    				if (vehicleMission.hasVehicle()) {
    					Vehicle vehicle = vehicleMission.getVehicle();
    					if (settlement == vehicle.getSettlement()) {
    						if (!vehicleMission.isVehicleLoaded()) {
    					        if (BuildingManager.getBuilding(vehicle) != null) {
    					            result.add(vehicleMission);
    					        }
    						}
    					}
    				}
    			}
    		}
    	}

    	return result;
    }

    /**
     * Gets a random vehicle mission loading at the settlement.
     * @return vehicle mission.
     * @throws Exception if error finding vehicle mission.
     */
    private VehicleMission getMissionNeedingLoading() {

    	VehicleMission result = null;
    	List<Mission> loadingMissions = null;
		if (person != null)
	   		loadingMissions = getAllMissionsNeedingLoading(person.getSettlement());
		else if (robot != null)
    		loadingMissions = getAllMissionsNeedingLoading(robot.getSettlement());

    	if (loadingMissions.size() > 0) {
    		int index = RandomUtil.getRandomInt(loadingMissions.size() - 1);
    		result = (VehicleMission) loadingMissions.get(index);
    	}

    	return result;
    }

    /**
     * Gets the vehicle being loaded.
     * @return vehicle
     */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /**
     * Performs the method mapped to the task's current phase.
     * @param time the amount of time (millisol) the phase is to be performed.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    protected double performMappedPhase(double time) {
    	if (getPhase() == null) {
    	    throw new IllegalArgumentException("Task phase is null");
    	}
    	else if (LOADING.equals(getPhase())) {
    	    return loadingPhase(time);
    	}
    	else {
    	    return time;
    	}
    }

    /**
     * Perform the loading phase of the task.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the amount of time (millisol) after performing the phase.
     */
    double loadingPhase(double time) {

    	if (settlement == null) {
    		endTask();
    		return 0D;
    	}
        int strength = 0;
        // Determine load rate.
		if (person != null)
	       	strength = person.getNaturalAttributeManager().getAttribute(NaturalAttributeType.STRENGTH);
		else if (robot != null)
        	strength = robot.getRoboticAttributeManager().getAttribute(RoboticAttributeType.STRENGTH);
        double strengthModifier = .1D + (strength * .018D);
        double amountLoading = LOAD_RATE * strengthModifier * time;

        // Temporarily remove rover from settlement so that inventory doesn't get mixed in.
        Inventory sInv = settlement.getInventory();
        boolean roverInSettlement = false;
        if (sInv.containsUnit(vehicle)) {
            roverInSettlement = true;
            sInv.retrieveUnit(vehicle);
        }

        // Load equipment
        if (amountLoading > 0D) {
            amountLoading = loadEquipment(amountLoading);
        }

        // Load resources
        try {
            amountLoading = loadResources(amountLoading);
        }
        catch (Exception e) {
            //logger.severe(e.getMessage());
		    LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, e.getMessage(), null);
        }

        // Put rover back into settlement.
        if (roverInSettlement) {
            sInv.storeUnit(vehicle);
        }

        if (isFullyLoaded(requiredResources, optionalResources, requiredEquipment,
                optionalEquipment, vehicle, settlement)) {
            endTask();
        }

        return 0D;
    }

    /**
     * Loads the vehicle with required resources from the settlement.
     * @param amountLoading the amount (kg) the person can load in this time period.
     * @return the remaining amount (kg) the person can load in this time period.
     * @throws Exception if problem loading resources.
     */
    private double loadResources(double amountLoading) {

        // Load required resources.
        Iterator<Integer> iR = requiredResources.keySet().iterator();
        while (iR.hasNext() && (amountLoading > 0D)) {
        	Integer resource = iR.next();
        	if (resource < FIRST_ITEM_RESOURCE) {
        		// Load amount resources
        		amountLoading = loadAmountResource(amountLoading, resource, true);
        	}
        	else if (resource >= FIRST_ITEM_RESOURCE) {
        		// Load item resources
        		amountLoading = loadItemResource(amountLoading, resource, true);
        	}
        }

        // Load optional resources.
        Iterator<Integer> iR2 = optionalResources.keySet().iterator();
        while (iR2.hasNext() && (amountLoading > 0D)) {
            Integer resource = iR2.next();
            if (resource < FIRST_ITEM_RESOURCE) {
                // Load amount resources
                amountLoading = loadAmountResource(amountLoading, resource, false);
            }
            else if (resource >= FIRST_ITEM_RESOURCE) {
                // Load item resources
                amountLoading = loadItemResource(amountLoading, resource, false);
            }
        }

        // Return remaining amount that can be loaded by person this time period.
        return amountLoading;
    }

    /**
     * Loads the vehicle with an amount resource from the settlement.
     * @param amountLoading the amount (kg) the person can load in this time period.
     * @param resource the amount resource to be loaded.
     * @param required true if the amount resource is required to load, false if optional.
     * @return the remaining amount (kg) the person can load in this time period.
     */
    private double loadAmountResource(double amountLoading, Integer resource, boolean required) {

    	Inventory vInv = vehicle.getInventory();
        Inventory sInv = settlement.getInventory();

    	double amountNeededTotal = 0D;
    	if (required) {
    	    amountNeededTotal = (Double) requiredResources.get(resource);
    	}
    	else {
    	    if (requiredResources.containsKey(resource)) {
    	        amountNeededTotal += (Double) requiredResources.get(resource);
    	    }
    	    amountNeededTotal += (Double) optionalResources.get(resource);
    	}

		double amountAlreadyLoaded = vInv.getAmountResourceStored(resource, false);

		if (amountAlreadyLoaded < amountNeededTotal) {
			double amountNeeded = amountNeededTotal - amountAlreadyLoaded;
			boolean canLoad = true;
			String loadingError = "";

			// Check if enough resource in settlement inventory.
			double settlementStored = sInv.getAmountResourceStored(resource, false);
			if (settlementStored < amountNeeded) {
			    if (required) {
			        canLoad = false;
			        loadingError = "Not enough resource stored at settlement to load "
			                + "resource: " + resource + " needed: " + Math.round(amountNeeded*100D)/100D + ", stored: "
			                + settlementStored;
			    }
			    else {
			        amountNeeded = settlementStored;
			    }
			}

			// Check remaining capacity in vehicle inventory.
			double remainingCapacity = vInv.getAmountResourceRemainingCapacity(resource, true, false);
			if (remainingCapacity < amountNeeded) {
			    if (required) {
			        if ((amountNeeded - remainingCapacity) < .00001D) {
			            amountNeeded = remainingCapacity;
			        }
			        else {
			            canLoad = false;
			            loadingError = "Not enough capacity in vehicle for loading resource "
			                    + resource + ": " + Math.round(amountNeeded*100D)/100D + ", remaining capacity: "
			                    + remainingCapacity;
			        }
			    }
			    else {
			        amountNeeded = remainingCapacity;
			    }
			}

			// Determine amount to load.
			double resourceAmount = amountNeeded;
            if (amountNeeded > amountLoading) {
                resourceAmount = amountLoading;
            }

			if (canLoad) {

			    // Load resource from settlement inventory to vehicle inventory.
                try {
                    sInv.retrieveAmountResource(resource, resourceAmount);
                    vInv.storeAmountResource(resource, resourceAmount, true);
       			 	// 2015-01-15 Add addSupplyAmount()
                    //vInv.addAmountSupplyAmount(resource, resourceAmount);
                }
                catch (Exception e) {
                    e.printStackTrace(System.err);
                }
                amountLoading -= resourceAmount;
			}
			else {
			    endTask();
			    LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, loadingError, null);
			    //throw new IllegalStateException(loadingError);
			    
			}
		}
		else {
		    if (required && optionalResources.containsKey(resource)) {
		        amountNeededTotal += (Double) optionalResources.get(resource);
		    }

		    if (amountAlreadyLoaded > amountNeededTotal) {

		        // In case vehicle wasn't fully unloaded first.
		        double amountToRemove = amountAlreadyLoaded - amountNeededTotal;
		        try {
		            vInv.retrieveAmountResource(resource, amountToRemove);
		            sInv.storeAmountResource(resource, amountToRemove, true);
		        }
		        catch (Exception e) {}
		    }
		}

		//  Return remaining amount that can be loaded by person this time period.
		return amountLoading;
    }

    /**
     * Loads the vehicle with an item resource from the settlement.
     * @param amountLoading the amount (kg) the person can load in this time period.
     * @param resource the item resource to be loaded.
     * @param required true if the item resource is required to load, false if optional.
     * @return the remaining amount (kg) the person can load in this time period.
     */
    private double loadItemResource(double amountLoading, Integer id, boolean required) {

    	Inventory vInv = vehicle.getInventory();
        Inventory sInv = settlement.getInventory();

        int numNeededTotal = 0;
        if (required) {
            numNeededTotal = (int) requiredResources.get(id);
        }
        else {
            if (requiredResources.containsKey(id)) {
                numNeededTotal += (int) requiredResources.get(id);
            }
            numNeededTotal += (int) optionalResources.get(id);
        }

		int numAlreadyLoaded = vInv.getItemResourceNum(id);

		Part p = ItemResourceUtil.findItemResource(id);
		
		if (numAlreadyLoaded < numNeededTotal) {
			int numNeeded = numNeededTotal - numAlreadyLoaded;
			boolean canLoad = true;
            String loadingError = "";

            // Check if enough resource in settlement inventory.
            int settlementStored = sInv.getItemResourceNum(id);
            if (settlementStored < numNeeded) {
                if (required) {
                    canLoad = false;
                    loadingError = "Not enough resource stored at settlement to load "
                            + "resource: " + p + " needed: " + numNeeded + ", stored: "
                            + settlementStored;
                }
                else {
                    numNeeded = settlementStored;
                }
            }

            // Check remaining capacity in vehicle inventory.
            double remainingMassCapacity = vInv.getRemainingGeneralCapacity(false);
            if (remainingMassCapacity < (numNeeded * p.getMassPerItem())) {
                if (required) {
                    canLoad = false;
                    loadingError = "Not enough capacity in vehicle for loading resource "
                            + p + ": " + numNeeded + ", remaining capacity: "
                            + remainingMassCapacity + " kg";
                }
                else {
                    numNeeded = (int) (remainingMassCapacity / p.getMassPerItem());
                }
            }

		    // Determine amount to load.
		    int resourceNum = (int) (amountLoading / p.getMassPerItem());
		    if (resourceNum < 1) {
		        resourceNum = 1;
		    }
		    if (resourceNum > numNeeded) {
		        resourceNum = numNeeded;
		    }

		    if (canLoad) {

		        // Load resource from settlement inventory to vehicle inventory.
		        sInv.retrieveItemResources(id, resourceNum);
		        vInv.storeItemResources(id, resourceNum);
		        amountLoading -= (resourceNum * p.getMassPerItem());
		        if (amountLoading < 0D) amountLoading = 0D;
		    }
		    else {
		        endTask();
		        throw new IllegalStateException(loadingError);
		    }
		}
		else {
		    if (required && optionalResources.containsKey(id)) {
		        numNeededTotal += (int) optionalResources.get(id);
		    }

		    if (numAlreadyLoaded > numNeededTotal) {

		        // In case vehicle wasn't fully unloaded first.
		        int numToRemove = numAlreadyLoaded - numNeededTotal;
		        try {
		            vInv.retrieveItemResources(id, numToRemove);
		            sInv.storeItemResources(id, numToRemove);
		        }
		        catch (Exception e) {}
		    }
		}

		// Return remaining amount that can be loaded by person this time period.
		return amountLoading;
    }

    /**
     * Loads the vehicle with required and optional equipment from the settlement.
     * @param amountLoading the amount (kg) the person can load in this time period.
     * @return the remaining amount (kg) the person can load in this time period.
     */
    private double loadEquipment(double amountLoading) {

        // Load required equipment.
        amountLoading = loadRequiredEquipment(amountLoading);

        // Load optional equipment.
        amountLoading = loadOptionalEquipment(amountLoading);

		// Return remaining amount that can be loaded by person this time period.
		return amountLoading;
    }

    /**
     * Loads the vehicle with required equipment from the settlement.
     * @param amountLoading the amount (kg) the person can load in this time period.
     * @return the remaining amount (kg) the person can load in this time period.
     */
    private double loadRequiredEquipment(double amountLoading) {

        Inventory vInv = vehicle.getInventory();
        Inventory sInv = settlement.getInventory();

        Iterator<Integer> iE = requiredEquipment.keySet().iterator();
        while (iE.hasNext() && (amountLoading > 0D)) {
        	Integer equipmentType = iE.next();
            int numNeededTotal = requiredEquipment.get(equipmentType);
            int numAlreadyLoaded = vInv.findNumUnitsOfClass(equipmentType);
            if (numAlreadyLoaded < numNeededTotal) {
                int numNeeded = numNeededTotal - numAlreadyLoaded;
                Collection<Unit> units = sInv.findAllUnitsOfClass(equipmentType);
                Object[] array  = units.toArray();

                if (units.size() >= numNeeded) {
                    int loaded = 0;
                    for (int x = 0; (x < units.size()) && (loaded < numNeeded) && (amountLoading > 0D); x++) {
                        Equipment eq = (Equipment) array[x];

                        boolean isEmpty = true;
                        Inventory eInv = eq.getInventory();
                        if (eInv != null) {
                            isEmpty = eq.getInventory().isEmpty(false);
                        }

                        if (isEmpty) {
                            if (vInv.canStoreUnit(eq, false)) {
                                sInv.retrieveUnit(eq);
                                vInv.storeUnit(eq);
                                amountLoading -= eq.getMass();
                                if (amountLoading < 0D) {
                                    amountLoading = 0D;
                                }
                                loaded++;
                            }
                            else {
                                //logger.warning(vehicle + " cannot store " + eq);
                			    LogConsolidated.log(logger, Level.WARNING, 5000, sourceName, 
                			    		vehicle + " cannot store " + eq, null);
                                endTask();
                            }
                        }
                    }

                    array = null;
                }
                else {
                    endTask();
                }
            }
            else {

                if (optionalEquipment.containsKey(equipmentType)) {
                    numNeededTotal += optionalEquipment.get(equipmentType);
                }

                if (numAlreadyLoaded > numNeededTotal) {

                    // In case vehicle wasn't fully unloaded first.
                    int numToRemove = numAlreadyLoaded - numNeededTotal;
                    Collection<Unit> units = vInv.findAllUnitsOfClass(equipmentType);
                    Object[] array = units.toArray();

                    for (int x = 0; x < numToRemove; x++) {
                        Equipment eq = (Equipment) array[x];
                        vInv.retrieveUnit(eq);
                        sInv.storeUnit(eq);
                    }

                    array = null;
                }
            }
        }

        // Return remaining amount that can be loaded by person this time period.
        return amountLoading;
    }

    /**
     * Loads the vehicle with optional equipment from the settlement.
     * @param amountLoading the amount (kg) the person can load in this time period.
     * @return the remaining amount (kg) the person can load in this time period.
     */
    private double loadOptionalEquipment(double amountLoading) {

        Inventory vInv = vehicle.getInventory();
        Inventory sInv = settlement.getInventory();

        Iterator<Integer> iE = optionalEquipment.keySet().iterator();
        while (iE.hasNext() && (amountLoading > 0D)) {
        	Integer equipmentType = iE.next();
            int numNeededTotal = optionalEquipment.get(equipmentType);
            if (requiredEquipment.containsKey(equipmentType)) {
                numNeededTotal += requiredEquipment.get(equipmentType);
            }
            int numAlreadyLoaded = vInv.findNumUnitsOfClass(equipmentType);
            if (numAlreadyLoaded < numNeededTotal) {
                int numNeeded = numNeededTotal - numAlreadyLoaded;
                Collection<Unit> units = sInv.findAllUnitsOfClass(equipmentType);
                Object[] array  = units.toArray();

                if (units.size() < numNeeded) {
                    numNeeded = units.size();
                }

                int loaded = 0;
                for (int x = 0; (x < units.size()) && (loaded < numNeeded) && (amountLoading > 0D); x++) {
                    Equipment eq = (Equipment) array[x];

                    boolean isEmpty = true;
                    Inventory eInv = eq.getInventory();
                    if (eInv != null) {
                        isEmpty = eq.getInventory().isEmpty(false);
                    }

                    if (isEmpty) {
                        if (vInv.canStoreUnit(eq, false)) {
                            sInv.retrieveUnit(eq);
                            vInv.storeUnit(eq);
                            amountLoading -= eq.getMass();
                            if (amountLoading < 0D) {
                                amountLoading = 0D;
                            }
                            loaded++;
                        }
                        else {
                            //logger.warning(vehicle + " cannot store " + eq);
                            LogConsolidated.log(logger, Level.WARNING, 5000, sourceName, 
            			    		vehicle + " cannot store " + eq, null);
                            endTask();
                        }
                    }
                }

                array = null;
            }
            else if (numAlreadyLoaded > numNeededTotal) {

                // In case vehicle wasn't fully unloaded first.
                int numToRemove = numAlreadyLoaded - numNeededTotal;
                Collection<Unit> units = vInv.findAllUnitsOfClass(equipmentType);
                Object[] array = units.toArray();

                for (int x = 0; x < numToRemove; x++) {
                    Equipment eq = (Equipment) array[x];
                    vInv.retrieveUnit(eq);
                    sInv.storeUnit(eq);
                }

                array = null;
            }
        }

        // Return remaining amount that can be loaded by person this time period.
        return amountLoading;
    }

	/**
	 * Adds experience to the person's skills used in this task.
	 * @param time the amount of time (ms) the person performed this task.
	 */
	protected void addExperience(double time) {
		// This task adds no experience.
	}

    /**
     * Checks if there are enough supplies in the settlement's stores to supply trip.
     * @param settlement the settlement the vehicle is at.
     * @param resources a map of resources required for the trip.
     * @param equipment a map of equipment required for the trip.
     * @param vehicleCrewNum the number of people in the vehicle crew.
     * @param tripTime the estimated time for the trip (millisols).
     * @return true if enough supplies
     * @throws Exception if error checking supplies.
     */
    public static synchronized boolean hasEnoughSupplies(Settlement settlement, Vehicle vehicle, Map <Integer, Number> resources,
    		Map<Integer, Integer> equipment, int vehicleCrewNum, double tripTime) {

    	// Check input parameters.
    	if (settlement == null) throw new IllegalArgumentException("settlement is null");

        boolean enoughSupplies = true;
        Inventory inv = settlement.getInventory();
        Inventory vInv = vehicle.getInventory();

        boolean roverInSettlement = false;
        if (inv.containsUnit(vehicle)) {
        	roverInSettlement = true;
        	inv.retrieveUnit(vehicle);
        }

        // Check if there are enough resources at the settlement.

        for (Integer resource : resources.keySet()) {

        	if (resource < FIRST_ITEM_RESOURCE) {       		
        		// Added all desserts to the matching test
        	 	boolean isDessert = false;
             	double amountDessertLoaded = 0;
            	double totalAmountDessertStored = 0;
        		double settlementDessertNeed = 0;
        		double amountDessertNeeded = (Double) resources.get(resource);
        		
           	  	// Put together a list of available dessert
                for (AmountResource dessert : availableDesserts) {
                	if (ResourceUtil.findAmountResource(resource).getName().equals(dessert.getName())) {
    	        		// 2015-03-15 Added the amount of all six desserts together
    	        		amountDessertLoaded += vInv.getARStored(resource, false);
    	        		totalAmountDessertStored += inv.getARStored(resource, false);
    	        		settlementDessertNeed += getSettlementNeed(settlement, vehicleCrewNum,
    	        		        resource, tripTime);
    	        		isDessert = true;
                	}
                }

                if (isDessert) {

	                double totalDessertNeeded = amountDessertNeeded + settlementDessertNeed - amountDessertLoaded;

	        		if (totalAmountDessertStored < totalDessertNeeded) {
	        			if (logger.isLoggable(Level.INFO))
	        				LogConsolidated.log(logger, Level.INFO, 5000, sourceName, "desserts needed: " 
	        						+ Math.round(totalDessertNeeded*10.0)/10.0  
	        						+ " total stored: " + Math.round(totalAmountDessertStored*10.0)/10.0 , null);
	        			enoughSupplies = false;
	        		}
                }

        		else  { // this resource is not a dessert
	        		double amountNeeded = (Double) resources.get(resource);
	        		double settlementNeed = getSettlementNeed(settlement, vehicleCrewNum, resource, tripTime);
	        		double amountLoaded = vInv.getARStored(resource, false);
	        		double totalNeeded = amountNeeded + settlementNeed - amountLoaded;
	        		if (inv.getARStored(resource, false) < totalNeeded) {
	        			double stored = inv.getARStored(resource, false);
	        			if (logger.isLoggable(Level.INFO))
	        				LogConsolidated.log(logger, Level.INFO, 5000, sourceName, ResourceUtil.findAmountResource(resource).getName() 
	        						+ " needed: " + Math.round(totalNeeded*10.0)/10.0 
	        						+ " stored: " + Math.round(stored*10.0)/10.0 , null);
	        			//enoughSupplies = false;
	        			return false;
	        		}
                }
        	}

        	else if (resource >= FIRST_ITEM_RESOURCE) {
        		int numNeeded = (Integer) resources.get(resource);
        		int settlementNumPartNeed = getRemainingSettlementNum(settlement, vehicleCrewNum, resource);
        		int numLoaded = vInv.getItemResourceNum(resource);
        		int totalNeeded = numNeeded + settlementNumPartNeed - numLoaded;
        		if (inv.getItemResourceNum(resource) < totalNeeded) {
        			int stored = inv.getItemResourceNum(resource);
        			if (logger.isLoggable(Level.INFO))
        				LogConsolidated.log(logger, Level.INFO, 5000, sourceName, ItemResourceUtil.findItemResource(resource).getName() 
        						+ " needed: " + totalNeeded + " stored: " + stored, null);
        			//enoughSupplies = false;
        			return false;
        		}
        	}
        	else throw new IllegalStateException("Unknown resource type: " + resource);
        }

        // Check if there is enough equipment at the settlement.
        Iterator<Integer> iE = equipment.keySet().iterator();
        while (iE.hasNext()) {
        	Integer equipmentType = iE.next();
        	int numNeeded = equipment.get(equipmentType);
        	int settlementEquipmentNumNeed = getRemainingSettlementNum(settlement, vehicleCrewNum, equipmentType);
        	int numLoaded = vInv.findNumUnitsOfClass(equipmentType);
    		int totalNeeded = numNeeded + settlementEquipmentNumNeed - numLoaded;
        	if (inv.findNumEmptyUnitsOfClass(equipmentType, false) < totalNeeded) {
        		int stored = inv.findNumEmptyUnitsOfClass(equipmentType, false);
    			if (logger.isLoggable(Level.INFO))
    				LogConsolidated.log(logger, Level.INFO, 5000, sourceName, 
    						equipmentType + " needed: " + totalNeeded + " stored: " + stored, null);
        		//enoughSupplies = false;
        		return false;
        	}
        }

        if (roverInSettlement) inv.storeUnit(vehicle);

        return enoughSupplies;
    }

    /**
     * Gets the amount of an amount resource that should remain at the settlement.
     * @param settlement the settlement
     * @param vehicleCrewNum the number of crew leaving on the vehicle.
     * @param resource the amount resource
     * @param double tripTime the estimated trip time (millisols).
     * @return remaining amount (kg)
     * @throws Exception if error getting the remaining amount.
     */
    static double getSettlementNeed(Settlement settlement, int vehicleCrewNum,
    		Integer resource, double tripTime) {
    	int remainingPeopleNum = settlement.getIndoorPeopleCount() - vehicleCrewNum;
    	double amountPersonPerSol = 0D;
    	double tripTimeSols = tripTime / 1000D;

    	if (personConfig == null)
    		personConfig = SimulationConfig.instance().getPersonConfiguration();
    	//	throw new IllegalArgumentException("personConfig is null");
    	
    	// Only life support resources are required at settlement at this time.
    	if (resource == oxygenID) 
    		amountPersonPerSol = personConfig.getNominalO2ConsumptionRate();
    	else if (resource == waterID) 
    		amountPersonPerSol = personConfig.getWaterConsumptionRate();
    	else if (resource == foodID) 
      	  // Divide by 3 arbitrarily since settlers prefer to have meals rather than preserved food
    		amountPersonPerSol = personConfig.getFoodConsumptionRate() / 3D; 
    	else {
	    	// check if this resource is a dessert
	        for (AmountResource dessert : availableDesserts) {
	           	if (ResourceUtil.findAmountResource(resource).getName().equals(dessert.getName())) {
	          		amountPersonPerSol = PreparingDessert.getDessertMassPerServing();
	          		break;
	        	}
	        }
        }

    	return remainingPeopleNum * (amountPersonPerSol * tripTimeSols);
    }

    /**
     * Gets the number of an item resource that should remain at the settlement.
     * @param settlement the settlement
     * @param vehicleCrewNum the number of crew leaving on the vehicle.
     * @param resource the item resource
     * @return remaining number
     * @throws Exception if error getting the remaining number.
     */
    private static int getRemainingSettlementNum(Settlement settlement, int vehicleCrewNum,
    		Integer resource) {
    	// No item resources required at settlement at this time.
    	return 1;
    }

    /**
     * Gets the number of an equipment type that should remain at the settlement.
     * @param settlement the settlement
     * @param vehicleCrewNum the number of crew leaving on the vehicle.
     * @param equipmentType the equipment type class.
     * @return remaining number.
     * @throws Exception if error getting the remaining number.
     */
    private static int getRemainingSettlementNum(Settlement settlement, int vehicleCrewNum,
            Class equipmentType) {
    	int remainingPeopleNum = settlement.getIndoorPeopleCount() - vehicleCrewNum;
    	// Leave one EVA suit for every four remaining people at settlement (min 1).
    	if (equipmentType == EVASuit.class) {
    		int minSuits = remainingPeopleNum / 4;
    		if (minSuits == 0) {
    		    minSuits = 1;
    		}
    		return minSuits;
    	}
    	else {
    	    return 0;
    	}
    }

    /**
     * Checks if a vehicle has enough storage capacity for the supplies needed on the trip.
     * @param resources a map of the resources required.
     * @param equipment a map of the equipment types and numbers needed.
     * @param vehicle the vehicle to check.
     * @param settlement the settlement to disembark from.
     * @return true if vehicle can carry supplies.
     * @throws Exception if error
     */
    public static boolean enoughCapacityForSupplies(Map<Integer, Number> resources,
            Map<Integer, Integer> equipment, Vehicle vehicle, Settlement settlement) {

    	boolean sufficientCapacity = true;

    	// Create vehicle inventory clone.
    	Inventory inv = vehicle.getInventory().clone(null);

    	try {
    		// Add equipment clones.
    		Iterator<Integer> i = equipment.keySet().iterator();
    		while (i.hasNext()) {
    			Integer equipmentType = i.next();
    			int num = equipment.get(equipmentType);
    			Coordinates defaultLoc = new Coordinates(0D, 0D);
    			for (int x = 0; x < num; x++)
    				inv.storeUnit(EquipmentFactory.createEquipment(
    						equipmentType, 
    						defaultLoc, 
    						false));
    		}

    		// Add all resources.
    		Iterator<Integer> j = resources.keySet().iterator();
    		while (j.hasNext()) {
    			Integer resource = j.next();
    			
    			if (resource < FIRST_ITEM_RESOURCE) {
    				double amount = (Double) (resources.get(resource));
    				inv.storeAR(resource, amount, true);
    			}
    			else {
    				int num = (Integer) (resources.get(resource));
    				inv.storeItemResources(resource, num);
    			}
    		}
    	}
    	catch (Exception e) {
            //logger.info(e.getMessage());
		    LogConsolidated.log(logger, Level.SEVERE, 5000, sourceName, 
		    		"Not enough capacity to store needed resources for the proposed mission. " + e.getMessage(), null);
    		sufficientCapacity = false;
    	}

    	return sufficientCapacity;
    }

    /**
     * Checks if the vehicle is fully loaded with supplies.
     * @param requiredResources the resources that are required for the trip.
     * @param optionalResources the resources that are optional for the trip.
     * @param requiredEquipment the equipment that is required for the trip.
     * @param optionalEquipment the equipment that is optional for the trip.
     * @param vehicle the vehicle that is being checked.
     * @param settlement the settlement that the vehicle is being loaded from.
     * @return true if vehicle is fully loaded.
     */
    public static boolean isFullyLoaded(Map<Integer, Number> requiredResources,
            Map<Integer, Number> optionalResources, Map<Integer, Integer> requiredEquipment,
            Map<Integer, Integer> optionalEquipment, Vehicle vehicle, Settlement settlement) {

    	boolean sufficientSupplies = true;

        // Check if there are enough resources in the vehicle.
        sufficientSupplies = isFullyLoadedWithResources(requiredResources, optionalResources,
                vehicle, settlement);

        // Check if there is enough equipment in the vehicle.
        if (sufficientSupplies) sufficientSupplies = isFullyLoadedWithEquipment(requiredEquipment,
                optionalEquipment, vehicle, settlement);

        return sufficientSupplies;
    }

    /**
     * Checks if the vehicle is fully loaded with resources.
     * @param requiredResources the resources that are required for the trip.
     * @param optionalResources the resources that are optional for the trip.
     * @param vehicle the vehicle.
     * @param settlement the settlement that the vehicle is being loaded from.
     * @return true if vehicle is loaded.
     */
    private static boolean isFullyLoadedWithResources(Map<Integer, Number> requiredResources,
            Map<Integer, Number> optionalResources, Vehicle vehicle, Settlement settlement) {

    	if (vehicle == null) {
    	    throw new IllegalArgumentException("vehicle is null");
    	}

    	boolean sufficientSupplies = true;
        Inventory vInv = vehicle.getInventory();
        Inventory sInv = settlement.getInventory();

        // Check that required resources are loaded first.
        Iterator<Integer> iR = requiredResources.keySet().iterator();
        while (iR.hasNext() && sufficientSupplies) {
        	Integer resource = iR.next();
        	if (resource < FIRST_ITEM_RESOURCE) {
        		double amount = (double) requiredResources.get(resource);
        		double storedAmount = vInv.getARStored(resource, false);
        		if (storedAmount < (amount - SMALL_AMOUNT_COMPARISON)) {
        		    sufficientSupplies = false;
        		}
        	}
        	else if (resource >= FIRST_ITEM_RESOURCE) {
        		int num = (Integer) (requiredResources.get(resource));
        		if (vInv.getItemResourceNum(resource) < num) {
        		    sufficientSupplies = false;
        		}
        	}
        	else {
        	    throw new IllegalStateException("Unknown resource type: " + resource);
        	}
        }

        // Check that optional resources are loaded or can't be loaded.
        Iterator<Integer> iR2 = optionalResources.keySet().iterator();
        while (iR2.hasNext() && sufficientSupplies) {
        	Integer resource = iR2.next();
            if (resource < FIRST_ITEM_RESOURCE) {

                //AmountResource amountResource = (AmountResource) resource;
                double amount = (Double) optionalResources.get(resource);
                if (requiredResources.containsKey(resource)) {
                    amount += (Double) requiredResources.get(resource);
                }

                double storedAmount = vInv.getARStored(resource, false);
                if (storedAmount < (amount - SMALL_AMOUNT_COMPARISON)) {
                    // Check if enough capacity in vehicle.
                    double vehicleCapacity = vInv.getARRemainingCapacity(resource, true, false);
                    boolean hasVehicleCapacity = (vehicleCapacity >= (amount - storedAmount));

                    // Check if enough stored in settlement.
                    double storedSettlement = sInv.getARStored(resource, false);
                    if (settlement.getParkedVehicles().contains(vehicle)) {
                        storedSettlement -= storedAmount;
                    }
                    boolean hasStoredSettlement = (storedSettlement >= (amount - storedAmount));

                    if (hasVehicleCapacity && hasStoredSettlement) {
                        sufficientSupplies = false;
                    }
                }
            }
            else if (resource >= FIRST_ITEM_RESOURCE) {

                //ItemResource itemResource = (ItemResource) resource;
                int num = (Integer) (optionalResources.get(resource));
                if (requiredResources.containsKey(resource)) {
                    num += (Integer) (requiredResources.get(resource));
                }

                int storedNum = vInv.getItemResourceNum(resource);
                if (storedNum < num) {
                    // Check if enough capacity in vehicle.
                    double vehicleCapacity = vInv.getRemainingGeneralCapacity(false);
                    boolean hasVehicleCapacity = (vehicleCapacity >= ((num - storedNum) * ItemResourceUtil.findItemResource(resource).getMassPerItem()));

                    // Check if enough stored in settlement.
                    int storedSettlement = sInv.getItemResourceNum(resource);
                    if (settlement.getParkedVehicles().contains(vehicle)) {
                        storedSettlement -= storedNum;
                    }
                    boolean hasStoredSettlement = (storedSettlement >= (num - storedNum));

                    if (hasVehicleCapacity && hasStoredSettlement) {
                        sufficientSupplies = false;
                    }
                }
            }
            else {
                throw new IllegalStateException("Unknown resource type: " + resource);
            }
        }

        return sufficientSupplies;
    }

    /**
     * Checks if the vehicle is fully loaded with resources.
     * @param requiredEquipment the equipment that is required for the trip.
     * @param optionalEquipment the equipment that is optional for the trip.
     * @param vehicle the vehicle.
     * @param settlement the settlement that the vehicle is being loaded from.
     * @return true if vehicle is full loaded.
     */
    private static boolean isFullyLoadedWithEquipment(Map<Integer, Integer> requiredEquipment,
            Map<Integer, Integer> optionalEquipment, Vehicle vehicle, Settlement settlement) {

    	if (vehicle == null) {
    	    throw new IllegalArgumentException("vehicle is null");
    	}

    	boolean sufficientSupplies = true;
        Inventory vInv = vehicle.getInventory();
        Inventory sInv = settlement.getInventory();

        // Check that required equipment is loaded first.
        Iterator<Integer> iE = requiredEquipment.keySet().iterator();
        while (iE.hasNext() && sufficientSupplies) {
        	Integer equipmentType = iE.next();
        	int num = requiredEquipment.get(equipmentType);
        	if (vInv.findNumUnitsOfClass(equipmentType) < num) {
        	    sufficientSupplies = false;
        	}
        }

        // Check that optional equipment is loaded or can't be loaded.
        Iterator<Integer> iE2 = optionalEquipment.keySet().iterator();
        while (iE2.hasNext() && sufficientSupplies) {
        	Integer equipmentType = iE2.next();
            int num = optionalEquipment.get(equipmentType);
            if (requiredEquipment.containsKey(equipmentType)) {
                num += requiredEquipment.get(equipmentType);
            }

            int storedNum = vInv.findNumUnitsOfClass(equipmentType);
            if (storedNum < num) {

                // Check if enough stored in settlement.
                int storedSettlement = sInv.findNumEmptyUnitsOfClass(equipmentType, false);
                if (settlement.getParkedVehicles().contains(vehicle)) {
                    storedSettlement -= storedNum;
                }
                boolean hasStoredSettlement = (storedSettlement >= (num - storedNum));

                if (hasStoredSettlement) {
                    sufficientSupplies = false;
                }
            }
        }

        return sufficientSupplies;
    }

	/**
	 * Gets the effective skill level a person has at this task.
	 * @return effective skill level
	 */
	public int getEffectiveSkillLevel() {
		return 0;
	}

	/**
	 * Gets a list of the skills associated with this task.
	 * May be empty list if no associated skills.
	 * @return list of skills
	 */
	public List<SkillType> getAssociatedSkills() {
		return new ArrayList<SkillType>(0);
	}

	@Override
	public void destroy() {
	    super.destroy();

	    vehicle = null;
	    settlement = null;

	    if (requiredResources != null) {
	        requiredResources.clear();
	    }
	    requiredResources = null;

	    if (optionalResources != null) {
	        optionalResources.clear();
        }
	    optionalResources = null;

	    if (requiredEquipment != null) {
	        requiredEquipment.clear();
	    }
	    requiredEquipment = null;

	    if (optionalEquipment != null) {
	        optionalEquipment.clear();
        }
	    optionalEquipment = null;
	}
}