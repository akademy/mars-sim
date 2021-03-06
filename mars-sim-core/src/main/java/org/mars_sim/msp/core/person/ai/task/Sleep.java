/**
 * Mars Simulation Project
 * Sleep.java
 * @version 3.1.0 2017-01-19
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.CircadianClock;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ShiftType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.RoboticStation;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * The Sleep class is a task for sleeping. The duration of the task is by
 * default chosen randomly, between 250 - 330 millisols. Note: Sleeping reduces
 * fatigue and stress.
 */
public class Sleep extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Sleep.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.sleep"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase SLEEPING = new TaskPhase(Msg.getString("Task.phase.sleeping")); //$NON-NLS-1$

	/** Task name for robot */
	private static final String SLEEP_MODE = Msg.getString("Task.description.sleepMode"); //$NON-NLS-1$

	/** Task phases for robot. */
	private static final TaskPhase SLEEP_MODE_PHASE = new TaskPhase(Msg.getString("Task.phase.sleepMode")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = -1.2D;
	/** The base alarm time (millisols) at 0 degrees longitude. */
	private static final double BASE_ALARM_TIME = 300D;

	// Data members
	/** The previous time (millisols). */
	private double previousTime;
	private double timeFactor;

	/** The living accommodations if any. */
	private LivingAccommodations accommodations;
	private RoboticStation station;
	private CircadianClock circadian;
	private PhysicalCondition pc;

	private static Simulation sim = Simulation.instance();
	private static MasterClock masterClock;
	private static MarsClock marsClock;

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	//
	// Organized into 9 branching decisions
	// A bed can be either empty(E) or occupied(O), either unmarked(U) or
	// designated(D).
	// thus a 2x2 matrix with 4 possibilities: EU, ED, OU, OD
	public Sleep(Person person) {
		super(NAME, person, false, false, STRESS_MODIFIER, true, (250D + RandomUtil.getRandomDouble(10D)));

		if (masterClock == null)
			masterClock = sim.getMasterClock();

		if (marsClock == null)
			marsClock = masterClock.getMarsClock();

		pc = person.getPhysicalCondition();
		circadian = person.getCircadianClock();

		timeFactor = 6D; // TODO: should vary this factor by person

		compute();
	}

	public void compute() {

		// boolean walkSite = false;
		// If person is in rover, walk to passenger activity spot.
		if (person.isInVehicle() && person.getVehicle() instanceof Rover) {
			walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), true);
		}

		// If person is in a settlement, try to find a living accommodations building.
		else if (person.isInSettlement()) {

			Settlement s1 = person.getSettlement();
			Settlement s0 = person.getAssociatedSettlement();

			// check to see if a person is a trader or on a trading mission
			if (!s1.equals(s0)) {// || s1 != s0) {
				// yes he is a trader/guest (Case 1-3)
				// logger.info(person + " is a guest of a trade mission and will use an
				// unoccupied bed randomly.");
				// find a best empty (EU, ED) bed
				Building q2 = getBestAvailableQuarters(person, false);

				if (q2 != null) {
					// find a best empty, unmarked (EU) bed
					Building q1 = getBestAvailableQuarters(person, true);
					if (q1 != null) {
						// Case 1 : (the BEST case for a guest) the settlement does have one or more
						// empty, unmarked (EU) bed(s)
						accommodations = q1.getLivingAccommodations();
						walkToActivitySpotInBuilding(q1, getLivingFunction(), false);
						// Building startBuilding = BuildingManager.getBuilding(person);
						// logger.fine("Case 1: " + person + " is walking from " + startBuilding + " to
						// use his/her temporary quarters at " + q1);

					} else {
						// Case 2 : the settlement has only empty, designated (ED) bed(s) available
						// Question : will the owner of this bed be coming back soon from duty ?
						// TODO : will split into Case 2a and Case 2b.
						accommodations = q2.getLivingAccommodations();
						walkToActivitySpotInBuilding(q2, getLivingFunction(), false);
						// Building startBuilding = BuildingManager.getBuilding(person);
						// logger.fine("Case 2: " + person + " is walking from " + startBuilding + " to
						// use his/her temporary quarters at " + q2);

					}

					accommodations.registerSleeper(person, true);
					// walkSite = true;

				} else {
					// Case 3 : the settlement has NO empty bed(s). OU and/or OD only
					// logger.info("Case 3: " + person + " couldn't find an empty bed at all. Will
					// find a spot to fall asleep wherever he/she likes.");
					// TODO: should allow him/her to sleep in gym or anywhere based on his/her usual
					// preferences
					// endTask();
					// Just walk to a random location.
					walkToRandomLocation(false);
				}

			} else {
				// He/she is an inhabitant in this settlement

				// Check if a person has a designated bed
				Building pq = person.getQuarters();

				if (pq != null) {
					// This person has his quarters assigned with a designated bed
					// logger.fine(person + " does have a designated bed at " + pq.getNickName());

					// check if this bed is currently empty or occupied (either ED or OD)
					Point2D bed = person.getBed();
					accommodations = pq.getLivingAccommodations();
					boolean empty = accommodations.isActivitySpotEmpty(bed);

					if (empty) {
						// Case 4: this designated bed is currently empty (ED)
						// Building startBuilding = BuildingManager.getBuilding(person);

						// logger.info("Case 4: " + person + " is walking from " + startBuilding + " to
						// his private quarters at " + pq);
						// addSubTask(new WalkSettlementInterior(person, quarters, bed.getX(),
						// bed.getY()));
						accommodations.registerSleeper(person, false);
						walkToBed(accommodations, person, false); // can cause StackOverflowError from excessive log or
																	// calling ExitAirlock
					} else {
						// Case 5: this designated bed is currently occupied (OD)
						// logger.info("Case 5: " + person + " has a designated bed but is currently
						// occupied. Will find a spot to fall asleep.");
						// TODO: should allow him/her to sleep in gym or anywhere based on his/her usual
						// preferences
						// Just walk to a random location.
						walkToRandomLocation(false);
					}

				} else {
					// this inhabitant has never been assigned a quarter and does not have a
					// designated bed so far
					// logger.fine(person + " has never been designated a bed so far");

					// find an empty (either marked or unmarked) bed
					Building q7 = getBestAvailableQuarters(person, false);

					if (q7 != null) {
						// yes it has empty (either marked or unmarked) bed

						// find an empty unmarked bed
						Building q6 = getBestAvailableQuarters(person, true);

						if (q6 != null) {
							// Case 6: an empty unmarked bed is available for assigning to the person

							// logger.info(q6.getNickName() + " has empty, unmarked bed (ED) that can be
							// assigned to " + person);
							// addSubTask(new WalkSettlementInterior(person, quarters, bed.getX(),
							// bed.getY()));
							// person.setQuarters(q6);
							// Point2D bed = person.getBed();
							accommodations = q6.getLivingAccommodations();
							accommodations.registerSleeper(person, false);
							walkToBed(accommodations, person, false);
							// walkToActivitySpotInBuilding(q7, BuildingFunction.LIVING_ACCOMODATIONS,
							// false);
							// Building startBuilding = BuildingManager.getBuilding(person);
							// logger.info("Case 6: " + person + " is walking from " + startBuilding + " to
							// use his/her new quarters at " + q6);

						} else {
							logger.fine(q7.getNickName() + " has an empty, already designated (ED) bed available for "
									+ person);
							// Case 7: the settlement has only empty, designated (ED) bed(s) available
							// Question : will the owner of this bed be coming back soon from duty ?
							// TODO : will split into Case 2a and Case 2b.
							/*
							 * accommodations = (LivingAccommodations)
							 * q7.getFunction(BuildingFunction.LIVING_ACCOMODATIONS);
							 * walkToActivitySpotInBuilding(q7, BuildingFunction.LIVING_ACCOMODATIONS,
							 * false); Building startBuilding = BuildingManager.getBuilding(person);
							 * logger.info("Case 7a: " + person + " is walking from " + startBuilding +
							 * " to use someone else's quarters at " + q7);
							 * accommodations.addSleeper(person, false);
							 */
							// logger.info("Case 7b: " + person + " will look for a spot to fall asleep.");

							// Walk to random location.
							walkToRandomLocation(false);
						}

					} else {

						// Case 8 : no empty bed at all
						logger.info("Case 8: " + person
								+ " couldn't find an empty bed at all. will look for a spot to fall asleep.");
						// TODO: should allow him/her to sleep in gym or anywhere.
						// Walk to random location.
						walkToRandomLocation(false);
					}
				}
			}
		}

		previousTime = marsClock.getMillisol();

		// Initialize phase
		addPhase(SLEEPING);
		setPhase(SLEEPING);
	}

	public Sleep(Robot robot) {
		super(SLEEP_MODE, robot, false, false, STRESS_MODIFIER, true, 10D);

		if (masterClock == null)
			masterClock = sim.getMasterClock();

		if (marsClock == null)
			marsClock = masterClock.getMarsClock();

		botCompute();
	}

	public void botCompute() {

		// If robot is in a settlement, try to find a living accommodations building.
		if (robot.isInSettlement()) {

			// TODO: if power is below a certain threshold, go to robotic station for
			// recharge, else stay at the same place

			// If currently in a building with a robotic station, go to a station activity
			// spot.
			// boolean atStation = false;
			Building currentBuilding = BuildingManager.getBuilding(robot);
			if (currentBuilding != null) {
				if (currentBuilding.hasFunction(getRoboticFunction())) {
					RoboticStation currentStation = currentBuilding.getRoboticStation();
					if (currentStation.getSleepers() < currentStation.getSlots()) {
						// atStation = true;
						station = currentStation;
						station.addSleeper();

						// Check if robot is currently at an activity spot for the robotic station.
						if (currentStation.hasActivitySpots() && !currentStation.isAtActivitySpot(robot)) {
							// Walk to an available activity spot.
							walkToActivitySpotInBuilding(currentBuilding, true);
						}
					}
				}
			} else {
				// if (!atStation) {
				Building building = getAvailableRoboticStationBuilding(robot);
				if (building != null) {
					// System.out.println("building.toString() is " + building.toString() );
					station = building.getRoboticStation();
					if (station != null) {
						// TODO: see https://github.com/mars-sim/mars-sim/issues/22
						// Question: why would the method below cause RepairBot to walk outside the
						// settlement to a vehicle ?
						walkToActivitySpotInBuilding(building, true);
						// TODO: need to add activity spots in every building or
						// walkToActivitySpotInBuilding(building, false) will fail
						// and create java.lang.NullPointerException
						station.addSleeper();
					}
				}
			}
		}

		previousTime = marsClock.getMillisol();

		// Initialize phase
		addPhase(SLEEP_MODE_PHASE);
		setPhase(SLEEP_MODE_PHASE);
	}

	@Override
	protected FunctionType getLivingFunction() {
		return FunctionType.LIVING_ACCOMODATIONS;
	}

	@Override
	protected FunctionType getRoboticFunction() {
		return FunctionType.ROBOTIC_STATION;
	}

	@Override
	protected double performMappedPhase(double time) {
		if (person != null) {
			if (getPhase() == null)
				throw new IllegalArgumentException("Task phase is null");
			else if (SLEEPING.equals(getPhase())) {
				return sleepingPhase(time);
			} else
				return time;
		}

		else if (robot != null) {
			if (getPhase() == null)
				throw new IllegalArgumentException("Task phase is null");
			else if (SLEEP_MODE_PHASE.equals(getPhase()))
				return sleepingPhase(time);
			else
				return time;
		}
		return time;
	}

	/**
	 * Performs the sleeping phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double sleepingPhase(double time) {
		if (masterClock == null)
			masterClock = sim.getMasterClock();

		if (marsClock == null)
			marsClock = masterClock.getMarsClock();// needed for loading a saved sim

		if (person != null) {

			pc.recoverFromSoreness(.05);

			// Obtain the fractionOfRest to restore fatigue faster in high fatigue case
			double fractionOfRest = time / 1000 * timeFactor;

			// Note : timeFactor is 3 since a person typically spent 1/3 of time sleeping in
			// a day.

			double f = pc.getFatigue();

			double residualFatigue = 0;

			if (f > 1000)
				residualFatigue = (f - 1000) / 2000;

			// Reduce person's fatigue
			double newFatigue = f - f * fractionOfRest + residualFatigue;

			pc.setFatigue(newFatigue);

			circadian.setAwake(false);
			circadian.getRested(time);

			circadian.setNumSleep(circadian.getNumSleep() + 1);
			circadian.updateSleepCycle((int) marsClock.getMillisol(), true);

			// Check if alarm went off
			double newTime = marsClock.getMillisol();
			double alarmTime = getAlarmTime();

			if ((previousTime <= alarmTime) && (newTime >= alarmTime)) {
				endTask();
				logger.finest(person.getName() + " woke up from alarm.");
			} else {
				previousTime = newTime;
			}

		}

		else if (robot != null) {
			// Check if alarm went off.
			double newTime = marsClock.getMillisol();
			double alarmTime = getAlarmTime();
			if ((previousTime <= alarmTime) && (newTime >= alarmTime)) {
				endTask();
				logger.finest(robot.getName() + " woke up from alarm.");
			} else {
				previousTime = newTime;
			}
		}

		return 0D;
	}

	@Override
	protected void addExperience(double time) {
		// This task adds no experience.
	}

	@Override
	public void endTask() {
		super.endTask();

		if (person != null) {
			// Remove person from living accommodations bed so others can use it.
			if (accommodations != null && accommodations.getSleepers() > 0) {
				accommodations.removeSleeper(person);
			}

			circadian.setAwake(true);

		} else if (robot != null) {
			// Remove robot from stations so other robots can use it.
			if (station != null && station.getSleepers() > 0) {
				station.removeSleeper();
				// TODO: assess how well this work
//	        	logger.info(robot.getNickName() + " was done sleeping and waking up.");
				walkToAssignedDutyLocation(robot, false);
			}
		}
	}

	/**
	 * Gets the best available living accommodations building that the person can
	 * use. Returns null if no living accommodations building is currently
	 * available.
	 * 
	 * @param person   the person
	 * @param unmarked does the person wants an unmarked(aka undesignated) bed or
	 *                 not.
	 * @return a building with available bed(s)
	 */
	public static Building getBestAvailableQuarters(Person person, boolean unmarked) {

		Building result = null;

		if (person.isInSettlement()) {
			// BuildingManager manager = person.getSettlement().getBuildingManager();
			List<Building> quartersBuildings = person.getSettlement().getBuildingManager()
					.getBuildings(FunctionType.LIVING_ACCOMODATIONS);
			quartersBuildings = BuildingManager.getNonMalfunctioningBuildings(quartersBuildings);
			quartersBuildings = getQuartersWithEmptyBeds(quartersBuildings, unmarked);
			if (quartersBuildings.size() > 0) {
				quartersBuildings = BuildingManager.getLeastCrowdedBuildings(quartersBuildings);
				// if (unmarked)
				// ;//logger.info("Stage 1: it has empty and unmarked (EU) beds");
				// else
				// ;//logger.info("Stage 1: it has empty (either unmarked or designated) beds");
			}
			// else if (quartersBuildings.isEmpty())
			// if (unmarked)
			// ;//logger.info("Stage 1: no buildings has empty and unmarked (EU) beds");
			// else
			// ;//logger.info("Stage 1: no buildings has empty (either unmarked or
			// designated) beds");

			if (quartersBuildings.size() > 0) {
				Map<Building, Double> quartersBuildingProbs = BuildingManager.getBestRelationshipBuildings(person,
						quartersBuildings);
				result = RandomUtil.getWeightedRandomObject(quartersBuildingProbs);
				// if (unmarked)
				// ;//logger.info("Stage 2: it has empty and unmarked (EU) beds");
				// else
				// ;//logger.info("Stage 2: it has empty (either unmarked or designated) beds");
			}
			// else if (quartersBuildings.isEmpty())
			// if (unmarked)
			// ;//logger.info("Stage 2: no buildings has empty and unmarked (EU) beds");
			// else
			// ;//logger.info("Stage 2: no buildings has empty (either unmarked or
			// designated) beds");

			// if (result != null)
			// ;//logger.info("Stage 3: " + result.getNickName() + " has a bed available : "
			// + result.getNickName());
			// else
			// ;//logger.info("Stage 3: no buildings are available");

		}

		return result;
	}

	public static Building getAvailableRoboticStationBuilding(Robot robot) {

		Building result = null;

		if (robot.isInSettlement()) {
			BuildingManager manager = robot.getSettlement().getBuildingManager();
			List<Building> buildings = manager.getBuildings(FunctionType.ROBOTIC_STATION);
			buildings = BuildingManager.getNonMalfunctioningBuildings(buildings);
			buildings = getRoboticStationsWithEmptySlots(buildings);
			if (RandomUtil.getRandomInt(2) == 0) // robot is not as inclined to move around
				buildings = BuildingManager.getLeastCrowded4BotBuildings(buildings);
			int size = buildings.size();
			// System.out.println("size is "+size);
			int selected = 0;
			if (size == 0)
				result = null;
			else if (size >= 1) {
				selected = RandomUtil.getRandomInt(size - 1);
				result = buildings.get(selected);
			}
			// System.out.println("selected is "+selected);
			// if (quartersBuildings.size() > 0) {
			// Map<Building, Double> quartersBuildingProbs =
			// BuildingManager.getBestRelationshipBuildings(
			// robot, quartersBuildings);
			// result = RandomUtil.getWeightedRandomObject(quartersBuildingProbs);
			// }
		}

		return result;
	}

	/**
	 * Gets living accommodations with empty beds from a list of buildings with the
	 * living accommodations function.
	 * 
	 * @param buildingList list of buildings with the living accommodations
	 *                     function.
	 * @param unmarked     does the person wants an unmarked(aka undesignated) bed
	 *                     or not.
	 * @return list of buildings with empty beds.
	 */
	private static List<Building> getQuartersWithEmptyBeds(List<Building> buildingList, boolean unmarked) {
		List<Building> result = new ArrayList<Building>();

		for (Building building : buildingList) {
			LivingAccommodations quarters = building.getLivingAccommodations();
			boolean notFull = quarters.getSleepers() < quarters.getBeds();
			// Check if an unmarked bed is wanted
			if (unmarked && notFull && quarters.hasAnUnmarkedBed()) {
				result.add(building);
			} else if (notFull) {
				result.add(building);
			}

		}

		return result;
	}

	private static List<Building> getRoboticStationsWithEmptySlots(List<Building> buildingList) {
		List<Building> result = new ArrayList<Building>();

		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			RoboticStation station = building.getRoboticStation();
			if (station.getSleepers() < station.getSlots()) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets the wakeup alarm time for the person's longitude.
	 * 
	 * @return alarm time in millisols.
	 */
	private double getAlarmTime() {
		double timeDiff = 0;
		double modifiedAlarmTime = 0;

		if (person != null) {
			ShiftType shiftType = person.getTaskSchedule().getShiftType();
			// Set to 50 millisols prior to the beginning of the duty shift hour
			if (shiftType == ShiftType.A)
				modifiedAlarmTime = 950;
			else if (shiftType == ShiftType.B)
				modifiedAlarmTime = 450;
			else if (shiftType == ShiftType.X)
				modifiedAlarmTime = 950;
			else if (shiftType == ShiftType.Y)
				modifiedAlarmTime = 283;
			else if (shiftType == ShiftType.Z)
				modifiedAlarmTime = 616;
			else if (shiftType == ShiftType.ON_CALL) { // if only one person is at the settlement, go with this schedule
				timeDiff = 1000D * (person.getCoordinates().getTheta() / (2D * Math.PI));
				modifiedAlarmTime = BASE_ALARM_TIME - timeDiff;
			}

		} else if (robot != null) {
			timeDiff = 1000D * (robot.getCoordinates().getTheta() / (2D * Math.PI));

		}

		if (modifiedAlarmTime < 0D) {
			modifiedAlarmTime += 1000D;
		}
		return modifiedAlarmTime;
	}

	@Override
	public int getEffectiveSkillLevel() {
		return 0;
	}

	@Override
	public List<SkillType> getAssociatedSkills() {
		List<SkillType> results = new ArrayList<SkillType>(0);
		return results;
	}

	@Override
	public void destroy() {
		super.destroy();
		station = null;
		accommodations = null;
		circadian = null;
		pc = null;
		// sim = null;
		// masterClock = null;
		// marsClock = null;
	}
}