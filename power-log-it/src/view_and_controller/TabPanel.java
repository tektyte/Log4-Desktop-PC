package view_and_controller;


import model.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;
import javax.swing.border.LineBorder;

import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.*;

@SuppressWarnings("serial")
public class TabPanel extends JPanel implements SeriesChangeListener, Observer, MouseListener {
	/** The standard fonts used within this panel */
	private static final String STD_FONT_STR = "Planer";
	
	/** The chart data associated with this Tab */
	private ElectricalDataModel dataModel;
	
	/** The chart panel displayed within this Tab */
	private ChartPanel miniChartPanel;
	
	/** Contains the tab title and the data concerning the tabs associated electrical data type */
	private JPanel infoPanel;
	
	/** Displays the Mean, Min and Max value concerning the electrical data type */
	private JPanel statisticsPanel;
	
	/** The Electrical Data Type the tab panel represents */
	private ElectricalDataTypes elecDataType;
	
	/** The color of the Tab */
	private Color backColor;
	
	/** Font size of the data statistics */
	private int fontSize;

	
	/**
	 * 
	 * @param monitorPanel register as a mouse click listener on the chart panel
	 * @param dataModel used for the chart within the tab and the tab statistics
	 * @param elecDataType the electrical type of the tab
	 */
	public TabPanel(MonitorPanel monitorPanel, ElectricalDataModel dataModel, ElectricalDataTypes elecDataType) {
		fontSize = 14;
		backColor = Color.WHITE;
		
		// Store the dataModel and the Electrical Data Type of the model
		this.dataModel = dataModel;
		this.elecDataType = elecDataType;
		
		// The title of the tab e.g. Power, Current
		String tabTitle = dataModel.getElectricalDataTitle();
		
		// Initialise the chat content
		miniChartPanel = createMiniGraphPanel(monitorPanel, dataModel);
		JPanel infoPanel = createInfoPanel(tabTitle);
		
		// Chart panel to the left of the title and statistics
		setLayout(new GridLayout(1,2));
		add(miniChartPanel);
		add(infoPanel);
		
		// Panel listens for change in power series data
		dataModel.addDataCollectionListener(this);
		
		addMouseListener(this);
	}
	
