/**
 * Mars Simulation Project
 * MainWindowFX.java
 * @version 3.08 2015-03-28
 * @author Lars Næsbye Christensen
 */

package org.mars_sim.msp.ui.javafx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.guide.GuideWindow;
import org.mars_sim.msp.ui.swing.tool.mission.MissionWindow;
import org.mars_sim.msp.ui.swing.tool.monitor.MonitorWindow;
import org.mars_sim.msp.ui.swing.tool.navigator.NavigatorWindow;
import org.mars_sim.msp.ui.swing.tool.resupply.ResupplyWindow;
import org.mars_sim.msp.ui.swing.tool.science.ScienceWindow;
import org.mars_sim.msp.ui.swing.tool.search.SearchWindow;
import org.mars_sim.msp.ui.swing.tool.settlement.SettlementWindow;
import org.mars_sim.msp.ui.swing.tool.time.TimeWindow;


public class MainSceneMenu extends MenuBar  {

    //private MainDesktopPane desktop;
	private CheckMenuItem showFullScreenItem ;
	
	private Stage stage;
	private Stage webStage;
	//private GreenhouseTool greenhouseTool;
	/** 
	 * Constructor.
	 * @param mainWindow the main window pane
	 * @param desktop our main frame
	 */
	public MainSceneMenu(MainScene mainScene, MainDesktopPane desktop) {	
		super();

		this.stage = mainScene.getStage();
       
        // --- Menu File
        Menu menuFile = new Menu("File");
        MenuItem newItem = new MenuItem("New...");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        MenuItem openAutoSaveItem = new MenuItem("Open autosave");
        openAutoSaveItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN));
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN , KeyCombination.SHIFT_DOWN));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));
        SeparatorMenuItem SeparatorMenuItem1 = new SeparatorMenuItem();
        SeparatorMenuItem SeparatorMenuItem2 = new SeparatorMenuItem();
        SeparatorMenuItem SeparatorMenuItem3 = new SeparatorMenuItem();

        menuFile.getItems().addAll(newItem, SeparatorMenuItem1, openItem, openAutoSaveItem, SeparatorMenuItem2, saveItem, saveAsItem, SeparatorMenuItem3, exitItem);
        
        // --- Menu Tools
        Menu menuTools = new Menu("Tools");
        CheckMenuItem marsNavigatorItem = new CheckMenuItem("Mars Navigator");
        marsNavigatorItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        CheckMenuItem searchToolItem = new CheckMenuItem("Search Tool");
        searchToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F2));
        CheckMenuItem timeToolItem = new CheckMenuItem("Time Tool");
        timeToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F3));
        CheckMenuItem monitorToolItem = new CheckMenuItem("Monitor Tool");
        monitorToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F4));
        CheckMenuItem missionToolItem = new CheckMenuItem("Mission Tool");
        missionToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F5));
        CheckMenuItem settlementMapToolItem = new CheckMenuItem("Settlement Map Tool");
        settlementMapToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F6));
        CheckMenuItem scienceToolItem = new CheckMenuItem("Science Tool");
        scienceToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F7));
        CheckMenuItem resupplyToolItem = new CheckMenuItem("Resupply Tool");
        resupplyToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F8));
        CheckMenuItem marsNetItem = new CheckMenuItem("Mars Net");
        marsNetItem.setAccelerator(new KeyCodeCombination(KeyCode.F9));
        CheckMenuItem webToolItem = new CheckMenuItem("Online Tool");
        webToolItem.setAccelerator(new KeyCodeCombination(KeyCode.F10));

        
        menuTools.getItems().addAll(marsNavigatorItem, searchToolItem,timeToolItem,
        		monitorToolItem, missionToolItem,settlementMapToolItem, 
        		scienceToolItem, resupplyToolItem, marsNetItem, webToolItem);
        
        
        // --- Menu Settings
        Menu menuSettings = new Menu("Settings");

        showFullScreenItem = new CheckMenuItem("Full Screen Mode");
        showFullScreenItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN));
		showFullScreenItem.setSelected(true);
		
        CheckMenuItem showUnitBarItem = new CheckMenuItem("Show Unit Bar");
        showUnitBarItem.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN));
        CheckMenuItem showToolBarItem = new CheckMenuItem("Show Tool Bar");
        showToolBarItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN));
        
        SeparatorMenuItem SeparatorMenuItem4 = new SeparatorMenuItem();
        
        MenuItem volumeUpItem = new MenuItem("Volume Up");
        volumeUpItem.setAccelerator(new KeyCodeCombination(KeyCode.UP, KeyCombination.CONTROL_DOWN));
        MenuItem volumeDownItem = new MenuItem("Volume Down");
        volumeDownItem.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.CONTROL_DOWN));
        CheckMenuItem muteItem = new CheckMenuItem("Mute");
        muteItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN));

        menuSettings.getItems().addAll(showFullScreenItem, showUnitBarItem,showToolBarItem, SeparatorMenuItem4, volumeUpItem, volumeDownItem,muteItem);
        
        // --- Menu Notification
        Menu menuNotification = new Menu("Notification");
        
        Menu newsPaneItem = new Menu("News Pane");
        
        CheckMenuItem slideFromTop = new CheckMenuItem("Slide from Top");
        slideFromTop.setSelected(true);
       
        CheckMenuItem showHideNewsPane = new CheckMenuItem("Toggle Show/Hide");
		//showNewsPaneItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
		showHideNewsPane.setSelected(false);
		
		newsPaneItem.getItems().addAll(slideFromTop, showHideNewsPane);
		
        Menu messageTypeItem = new Menu("Message Type");
        CheckMenuItem medicalItem = new CheckMenuItem("Medical");
        CheckMenuItem malfunctionItem = new CheckMenuItem("Malfunction");
        messageTypeItem.getItems().addAll(medicalItem, malfunctionItem);
        
        Menu displayTimeItem = new Menu("Display Time");
        ToggleGroup displayTimeToggleGroup = new ToggleGroup();
        RadioMenuItem confirmEachItem = new RadioMenuItem("Confirm each");
        confirmEachItem.setToggleGroup(displayTimeToggleGroup);
        RadioMenuItem threeSecondsItem = new RadioMenuItem("3 seconds");
        threeSecondsItem.setToggleGroup(displayTimeToggleGroup);
        RadioMenuItem twoSecondsItem = new RadioMenuItem("2 seconds");
        twoSecondsItem.setToggleGroup(displayTimeToggleGroup);
        RadioMenuItem oneSecondItem = new RadioMenuItem("1 second");
        oneSecondItem.setToggleGroup(displayTimeToggleGroup);
        displayTimeItem.getItems().addAll(confirmEachItem, threeSecondsItem, twoSecondsItem, oneSecondItem);
        
        Menu queueSizeItem = new Menu("Queue Size");
        ToggleGroup queueSizeToggleGroup = new ToggleGroup();
        RadioMenuItem unlimitedItem = new RadioMenuItem("Unlimited");
        unlimitedItem.setToggleGroup(queueSizeToggleGroup);
        RadioMenuItem threeItem = new RadioMenuItem("3");
        threeItem.setToggleGroup(queueSizeToggleGroup);
        RadioMenuItem oneItem = new RadioMenuItem("1");
        oneItem.setToggleGroup(queueSizeToggleGroup);
        queueSizeItem.getItems().addAll(unlimitedItem, threeItem, oneItem);

        menuNotification.getItems().addAll(newsPaneItem, messageTypeItem,displayTimeItem,queueSizeItem);
        
        // --- Menu Help
        Menu menuHelp = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        MenuItem tutorialItem = new MenuItem("Tutorial");
        tutorialItem.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.CONTROL_DOWN));
        SeparatorMenuItem SeparatorMenuItem5 = new SeparatorMenuItem();
        MenuItem userGuideItem = new MenuItem("User Guide");
        userGuideItem.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCombination.CONTROL_DOWN));

        menuHelp.getItems().addAll(aboutItem, tutorialItem,SeparatorMenuItem5, userGuideItem);

        super.getMenus().addAll(menuFile, menuTools, menuSettings, menuNotification, menuHelp);
		
  
        newItem.setOnAction(new EventHandler<ActionEvent>() {
     	   @Override 
     	   public void handle(ActionEvent e) {
     		   mainScene.newSimulation();
     	   }
     	});
        
        openItem.setOnAction(new EventHandler<ActionEvent>() {
     	   @Override 
     	   public void handle(ActionEvent e) {
     		   mainScene.loadSimulation(MainScene.OTHER);
     	   }
     	});
        
        openAutoSaveItem.setOnAction(new EventHandler<ActionEvent>() {
     	   @Override 
     	   public void handle(ActionEvent e) {
     		   mainScene.loadSimulation(MainScene.AUTOSAVE);
     	   }
     	});
        
        exitItem.setOnAction(new EventHandler<ActionEvent>() {
        	   @Override 
        	   public void handle(ActionEvent e) {
        		   //mainScene.exitSimulation();
        		   //mainScene.getStage().close();
        		   mainScene.alertOnExit();
        	   }
        	});
        
        saveItem.setOnAction(new EventHandler<ActionEvent>() {
     	   @Override 
     	   public void handle(ActionEvent e) {
     		   mainScene.saveSimulation(MainScene.DEFAULT);
     	   }
     	});
        
        saveAsItem.setOnAction(new EventHandler<ActionEvent>() {
      	   @Override 
      	   public void handle(ActionEvent e) {
      		   mainScene.saveSimulation(MainScene.SAVE_AS);
      	   }
      	});        
        
        marsNavigatorItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (marsNavigatorItem.isSelected()) desktop.openToolWindow(NavigatorWindow.NAME);
    			else desktop.closeToolWindow(NavigatorWindow.NAME);
    			//if (desktop == null) System.out.println("MainWindowFXMenu : marsNav. ");
            }
        });
        
        searchToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (searchToolItem.isSelected()) desktop.openToolWindow(SearchWindow.NAME);
    			else desktop.closeToolWindow(SearchWindow.NAME);
            }
        });
        
        timeToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (timeToolItem.isSelected()) desktop.openToolWindow(TimeWindow.NAME);
    			else desktop.closeToolWindow(TimeWindow.NAME);
            }
        });
        
        monitorToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (monitorToolItem.isSelected()) desktop.openToolWindow(MonitorWindow.NAME);
    			else desktop.closeToolWindow(MonitorWindow.NAME);
            }
        });
        
        missionToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (missionToolItem.isSelected()) desktop.openToolWindow(MissionWindow.NAME);
    			else desktop.closeToolWindow(MissionWindow.NAME);
            }
        });
        
        settlementMapToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (settlementMapToolItem.isSelected()) desktop.openToolWindow(SettlementWindow.NAME);
    			else desktop.closeToolWindow(SettlementWindow.NAME);
            }
        });
        
        scienceToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (scienceToolItem.isSelected()) desktop.openToolWindow(ScienceWindow.NAME);
    			else desktop.closeToolWindow(ScienceWindow.NAME);
            }
        });
        
        resupplyToolItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			if (resupplyToolItem.isSelected()) desktop.openToolWindow(ResupplyWindow.NAME);
    			else desktop.closeToolWindow(ResupplyWindow.NAME);
            }
        });
        
        marsNetItem.setOnAction(e ->  {
    			if (marsNetItem.isSelected())
    				mainScene.openMarsNet();	
    			else 
    				mainScene.closeMarsNet();
        });
        
        webToolItem.setOnAction(e -> {
    			if (webToolItem.isSelected())  {
    				webStage = startWebTool();
    				webStage.show();
    			}
    			else 
    				webStage.close() ;
        });
        
        showFullScreenItem.setOnAction(e -> {
            	boolean isFullScreen =  mainScene.getStage().isFullScreen();
            	if (!isFullScreen) {
	            	//mainScene.getStage().sizeToScene();
            		showFullScreenItem.setSelected(true);
	            	mainScene.getStage().setFullScreen(true);
            	}
            	else { 
            		showFullScreenItem.setSelected(false);
	            	mainScene.getStage().setFullScreen(false);
            	}
        });

		showHideNewsPane.setOnAction(e -> {
                if (!mainScene.getNotificationPane().isShowing()) {
                	mainScene.getNotificationPane().show(); // setNotificationPane(true);
                	showHideNewsPane.setSelected(false);
                } else {
                	mainScene.getNotificationPane().hide(); // setNotificationPane(false);
                	showHideNewsPane.setSelected(false);
                }
        });
 
		slideFromTop.setOnAction(e -> {
                if (!mainScene.getNotificationPane().isShowFromTop()) {
                	mainScene.getNotificationPane().setShowFromTop(true);
                	slideFromTop.setText("Slide from Top");
                	slideFromTop.setSelected(true);
                } else {
                	mainScene.getNotificationPane().setShowFromTop(false);
                	slideFromTop.setText("Slide from Bottom");
                	slideFromTop.setSelected(true);
                }
        });		      
	        
        volumeUpItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
            	float oldvolume = desktop.getSoundPlayer().getVolume();
    			desktop.getSoundPlayer().setVolume(oldvolume+0.1F);
            }
        });
		
        volumeDownItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
            	float oldvolume = desktop.getSoundPlayer().getVolume();
    			desktop.getSoundPlayer().setVolume(oldvolume-0.1F);
            }
        });
        
        muteItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			desktop.getSoundPlayer().setMute(muteItem.isSelected());
            }
        });
		
        aboutItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			desktop.openToolWindow(GuideWindow.NAME);
    			GuideWindow ourGuide;
    			ourGuide = (GuideWindow)desktop.getToolWindow(GuideWindow.NAME);
    			ourGuide.setURL(Msg.getString("doc.about")); //$NON-NLS-1$
    		}
        });
 
        tutorialItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			desktop.openToolWindow(GuideWindow.NAME);
    			GuideWindow ourGuide;
    			ourGuide = (GuideWindow)desktop.getToolWindow(GuideWindow.NAME);
    			ourGuide.setURL(Msg.getString("doc.tutorial")); //$NON-NLS-1$
    		}
        });

        userGuideItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
    			desktop.openToolWindow(GuideWindow.NAME);
    			GuideWindow ourGuide;
    			ourGuide = (GuideWindow)desktop.getToolWindow(GuideWindow.NAME);
    			ourGuide.setURL(Msg.getString("doc.guide")); //$NON-NLS-1$
    		}
        });		
	}
	
	public Stage startWebTool() {
		
	    Stage webStage = new Stage();
	    
	    final WebView browser = new WebView();
	    final WebEngine webEngine = browser.getEngine();

	    ScrollPane scrollPane = new ScrollPane();
	    scrollPane.setFitToWidth(true);
	    scrollPane.setContent(browser);
	    
	    webEngine.getLoadWorker().stateProperty()
	        .addListener(new ChangeListener<State>() {
	          @Override
	          public void changed(@SuppressWarnings("rawtypes") ObservableValue ov, State oldState, State newState) {
	            if (newState == Worker.State.SUCCEEDED) {
	            	webStage.setTitle(webEngine.getLocation());
	            }
	          }
	        });
	    
	    webEngine.load("http://mars-sim.sourceforge.net/#development");
	    
	    
	    Scene webScene = new Scene(scrollPane);
	    webStage.setScene(webScene);
	    
	    return webStage;
	}
	
	// Toggle the full screen mode off
	public void exitFullScreen() {
		showFullScreenItem.setSelected(false);
	}

	
}