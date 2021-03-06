/**
 * Mars Simulation Project
 * MarsProjectHeadless.java
* @version 3.1.0 2018-06-14
 * @author Manny Kung
 * $LastChangedDate$
 * $LastChangedRevision$
 */
package org.mars_sim.headless;

import java.io.File;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.lang.Runnable;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;

/**
 * MarsProjectHeadless is the main class for starting mars-sim in purely
 * headless mode.
 */
public class MarsProjectHeadless {

	/** initialized logger for this class. */
	private static Logger logger = Logger.getLogger(MarsProjectHeadless.class.getName());

	static String[] args;

	/** true if displaying graphic user interface. */
	// private boolean useHeadless = true;

	/**
	 * Constructor 1.
	 * 
	 * @param args command line arguments.
	 */
	public MarsProjectHeadless(String args[]) {
		// logger.info("MarsProject's constructor is on
		// "+Thread.currentThread().getName() + " Thread");
		Simulation.instance().startSimExecutor();
		Simulation.instance().getSimExecutor().submit(new SimulationTask());
	}

	public class SimulationTask implements Runnable {

		public void run() {
			// new Simulation(); // NOTE: NOT supposed to start another instance of the
			// singleton Simulation
			logger.info("Starting " + Simulation.title);
			// headless mode
			// Initialize the simulation.
			initializeSimulation(args);
		}
	}

	/**
	 * Initialize the simulation.
	 * 
	 * @param args the command arguments.
	 * @return true if new simulation (not loaded)
	 */
	boolean initializeSimulation(String[] args) {
		boolean result = false;
		int userTimeRatio = -1;

		// Create a simulation
		List<String> argList = Arrays.asList(args);

		if (argList.contains("-512x"))
			userTimeRatio = 512;
		if (argList.contains("-1024x"))
			userTimeRatio = 1024;
		else if (argList.contains("-2048x"))
			userTimeRatio = 2048;
		if (argList.contains("-4096x"))
			userTimeRatio = 4096;
		else if (argList.contains("-8192x"))
			userTimeRatio = 8192;

		if (argList.contains("-new")) {
			// If new argument, create new simulation.
			handleNewSimulation(userTimeRatio); // if this fails we always exit, continuing is useless
			result = true;

		} else if (argList.contains("-load")) {
			// If load argument, load simulation from file.
			try {
				// Initialize the simulation.
				SimulationConfig.loadConfig();
				Simulation.createNewSimulation(userTimeRatio);
				handleLoadDefaultSimulation();

				// FIXME : make it work
			} catch (Exception e) {
				showError("Could not load the desired simulation. Staring a new Simulation instead. ", e);
				handleNewSimulation(userTimeRatio);
				result = true;
			}
		} else {
			// if there is no args, load default.sim
//                showError("Could not load the default simulation, trying to create a new Simulation...", e);
			handleNewSimulation(userTimeRatio);
			result = true;
		}

		return result;
	}

	/**
	 * Exit the simulation with an error message.
	 * 
	 * @param message the error message.
	 * @param e       the thrown exception or null if none.
	 */
	private void exitWithError(String message, Exception e) {
		showError(message, e);
		System.exit(1);
	}

	/**
	 * Show a modal error message dialog.
	 * 
	 * @param message the error message.
	 * @param e       the thrown exception or null if none.
	 */
	private void showError(String message, Exception e) {
		if (e != null) {
			logger.log(Level.SEVERE, message, e);
		} else {
			logger.log(Level.SEVERE, message);
		}
	}

	/**
	 * Loads the simulation from the default save file.
	 * 
	 * @throws Exception if error loading the default saved simulation.
	 */
	private void handleLoadDefaultSimulation() throws Exception {
		// logger.info("handleLoadDefaultSimulation() is on
		// "+Thread.currentThread().getName() + " Thread");

		try {
			// Load the default simulation
			Simulation.instance().loadSimulation(null);

		} catch (Exception e) {
			// logger.log(Level.WARNING, "Could not load default simulation", e);
			// throw e;
			exitWithError("Problem loading the default simulation.", e);
		}

		// Start simulation.
		startSimulation(true);
	}

	/**
	 * Create a new simulation instance.
	 */
	private void handleNewSimulation(int userTimeRatio) {
		// logger.info("MarsProject's handleNewSimulation() is on
		// "+Thread.currentThread().getName() + " Thread");

		try {
			SimulationConfig.loadConfig();

			// Correct order:
			Simulation.instance().destroyOldSimulation();
			Simulation.createNewSimulation(userTimeRatio);
			Simulation.instance().start(true);

		} catch (Exception e) {
			e.printStackTrace();
			exitWithError("Could not create a new simulation, startup cannot continue", e);
		}
	}

	/**
	 * Start the simulation instance.
	 */
	public void startSimulation(boolean useDefaultName) {
		// logger.info("MarsProject's startSimulation() is on
		// "+Thread.currentThread().getName() + " Thread");

		// Start the simulation.
		Simulation.instance().start(useDefaultName);
	}

	/**
	 * The starting method for the application
	 *
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {

		Logger.getLogger("").setLevel(Level.FINE);

		MarsProjectHeadless.args = args;

		new File(System.getProperty("user.home"), ".mars-sim" + File.separator + "logs").mkdirs();

		try {
			LogManager.getLogManager()
					.readConfiguration(MarsProjectHeadless.class.getResourceAsStream("/logging.properties"));
		} catch (IOException e) {
			logger.log(Level.WARNING, "Could not load logging properties", e);
			try {
				LogManager.getLogManager().readConfiguration();
			} catch (IOException e1) {
				logger.log(Level.WARNING, "Could read logging default config", e);
			}
		}

		/*
		 * // 2015-10-13 Added command prompt console Console console =
		 * System.console(); if(console == null && !GraphicsEnvironment.isHeadless()){
		 * String filename =
		 * MarsProject.class.getProtectionDomain().getCodeSource().getLocation().
		 * toString().substring(6); Runtime.getRuntime().exec(new
		 * String[]{"cmd","/c","start","cmd","/k","java -jar \"" + filename + "\""});
		 * }else{ MarsProject.main(new String[0]); System.out.
		 * println("Program has ended, please type 'exit' to close the console"); }
		 */

		// starting the simulation
		MarsProjectHeadless mp = new MarsProjectHeadless(args);

	}
}