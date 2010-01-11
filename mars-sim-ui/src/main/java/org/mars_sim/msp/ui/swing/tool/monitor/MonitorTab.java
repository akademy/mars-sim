/**
 * Mars Simulation Project
 * MonitorTab.java
 * @version 2.75 2005-08-03
 * @author Barry Evans
 */

package org.mars_sim.msp.ui.swing.tool.monitor;

import java.util.List;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.ui.swing.*;

/**
 * This class represents an absraction of a view displayed in the Monitor Window.
 * The view is displayed inside a tab panel within the window and depends on
 * a UnitTableModel
 */
abstract class MonitorTab extends JPanel {
    
    private MonitorModel model;    // Mode providing the data
    private Icon icon;
    private boolean mandatory;

    /**
     * Create a view within a tab displaying the specified model.
     * @param model The model of entities to display.
     * @param mandatory This view is a mandatory view can can not be removed.
     * @param icon Iconic representation.
     */
    public MonitorTab(MonitorModel model, boolean mandatory, Icon icon) {
        this.model = model;
        this.icon = icon;
        this.mandatory = mandatory;

        // Create a panel
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
    }

    /**
     * Remove this view.
     */
    public void removeTab() {
    	model.destroy();
        model = null;
    }

    /**
     * Display details for selected rows
     */
    public void displayDetails(MainDesktopPane desktop) {
        List rows = getSelection();
        Iterator it = rows.iterator();
        while(it.hasNext()) {
            Object selected = it.next();
            if (selected instanceof Unit) desktop.openUnitWindow((Unit) selected, false);
        }
    }

    /**
     * Center the map on the first selected row.
     * @param desktop Main window of application.
     */
    public void centerMap(MainDesktopPane desktop) {
        List rows = getSelection();
        Iterator it = rows.iterator();
        if (it.hasNext()) {
            Unit unit = (Unit) it.next();
            desktop.centerMapGlobe(unit.getCoordinates());
        }
    }

    /**
     * Display property window controlling this view.
     */
    abstract public void displayProps(MainDesktopPane desktop);

    /**
     * This return the selected objects that are current
     * selected in this tab.
     *
     * @return List of objects selected in this tab.
     */
    abstract protected List getSelection();
    
    /**
     * Gets the tab count string.
     */
    public String getCountString() {
    	return model.getCountString();
    }

    /**
     * Get the icon associated with this view.
     * @return Icon for this view
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * Get the associated model.
     * @return Monitored model associated to the tab.
     */
    public MonitorModel getModel() {
        return model;
    }

    /**
     * Get the mandatory state of this view
     * @return Mandatory view.
     */
    public boolean getMandatory() {
        return mandatory;
    }
}