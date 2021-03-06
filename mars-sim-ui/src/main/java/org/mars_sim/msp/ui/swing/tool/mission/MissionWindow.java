/**
 * Mars Simulation Project
 * MissionWindow.java
 * @version 3.1.0 2017-02-03
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.JList;

import javax.swing.ListSelectionModel;

import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.ui.javafx.MainScene;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.mission.create.CreateMissionWizard;
import org.mars_sim.msp.ui.swing.tool.mission.edit.EditMissionDialog;
import org.mars_sim.msp.ui.swing.toolWindow.ToolWindow;

import com.alee.laf.button.WebButton;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.tabbedpane.WebTabbedPane;

/**
 * Window for the mission tool.
 */
public class MissionWindow
extends ToolWindow {

	/** Tool name. */
	public static final String NAME = "Mission Tool";

	// Private members
	private double previous;
	
	private WebTabbedPane tabPane;
	private JList<Mission> missionList;
	
	private NavpointPanel navpointPane;
	private MainScene mainScene;
	private CreateMissionWizard createMissionWizard;
	private EditMissionDialog editMissionDialog;
	

	/**
	 * Constructor.
	 * @param desktop {@link MainDesktopPane} the main desktop panel.
	 */
	public MissionWindow(MainDesktopPane desktop) {

		// Use ToolWindow constructor
		super(NAME, desktop);
		mainScene = desktop.getMainScene();

		// Create content panel.
		WebPanel mainPane = new WebPanel(new BorderLayout());
		mainPane.setBorder(MainDesktopPane.newEmptyBorder());
		setContentPane(mainPane);

		// Create the mission list panel.
		WebPanel missionListPane = new WebPanel(new BorderLayout());
		missionListPane.setPreferredSize(new Dimension(200, 200));
		mainPane.add(missionListPane, BorderLayout.WEST);

		// Create the mission list.
		missionList = new JList<Mission>(new MissionListModel());
		missionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		missionListPane.add(new WebScrollPane(missionList), BorderLayout.CENTER);

		// Create the info tab panel.
		tabPane = new WebTabbedPane();
		mainPane.add(tabPane, BorderLayout.CENTER);

		// Create the main detail panel.
		MainDetailPanel infoPane = new MainDetailPanel(desktop, this);
		missionList.addListSelectionListener(infoPane);
		tabPane.add("Info", infoPane);

		// Create the navpoint panel.
		navpointPane = new NavpointPanel(desktop);
		missionList.addListSelectionListener(navpointPane);
		tabPane.add("Navigation", navpointPane);

		// Create the button panel.
		WebPanel buttonPane = new WebPanel(new FlowLayout());
		mainPane.add(buttonPane, BorderLayout.SOUTH);

		// Create the create mission button.
		WebButton createButton = new WebButton("Create New Mission");
		createButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Create new mission.
						createNewMission();
					}
				});
		buttonPane.add(createButton);

		// Create the edit mission button.
		final WebButton editButton = new WebButton("Modify Mission");
		editButton.setEnabled(false);
/*
		editButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Edit the mission.
						mission = (Mission) missionList.getSelectedValue();
						if (mission != null) editMission(mission);
					}
				});
		missionList.addListSelectionListener(
			new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {
					// Enable button if mission is selected in list.
					editButton.setEnabled(missionList.getSelectedValue() != null);
				}
			}
		);
*/
		buttonPane.add(editButton);

		// Create the abort mission button.
		final WebButton abortButton = new WebButton("Abort Mission");
		abortButton.setEnabled(false);
/*		
		abortButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// End the mission.
						mission = (Mission) missionList.getSelectedValue();
						if (mission != null) endMission(mission);
					}
				});
		missionList.addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						abortButton.setEnabled(missionList.getSelectedValue() != null);
					}
				});
*/
		buttonPane.add(abortButton);

		setSize(new Dimension(640, 640));//736));
		setMaximizable(true);
		setResizable(false);

		if (desktop.getMainScene() != null) {
			//setClosable(false);
		}

		setVisible(true);
		//pack();

		Dimension desktopSize = desktop.getSize();
	    Dimension jInternalFrameSize = this.getSize();
	    int width = (desktopSize.width - jInternalFrameSize.width) / 2;
	    int height = (desktopSize.height - jInternalFrameSize.height) / 2;
	    setLocation(width, height);

	}

	/**
	 * Selects a mission for display.
	 * @param mission the mission to select.
	 */
	public void selectMission(Mission mission) {
		MissionListModel model = (MissionListModel) missionList.getModel();
		if (model.containsMission(mission)) missionList.setSelectedValue(mission, true);
	}

	/**
	 * Open wizard to create a new mission.
	 */
	private void createNewMission() {
		if (mainScene != null)  {
			previous = mainScene.slowDownTimeRatio();
			createMissionWizard = new CreateMissionWizard(desktop, this);
			mainScene.speedUpTimeRatio(previous);
		} 
		else {
			createMissionWizard = new CreateMissionWizard(desktop, this);
		}

	}

	/**
	 * Open wizard to edit a mission.
	 * @param mission the mission to edit.

	private void editMission(Mission mission) {

		if (ms != null)  {
			// 2015-12-16 Track the current pause state
			boolean previous = ms.startPause();

			editMissionDialog = new EditMissionDialog(desktop, mission, this);

			ms.endPause(previous);

		} else

			editMissionDialog = new EditMissionDialog(desktop, mission, this);
	}
*/
	
	/**
	 * Ends the mission.
	 * @param mission the mission to end.

	private void endMission(Mission mission) {
		//logger.info("End mission: " + mission.getName());
		mission.endMission(Mission.USER_ABORTED_MISSION);
		repaint();
	}
	
*/
	
	public CreateMissionWizard getCreateMissionWizard() {
		return createMissionWizard;
	}

	public MainDesktopPane getDesktop() {
		return desktop;
	}
	
	public boolean isNavPointsMapTabOpen() {
		if (tabPane.getSelectedIndex() == 1)
			return true;
		else
			return false;
	}
	
	/**
	 * Prepares tool window for deletion.
	 */
	@Override
	public void destroy() {
		missionList.clearSelection();
		((MissionListModel) missionList.getModel()).destroy();
		navpointPane.destroy();
	}
}