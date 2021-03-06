/**
 * Mars Simulation Project
 * Maintenance.java
 * @version 3.07 2015-02-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.NaturalAttributeType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RoboticAttributeType;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The Maintenance class is a task for performing
 * preventive maintenance on vehicles, settlements and equipment.
 */
public class Maintenance
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Maintenance.class.getName());

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.maintenance"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase MAINTAIN = new TaskPhase(Msg.getString(
            "Task.phase.maintain")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .1D;

	// Data members
	/** Entity to be maintained. */
	private Malfunctionable entity;

	/**
	 * Constructor.
	 * @param person the person to perform the task
	 */
	public Maintenance(Person person) {
        super(NAME, person, true, false, STRESS_MODIFIER,
                true, 10D + RandomUtil.getRandomDouble(40D));

        try {
            entity = getMaintenanceMalfunctionable();
            if (entity != null) {
                if (isInhabitableBuilding(entity)) {
                    // Walk to random location in building.
                    walkToRandomLocInBuilding((Building) entity, false);
                }
                else {

                    if (person.isInVehicle()) {//person.getLocationSituation() == LocationSituation.IN_VEHICLE) {
                        // If person is in rover, walk to passenger activity spot.
                        if (person.getVehicle() instanceof Rover) {
                            walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), false);
                        }
                    }
                    else {
                        // Walk to random location.
                        walkToRandomLocation(true);
                    }
                }
            }
            else {
                endTask();
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE,"Maintenance.constructor()",e);
            endTask();
        }

        // Initialize phase.
        addPhase(MAINTAIN);
        setPhase(MAINTAIN);
    }

	public Maintenance(Robot robot) {
        super(NAME, robot, true, false, STRESS_MODIFIER,
                true, 10D + RandomUtil.getRandomDouble(40D));

        try {
            entity = getMaintenanceMalfunctionable();
            if (entity != null) {
                if (isInhabitableBuilding(entity)) {
                    // Walk to random location in building.
                    walkToRandomLocInBuilding((Building) entity, false);
                }
                else {

                    if (robot.isInVehicle()) {//getLocationSituation() == LocationSituation.IN_VEHICLE) {
                        // If robot is in rover, walk to passenger activity spot.
//                        if (robot.getVehicle() instanceof Rover) {
//                            walkToPassengerActivitySpotInRover((Rover) robot.getVehicle(), false);
//                        }
                    }
                    else {
                        // Walk to random location.
                        walkToRandomLocation(false);
                    }
                }
            }
            else {
                endTask();
            }
        }
        catch (Exception e) {
            logger.log(Level.SEVERE,"Maintenance.constructor()",e);
            endTask();
        }

        // Initialize phase.
        addPhase(MAINTAIN);
        setPhase(MAINTAIN);

    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (MAINTAIN.equals(getPhase())) {
            return maintainPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the maintain phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double maintainPhase(double time) {
        MalfunctionManager manager = entity.getMalfunctionManager();

		if (person != null) {
	        // If person is incapacitated, end task.
	        if (person.getPerformanceRating() == 0D) {
	            endTask();
	        }
		}
		else if (robot != null) {
	        // If robot is incapacitated, end task.
	        if (robot.getPerformanceRating() == 0D) {
	            endTask();
	        }
		}


        // Check if maintenance has already been completed.
        if (manager.getEffectiveTimeSinceLastMaintenance() < 1000D) {
            endTask();
        }

        // If equipment has malfunction, end task.
        if (manager.hasMalfunction()) {
            endTask();
        }

        if (isDone()) {
            return time;
        }

        // Determine effective work time based on "Mechanic" skill.
        double workTime = time;
        int mechanicSkill = getEffectiveSkillLevel();
        if (mechanicSkill == 0) {
            workTime /= 2;
        }
        if (mechanicSkill > 1) {
            workTime += workTime * (.2D * mechanicSkill);
        }

        // Add repair parts if necessary.
        boolean repairParts = false;
        Unit container = null;

		if (person != null)
	        container = person.getTopContainerUnit();

		else if (robot != null)
	        container = robot.getTopContainerUnit();


        if (container != null) {
            Inventory inv = container.getInventory();
            if (Maintenance.hasMaintenanceParts(inv, entity)) {
                repairParts = true;
                Map<Integer, Integer> parts = new HashMap<>(manager.getMaintenanceParts());
                Iterator<Integer> j = parts.keySet().iterator();
                while (j.hasNext()) {
                	Integer part = j.next();
                    int number = parts.get(part);
                    inv.retrieveItemResources(part, number);
                    manager.maintainWithParts(part, number);
                }
            }
        }
        if (!repairParts) {
            endTask();
            return time;
        }

        // Add work to the maintenance
        manager.addMaintenanceWorkTime(workTime);

        // Add experience points
        addExperience(time);

        // If maintenance is complete, task is done.
        if (manager.getEffectiveTimeSinceLastMaintenance() == 0D) {
            endTask();
        }

        // Check if an accident happens during maintenance.
        checkForAccident(time);

        return 0D;
    }

    @Override
    protected void addExperience(double time) {
        // Add experience to "Mechanics" skill
        // (1 base experience point per 100 millisols of work)
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        double newPoints = time / 100D;
        int experienceAptitude = 0;
		if (person != null)
	        experienceAptitude = person.getNaturalAttributeManager().getAttribute(
	                NaturalAttributeType.EXPERIENCE_APTITUDE);
		else if (robot != null)
			experienceAptitude = robot.getRoboticAttributeManager().getAttribute(
					RoboticAttributeType.EXPERIENCE_APTITUDE);

        newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
        newPoints *= getTeachingExperienceModifier();
		if (person != null)
	        person.getMind().getSkillManager().addExperience(SkillType.MECHANICS, newPoints);
		else if (robot != null)
			robot.getBotMind().getSkillManager().addExperience(SkillType.MECHANICS, newPoints);

    }

    /**
     * Check for accident with entity during maintenance phase.
     * @param time the amount of time (in millisols)
     */
    private void checkForAccident(double time) {

        double chance = .005D;

        // Mechanic skill modification.
        int skill = 0;
		if (person != null)
		       skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);
		else if (robot != null)
			skill = robot.getBotMind().getSkillManager().getEffectiveSkillLevel(SkillType.MECHANICS);

        if (skill <= 3) {
            chance *= (4 - skill);
        }
        else {
            chance /= (skill - 2);
        }

        // Modify based on the entity's wear condition.
        chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();

        if (RandomUtil.lessThanRandPercent(chance * time)) {
			if (person != null) {
//	            logger.info("[" + person.getLocationTag().getShortLocationName() +  "] " + person.getName() + " has accident while performing maintenance on "
//	                    + entity.getNickName()
//	                    + ".");
	            entity.getMalfunctionManager().createASeriesOfMalfunctions(person);
			}
			else if (robot != null) {
//	            logger.info("[" + robot.getLocationTag().getShortLocationName() +  "] " + robot.getName() + " has accident while performing maintenance on "
//	                    + entity.getNickName()
//	                    + ".");
	            entity.getMalfunctionManager().createASeriesOfMalfunctions(robot);   
			}


        }

    }

    /**
     * Gets the entity the person is maintaining.
     * Returns null if none.
     * @return entity
     */
    public Malfunctionable getEntity() {
        return entity;
    }

    /**
     * Gets a random malfunctionable to perform maintenance on.
     * @return malfunctionable or null.
     */
    private Malfunctionable getMaintenanceMalfunctionable() {
        Malfunctionable result = null;

        // Determine all malfunctionables local to the person.
        Map<Malfunctionable, Double> malfunctionables = new HashMap<Malfunctionable, Double>();
		if (person != null) {
	        Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
	        while (i.hasNext()) {
	            Malfunctionable entity = i.next();
	            double probability = getProbabilityWeight(entity);
	            if (probability > 0D) {
	                malfunctionables.put(entity, probability);
	            }
	        }
		}
		else if (robot != null) {
			Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(robot).iterator();
	        while (i.hasNext()) {
	            Malfunctionable entity = i.next();
	            double probability = getProbabilityWeight(entity);
	            if (probability > 0D) {
	                malfunctionables.put(entity, probability);
	            }
	        }
		}

        if (!malfunctionables.isEmpty()) {
            result = RandomUtil.getWeightedRandomObject(malfunctionables);
        }

        if (result != null) {
            setDescription(Msg.getString("Task.description.maintenance.detail",
                    result.getNickName())); //$NON-NLS-1$
        }

        return result;
    }

    /**
     * Checks if a malfunctionable is an inhabitable building.
     * @param malfunctionable the malfunctionable.
     * @return true if inhabitable building.
     */
    private boolean isInhabitableBuilding(Malfunctionable malfunctionable) {
        boolean result = false;
        if (malfunctionable instanceof Building) {
            Building building = (Building) malfunctionable;
            if (building.hasFunction(FunctionType.LIFE_SUPPORT)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Gets the probability weight for a malfunctionable.
     * @param malfunctionable the malfunctionable
     * @return the probability weight.
     * @throws Exception if error determining probability weight.
     */
    private double getProbabilityWeight(Malfunctionable malfunctionable)  {
        double result = 0D;
        boolean isVehicle = (malfunctionable instanceof Vehicle);
        boolean uninhabitableBuilding = false;
        if (malfunctionable instanceof Building)
            uninhabitableBuilding = !((Building) malfunctionable).hasFunction(FunctionType.LIFE_SUPPORT);
        MalfunctionManager manager = malfunctionable.getMalfunctionManager();
        boolean hasMalfunction = manager.hasMalfunction();
        double effectiveTime = manager.getEffectiveTimeSinceLastMaintenance();
        boolean minTime = (effectiveTime >= 1000D);

		if (person != null) {
		       boolean enoughParts = hasMaintenanceParts(person, malfunctionable);

		        if (!isVehicle && !uninhabitableBuilding && !hasMalfunction && minTime && enoughParts) {
		            result = effectiveTime;
		            if (malfunctionable instanceof Building) {
		                Building building = (Building) malfunctionable;
		                if (isInhabitableBuilding(malfunctionable)) {
		                    result *= Task.getCrowdingProbabilityModifier(person, building);
		                    result *= Task.getRelationshipModifier(person, building);
		                }
		            }
		        }
		}
		else if (robot != null) {
		       boolean enoughParts = hasMaintenanceParts(robot, malfunctionable);

		        if (!isVehicle && !uninhabitableBuilding && !hasMalfunction && minTime && enoughParts) {
		            result = effectiveTime;
		            if (malfunctionable instanceof Building) {
		                //Building building = (Building) malfunctionable;
		                if (isInhabitableBuilding(malfunctionable)) {
		                    //result *= Task.getCrowdingProbabilityModifier(robot, building);
		                    //result *= Task.getRelationshipModifier(robot, building);
		                }
		            }
		        }
		}



        return result;
    }

    /**
     * Checks if there are enough local parts to perform maintenance.
     * @param person the person performing the maintenance.
     * @param malfunctionable the entity needing maintenance.
     * @return true if enough parts.
     * @throws Exception if error checking parts availability.
     */
    public static boolean hasMaintenanceParts(Person person, Malfunctionable malfunctionable) {
        Inventory inv = null;
        if (person.getTopContainerUnit() != null) inv = person.getTopContainerUnit().getInventory();
        else inv = person.getInventory();
        return hasMaintenanceParts(inv, malfunctionable);
    }

    public static boolean hasMaintenanceParts(Robot robot, Malfunctionable malfunctionable) {
        Inventory inv = null;
        if (robot.getTopContainerUnit() != null) inv = robot.getTopContainerUnit().getInventory();
        else inv = robot.getInventory();
        return hasMaintenanceParts(inv, malfunctionable);
    }

    /**
     * Checks if there are enough local parts to perform maintenance.
     * @param inventory inventory holding the needed parts.
     * @param malfunctionable the entity needing maintenance.
     * @return true if enough parts.
     * @throws Exception if error checking parts availability.
     */
    static boolean hasMaintenanceParts(Inventory inv, Malfunctionable malfunctionable) {
        boolean result = true;

        Map<Integer, Integer> parts = malfunctionable.getMalfunctionManager().getMaintenanceParts();
        Iterator<Integer> i = parts.keySet().iterator();
        while (i.hasNext()) {
        	Integer part = i.next();
            int number = parts.get(part);
            if (inv.getItemResourceNum(part) < number) result = false;
        }

        return result;
    }

    @Override
    public int getEffectiveSkillLevel() {
        SkillManager manager = null;
		if (person != null)
	        manager = person.getMind().getSkillManager();
		else if (robot != null)
			manager = robot.getBotMind().getSkillManager();

        return manager.getEffectiveSkillLevel(SkillType.MECHANICS);
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(1);
        results.add(SkillType.MECHANICS);
        return results;
    }

    @Override
    public void destroy() {
        super.destroy();

        entity = null;
    }
}