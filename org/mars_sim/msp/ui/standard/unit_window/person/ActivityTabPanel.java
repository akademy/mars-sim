/**
 * Mars Simulation Project
 * ActivityTabPanel.java
 * @version 2.75 2003-06-18
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard.unit_window.person;

import java.awt.*;
import javax.swing.*;
import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.person.ai.*;
import org.mars_sim.msp.simulation.person.medical.DeathInfo;
import org.mars_sim.msp.ui.standard.*;
import org.mars_sim.msp.ui.standard.unit_window.TabPanel;

/** 
 * The ActivityTabPanel is a tab panel for a person's current activities.
 */
public class ActivityTabPanel extends TabPanel {
    
    private JTextArea taskTextArea;
    private JTextArea taskPhaseTextArea;
    private JTextArea missionTextArea;
    private JTextArea missionPhaseTextArea;
    
    // Data cache
    private String taskCache = "";
    private String taskPhaseCache = "";
    private String missionCache = "";
    private String missionPhaseCache = "";
    
    /**
     * Constructor
     *
     * @param proxy the UI proxy for the unit.
     * @param desktop the main desktop.
     */
    public ActivityTabPanel(UnitUIProxy proxy, MainDesktopPane desktop) { 
        // Use the TabPanel constructor
        super("Activity", null, "Activity", proxy, desktop);
        
        Person person = (Person) proxy.getUnit();
        Mind mind = person.getMind();
        boolean dead = person.getPhysicalCondition().isDead();
        DeathInfo deathInfo = person.getPhysicalCondition().getDeathDetails();
        
        // Prepare activity label panel
        JPanel activityLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topContentPanel.add(activityLabelPanel);
        
        // Prepare activity label
        JLabel activityLabel = new JLabel("Activity", JLabel.CENTER);
        activityLabelPanel.add(activityLabel);
        
        // Prepare activity panel
        JPanel activityPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        centerContentPanel.add(activityPanel);
        
        // Prepare task top panel
        JPanel taskTopPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        taskTopPanel.setBorder(new MarsPanelBorder());
        activityPanel.add(taskTopPanel);
        
        // Prepare task panel
        JPanel taskPanel = new JPanel(new BorderLayout(0, 0));
        taskTopPanel.add(taskPanel);
        
        // Prepare task label
        JLabel taskLabel = new JLabel("Task", JLabel.CENTER);
        taskPanel.add(taskLabel, BorderLayout.NORTH);
        
        // Prepare task text area
        if (dead) taskCache = deathInfo.getTask();
        else taskCache = mind.getTaskManager().getTaskDescription();
        taskTextArea = new JTextArea(2, 20);
        if (taskCache != null) taskTextArea.setText(taskCache);
        taskTextArea.setLineWrap(true);
        taskPanel.add(new JScrollPane(taskTextArea), BorderLayout.CENTER);
        
        // Prepare task phase panel
        JPanel taskPhasePanel = new JPanel(new BorderLayout(0, 0));
        taskTopPanel.add(taskPhasePanel);
        
        // Prepare task phase label
        JLabel taskPhaseLabel = new JLabel("Task Phase", JLabel.CENTER);
        taskPhasePanel.add(taskPhaseLabel, BorderLayout.NORTH);
        
        // Prepare task phase text area
        if (dead) taskPhaseCache = deathInfo.getTaskPhase();
        else taskPhaseCache = mind.getTaskManager().getPhase();
        taskPhaseTextArea = new JTextArea(2, 20);
        if (taskPhaseCache != null) taskPhaseTextArea.setText(taskPhaseCache);
        taskPhaseTextArea.setLineWrap(true);
        taskPhasePanel.add(new JScrollPane(taskPhaseTextArea), BorderLayout.CENTER);
        
        // Prepare mission top panel
        JPanel missionTopPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        missionTopPanel.setBorder(new MarsPanelBorder());
        activityPanel.add(missionTopPanel);
        
        // Prepare mission panel
        JPanel missionPanel = new JPanel(new BorderLayout(0, 0));
        missionTopPanel.add(missionPanel);
        
        // Prepare mission label
        JLabel missionLabel = new JLabel("Mission", JLabel.CENTER);
        missionPanel.add(missionLabel, BorderLayout.NORTH);
        
        // Prepare mission text area
        if (dead) missionCache = deathInfo.getMission();
        else if (mind.getMission() != null) missionCache = mind.getMission().getDescription();
        missionTextArea = new JTextArea(2, 20);
        if (missionCache != null) missionTextArea.setText(missionCache);
        missionTextArea.setLineWrap(true);
        missionPanel.add(new JScrollPane(missionTextArea), BorderLayout.CENTER);
        
        // Prepare mission phase panel
        JPanel missionPhasePanel = new JPanel(new BorderLayout(0, 0));
        missionTopPanel.add(missionPhasePanel);
        
        // Prepare mission phase label
        JLabel missionPhaseLabel = new JLabel("Mission Phase", JLabel.CENTER);
        missionPhasePanel.add(missionPhaseLabel, BorderLayout.NORTH);
        
        // Prepare mission phase text area
        if (dead) missionPhaseCache = deathInfo.getMissionPhase();
        else if (mind.getMission() != null) missionPhaseCache = mind.getMission().getPhase();
        missionPhaseTextArea = new JTextArea(2, 20);
        if (missionPhaseCache != null) missionPhaseTextArea.setText(missionPhaseCache);
        missionPhaseTextArea.setLineWrap(true);
        missionPhasePanel.add(new JScrollPane(missionPhaseTextArea), BorderLayout.CENTER);
    }
    
    /**
     * Updates the info on this panel.
     */
    public void update() {
        
        Person person = (Person) proxy.getUnit();
        Mind mind = person.getMind();
        boolean dead = person.getPhysicalCondition().isDead();
        DeathInfo deathInfo = person.getPhysicalCondition().getDeathDetails();
        TaskManager taskManager = null;
        Mission mission = null;
        if (!dead) {
            taskManager = mind.getTaskManager();
            mission = mind.getMission();
        }
        
        // Update task text area if necessary.
        if (dead) taskCache = deathInfo.getTask();
        else taskCache = taskManager.getTaskDescription();
        if (!taskCache.equals(taskTextArea.getText())) 
            taskTextArea.setText(taskCache);
        
        // Update task phase text area if necessary.
        if (dead) taskPhaseCache = deathInfo.getTaskPhase();
        else taskPhaseCache = taskManager.getPhase();
        if (!taskPhaseCache.equals(taskPhaseTextArea.getText())) 
            taskPhaseTextArea.setText(taskPhaseCache);
        
        // Update mission text area if necessary.
        if (dead) missionCache = deathInfo.getMission();
        else if (mission != null) missionCache = mission.getDescription();
        if (!missionCache.equals(missionTextArea.getText())) 
            missionTextArea.setText(missionCache);
        
        // Update mission phase text area if necessary.
        if (dead) missionPhaseCache = deathInfo.getMissionPhase();
        else if (mission != null) missionPhaseCache = mission.getPhase();
        if (!missionPhaseCache.equals(missionPhaseTextArea.getText())) 
            missionPhaseTextArea.setText(missionPhaseCache);
    }
}       
