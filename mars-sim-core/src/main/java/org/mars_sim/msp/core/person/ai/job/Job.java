/**
 * Mars Simulation Project
 * Job.java
 * @version 2.76 2004-06-10
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.*;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;

/** 
 * The Job class represents a person's job.
 */
public abstract class Job implements Serializable {

	// Probability penalty for starting a non-job-related task.
	private static final double NON_JOB_TASK_PENALTY = .1D;
	
	// Probability penalty for starting a non-job-related mission.
	private static final double NON_JOB_MISSION_START_PENALTY = 0D;
	
	// Probability penalty for joining a non-job-related mission.
	private static final double NON_JOB_MISSION_JOIN_PENALTY = 0D;

	// Domain members
	private String name; // Name of the job.
	protected List<Class> jobTasks; // List of tasks related to the job.
	protected List<Class> jobMissionStarts; // List of missions to be started by a person with this job.
	protected List<Class> jobMissionJoins; // List of missions to be joined by a person with this job.

	/**
	 * Constructor
	 * @param name the name of the job.
	 */
	public Job(String name) {
		this.name = name;
		jobTasks = new ArrayList<Class>();
		jobMissionStarts = new ArrayList<Class>();
		jobMissionJoins = new ArrayList<Class>();
	}
	
	/**
	 * Gets the job's name.
	 * @return name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets a person's capability to perform this job.
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public abstract double getCapability(Person person);
	
	/**
	 * Gets the probability modifier for starting a non-job-related task.
	 * @param taskClass the task class
	 * @return modifier >= 0.0
	 */
	public double getStartTaskProbabilityModifier(Class taskClass) {
		double result = 1D;
		if (!jobTasks.contains(taskClass)) result = NON_JOB_TASK_PENALTY;
		return result;
	}
	
	/**
	 * Gets the probability modifier for starting a non-job-related mission.
	 * @param missionClass the mission class
	 * @return modifier >= 0.0
	 */
	public double getStartMissionProbabilityModifier(Class missionClass) {
		double result = 1D;
		if (!jobMissionStarts.contains(missionClass)) result = NON_JOB_MISSION_START_PENALTY;
		return result;
	}
	
	/**
	 * Gets the probability modifier for joining a non-job-related mission.
	 * @param missionClass the mission class
	 * @return modifier >= 0.0
	 */
	public double getJoinMissionProbabilityModifier(Class missionClass) {
		double result = 1D;
		if (!jobMissionJoins.contains(missionClass)) result = NON_JOB_MISSION_JOIN_PENALTY;
		return result;
	}
	
	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public abstract double getSettlementNeed(Settlement settlement);
	
	/**
	 * Checks if a task is related to this job.
	 * @param taskClass the task class
	 * @return true if job related task.
	 */
	public boolean isJobRelatedTask(Class taskClass) {
		if (jobTasks.contains(taskClass)) return true;
		else return false;
	}
}