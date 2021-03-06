/**
 * Mars Simulation Project
 * EatMeal.java
 * @version 3.1.0 2017-09-13
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.meta.HaveConversationMeta;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.cooking.CookedMeal;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparedDessert;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.tool.Conversion;

/**
 * The EatMeal class is a task for eating a meal.
 * The duration of the task is 40 millisols.
 * Note: Eating a meal reduces hunger to 0.
 */
public class EatMeal extends Task implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
	/** default logger. */
	private static Logger logger = Logger.getLogger(EatMeal.class.getName());

    private static String sourceName = logger.getName();
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.eatMeal"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase PICK_UP_MEAL = new TaskPhase(Msg.getString(
            "Task.phase.pickUpMeal")); //$NON-NLS-1$
    private static final TaskPhase PICK_UP_DESSERT = new TaskPhase(Msg.getString(
            "Task.phase.pickUpDessert")); //$NON-NLS-1$
    private static final TaskPhase EATING_MEAL = new TaskPhase(Msg.getString(
            "Task.phase.eatingMeal")); //$NON-NLS-1$
    private static final TaskPhase EATING_DESSERT = new TaskPhase(Msg.getString(
            "Task.phase.eatingDessert")); //$NON-NLS-1$

    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.4D;
    private static final double DESSERT_STRESS_MODIFIER = -.4D;
    private static final int NUMBER_OF_MEAL_PER_SOL = 4;
    private static final int NUMBER_OF_DESSERT_PER_SOL = 4;
    /** The proportion of the task for eating a meal. */
    private static final double MEAL_EATING_PROPORTION = .75D;
    /** The proportion of the task for eating dessert. */
    private static final double DESSERT_EATING_PROPORTION = .25D;
    /** Percentage chance that preserved food has gone bad. */
    //private static final double PRESERVED_FOOD_BAD_CHANCE = 1D; // in %
    /** Percentage chance that unprepared dessert has gone bad. */
    //private static final double UNPREPARED_DESSERT_BAD_CHANCE = 1D; // in %
    /** Mass (kg) of single napkin for meal. */
    private static final double NAPKIN_MASS = .0025D;
    /** The factor during water ration when settlers are expected to conserve the remaining amount. */
    private static final double RATION_FACTOR = .5;
    
    private static double foodConsumptionRate;
    private static double dessertConsumptionRate;

    // Data members
    private double totalMealEatingTime = 0D;
    private double mealEatingDuration = 0D;
    private double totalDessertEatingTime = 0D;
    private double dessertEatingDuration = 0D;
    private double startingHunger;
    private double currentHunger;

    private double energy;
    private double waterEachServing;
    
    private boolean hasNapkin;
    
    private CookedMeal cookedMeal;
    private PreparedDessert nameOfDessert;
    private Cooking kitchen;
    private PreparingDessert dessertKitchen;
    private PhysicalCondition condition;
    
    private AmountResource unpreparedDessertAR;

    /**
     * Constructor.
     * @param person the person to perform the task
     */
    public EatMeal(Person person) {
        super(NAME, person, false, false, STRESS_MODIFIER, true, 20D +
                RandomUtil.getRandomDouble(10D));

        sourceName = sourceName.substring(sourceName.lastIndexOf(".") + 1, sourceName.length());
  
        condition = person.getPhysicalCondition();
        
        //double thirst = condition.getThirst();
        energy = condition.getEnergy();
        startingHunger = condition.getHunger();
        currentHunger = startingHunger;
        
        boolean notHungry = startingHunger < 150 && energy > 1500;
        
        // Check if person is outside and is not thirsty
		if (person.isOutside() && !condition.isThirsty()) {
			// TODO : if a person is on EVA suit, should be able to drink water from the helmet tube
			LogConsolidated.log(logger, Level.WARNING, 3000, sourceName, 
					person + " was trying to eat a meal, but is not inside a settlement/vehicle.", null);
			endTask();
		}
	        
        waterEachServing = condition.getWaterConsumedPerServing() *1000D;
        
        boolean want2Chat = true;
        // See if a person wants to chat while eating
        int score = person.getPreference().getPreferenceScore(new HaveConversationMeta());
        if (score > 0)
        	want2Chat = true;
        else if (score < 0)
        	want2Chat = false;
        else {
        	int rand = RandomUtil.getRandomInt(1);
        	if (rand == 0)
        		want2Chat = false;
        }
        
        if (person.isInSettlement()) {
	        Building diningBuilding = EatMeal.getAvailableDiningBuilding(person, want2Chat);
	        if (diningBuilding != null) {
	        	// Walk to that building.
	        	walkToActivitySpotInBuilding(diningBuilding, FunctionType.DINING, true);
	        }
        }

        // if a person is thirsty and not hungry
        if (condition.isThirsty() && notHungry) {
        	consumeWater(true);
        	endTask();
        }

        // Initialize data members.
        double dur = getDuration();
        mealEatingDuration = dur * MEAL_EATING_PROPORTION;
        dessertEatingDuration = dur * DESSERT_EATING_PROPORTION;

        PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
        foodConsumptionRate = config.getFoodConsumptionRate() / NUMBER_OF_MEAL_PER_SOL;
        dessertConsumptionRate = config.getDessertConsumptionRate() / NUMBER_OF_DESSERT_PER_SOL;

        // Take napkin from inventory if available.
        if (person.isInSettlement()) {
	        Unit container = person.getTopContainerUnit();
	        if (container != null) {
	        	Inventory inv = container.getInventory();
	            if (inv != null)
	            	hasNapkin = Storage.retrieveAnResource(NAPKIN_MASS, ResourceUtil.napkinAR, inv, false);
	            //else
	            //	endTask();
	        }
        }  
    
    	// if a person is just a little thirsty and NOT that hungry  
        if (notHungry) {
	        // Initialize task phase.
            addPhase(PICK_UP_DESSERT);
            addPhase(EATING_DESSERT);
            
            setPhase(PICK_UP_DESSERT);
        }
        else {//if (startingHunger >= 150 && energy <= 1000) {
        	// if a person is a thirsty and NOT that hungry  
	        // Initialize task phase.
	        addPhase(PICK_UP_MEAL);
	        addPhase(PICK_UP_DESSERT);
	        addPhase(EATING_MEAL);
	        addPhase(EATING_DESSERT);
	
	        setPhase(PICK_UP_MEAL);
        }
    }

    @Override
    protected FunctionType getLivingFunction() {
        return FunctionType.DINING;
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
        else if (PICK_UP_MEAL.equals(getPhase())) {
            return pickUpMealPhase(time);
        }
        else if (EATING_MEAL.equals(getPhase())) {
            return eatingMealPhase(time);
        }
        else if (PICK_UP_DESSERT.equals(getPhase())) {
            return pickUpDessertPhase(time);
        }
        else if (EATING_DESSERT.equals(getPhase())) {
            return eatingDessertPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Perform the pick up meal phase.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    private double pickUpMealPhase(double time) {

      // Determine preferred kitchen to get meal.
      if (kitchen == null) {
          kitchen = getKitchenWithMeal(person);

          if (kitchen != null) {
              // Walk to kitchen.
              walkToActivitySpotInBuilding(kitchen.getBuilding(), FunctionType.DINING, true);
              return time;
          }
          else {
              // If no kitchen found, look for dessert.
              setPhase(EATING_MEAL);//PICK_UP_DESSERT);
              return time;
          }
      }

      if (kitchen != null) {
	      // Pick up a meal at kitchen if one is available.
	      cookedMeal = kitchen.chooseAMeal(person);
	      if (cookedMeal != null) {
	          logger.fine(person + " picking up a cooked meal to eat: " + cookedMeal.getName());
	      }
      }

      setPhase(EATING_MEAL);//PICK_UP_DESSERT);
      return time;
    }

    /**
     * Performs the eating meal phase of the task.
     * @param time the amount of time (millisol) to perform the eating meal phase.
     * @return the amount of time (millisol) left after performing the eating meal phase.
     */
    private double eatingMealPhase(double time) {

        double remainingTime = 0D;

        double eatingTime = time;
        if ((totalMealEatingTime + eatingTime) >= mealEatingDuration) {
            eatingTime = mealEatingDuration - totalMealEatingTime;
        }

        if (eatingTime > 0D) {
        	
            if (cookedMeal != null) {
                // Eat cooked meal.
                setDescription(Msg.getString("Task.description.eatMeal.cooked.detail", cookedMeal.getName())); //$NON-NLS-1$
                eatCookedMeal(eatingTime);
            }
            else {
                // Eat preserved food.
                setDescription(Msg.getString("Task.description.eatMeal.preserved")); //$NON-NLS-1$
                boolean enoughFood = eatPreservedFood(eatingTime);

                // If not enough preserved food available, change to dessert phase.
                if (!enoughFood) {
                    setPhase(PICK_UP_DESSERT);//EATING_DESSERT);
                    remainingTime = time * .6;
                }
                //else {
                //	consumeWater(false);
                //}
            }     
        }

        totalMealEatingTime += eatingTime;

        // If finished eating, change to dessert phase.
        if (eatingTime < time) {
            setPhase(PICK_UP_DESSERT);//EATING_DESSERT);
            remainingTime = time * .6 - eatingTime;
        }
        
        if (condition.isThirsty())
        	consumeWater(false);
 
        return remainingTime;
    }


    /**
     * Perform the pick up dessert phase.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    private double pickUpDessertPhase(double time) {

        // Determine preferred kitchen to get dessert.
        if (dessertKitchen == null) {
            dessertKitchen = getKitchenWithDessert(person);

            if (dessertKitchen != null) {
                // Walk to dessert kitchen.
                walkToActivitySpotInBuilding(dessertKitchen.getBuilding(), FunctionType.DINING, true);
                return time;
            }
            else {
                // If no dessert kitchen found, go eat meal.
                setPhase(EATING_DESSERT);//EATING_MEAL);
                return time;
            }
        }

        if (dessertKitchen != null) {
	        // Pick up a dessert at kitchen if one is available.
	        nameOfDessert = dessertKitchen.chooseADessert(person);
	        if (nameOfDessert != null) {
	            logger.fine(person + " picking up a prepared dessert to eat: " + nameOfDessert.getName());
	        }
        }

        setPhase(EATING_DESSERT);//EATING_MEAL);
        return time;
    }


    /**
     * Eat a cooked meal.
     * @param eatingTime the amount of time (millisols) to eat.
     */
    private void eatCookedMeal(double eatingTime) {

        // Proportion of meal being eaten over this time period.
        double mealProportion = eatingTime / mealEatingDuration;

        //PhysicalCondition condition = person.getPhysicalCondition();

        // Reduce person's hunger by proportion of meal eaten.
        // Entire meal will reduce person's hunger to 0.
        currentHunger -= (startingHunger * mealProportion);
        if (currentHunger < 0D) {
            currentHunger = 0D;
        }
        condition.setHunger(currentHunger);

        // Reduce person's stress over time from eating a cooked meal.
        // This is in addition to normal stress reduction from eating task.
        double mealStressModifier = STRESS_MODIFIER * (cookedMeal.getQuality() + 1D);
        double newStress = condition.getStress() - (mealStressModifier * eatingTime);
        condition.setStress(newStress);

        // Add caloric energy from meal.
        double caloricEnergyFoodAmount = cookedMeal.getDryMass() * mealProportion;
        condition.addEnergy(caloricEnergyFoodAmount);
        
    }

    /**
     * Eat a meal of preserved food.
     * @param eatingTime the amount of time (millisols) to eat.
     * @return true if enough preserved food available to eat.
     */
    private boolean eatPreservedFood(double eatingTime) {

        boolean result = true;

        //PhysicalCondition condition = person.getPhysicalCondition();

        // Determine total preserved food amount eaten during this meal.
        //PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
        //double totalFoodAmount = config.getFoodConsumptionRate() / NUMBER_OF_MEAL_PER_SOL;

        // Proportion of meal being eaten over this time period.
        double mealProportion = eatingTime / mealEatingDuration;

        // Food amount eaten over this period of time.
        double foodAmount = foodConsumptionRate * mealProportion;
        Unit container = person.getTopContainerUnit();
        if (container != null) {
        	Inventory inv = container.getInventory();

            // Take preserved food from inventory if it is available.
            if (Storage.retrieveAnResource(foodAmount, ResourceUtil.foodID, inv, true)) {

                // Consume preserved food.

                // Reduce person's hunger by proportion of meal eaten.
                // Entire meal will reduce person's hunger to 0.
                currentHunger -= (startingHunger * mealProportion);
                if (currentHunger < 0D) {
                    currentHunger = 0D;
                }
                condition.setHunger(currentHunger);

                // Add caloric energy from meal.
                condition.addEnergy(foodAmount);
                
/*            	
                // Check if preserved food has gone bad.
                if (RandomUtil.lessThanRandPercent(PRESERVED_FOOD_BAD_CHANCE)) {
                    //if (inv == null) 
                    	//logger.info("preserved food gone bad, turn into food waste");
                    // Throw food out.
                	if (foodAmount > 0)
                		Storage.storeAnResource(foodAmount, ResourceUtil.foodWasteAR, inv, sourceName + "::eatPreservedFood");
                }
                else {
                    // Consume preserved food.

                    // Reduce person's hunger by proportion of meal eaten.
                    // Entire meal will reduce person's hunger to 0.
                    currentHunger -= (startingHunger * mealProportion);
                    if (currentHunger < 0D) {
                        currentHunger = 0D;
                    }
                    condition.setHunger(currentHunger);

                    // Add caloric energy from meal.
                    condition.addEnergy(foodAmount);
                }
                
*/                
            }
            else {
                // Not enough food available to eat.
                result = false;
            }
        }
        else {
            // Person is not inside a container unit, so end task.
             result = false;
            endTask();
        }

        return result;
    }

    /**
     * Performs the eating dessert phase of the task.
     * @param time the amount of time (millisol) to perform the eating dessert phase.
     * @return the amount of time (millisol) left after performing the eating dessert phase.
     */
    private double eatingDessertPhase(double time) {

        double remainingTime = 0D;

        double eatingTime = time;
        if ((totalDessertEatingTime + eatingTime) >= dessertEatingDuration) {
            eatingTime = dessertEatingDuration - totalDessertEatingTime;
        }

        if (eatingTime > 0D) {

            if (nameOfDessert != null) {
                // Eat prepared dessert.
            	checkInDescription(PreparingDessert.convertString2AR(nameOfDessert.getName()), true);
                eatPreparedDessert(eatingTime);

            }
            else {
                // Eat unprepared dessert (fruit, soymilk, etc).
                 boolean enoughDessert = eatUnpreparedDessert(eatingTime);

                 if (enoughDessert) {
                	 checkInDescription(unpreparedDessertAR, false);
                 }

                // If not enough unprepared dessert available, end task.
                 else {//if (!enoughDessert) {
                	 remainingTime = time;
                	 endTask();
                 }
            }
        }

        totalMealEatingTime += eatingTime;

        // If finished eating, end task.
        if (eatingTime < time) {
            remainingTime = time - eatingTime;
            endTask();
        }

        return remainingTime;
    }

    private void checkInDescription(AmountResource dessertAR, boolean prepared) {
    	String s = dessertAR.getName();
    	if (s.contains("milk") || s.contains("juice")) {
    		if (prepared)
    			setDescription(Msg.getString("Task.description.eatMeal.preparedDessert.drink", Conversion.capitalize(s))); //$NON-NLS-1$
    		else
               	setDescription(Msg.getString("Task.description.eatMeal.unpreparedDessert.drink", Conversion.capitalize(s))); //$NON-NLS-1$
    	}
    	else {
    		if (prepared)
    			setDescription(Msg.getString("Task.description.eatMeal.preparedDessert.eat", Conversion.capitalize(s))); //$NON-NLS-1$
    		else
               	setDescription(Msg.getString("Task.description.eatMeal.unpreparedDessert.eat", Conversion.capitalize(s))); //$NON-NLS-1$
    	}
    }


    /**
     * Eat a prepared dessert.
     * @param eatingTime the amount of time (millisols) to eat.
     */
    private void eatPreparedDessert(double eatingTime) {

        // Proportion of dessert being eaten over this time period.
        double dessertProportion = eatingTime / dessertEatingDuration;

        //PhysicalCondition condition = person.getPhysicalCondition();

        // Reduce person's stress over time from eating a prepared.
        // This is in addition to normal stress reduction from eating task.
        double mealStressModifier = DESSERT_STRESS_MODIFIER * (nameOfDessert.getQuality() + 1D);
        double newStress = condition.getStress() - (mealStressModifier * eatingTime);
        condition.setStress(newStress);

        double dryMass = nameOfDessert.getDryMass();
        // Consume water
        consumeWater(dryMass);
        
        // Add caloric energy from dessert.
        double caloricEnergyFoodAmount = dryMass * dessertProportion;
        condition.addEnergy(caloricEnergyFoodAmount);
        
    }

    /**
     * Calculates the amount of water to consume during a dessert
     */
    public void consumeWater(double dryMass) {
    	double currentThirst = condition.getThirst();
    	double waterFinal = Math.min(waterEachServing, currentThirst);

        // Note that the water content within the dessert has already been deducted from the settlement 
        // when the dessert was made.
        double waterPortion = 1000 * (PreparingDessert.getDessertMassPerServing() - dryMass);
        if (waterPortion > 0) {
        	waterFinal = waterFinal - waterPortion;
        }

	    if (waterFinal > 0)  {
		   	double new_thirst = (currentThirst - waterFinal)/8;   	
	    	condition.setThirst(new_thirst);  	
	    	//condition.setThirsty(false);
    	}
    }
    
    /**
     * Calculates the amount of water to consume after a meal
     */
    public synchronized void consumeWater(boolean waterOnly) {
    	double currentThirst = condition.getThirst();
    	Unit containerUnit = person.getTopContainerUnit();
        if (containerUnit != null && currentThirst > 50) {
            Inventory inv = containerUnit.getInventory();
	        double waterFinal = Math.min(waterEachServing, currentThirst);

		    if (waterFinal > 0)  {

		    	double new_thirst = (currentThirst - waterFinal)/10;
		    	// Test to see if there's enough water
		    	boolean haswater = Storage.retrieveAnResource(waterFinal/1000D, ResourceUtil.waterID, inv , false);  
		    	
		    	if (haswater) {
		    		condition.setThirsty(false);
		    		person.setWaterRation(false);
		    		condition.setThirst(new_thirst);
		    		if (waterOnly)
		    			setDescription(Msg.getString("Task.description.eatMeal.water")); //$NON-NLS-1$
		    		double amount = waterFinal/1000D;	    	
		    		inv.retrieveAmountResource(ResourceUtil.waterID, amount);
		    		inv.addAmountDemand(ResourceUtil.waterID, amount);
		    		//LogConsolidated.log(logger, Level.INFO, 1000, sourceName, 
		    				
		    		//logger.info(person + " is drinking " + Math.round(amount * 1000.0)/1000.0 + " kg of water"
    				//		+ "   thirst : " + Math.round(currentThirst* 100.0)/100.0
    				//		+ "   waterEachServing : " + Math.round(waterEachServing* 100.0)/100.0
		    		//		+ "   waterFinal : " + Math.round(waterFinal* 100.0)/100.0
		    		//		+ "   new_thirst : " + Math.round(new_thirst* 100.0)/100.0);
		    		//		, null);		
		    	}
		    	
		    	if (person.getWaterRation() || !haswater) {
		    		if (!haswater) 
		    			person.setWaterRation(true);
			    	// Test to see if there's just half of the amount of water
		    		haswater = Storage.retrieveAnResource(waterFinal/1000D*RATION_FACTOR, ResourceUtil.waterID, inv , false);
		    		if (haswater) {
		    			condition.setThirsty(false);
		    			new_thirst = new_thirst * (1 - RATION_FACTOR)/2D;
			    		condition.setThirst(new_thirst);
			    		if (waterOnly)
			    			setDescription(Msg.getString("Task.description.eatMeal.water")); //$NON-NLS-1$
			    		double amount = waterFinal/1000D*RATION_FACTOR;
			    		LogConsolidated.log(logger, Level.INFO, 1000, sourceName,
			    				"[" + person.getLocationTag().getQuickLocation() +  "] " +
			    				person + " is on ration when drinking " + Math.round(amount * 1000.0)/1000.0 + " kg of water", null);	
			    		inv.retrieveAmountResource(ResourceUtil.waterID, amount);
			    		inv.addAmountDemand(ResourceUtil.waterID, amount);
			    	}
		    	}
		    	
		    	if (!haswater) {
		    		person.setWaterRation(true);
			    	// Test to see if there's just a quarter of the amount of water
		    		haswater = Storage.retrieveAnResource(waterFinal/2000D*RATION_FACTOR, ResourceUtil.waterID, inv , false);
		    		if (haswater) {
		    			condition.setThirsty(false);
		    			new_thirst = new_thirst * (1 - RATION_FACTOR)/1.5;
			    		condition.setThirst(new_thirst);
			    		if (waterOnly)
			    			setDescription(Msg.getString("Task.description.eatMeal.water")); //$NON-NLS-1$
			    		double amount = waterFinal/2000D*RATION_FACTOR;
			    		LogConsolidated.log(logger, Level.INFO, 1000, sourceName,
			    				"[" + person.getLocationTag().getQuickLocation() +  "] " +
			    				person + " is on ration when drinking " + Math.round(amount * 1000.0)/1000.0 + " kg of water", null);	
			    		inv.retrieveAmountResource(ResourceUtil.waterID, amount);
			    		inv.addAmountDemand(ResourceUtil.waterID, amount);
			    	}
		    	}
	    	}	        
        }
        
        //if (condition.getThirst() > 50) {
        //	logger.info(person + " new thirst : " + Math.round(condition.getThirst() * 1000.0)/1000.0);        	
        //}
    }
    
    
    
    /**
     * Eat an unprepared dessert.
     * @param eatingTime the amount of time (millisols) to eat.
     * @return true if enough unprepared dessert was available to eat.
     */
    private boolean eatUnpreparedDessert(double eatingTime) {

        boolean result = true;

        // Determine dessert resource type if not known.
        if (unpreparedDessertAR == null) {

        	boolean isThirsty = false;
        	if (condition.getThirst() > 50)
        		isThirsty = true;
            // Determine list of available dessert resources.
            List<AmountResource> availableDessertResources = getAvailableDessertResources(dessertConsumptionRate, isThirsty);
            if (availableDessertResources.size() > 0) {

                // Randomly choose available dessert resource.
                int index = RandomUtil.getRandomInt(availableDessertResources.size() - 1);
                unpreparedDessertAR = availableDessertResources.get(index);
            }
            else {
                result = false;
            }
        }

        // Consume portion of unprepared dessert resource.
        if (unpreparedDessertAR != null) {
            // Proportion of dessert being eaten over this time period.
            double dessertProportion = eatingTime / dessertEatingDuration;

            // Dessert amount eaten over this period of time.
            double dessertAmount = dessertConsumptionRate * dessertProportion;
            Unit containerUnit = person.getTopContainerUnit();
            if (containerUnit != null) {
                Inventory inv = containerUnit.getInventory();
                // Take dessert resource from inventory if it is available.
                if (Storage.retrieveAnResource(dessertAmount, unpreparedDessertAR, inv, true)) {
                	// Consume unpreserved dessert.

                    // Add caloric energy from dessert.
                    condition.addEnergy(dessertAmount);                
                }
                else {
                    // Not enough dessert resource available to eat.
                    result = false;
                }
                

                double dryMass = PreparingDessert.getDryMass(PreparingDessert.convertAR2String(unpreparedDessertAR));
                // Consume water
                consumeWater(dryMass);
                
            }
            //else {
                // Person is not inside a container unit, so end task.
            //    result = false;
            //    endTask();
            //}
        }

        return result;
    }

    /**
     * Gets a list of available unprepared dessert AmountResource.
     * @param amountNeeded the amount (kg) of unprepared dessert needed for eating.
     * @return list of AmountResource.
     */
    private List<AmountResource> getAvailableDessertResources(double amountNeeded, boolean isThirsty) {

        List<AmountResource> result = new ArrayList<AmountResource>();

        Unit containerUnit = person.getTopContainerUnit();
        if (containerUnit != null) {
            Inventory inv = containerUnit.getInventory();
/*

            int size = ARs.length;
            for (int x = 0; x < size; x++) {
            	AmountResource dessertAR = ARs[x];
                boolean available = Storage.retrieveAnResource(amountNeeded, dessertAR, inv, false);
                if (available) {
                    result.add(dessertAR);
                }
            }
*/
            boolean option = true;


            AmountResource[] ARs = PreparingDessert.getArrayOfDessertsAR();
        	for (AmountResource ar : ARs) {
                if (isThirsty) 
                	option = ar.getName().contains("juice") || ar.getName().contains("milk");
                if (option && Storage.retrieveAnResource(amountNeeded, ar, inv, false)) {
                    result.add(ar);
                }
        	}
        }

        return result;
    }

    /**
     * Adds experience to the person's skills used in this task.
     * @param time the amount of time (ms) the person performed this task.
     */
    protected void addExperience(double time) {
        // This task adds no experience.
    }

    /**
     * Gets an available dining building that the person can use.
     * Returns null if no dining building is currently available.
     *
     * @param person the person
     * @return available dining building
     * @throws BuildingException if error finding dining building.
     */
    public static Building getAvailableDiningBuilding(Person person, boolean canChat) {
        Building result = null;

        if (LocationSituation.IN_SETTLEMENT == person.getLocationSituation()) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> diningBuildings = manager.getBuildings(FunctionType.DINING);
            diningBuildings = BuildingManager.getWalkableBuildings(person, diningBuildings);
            diningBuildings = BuildingManager.getNonMalfunctioningBuildings(diningBuildings);
            if (canChat)
                // Choose between the most crowded or the least crowded dining hall 
            	diningBuildings = BuildingManager.getChattyBuildings(diningBuildings);
            else
                diningBuildings = BuildingManager.getLeastCrowdedBuildings(diningBuildings);

            if (diningBuildings.size() > 0) {
                Map<Building, Double> diningBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                        person, diningBuildings);
                result = RandomUtil.getWeightedRandomObject(diningBuildingProbs);
            }
        }

        return result;
    }

    /**
     * Gets a kitchen in the person's settlement that currently has cooked meals.
     * @param person the person to check for
     * @return the kitchen or null if none.
     */
    public static Cooking getKitchenWithMeal(Person person) {
        Cooking result = null;

        if (LocationSituation.IN_SETTLEMENT == person.getLocationSituation()) {
            BuildingManager manager = person.getSettlement().getBuildingManager();
            List<Building> cookingBuildings = manager.getBuildings(FunctionType.COOKING);
            for (Building building : cookingBuildings) {
                Cooking kitchen = building.getCooking();
                if (kitchen.hasCookedMeal()) {
                    result = kitchen;
                }
            }
        }

        return result;
    }

    /**
     * Gets a kitchen in the person's settlement that currently has prepared desserts.
     * @param person the person to check for
     * @return the kitchen or null if none.
     */
    public static PreparingDessert getKitchenWithDessert(Person person) {
        PreparingDessert result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> dessertBuildings = manager.getBuildings(FunctionType.PREPARING_DESSERT);
            for (Building building : dessertBuildings) { 
                PreparingDessert kitchen = building.getPreparingDessert();
                if (kitchen.hasFreshDessert()) {
                    result = kitchen;
                }
            }
        }

        return result;
    }

    /**
     * Checks if there is preserved food available for the person.
     * @param person the person to check.
     * @return true if preserved food is available.
     */
    public static boolean isPreservedFoodAvailable(Person person) {
        boolean result = false;

        Unit containerUnit = person.getTopContainerUnit();
        if (containerUnit != null) {
            //try {
                Inventory inv = containerUnit.getInventory();
                result = Storage.retrieveAnResource(foodConsumptionRate, ResourceUtil.foodID, inv, false);
            //}
            //catch (Exception e) {
            //    e.printStackTrace(System.err);
            //}
        }
        return result;
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
    public void endTask() {
        super.endTask();

        // Throw away napkin waste if one was used.
        if (hasNapkin) {
        	Unit containerUnit = person.getTopContainerUnit();
        	if (containerUnit != null) {
        		Inventory inv = containerUnit.getInventory();
        		if (NAPKIN_MASS > 0)
        			Storage.storeAnResource(NAPKIN_MASS, ResourceUtil.solidWasteAR, inv, sourceName + "::endTask");
        	}
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        kitchen = null;
        cookedMeal = null;
        dessertKitchen = null;
        nameOfDessert = null;
    }
}