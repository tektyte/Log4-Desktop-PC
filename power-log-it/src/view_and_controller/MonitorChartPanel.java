package view_and_controller;


import model.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.Layer;
import org.jfree.data.Range;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesChangeListener;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;

@SuppressWarnings("serial")
public class MonitorChartPanel extends JPanel implements MouseListener, MouseMotionListener,
															Observer, ActionListener, SeriesChangeListener {
	/** The standard fonts used within this panel */
	private static final String STD_FONT_STR = "Planer";
	
	/** Fills the entire MonitorChartPanel */
	private ChartPanel chartPanel;
	
	/** The plot associated with chartPanel */
	private XYPlot plot;
	
	/** Contains the plot cursors to be displayed on the screen */
	private ArrayList<MyValueMarker> myMarkers;
	
	/** Used to mark the area between cursors, or before or after a single cursor */
	private IntervalMarker intervalMarker;
    
	/** Scaling factor used for determining a cursor click */
    private static final double FRAC_OF_DOMAIN = 0.0129;
    
    /** */
    private ElectricalDataModel dataModel;
    
    
    
	public MonitorChartPanel(ElectricalDataModel dataModel,
			JMenuBar menuBar, ArrayList<MyValueMarker> myMarkers, IntervalMarker intervalMarker) {
		// Initialise the main chart panel
		//setLayout(new GridLayout(1,1));
		setLayout(new BorderLayout());
		chartPanel = initialiseChartPanel(dataModel);
		add(chartPanel);
		
		// Initialise the main chart cursors
		this.myMarkers = myMarkers;
		this.intervalMarker = intervalMarker;
		
		// Register this component as an ActionListener with the cursor activation menu buttons
		JMenu viewMenu = menuBar.getMenu(menuBar.getMenuCount()-2);
		JMenu cursorMenu = (JMenu) viewMenu.getSubElements()[0].getSubElements()[0];
		MenuElement[] cursorEnableButtons = cursorMenu.getSubElements()[0].getSubElements();
		
		for (int i=0; i < cursorEnableButtons.length; i++) {
			JRadioButtonMenuItem currItem = (JRadioButtonMenuItem) cursorEnableButtons[i];
			currItem.addActionListener(this);
		} 
		
		// Panel listens to changes in series data to adjust interval marker when appropriate
		this.dataModel = dataModel;
		dataModel.addDataCollectionListener(this);
	}
	
	/**
	 * 
	 * @param dataModel the data for an electrical data type
	 * @return chart panel for the given electrical data type data
	 */
	private ChartPanel initialiseChartPanel(ElectricalDataModel dataModel) {
		// Initialise the main chart title and axes titles to correspond to dataModel
		String chartTitle = dataModel.getElectricalDataTitle() + " Monitor";
		String chartAxisLabelY = dataModel.getElectricalDataTitle();
		
		// Initialise plot color based on the electrical data type
		Color plotColor = dataModel.getElectricalColour();
		
		// Generate the graph
		JFreeChart chart = ChartFactory.createXYAreaChart(
		chartTitle, 					// Title
		"", 							// x-axis Label
		dataModel.getElectricalDataTitle() + " (" + dataModel.getBaseDataUnitString() + ")", // y-axis Label
		dataModel.getDataCollection(),	// data
		PlotOrientation.VERTICAL,
		false, 						// show Legend?
		true, 						// use tooltips?
		false );					// generate URLs?
		
		// change the chart title font
		chart.getTitle().setFont(new Font(STD_FONT_STR, Font.BOLD, 30));
		
		// Change the domain axis to a dateaxis
		plot = (XYPlot) chart.getXYPlot();
		plot.setDomainAxis(new DateAxis(""));
		
		// Initialise the plot settings
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.GRAY);
		plot.setRangeGridlinePaint(Color.GRAY);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.getDomainAxis().setLowerMargin(0.001);
		plot.getDomainAxis().setUpperMargin(0.0);		
		((NumberAxis)plot.getRangeAxis()).setAutoRangeIncludesZero(true);
		((NumberAxis)plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0.00E0"));
		
		// Set the plot fonts
		Font axesFont = new Font(STD_FONT_STR, Font.PLAIN, 20);
		plot.getDomainAxis().setLabelFont(axesFont);
		plot.getRangeAxis().setLabelFont(axesFont);
		plot.getDomainAxis().setTickLabelFont(axesFont);
		plot.getRangeAxis().setTickLabelFont(axesFont);
		
		// Set the 2nd plot renderer for rendering darker line across the area
		XYAreaRenderer renderer = new XYAreaRenderer();
		renderer.setSeriesVisible(0, true);
		renderer.setSeriesVisible(1, false);
		renderer.setSeriesPaint(0,
        		new Color(plotColor.getRed(), plotColor.getGreen(), plotColor.getBlue(), 50));
		plot.setRenderer(0, renderer);
		
        plot.setDataset(1, dataModel.getLineDataCollection());
        XYLineAndShapeRenderer lineAndShapeRenderer = new XYLineAndShapeRenderer(true, false);
        lineAndShapeRenderer.setSeriesPaint(0, plotColor);
        lineAndShapeRenderer.setSeriesStroke(0, new BasicStroke(1f));
        plot.setRenderer(1, lineAndShapeRenderer);
        //((NumberAxis)plot.getRangeAxis()).centerRange(0.0);
		
		// Add the graph to the panel
		chartPanel = new ChartPanel(chart);
		chartPanel.createToolTip();
		chartPanel.setMouseWheelEnabled(true);
		chartPanel.addMouseListener(this);
		chartPanel.addMouseMotionListener(this);
		
		// Disable mouse wheel zooming 
		chartPanel.setMouseWheelEnabled(false);
		
		//set min and max draw dimensions for resizing
		chartPanel.setMinimumDrawHeight(0);
		chartPanel.setMinimumDrawWidth(0);
		chartPanel.setMaximumDrawHeight(30000);
		chartPanel.setMaximumDrawWidth(30000);
		
		removeUnwantedChartProperties();
		
		return chartPanel;
	}
	
	/**
	 * Removes the unwanted right click options from the main chart panel.
	 */
	private void removeUnwantedChartProperties() {
		// remove the 'properties' option from the chart right-click
		chartPanel.getPopupMenu().remove(0);
		chartPanel.getPopupMenu().remove(0);
		
		// remove the 'print' option from the chart right-click
		chartPanel.getPopupMenu().remove(2);
		chartPanel.getPopupMenu().remove(2);
		
		// remove the option to save as a pdf
		((JMenu) chartPanel.getPopupMenu().getComponent(1)).remove(2);
	}
	
	/* Mouse listeners */
	
	@Override
	/**
	 *  Changes the mouse cursor image if a marker is moused over
	 */
	public void mouseMoved(MouseEvent event) {
		if (checkMouseOnMarker(getCursorXPosition(event.getPoint()),
				getCursorYPosition(event.getPoint())) >= 0) {
			// mouse is on a cursor
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		} else {
			// mouse is NOT on a cursor
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	} 
	
	@Override
	/**
	 * If the mouse is pressed on a cursor, set the cursor dragging true and
	 * disable mouse zooming.
	 */
	public void mousePressed(MouseEvent event) {
		int markerIndex = checkMouseOnMarker(getCursorXPosition(event.getPoint()),
				getCursorYPosition(event.getPoint()));
		
		if(markerIndex >= 0) {
			chartPanel.setMouseZoomable(false);
			myMarkers.get(markerIndex).setDragging(true);
		}
	}

	@Override
	/**
	 * On a mouse release ensure all the markers have dragging set to false
	 * and allow the chart to be mouse zoomable again.
	 */
	public void mouseReleased(MouseEvent e) {
		for(int i=0; i < myMarkers.size(); i++)
			myMarkers.get(i).setDragging(false);
		chartPanel.setMouseZoomable(true);
	}
	
	@Override
	/**
	 * Used for updating a plot cursor value if it is being dragged.
	 */
	public void mouseDragged(MouseEvent event) {
		// If there are no cursors on the plot do nothing
		if(plot.getDomainMarkers(Layer.FOREGROUND) == null)
			return;
				
		double mouseChartDomainValu = getCursorXPosition(event.getPoint());
		
		// upper bound and lower bound on data series domain values
		double lowerBound = dataModel.getSeriesXLowerBound();
		double upperBound = dataModel.getSeriesXUpperBound();
		
		// displayed domain axis range
		Range domainAxisRange = plot.getDomainAxis().getRange();
		
		// Set upper bound to be minimum of the dataset maximum bound and the displayed domain maximum bound 
		if (domainAxisRange.getUpperBound() < upperBound){
			upperBound = domainAxisRange.getUpperBound();
		}
		
		// Set lower bound to be maximum of the dataset lower bound and the displayed domain lower bound 
		if (domainAxisRange.getLowerBound() > lowerBound) {
			lowerBound = domainAxisRange.getLowerBound();
		}
		
		
		// Adjust the mouse value to max or min of domain if it is off the chart
		if(mouseChartDomainValu > upperBound)
			mouseChartDomainValu = upperBound;
		else if (mouseChartDomainValu < lowerBound)
			mouseChartDomainValu = lowerBound;
		
		// Go through the list of plot cursors and adjust the value of the cursor being dragged
		for(int i=0; i<myMarkers.size(); i++) {
			if (myMarkers.get(i).isDragging()) {
				myMarkers.get(i).getValueMarker().setValue(mouseChartDomainValu);
				myMarkers.get(i).setOffset(mouseChartDomainValu-dataModel.getSeriesXLowerBound());
				addOrRemoveIntervalMarker();
				return;
			}
		}
		
	}
	
	/**
	 * 
	 * @param domainPressPos the position of a mouse press in terms of the chart domain
	 * @param rangePressPos  the position of a mouse press in terms of the chart range
	 * @return -1 if the mouse is not on a marker, else the index of the marker the mouse is on
	 */
	private int checkMouseOnMarker(double domainPressPos, double rangePressPos) {
		ArrayList<ValueMarker> plotMarkers;
		
		// No cursors are on the plot, therefore mouse is not on a marker
		if(plot.getDomainMarkers(Layer.FOREGROUND) == null)
			return -1;
		
		// get the cursors currently on the plot
		plotMarkers = new ArrayList<ValueMarker>(plot.getDomainMarkers(Layer.FOREGROUND));
		
		// get the axes ranges (in terms of what is displayed)
		Range domainAxisRange = plot.getDomainAxis().getRange();
		Range rangeAxisRange = plot.getRangeAxis().getRange();
	
		// the user error in a cursor click, in terms of the domain range
		double eps = domainAxisRange.getLength() * FRAC_OF_DOMAIN;
		
		for(int i=0; i<myMarkers.size(); i++) {
			ValueMarker currMarker = myMarkers.get(i).getValueMarker();
			if(plotMarkers.contains(currMarker) 
					&& domainPressPos > currMarker.getValue() - eps/2 
					&& domainPressPos < currMarker.getValue() + eps/2 
					&& rangePressPos > rangeAxisRange.getLowerBound() 
					&& rangePressPos < rangeAxisRange.getUpperBound()) {
				return i;
			}
		}
		
		return -1;
	} 
	
	/**
	 * 
	 * @param p point corresponding to the mouse press, in terms of panel press
	 * @return the domain axis value corresponding to where the user pressed
	 */
	private double getCursorXPosition(Point p) {
		Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
	}
	
	/**
	 * 
	 * @param p point corresponding to the mouse press, in terms of panel press
	 * @return the range axis value corresponding to where the user pressed
	 */
	private double getCursorYPosition(Point p){
		Rectangle2D plotArea = chartPanel.getScreenDataArea();
		return plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());
	}
	
	/* Action listeners */
	
	@Override
	/**
	 * Used for adding and removing cursors corresponding to view menu selections.
	 */
	public void actionPerformed(ActionEvent event) {
		// Check if the event source is a JRadioButtonMenuItem (i.e. a cursor selection)
		if (event.getSource() instanceof JRadioButtonMenuItem) {
			JRadioButtonMenuItem button = (JRadioButtonMenuItem) event.getSource();
			
			// Add the cursors (markers) to the plot
			if(event.getActionCommand().equals("Cursor 0") && button.isSelected()) {
				double offset =addMarkerToPlot(myMarkers.get(0).getValueMarker());
				
				myMarkers.get(0).setOffset(offset);
			} else if (event.getActionCommand().equals("Cursor 1") && button.isSelected()) {
				double offset = addMarkerToPlot(myMarkers.get(1).getValueMarker());
				myMarkers.get(1).setOffset(offset);
			} 
			// remove markers from the plot
			else if (event.getActionCommand().equals("Cursor 0") && !button.isSelected()) {
				plot.removeDomainMarker(myMarkers.get(0).getValueMarker());
			} else if (event.getActionCommand().equals("Cursor 1") && !button.isSelected()) {
				plot.removeDomainMarker(myMarkers.get(1).getValueMarker());
			}
			
			addOrRemoveIntervalMarker();
		}
	}
	
	/**
	 * Used when a plot cursor is initially enabled to add the cursor to the 
	 * plot, centered in the middle of the domain axis.
	 * @param marker
	 */
	private double addMarkerToPlot(ValueMarker marker) {
		double lowerBound = dataModel.getSeriesXLowerBound();
		double upperBound = dataModel.getSeriesXUpperBound();
		double offset = ((upperBound-lowerBound)/2);
		marker.setValue(lowerBound+offset);
//		marker.setValue(plot.getDomainAxis().getRange().getCentralValue());
		plot.addDomainMarker(marker);
		return offset;
	}
	
	
	private void addOrRemoveIntervalMarker() {
		// remove the interval marker from the plot
		plot.removeDomainMarker(intervalMarker, Layer.BACKGROUND);
		
		// check if both cursors are on the graph
		if(plot.getDomainMarkers(Layer.FOREGROUND) != null &&
				plot.getDomainMarkers(Layer.FOREGROUND).size() == 2) {
			// both cursors are on the graph
			double start, end;
			
			// check which plot cursor has the smaller domain value
			if(myMarkers.get(0).getValueMarker().getValue() < myMarkers.get(1).getValueMarker().getValue()) {
				start = myMarkers.get(0).getValueMarker().getValue();
				end = myMarkers.get(1).getValueMarker().getValue();
			} else {
				start = myMarkers.get(1).getValueMarker().getValue();
				end = myMarkers.get(0).getValueMarker().getValue();
			}
			
			// mark the interval between the cursors
			intervalMarker.setStartValue(start);
			intervalMarker.setEndValue(end);
			plot.addDomainMarker(intervalMarker, Layer.BACKGROUND);
		} 
		// check if cursor 0 is on the graph
		else if (plot.getDomainMarkers(Layer.FOREGROUND) != null &&
				plot.getDomainMarkers(Layer.FOREGROUND).contains(myMarkers.get(0).getValueMarker())) {
			// mark the interval below cursor 0, uses the smallest data time value (ignores the displayed domain)
			intervalMarker.setStartValue(dataModel.getSeriesXLowerBound());
			intervalMarker.setEndValue(myMarkers.get(0).getValueMarker().getValue());
			plot.addDomainMarker(intervalMarker, Layer.BACKGROUND);
		} else if (plot.getDomainMarkers(Layer.FOREGROUND) != null &&
				plot.getDomainMarkers(Layer.FOREGROUND).contains(myMarkers.get(1).getValueMarker())) {
			// mark the interval above cursor 1, uses the largest data time value (ignores the displayed domain)
			intervalMarker.setStartValue(myMarkers.get(1).getValueMarker().getValue());
			intervalMarker.setEndValue(dataModel.getSeriesXUpperBound());
			plot.addDomainMarker(intervalMarker, Layer.BACKGROUND);
		}
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof ObTimeSeriesCollection) {	
			// A channel has been added or removed from the model
			// Set the default statistics listeners to the first channel
			dataModel.addSeriesChangeListener(this, 0);
		}
		
	}
	
	@Override
	/**
	 * Used to adjust the interval marker, when it extends above a cursor to the maximum value
	 * in the plots data set. 
	 */
	public void seriesChanged(SeriesChangeEvent arg0) {
		if(plot.getDomainMarkers(Layer.FOREGROUND) == null)
			return;
		
		// plot markers displayed on the graph
		ArrayList<ValueMarker> plotMarkers = new ArrayList<ValueMarker>(plot.getDomainMarkers(Layer.FOREGROUND));
		
		for (int i=0; i<2; i++){
			if (plotMarkers!= null && plotMarkers.contains(myMarkers.get(i).getValueMarker())){
				myMarkers.get(i).getValueMarker().setValue(dataModel.getSeriesXLowerBound()+myMarkers.get(i).getOffset());
			}
			//addOrRemoveIntervalMarker();
			// Adjust plot so if only cursor 1 is on the graph it extends to the far right data value
			if (plotMarkers != null && plotMarkers.size() == 1){
					if(plotMarkers.contains(myMarkers.get(1).getValueMarker())){
						intervalMarker.setEndValue(dataModel.getSeriesXUpperBound());
						intervalMarker.setStartValue(myMarkers.get(1).getValueMarker().getValue());
					}
					else if (plotMarkers.contains(myMarkers.get(0).getValueMarker())){
						intervalMarker.setEndValue(myMarkers.get(0).getValueMarker().getValue());
						intervalMarker.setStartValue(dataModel.getSeriesXLowerBound());
					}
			}
			else if (plotMarkers != null && plotMarkers.size() ==2){
				if (myMarkers.get(0).getValueMarker().getValue() > myMarkers.get(1).getValueMarker().getValue()){
					intervalMarker.setStartValue(myMarkers.get(1).getValueMarker().getValue());
					intervalMarker.setEndValue(myMarkers.get(0).getValueMarker().getValue());
				}
				else {
					intervalMarker.setStartValue(myMarkers.get(0).getValueMarker().getValue());
					intervalMarker.setEndValue(myMarkers.get(1).getValueMarker().getValue());
				}
			}
		}
	}
	
	/* Getters and Setters */
	public ArrayList<MyValueMarker> getMarkers() {
		return myMarkers;
	}
	
	public XYPlot getXYPlot() {
		return plot;
	}
	
	public boolean getCursorStatus(int index){
		if (plot.getDomainMarkers(Layer.FOREGROUND) != null &&
				plot.getDomainMarkers(Layer.FOREGROUND).contains(myMarkers.get(index).getValueMarker())){
			return true;
		}
		else {
			return false;
		}
	}
	
	/* Unused listener methods */

	@Override
	public void mouseClicked(MouseEvent e) { }

	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) { }
	
}
