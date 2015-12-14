package view_and_controller;

import javax.swing.*;
import javax.swing.border.*;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.ui.RectangleInsets;

import model.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

@SuppressWarnings("serial")
public class MonitorPanel extends JPanel implements MouseListener {
	/** Contains the tabs used for switching between the viewed electrical data type statistics */
	private JPanel leftTabPanel;
	
	/** Dictionary of the main panels */
	private Map<ElectricalDataTypes,JPanel> graphAndStatsPanels;
	
	/** Dictionary of the individual tabs for each electrical data type */
	private Map<ElectricalDataTypes,TabPanel> singleTabPanels;
	
	/** Indicates the current main panel being displayed */
	private ElectricalDataTypes currPanelType;
	
	/** The color of the tab that is selected (Gray) */
	private static final Color SELECTED_TAB_COLOR = new Color(Color.GRAY.getRed(),
			Color.GRAY.getGreen(),Color.GRAY.getBlue(), 1);
	
	/** The color of the tab that the mouse moves over (Darker Gray) */
	private static final Color HIGHLIGHT_COLOR = new Color(Color.GRAY.getRed(),
			Color.GRAY.getGreen(),Color.GRAY.getBlue(), 100);;
	
	/** The shared chart cursors */
	ArrayList<MyValueMarker> myMarkers;
	IntervalMarker intervalMarker;
	
	public MonitorPanel(Map<ElectricalDataTypes,ElectricalDataModel> dataModels, 
			ObservableModelDetails observableModelDetails, JMenuBar menuBar) {
		// Set the layout and the background color
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);

		// Initialise the left tab panel
		leftTabPanel = initialiseLeftTabPanel(dataModels);
		leftTabPanel.setPreferredSize(new Dimension(300,500));
		add(leftTabPanel, BorderLayout.WEST);
		
		// Initialise the shared chart cursors
		initialiseChartCursors(dataModels, Color.GRAY);
		
		// Initialise power, voltage, current, resistance and energy main panels
		graphAndStatsPanels = new HashMap<ElectricalDataTypes, JPanel>();
		graphAndStatsPanels.put(ElectricalDataTypes.POWER, 
				initMainElectricalDataTypePanels(dataModels.get(
						ElectricalDataTypes.POWER), observableModelDetails, menuBar));
		graphAndStatsPanels.put(ElectricalDataTypes.VOLTAGE, 
				initMainElectricalDataTypePanels(dataModels.get(
						ElectricalDataTypes.VOLTAGE), observableModelDetails, menuBar));
		graphAndStatsPanels.put(ElectricalDataTypes.CURRENT, 
				initMainElectricalDataTypePanels(dataModels.get(
						ElectricalDataTypes.CURRENT), observableModelDetails, menuBar));
		graphAndStatsPanels.put(ElectricalDataTypes.RESISTANCE, 
				initMainElectricalDataTypePanels(dataModels.get(
						ElectricalDataTypes.RESISTANCE), observableModelDetails, menuBar));
		graphAndStatsPanels.put(ElectricalDataTypes.ENERGY,
				initMainElectricalDataTypePanels(dataModels.get(
						ElectricalDataTypes.ENERGY), observableModelDetails, menuBar));
		
