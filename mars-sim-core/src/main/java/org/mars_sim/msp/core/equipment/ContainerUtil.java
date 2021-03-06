/**
 * Mars Simulation Project
 * ContainerUtil.java
 * @version 3.1.0 2017-09-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.equipment;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.resource.PhaseType;
import org.mars_sim.msp.core.resource.ResourceUtil;

/**
 * A utility class for containers.
 */
public final class ContainerUtil {

	private static final Coordinates coordinates = new Coordinates(0D, 0D);

	/**
	 * Private constructor for utility class.
	 */
	private ContainerUtil() {};

	/**
	 * Gets the type of container needed to hold a particular resource.
	 * @param resource the id of the resource to hold.
	 * @return container id.
	 */
	public static int getContainerClassIDToHoldResource(
			int resource
		) {	
			return getContainerID(ResourceUtil.findAmountResource(resource).getPhase());
		}
	
	/**
	 * Gets the type of container needed to hold a particular resource.
	 * @param resource the id of the resource to hold.
	 * @return container class or null if none found.
	 */
	public static Class<? extends Container> getContainerClassToHoldResource(
		int resource
	) {	
		return getContainerTypeNeeded(ResourceUtil.findAmountResource(resource).getPhase());
	}

	/**
	 * Gets the container type needed for an amount resource phase.
	 * @param phase the phase type of the amount resource.
	 * @return container id.
	 */
	public static int getContainerID(PhaseType phase) {
		int result = -1;
		switch (phase) {
			case GAS : result = EquipmentType.GAS_CANISTER.ordinal();//str2int("Gas Canister");; 
			break;
			case LIQUID : result = EquipmentType.BARREL.ordinal();//.str2int("Barrel"); 
			break;
			case SOLID : result = EquipmentType.BAG.ordinal();//.str2int("Bag");
			break;
			//System.out.println("ContainerUtil : Can't match " + phase + " to any known phases.");
		}
		return result;
	}
	
	/**
	 * Gets the container type needed for an amount resource phase.
	 * @param phase the phase type of the amount resource.
	 * @return container class.
	 */
	public static Class<? extends Container> getContainerTypeNeeded(PhaseType phase) {
		Class<? extends Container> result = null;
		switch (phase) {
			case GAS : result = GasCanister.class; break;
			case LIQUID : result = Barrel.class; break;
			case SOLID : result = Bag.class;
			//System.out.println("ContainerUtil : Can't match " + phase + " to any known phases.");
		}
		return result;
	}

	/**
	 * Gets the capacity of the container.
	 * @param containerClass the container class.
	 * @return capacity (kg).
	 */
	public static double getContainerCapacity(Class<? extends Container> containerClass) {

		if (containerClass == GasCanister.class)
			return GasCanister.CAPACITY;
		else if (containerClass == Barrel.class)
			return Barrel.CAPACITY;
		else if (containerClass == Bag.class)
			return Bag.CAPACITY;
		else 
			return 0;
		
		// Note : not an inefficient way of finding the phase type of a container 
//		double result = 0D;
////		Class<? extends Equipment> equipmentClass = (Class<? extends Equipment>) containerClass;
//		Container container = (Container) EquipmentFactory.createEquipment((Class<? extends Equipment>) containerClass, coordinates, true);
//		if (container != null) {
//			result = container.getTotalCapacity();
//		}
//
//		return result;
	}
	
	/**
	 * Gets the capacity of the container.
	 * @param containerClass the container class.
	 * @return capacity (kg).
	 */
	public static double getContainerCapacity(int id) {

		if (id == EquipmentType.GAS_CANISTER.ordinal())
			return GasCanister.CAPACITY;
		else if (id == EquipmentType.BARREL.ordinal())
			return Barrel.CAPACITY;
		else if (id == EquipmentType.BAG.ordinal())
			return Bag.CAPACITY;
		else 
			return 0;

// Note : inefficient way of finding the total capacity of a container to create a container
//		double result = 0D;		
////		Class<? extends Equipment> u = EquipmentFactory.getEquipmentClass(EquipmentType.int2enum(id).getName());		
//		Container container = (Container) EquipmentFactory.createEquipment(
//				EquipmentFactory.getEquipmentClass(EquipmentType.int2enum(id).getName()),
//				coordinates, true);
//		if (container != null) {
//			result = container.getTotalCapacity();
//		}
//
//		return result;
	}
	/**
	 * Gets the phase of amount resource that a container can hold.
	 * @param containerClass the container class.
	 * @return amount resource phase.
	 */
	public static PhaseType getContainerPhase(Class<? extends Container> containerClass) {
	    
	    PhaseType result = null;
	    
	 // Note : not an inefficient way of finding the phase type of a container 
	    Class<? extends Equipment> equipmentClass = (Class<? extends Equipment>) containerClass;
	    Container container = (Container) EquipmentFactory.createEquipment(equipmentClass, coordinates, true);
        if (container != null) {
            result = container.getContainingResourcePhase();
        }
	    
        return result;
	}
}