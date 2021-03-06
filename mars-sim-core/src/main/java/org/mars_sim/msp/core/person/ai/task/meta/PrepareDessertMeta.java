/**
 * Mars Simulation Project
 * PrepareDessertMeta.java
 * @version 3.1.0 2017-09-07
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

//import java.util.logging.Logger;
import java.io.Serializable;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.FavoriteType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.CookMeal;
import org.mars_sim.msp.core.person.ai.task.PrepareDessert;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Chefbot;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;

/**
 * Meta task for the PrepareSoymilk task.
 */
public class PrepareDessertMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.prepareDessertMeta"); //$NON-NLS-1$

    /** default logger. */
    //private static Logger logger = Logger.getLogger(PrepareDessertMeta.class.getName());

    public PrepareDessertMeta() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new PrepareDessert(person);
    }


    @Override
    public Task constructInstance(Robot robot) {
        return new PrepareDessert(robot);
    }


    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isInside() && CookMeal.isMealTime(person.getCoordinates())) {
            // Desserts should be prepared during meal times.
        	
            // See if there is an available kitchen.
            Building kitchenBuilding = PrepareDessert.getAvailableKitchen(person);

            if (kitchenBuilding != null) {

                PreparingDessert kitchen = kitchenBuilding.getPreparingDessert();

                // Check if there are enough ingredients to prepare a dessert.
                int numGoodRecipes = kitchen.getAListOfDesserts().size();

                // Check if enough desserts have been prepared at kitchen for this meal time.
                boolean enoughMeals = kitchen.getMakeNoMoreDessert();

                if ((numGoodRecipes > 0) && !enoughMeals) {

                    result = 20D;

                    // Crowding modifier.
                    result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, kitchenBuilding);
                    result *= TaskProbabilityUtil.getRelationshipModifier(person, kitchenBuilding);

                    // Effort-driven task modifier.
                    result *= person.getPerformanceRating();

                    // Job modifier.
                    Job job = person.getMind().getJob();
                    if (job != null) {
                        result *= job.getStartTaskProbabilityModifier(CookMeal.class);
                    }

                    // Modify if cooking is the person's favorite activity.
                    if (person.getFavorite().getFavoriteActivity() == FavoriteType.COOKING) {
                        result *= 2D;
                    }

                    // 2015-06-07 Added Preference modifier
                    if (result > 0D) {
                        result = result + result * person.getPreference().getPreferenceScore(this)/5D;
                    }

        	        if (result < 0) result = 0;
                }
            }
        }

        return result;
    }


	@Override
	public double getProbability(Robot robot) {

       double result = 0D;

       if (CookMeal.isMealTime(robot) && robot.getBotMind().getRobotJob() instanceof Chefbot) {
           // See if there is an available kitchen.
           Building kitchenBuilding = PrepareDessert.getAvailableKitchen(robot);

           if (kitchenBuilding != null) {

               PreparingDessert kitchen = kitchenBuilding.getPreparingDessert();

               // Check if there are enough ingredients to prepare a dessert.
               int numGoodRecipes = kitchen.getAListOfDesserts().size();

               // Check if enough desserts have been prepared at kitchen for this meal time.
               boolean enoughMeals = kitchen.getMakeNoMoreDessert();

               if ((numGoodRecipes > 0) && !enoughMeals) {

                   result = 50D;

                   // Crowding modifier.
                   result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(robot, kitchenBuilding);

                   // Effort-driven task modifier.
                   result *= robot.getPerformanceRating();
               }
           }
       }

       return result;
	}
}