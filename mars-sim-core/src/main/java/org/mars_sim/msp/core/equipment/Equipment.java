/**
 * Mars Simulation Project
 * Equipment.java
 * @version 3.1.0 2017-09-07
 * @author Scott Davis
 */

package org.mars_sim.msp.core.equipment;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.manufacture.Salvagable;
import org.mars_sim.msp.core.manufacture.SalvageInfo;
import org.mars_sim.msp.core.manufacture.SalvageProcessInfo;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.Repair;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.Indoor;
import org.mars_sim.msp.core.vehicle.Vehicle;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The Equipment class is an abstract class that represents
 * a useful piece of equipment, such as a EVA suit or a
 * medpack.
 */
public abstract class Equipment 
extends Unit
implements Indoor, Salvagable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members.
	private boolean isSalvaged;
	
	private SalvageInfo salvageInfo;

	private Unit lastOwner;

	private static UnitManager unitManager;
	
	/** Constructs an Equipment object
	 *  @param name the name of the unit
	 *  @param location the unit's location
	 */
	protected Equipment(String name, Coordinates location) {
		super(name, location);

		//this.name = name;
		// Initialize data members.
		isSalvaged = false;
		salvageInfo = null;
		
		unitManager = Simulation.instance().getUnitManager();
	}
	
	/**
	 * Gets a collection of people affected by this entity.
	 * @return person collection
	 */
	public Collection<Person> getAffectedPeople() {
		Collection<Person> people = new ConcurrentLinkedQueue<Person>();

		Person owner = null;
		if (lastOwner != null && lastOwner instanceof Person) {
			owner = (Person) lastOwner;
			people.add(owner);
		}

		// Check all people.
		Iterator<Person> i = unitManager.getPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();			
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this equipment.
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!people.contains(person)) 
						people.add(person);
				}
			}

			// Add all people repairing this equipment.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!people.contains(person)) 
						people.add(person);
				}
			}
		}

		return people;
	}

	/**
	 * Checks if the item is salvaged.
	 * @return true if salvaged.
	 */
	public boolean isSalvaged() {
		return isSalvaged;
	}

	//public String getName() {
	//	return name;
	//}
	/**
	 * Indicate the start of a salvage process on the item.
	 * @param info the salvage process info.
	 * @param settlement the settlement where the salvage is taking place.
	 */
	public void startSalvage(SalvageProcessInfo info, Settlement settlement) {
		salvageInfo = new SalvageInfo(this, info, settlement);
		isSalvaged = true;
	}

	/**
	 * Gets the salvage info.
	 * @return salvage info or null if item not salvaged.
	 */
	public SalvageInfo getSalvageInfo() {
		return salvageInfo;
	}

//	/**
//	 * Get settlement equipment is at, null if not at a settlement
//	 *
//	 * @return the equipment's settlement
//	 */
//    @Override
//	public Settlement getSettlement() {
//    	LocationSituation ls = getLocationSituation();
//		if (LocationSituation.IN_SETTLEMENT == ls) {
//			return (Settlement) getContainerUnit();
//		}
//
//		else if (LocationSituation.OUTSIDE == ls)
//			return null;
//
//		else if (LocationSituation.IN_VEHICLE == ls) {
//			Vehicle vehicle = (Vehicle) getContainerUnit();
//			Settlement settlement = (Settlement) vehicle.getContainerUnit();
//			return settlement;
//		}
//
//		else if (LocationSituation.BURIED == ls) {
//			// should not be the case
//			return null;
//		}
//
//		else {
//			System.err.println("Equipment : error in determining " + getName() + "'s getSettlement() ");
//			return null;
//		}
//	}

	/**
	 * Get the equipment's settlement, null if equipment is not at a settlement
	 *
	 * @return {@link Settlement} the equipment's settlement
	 */
	public Settlement getSettlement() {

		Unit container = getContainerUnit();

		if (container instanceof Settlement) {
			return (Settlement) container;
		}

		else if (container instanceof Person) {
			Unit c = ((Person) container).getContainerUnit();
			if (c instanceof Settlement) {
				return (Settlement) container;
			}
		}
		
		else if (container instanceof Vehicle) {
			Building b = BuildingManager.getBuilding((Vehicle) getContainerUnit());
			if (b != null)
				// still inside the garage
				return b.getSettlement();
			else
				// either at the vicinity of a settlement or already outside on a mission
				// TODO: need to differentiate which case in future better granularity
				return null;
		}

		else if (container == null) {
			return null;

		}

//		logger.warning("Error in determining " + getName() + "'s getSettlement() ");
		return null;
	}
	
	/**
	 * Get vehicle the equipment is in, null if not in vehicle
	 *
	 * @return {@link Vehicle} the equipment's vehicle
	 */
	public Vehicle getVehicle() {
		Unit container = getContainerUnit();
		if (container instanceof Vehicle)
			return (Vehicle) container;
		else
			return null;
	}

	/**
	 * Get the equipment's location
	 * @deprecated use other more efficient methods
	 * @return {@link LocationSituation} the person's location
	 */
	public LocationSituation getLocationSituation() {
		Unit container = getContainerUnit();
		if (container instanceof Settlement)
			return LocationSituation.IN_SETTLEMENT;
		else if (container instanceof Vehicle)
			return LocationSituation.IN_VEHICLE;
		else if (container == null)
			return LocationSituation.OUTSIDE;
		else 
			return LocationSituation.UNKNOWN;
	}

	/**
	 * Is the equipment's immediate container a settlement ?
	 * 
	 * @return true if yes
	 */
	public boolean isInSettlement() {
		if (getContainerUnit() instanceof Settlement)
			return true;
		else
			return false;
	}
	
	/**
	 * Is the equipment's immediate container a person ?
	 * 
	 * @return true if yes
	 */
	public boolean isInPerson() {
		if (getContainerUnit() instanceof Person)
			return true;
		else
			return false;
	}
	
	/**
	 * Is the equipment's immediate container a vehicle ?
	 * 
	 * @return true if yes
	 */
	public boolean isInVehicle() {
		if (getContainerUnit() instanceof Vehicle)
			return true;
		else
			return false;
	}

	/**
	 * Is the equipment outside on the surface of Mars
	 * 
	 * @return true if the equipment is outside
	 */
	public boolean isOutside() {
		if (getContainerUnit() == null)
			return true;
		else
			return false;
	}
	
	public void setLastOwner(Unit unit) {
		lastOwner = unit;
	}
	
	public Unit getLastOwner() {
		return lastOwner;
	}
	
	@Override
	public void destroy() {
		super.destroy();
		if (salvageInfo != null) salvageInfo.destroy();
		salvageInfo = null;
	}
}