	/**
	 * Initialise the text part of the tab
	 * @param title of the tab
	 * @return panel containing the text
	 */
	private JPanel createInfoPanel(String title) {
		JLabel titleLbl = new JLabel(title);
		
		// stores the title on top of the data
		infoPanel = new JPanel(new GridLayout(2,1));
		
		// background color is initially white
		infoPanel.setBackground(backColor);
		
		// title
		titleLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 28));
		titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
		
		infoPanel.add(titleLbl);
		
		// Initialise the data panel with the three statistics 
		updateTabInfo();
		
		return infoPanel;
	}
	
	/**
	 * Creates a new tab with the updated statistical information,
	 * old tab is then swapped out for the freshly created tab.
	 */
	private void updateTabInfo() {
		JPanel statPanel;
		
		JPanel newStatisticsPanel = new JPanel(new GridLayout(3,1));
		
		// Add the mean, opaqueness ensures every pixel in the label is painted
		statPanel = createStatisticPanel("Mean");
		newStatisticsPanel.add(statPanel);
		
		// Add the min
		statPanel = createStatisticPanel("Min");
		newStatisticsPanel.add(statPanel);

		// Add the max
		statPanel = createStatisticPanel("Max");
		newStatisticsPanel.add(statPanel);
		
		if(statisticsPanel != null)
			infoPanel.remove(statisticsPanel);
		
		infoPanel.add(newStatisticsPanel);
		statisticsPanel = newStatisticsPanel;
		
		infoPanel.revalidate();
	}
	
	private JPanel createStatisticPanel(String title) {
		JPanel statPanel = new JPanel(new GridLayout(1,3));
		Font font = new Font(STD_FONT_STR, Font.PLAIN, fontSize);
		
		JLabel statTitle;
		if(title.length() < 4)
			statTitle = new JLabel(title + " ");
		else
			statTitle = new JLabel(title);
		
		JLabel statValu  = new JLabel(dataModel.getFormattedStatistics().get(title).getDataStr());
		JLabel statUnit  = new JLabel(dataModel.getFormattedStatistics().get(title).getUnitStr() +"  ");
		statUnit.setHorizontalAlignment(SwingConstants.RIGHT);
		
		
		
		statTitle.setFont(font);
		statValu.setFont(font);
		statUnit.setFont(font);
		
		statTitle.setOpaque(false);
		statValu.setOpaque(false);
		statUnit.setOpaque(false);
		
		statPanel.setBackground(backColor);
		statPanel.setOpaque(true);
		
		statPanel.add(statTitle);
		statPanel.add(statValu);
		statPanel.add(statUnit);
		
		return statPanel;
	}
	
	/**
	 * 
	 * @param monitorPanel registered as a listener for clicks on the tab chart
	 * @param dataModel contains the chart panel for this tab
	 * @return the initialised mini-chart panel
	 */
	private ChartPanel createMiniGraphPanel(MonitorPanel monitorPanel, ElectricalDataModel dataModel) {
		ChartPanel chartPanel;
		Color lineColor = null;
		
		// generate the graph
		JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
		"", 						// Title
		"", 						// x-axis Label
		"", 						// y-axis Label
		dataModel.getDataCollection(),	// data
		false, 						// show legend?
		false, 						// use tooltips?
		false );					// generate URLs?
		
		XYPlot plot = lineChart.getXYPlot();
		//XYItemRenderer renderer = plot.getRenderer();
		MyXYAreaRenderer renderer = new MyXYAreaRenderer();
		plot.setRenderer(renderer);
		
		// initialise the plot settings
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.GRAY);
		plot.setRangeGridlinePaint(Color.GRAY);
		
		// hide the x and y axis
		plot.getDomainAxis().setVisible(false);
		plot.getRangeAxis().setVisible(false);
		plot.getDomainAxis().setLowerMargin(0.0);
		plot.getDomainAxis().setUpperMargin(0.0);
		((NumberAxis)plot.getRangeAxis()).setAutoRangeIncludesZero(true);
		
		// increase the thickness of the graph line
		lineColor = dataModel.getElectricalColour();
			
        renderer.setSeriesOutlinePaint(0, lineColor);
        renderer.setBaseOutlineStroke(new BasicStroke(1.0f));
		renderer.setSeriesFillPaint(0, 
        		new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 50));
        renderer.setOutline(true);
		
		// add the graph to the panel
        chartPanel = new ChartPanel(lineChart);
        chartPanel.addMouseListener(monitorPanel);
        
        // disable the chart options and zoom for side tab charts
        chartPanel.setPopupMenu(null);
		chartPanel.setDomainZoomable(false);
		chartPanel.setRangeZoomable(false);
        
        return chartPanel;
	}
	
	/* Get and Set */
	
	/**
	 * Used by the monitor panel to determine which electrical data type is currently
	 * being displayed in the main panel.
	 * @return
	 */
	public ElectricalDataTypes getElecDataType() {
		return elecDataType;
	}
	
	/**
	 * Used by the MonitorPanel to set the tab panel background color based on whether it is selected,
	 * the mouse is hovering over the tab, or it is NOT selected.
	 * @param color
	 */
	public void setTabBackground(Color color) {
		backColor = color;
		
		// background color of the chart
		miniChartPanel.getChart().setBackgroundPaint(color);
		
		infoPanel.setBackground(color);
		for(Component comp : statisticsPanel.getComponents()) {
			comp.setBackground(color);
		}
		
		if(color.getAlpha()+50 <= 255)
			setBorder(new LineBorder(
				new Color(color.getRed(),color.getGreen(), color.getGreen(), 
						(int) (color.getAlpha()+100)),2));
		else
			setBorder(new LineBorder(color,2));
	}
	
	
	/* Listeners */
	

	@Override
	public void update(Observable o, Object arg) {
		if(o instanceof ObTimeSeriesCollection) {
			// A channel has been added or removed from the model
			// Set the default statistics listeners to the first channel
			dataModel.addSeriesChangeListener(this, 0);
		}
	}
	
	
	@Override
	public void seriesChanged(SeriesChangeEvent arg0) {
		updateTabInfo();
	}

	/* Unused mouse listener methods*/
	@Override
	public void mouseClicked(MouseEvent arg0) {}
	
	@Override
	public void mouseEntered(MouseEvent e) {}
	
	@Override
	public void mouseExited(MouseEvent arg0) {}
	
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}
}