		// Initially display the power panel
		currPanelType = ElectricalDataTypes.POWER;
		singleTabPanels.get(currPanelType).setTabBackground(SELECTED_TAB_COLOR);
		add(graphAndStatsPanels.get(currPanelType), BorderLayout.CENTER);
	}
	
	/**
	 * Initialises the chart cursor settings
	 * @param plotColor the color of the plot 
	 */
	private void initialiseChartCursors(Map<ElectricalDataTypes, ElectricalDataModel> dataModels, Color plotColor) {
		// Set up the movable cursors
		myMarkers = new ArrayList<MyValueMarker>();
		myMarkers.add(new MyValueMarker( new ValueMarker(-1, Color.BLUE, new BasicStroke(4))) );
		myMarkers.add(new MyValueMarker( new ValueMarker(-1, Color.RED, new BasicStroke(4))) );
		
		// Set the cursor labels
		for (int i=0; i < myMarkers.size(); i++) {
			Marker marker = myMarkers.get(i).getValueMarker();
			marker.setLabel("Cursor " + i);
			marker.setLabelOffset(new RectangleInsets(10,0,0,0));
			marker.setLabelFont(new Font("Calibri", Font.PLAIN, 16));
		}
		
		// Register the data models with the cursors
		for(ElectricalDataModel dataModel : dataModels.values()) {
			myMarkers.get(0).getValueMarker().addChangeListener(dataModel);
			myMarkers.get(1).getValueMarker().addChangeListener(dataModel);
			
			dataModel.setMyMarkers(myMarkers);
		}
		
		// Set up the interval marker used for highlighting cursor area
		intervalMarker = new IntervalMarker(0, 0);
		intervalMarker.setPaint(new Color(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), 50));
	}
	
	/**
	 * Method constructs the side panel used for switching between key statistic views.
	 * 
	 * @param dataModels dictionary containing the recorded data for the various
	 * electrical data types.
	 * @return the side panel used for switching between electrical data type key statistic views
	 */
	private JPanel initialiseLeftTabPanel(Map<ElectricalDataTypes,ElectricalDataModel> dataModels) {
		// Contains 5 tab panels, stacked on top of each other
		JPanel leftTabPanel = new JPanel(new GridLayout(5,1));
		leftTabPanel.setBackground(Color.WHITE);
		
		// Initialise a border with 2px spacing left and right
		CompoundBorder border;
		border = new CompoundBorder(new EmptyBorder(0,2,0,0), 
				BorderFactory.createMatteBorder(0,0,0,1, Color.BLACK));
		border = new CompoundBorder(border, new EmptyBorder(0,0,0,2));
		
		leftTabPanel.setBorder(border);
		
		// Dictionary of the tabs contained within the leftTabPanel
		singleTabPanels = new HashMap<ElectricalDataTypes,TabPanel>();
		
		// monitor panel listens for mouse clicks on the tabs and updates the main panel appropriately
		singleTabPanels.put(ElectricalDataTypes.POWER,
				new TabPanel(this, dataModels.get(ElectricalDataTypes.POWER),ElectricalDataTypes.POWER));
		singleTabPanels.get(ElectricalDataTypes.POWER).addMouseListener(this);
		
		singleTabPanels.put(ElectricalDataTypes.CURRENT,
				new TabPanel(this, dataModels.get(ElectricalDataTypes.CURRENT),ElectricalDataTypes.CURRENT));
		singleTabPanels.get(ElectricalDataTypes.CURRENT).addMouseListener(this);
		
		singleTabPanels.put(ElectricalDataTypes.VOLTAGE,
				new TabPanel(this, dataModels.get(ElectricalDataTypes.VOLTAGE),ElectricalDataTypes.VOLTAGE));
		singleTabPanels.get(ElectricalDataTypes.VOLTAGE).addMouseListener(this);
		
		singleTabPanels.put(ElectricalDataTypes.RESISTANCE, 
				new TabPanel(this, dataModels.get(ElectricalDataTypes.RESISTANCE), ElectricalDataTypes.RESISTANCE));
		singleTabPanels.get(ElectricalDataTypes.RESISTANCE).addMouseListener(this);
		
		singleTabPanels.put(ElectricalDataTypes.ENERGY, 
				new TabPanel(this, dataModels.get(ElectricalDataTypes.ENERGY), ElectricalDataTypes.ENERGY));
		singleTabPanels.get(ElectricalDataTypes.ENERGY).addMouseListener(this);
		
		// Add all the tabs to the left tab panel
		leftTabPanel.add(singleTabPanels.get(ElectricalDataTypes.POWER));
		leftTabPanel.add(singleTabPanels.get(ElectricalDataTypes.VOLTAGE));
		leftTabPanel.add(singleTabPanels.get(ElectricalDataTypes.CURRENT));
		leftTabPanel.add(singleTabPanels.get(ElectricalDataTypes.RESISTANCE));
		leftTabPanel.add(singleTabPanels.get(ElectricalDataTypes.ENERGY));
		
		// Initially color all the tabs white
		for(TabPanel tabPanel : singleTabPanels.values())
			tabPanel.setTabBackground(Color.WHITE);
		
		// Ensure the left tab panels horizontal space is consistent
		leftTabPanel.setPreferredSize(new Dimension(300, Integer.MAX_VALUE));
		return leftTabPanel;
	}
	
	/**
	 * 
	 * @param dataModel the data store for an electrical data type
	 * @param menuBar the main menu bar for the in-line power GUI
	 * @return the main panel for an electrical data type, determined by the dataModel passed in
	 * as an argument.
	 */
	private JPanel initMainElectricalDataTypePanels(ElectricalDataModel dataModel, 
			ObservableModelDetails observableModelDetails, JMenuBar menuBar) {
		JPanel mainElecDataTypePanel = new JPanel();
		
		// half of the panel for the main chart, half of the panel for the statistics (including histogram)
		mainElecDataTypePanel.setLayout(new GridLayout(2,1));
		mainElecDataTypePanel.setBackground(Color.WHITE);
		mainElecDataTypePanel.setBorder(new EmptyBorder(0, 2, 0, 2));
		
		// main chart panel
		MonitorChartPanel chartPanel = new MonitorChartPanel(dataModel, menuBar, myMarkers, intervalMarker);
		mainElecDataTypePanel.add((JPanel) chartPanel);		
		
		// statistics panel
		JPanel statsPanel = new MonitorStatsPanel(dataModel, observableModelDetails, chartPanel, menuBar);
		mainElecDataTypePanel.add(statsPanel);
		
		return mainElecDataTypePanel;
	}
	
	@Override
	/**
	 * Called when a single tab panel is clicked
	 * (MonitorPanel is only listening for mouse events on the individual tab panels).
	 */
	public void mouseClicked(MouseEvent event) {
		TabPanel selectedTabPanel = null;
		
		// If the chart is clicked within the TabPanel a ChartPanel click event is fired... check for both
		if (event.getSource() instanceof TabPanel)
			selectedTabPanel = (TabPanel) event.getSource();
		else if (event.getSource() instanceof ChartPanel) 
			selectedTabPanel = (TabPanel) ((ChartPanel)event.getSource()).getParent();
		
		// If the user clicked the already selected tab panel do nothing
		if(currPanelType == selectedTabPanel.getElecDataType())
			return;
		
		// Change the de-selected tab panels background to white
		singleTabPanels.get(currPanelType).setTabBackground(Color.WHITE);
		remove(graphAndStatsPanels.get(currPanelType));
		
		// Change the selected tab panel color and change the main display to match
		currPanelType = selectedTabPanel.getElecDataType();
		selectedTabPanel.setTabBackground(SELECTED_TAB_COLOR);
		add(graphAndStatsPanels.get(currPanelType), BorderLayout.CENTER);
		
		// repaint the entire monitor panel window
		revalidate();
		repaint();
	}
	
	@Override
	/**
	 * Used to highlight the tab which has the mouse over it.
	 */
	public void mouseEntered(MouseEvent e) {
		TabPanel hoveredTabPanel = null;
		
		// Tab or mini-chart panel is clicked (mini-chart panel is within a tab)
		if(e.getSource() instanceof TabPanel)
			hoveredTabPanel = (TabPanel) e.getSource();
		else if (e.getSource() instanceof ChartPanel) 
			hoveredTabPanel = (TabPanel) ((ChartPanel)e.getSource()).getParent();
		
		hoveredTabPanel.setTabBackground(HIGHLIGHT_COLOR);
		leftTabPanel.repaint();
	}
	
	@Override
	/**
	 * Used to remove hover highlight when the mouse exits the tab area.
	 */
	public void mouseExited(MouseEvent e) {
		TabPanel exitedTabPanel = null;
		if(e.getSource() instanceof TabPanel)
			exitedTabPanel = (TabPanel) e.getSource();
		else if (e.getSource() instanceof ChartPanel) 
			exitedTabPanel = (TabPanel) ((ChartPanel)e.getSource()).getParent();
			
		
		if(currPanelType == exitedTabPanel.getElecDataType())
			exitedTabPanel.setTabBackground(SELECTED_TAB_COLOR);
		else
			exitedTabPanel.setTabBackground(Color.WHITE);
				
		leftTabPanel.repaint();
	} 
	
	/* Unused mouse listeners */
	
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}


}
