/**
 * Mars Simulation Project
 * LoadVehicleTest.java
 * @version 3.1.0 2017-01-21
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LifeSupportType;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.equipment.SpecimenContainer;
import org.mars_sim.msp.core.person.GenderType;
import org.mars_sim.msp.core.person.NaturalAttributeType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.structure.MockSettlement;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.MockBuilding;
import org.mars_sim.msp.core.structure.building.function.BuildingAirlock;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.vehicle.MockVehicle;
import org.mars_sim.msp.core.vehicle.Vehicle;

import junit.framework.TestCase;

public class UnloadVehicleTest
extends TestCase {

	@Override
	public void setUp() throws Exception {
		SimulationConfig.loadConfig();
	}

    public void testUnloadingPhase() throws Exception {

		AmountResource oxygen = AmountResource.findAmountResource(LifeSupportType.OXYGEN);
		AmountResource food = AmountResource.findAmountResource(LifeSupportType.FOOD);
		AmountResource water = AmountResource.findAmountResource(LifeSupportType.WATER);
		AmountResource methane = AmountResource.findAmountResource("methane");

        String resourceName = "hammer";
        String description = "a tool";
        int id = 1;
        double massPerItem = 1.4D;
        ItemResource hammer = ItemResource.createBrandNewItemResource(resourceName,id,description,massPerItem, 1);
		Settlement settlement = new MockSettlement();
		Inventory settlementInv = settlement.getInventory();
		settlementInv.addAmountResourceTypeCapacity(oxygen, 100D);
		settlementInv.addAmountResourceTypeCapacity(food, 100D);
		settlementInv.addAmountResourceTypeCapacity(water, 100D);
		settlementInv.addAmountResourceTypeCapacity(methane, 100D);

		Vehicle vehicle = new MockVehicle(settlement);
		Inventory vehicleInv = vehicle.getInventory();
		vehicleInv.addAmountResourceTypeCapacity(oxygen, 100D);
		vehicleInv.storeAmountResource(oxygen, 100D, true);
		vehicleInv.addAmountResourceTypeCapacity(food, 100D);
		vehicleInv.storeAmountResource(food, 100D, true);
		vehicleInv.addAmountResourceTypeCapacity(water, 100D);
		vehicleInv.storeAmountResource(water, 100D, true);
		vehicleInv.addAmountResourceTypeCapacity(methane, 100D);
		vehicleInv.storeAmountResource(methane, 100D, true);
		vehicleInv.addGeneralCapacity(100D);
		vehicleInv.storeItemResources(hammer, 5);
		for (int x = 0; x < 5; x++) {
			vehicleInv.storeUnit(new SpecimenContainer(settlement.getCoordinates()));
		}

		BuildingManager buildingManager = settlement.getBuildingManager();
        MockBuilding building0 = new MockBuilding(buildingManager);
        building0.setTemplateID(0);
        building0.setName("building 0");
        building0.setWidth(9D);
        building0.setLength(9D);
        building0.setXLocation(0D);
        building0.setYLocation(0D);
        building0.setFacing(0D);
        buildingManager.addBuilding(building0, false);

        BuildingAirlock airlock0 = new BuildingAirlock(building0, 1, 0D, 0D, 0D, 0D, 0D, 0D);
        building0.addFunction(new EVA(building0, airlock0));

		//Person person = new Person("test person", PersonGender.MALE, null, settlement, "Mars Society (MS)");
		// 2017-04-11 Use Builder Pattern for creating an instance of Person
		Person person = Person.create("test person", settlement)
								.setGender(GenderType.MALE)
								.setCountry(null)
								.setSponsor("Mars Society (MS)")
								.build();
		person.initialize();
		person.getNaturalAttributeManager().setAttribute(NaturalAttributeType.STRENGTH, 100);

		UnloadVehicleGarage unloadVehicle = new UnloadVehicleGarage(person, vehicle);
		unloadVehicle.unloadingPhase(11D);
		unloadVehicle.unloadingPhase(12D);
		unloadVehicle.unloadingPhase(13D);
		unloadVehicle.unloadingPhase(14D);
		unloadVehicle.unloadingPhase(15D);
		unloadVehicle.unloadingPhase(16D);
		unloadVehicle.unloadingPhase(100D);

		assertEquals("Vehicle unloaded correctly.", 0D, vehicleInv.getTotalInventoryMass(false), 0D);
	}
}