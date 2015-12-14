package view_and_controller;

import model.*;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import org.jfree.chart.*;
import org.jfree.chart.event.MarkerChangeEvent;
import org.jfree.chart.event.MarkerChangeListener;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.*;
import org.jfree.data.statistics.HistogramDataset;



@SuppressWarnings("serial")
public class MonitorStatsPanel extends JPanel implements SeriesChangeListener, Observer,
													     MarkerChangeListener, ActionListener {
	/** The standard fonts used within this panel */
	private static final String STD_FONT_STR = "Planer";
	private static final String ALT_FONT_STR = "Planer";
	
	/** Constants */
	private static final Font TITLE_FONT = new Font("Planer", Font.BOLD, 26);
	private static final Font CHANNEL_BOX_FONT = new Font("Planer", Font.BOLD, 20);
	private static final int FONT_SIZE_DATA_LBL = 14;
	private static final int FONT_SIZE_DATA = 20;
	private static final int NUMBER_OF_HIST_BINS = 30;
	
	/** Electrical Data Model associated with this statistics display*/
	private ElectricalDataModel dataModel;
	
	/** Histogram panel contains the histogram and combo-box for selecting channel data*/
	private ChartPanel histogramPanel;
	
	/** The panels used for displaying the cursor values and data statistics */
	private JPanel statsPanel;
	private JPanel statsTablePanel;
	private JPanel longTermStatsTablePanel;
	private JPanel longTermStatPanel;
	
	/** Histogram globals */
	//private HistogramDataset histDataset;
	private JFreeChart histogram;
	
	/** Stores the current (x,y) point of the cursors (or 'inactive') */
	private JLabel[] cursorLabels;	
	
	/** Menu bar buttons which contain cursor status */
	private JRadioButtonMenuItem[] cursorEnableButtons;
	
	/** Drop down boxes for selecting which channel data to display */
	private JComboBox<String> channelBox;
	
	public MonitorStatsPanel(ElectricalDataModel dataModel, ObservableModelDetails observableModelDetails, 
			MonitorChartPanel mainChartPanel, JMenuBar menuBar) {
		
		// Statistics Table and Histogram Displayed side-by-side (width = 2)
		setLayout(new GridLayout(1,2));
		
		// Used for listening for changes in data
		this.dataModel = dataModel;
		dataModel.addDataCollectionListener(this);
		
		// Initialise the histogram panel
		histogramPanel = initHistogram(dataModel);
		
		// Initialise the statistics panel
		statsPanel = initStatsPanel();
		
		// Store the cursors that can be displayed on the graph
		ArrayList<MyValueMarker> myMarkers = mainChartPanel.getMarkers();
		myMarkers.get(0).getValueMarker().addChangeListener(this);
		myMarkers.get(1).getValueMarker().addChangeListener(this);
		
		// Register this component with the cursor activation menu buttons
		JMenu viewMenu = menuBar.getMenu(2);
		JMenu cursorMenu = (JMenu) viewMenu.getSubElements()[0].getSubElements()[0];
		MenuElement[] cursorEnableButtons = cursorMenu.getSubElements()[0].getSubElements();
		
		// cannot type cast between arrays
		this.cursorEnableButtons = new JRadioButtonMenuItem[cursorEnableButtons.length];
		for (int i=0; i < cursorEnableButtons.length; i++) {
			
			JRadioButtonMenuItem currItem = (JRadioButtonMenuItem) cursorEnableButtons[i];
			this.cursorEnableButtons[i] = currItem;
			currItem.addActionListener(this);
		}
		
		// Listen for changes in the connected device details
		observableModelDetails.addObserver(this);
		
		// Add the statistics panel and the histogram panel to this panel
		add(statsPanel);
		add(histogramPanel);
	}
	
	private JPanel initStatsPanel() {
		JPanel statsPanel = new JPanel(new BorderLayout());
		JPanel numbersAndCursorsPanel = new JPanel(new GridLayout(3,1));
		JPanel titlePanel;
		
		statsPanel.setBackground(Color.WHITE);
		numbersAndCursorsPanel.setBackground(Color.WHITE);
		
		// title (which includes combo-box for channel selection)
		titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		titlePanel.setBackground(Color.WHITE);
		
		//TODO enable channel selection box based on device model
		//channelBox = new JComboBox<String>();
		//channelBox.setFont(CHANNEL_BOX_FONT);
		//channelBox.setBackground(Color.WHITE);
		//channelBox.addActionListener(this);
		
		JLabel title = new JLabel(dataModel.getElectricalDataTitle() + " Statistics");
		title.setFont(TITLE_FONT);
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setVerticalAlignment(JLabel.TOP);
		
		titlePanel.add(title);
		//titlePanel.add(channelBox);
		statsPanel.add(titlePanel, BorderLayout.PAGE_START);
		
		// cursors
		numbersAndCursorsPanel.add(initCursors());
		
		// statistics
		numbersAndCursorsPanel.add(initStatsTable());
		numbersAndCursorsPanel.add(initLongTermStatsTable());
		
		statsPanel.add(numbersAndCursorsPanel, BorderLayout.CENTER);
		return statsPanel;
	}
	
	private JPanel initCursors() {
		JPanel cursorPanel = new JPanel();
		JPanel[] singleCursorPanels = new JPanel[2];
		cursorLabels = new JLabel[2];
		
		cursorPanel.setLayout(new GridLayout(1,2));
		cursorPanel.setBackground(Color.WHITE);
		
		for(int i=0; i<singleCursorPanels.length; i++) {
			singleCursorPanels[i] = new JPanel(new GridLayout(3,1));
			singleCursorPanels[i].setOpaque(false);
			
			// title
			singleCursorPanels[i].add(setupCursorTitle("Cursor " + i));
			
			// label
			cursorLabels[i] = setupCursorLbl("Inactive");
			singleCursorPanels[i].add(cursorLabels[i]);
			
			// add cursor display to panel
			cursorPanel.add(singleCursorPanels[i]);
		}

		return cursorPanel;
	}
	
	private JLabel setupCursorTitle(String title) {
		JLabel titleLbl = new JLabel(title);
		
		titleLbl.setFont(new Font(STD_FONT_STR, Font.BOLD, 22));
		titleLbl.setHorizontalAlignment(JLabel.CENTER);
		
		return titleLbl;
	}
	
	private JLabel setupCursorLbl(String content) {
		JLabel cursorLbl = new JLabel("Inactive");
		cursorLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 18));
		cursorLbl.setOpaque(false);
		cursorLbl.setHorizontalAlignment(JLabel.CENTER);
		return cursorLbl;
	}
	
	/**
	 * Entries must come in sorted and ascending order of row number!!
	 * @param statsPanel
	 * @param entries
	 */
	private JPanel initStatsTable() {
		Map<String,Statistic> mp = dataModel.getFormattedStatistics();
		JPanel statPanel;
		
		statsTablePanel = new JPanel(new GridLayout(2,3));
		statsTablePanel.setBackground(Color.WHITE);
		
		for(Statistic value : mp.values()) {
			if(!value.getTitleStr().equals("Sum")) {
				statPanel = createStatisticPanel(value);
				statsTablePanel.add(statPanel);
			}
		}
		return statsTablePanel;
	}
	
	private JPanel initLongTermStatsTable() {
		Map<String,Statistic> mp = dataModel.getLongTermStatistics();
		JPanel statPanel;
		JPanel titlePanel;
		
		
		longTermStatsTablePanel = new JPanel(new GridLayout(3,1));
		longTermStatsTablePanel.setBackground(Color.WHITE);
		
		titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		titlePanel.setBackground(Color.WHITE);
		JLabel title = new JLabel("Long Term Statistics");
		title.setFont(TITLE_FONT);
		title.setHorizontalAlignment(JLabel.CENTER);
		title.setVerticalAlignment(JLabel.TOP);
		titlePanel.add(title);
		longTermStatsTablePanel.add(titlePanel, BorderLayout.PAGE_START);
		
		longTermStatPanel = new JPanel(new GridLayout(1,3));
		longTermStatPanel.setBackground(Color.WHITE);
		
		
		
		for(Statistic value : mp.values()) {
			if(value.getTitleStr().equals("Min")||value.getTitleStr().equals("Max")||value.getTitleStr().equals("Mean")) {
				statPanel = createStatisticPanel(value);
				longTermStatPanel.add(statPanel);
			}
		}
		longTermStatsTablePanel.add(longTermStatPanel);
		

		return longTermStatsTablePanel;
	}
	
	private JPanel createStatisticPanel(Statistic stat) {
		JPanel statPanel = new JPanel();
		GridBagConstraints c = new GridBagConstraints();
		JLabel titleLbl = new JLabel(stat.getTitleStr());
		JLabel dataLbl = new JLabel(stat.getDataStr() + stat.getUnitStr());
		
		statPanel.setBackground(Color.WHITE);
		statPanel.setLayout(new GridBagLayout());
		
		// title
		c.gridx = 0; c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		
		titleLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, FONT_SIZE_DATA_LBL));
		statPanel.add(titleLbl, c);
		
		// data
		c.gridy = 1;
		
		dataLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, FONT_SIZE_DATA));
		statPanel.add(dataLbl, c);
		
		return statPanel;
		
	}
	
	private ChartPanel initHistogram(ElectricalDataModel dataModel) {
		//String title = dataModel.getElectricalDataTitle() + " Monitor Histogram";
		
		String axisLabelX = dataModel.getElectricalDataTitle() + " (" +
				dataModel.getBaseDataUnitString() + ")";
		
		HistogramDataset histDataset = new HistogramDataset();
		
		// generate the graph
		histogram = ChartFactory.createHistogram(
		"", 	// Title
		axisLabelX, 				// x-axis Label
		"Number of Samples", 		// y-axis Label
		histDataset, 				// Dataset
		PlotOrientation.VERTICAL, 	// Plot Orientation
		false, 						// Show Legend
		true, 						// Use tooltips
		false );					// generate URLs?
		((TextTitle) histogram.getTitle()).setFont(new Font(STD_FONT_STR, Font.BOLD, 28));

		XYPlot plot = (XYPlot) histogram.getPlot();
		
		// initialise the plot settings
		plot.setBackgroundPaint(Color.WHITE);
		plot.setDomainGridlinePaint(Color.GRAY);
		plot.setRangeGridlinePaint(Color.GRAY);
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);
		plot.getDomainAxis().setLabelFont(new Font(STD_FONT_STR,Font.PLAIN, 20));
		plot.getRangeAxis().setLabelFont(new Font(STD_FONT_STR,Font.PLAIN, 20));
		plot.getDomainAxis().setTickLabelFont(new Font(STD_FONT_STR,Font.PLAIN, 20));
		plot.getRangeAxis().setTickLabelFont(new Font(STD_FONT_STR,Font.PLAIN, 20));
		
		// remove light streak through the bars on the histogram
		XYBarRenderer renderer = (XYBarRenderer) ((XYPlot) histogram.getPlot()).getRenderer();
		renderer.setBarPainter(new StandardXYBarPainter());
		
		// add the graph to the panel
		ChartPanel chartPanel = new ChartPanel(histogram);
		chartPanel.createToolTip();
		chartPanel.setMouseWheelEnabled(true);
		
		//set min and max draw dimensions for resizing
		chartPanel.setMinimumDrawHeight(0);
		chartPanel.setMinimumDrawWidth(0);
		chartPanel.setMaximumDrawHeight(30000);
		chartPanel.setMaximumDrawWidth(30000);
		
		// remove the 'properties' option from the chart right-click
		chartPanel.getPopupMenu().remove(0);
		chartPanel.getPopupMenu().remove(0);
		
		// remove the 'print' option from the chart right-click
		chartPanel.getPopupMenu().remove(2);
		chartPanel.getPopupMenu().remove(2);
		
		// remove the option to save as a pdf
		((JMenu) chartPanel.getPopupMenu().getComponent(1)).remove(2);
		
		return chartPanel;
	}

	@Override
	public void update(Observable o, Object arg) {
		if(o instanceof ObTimeSeriesCollection) {
			// A channel has been added or removed from the model
			// Set the default statistics listeners to the first channel
			dataModel.addSeriesChangeListener(this, 0);
		} else if (o instanceof ObservableModelDetails) {
			ObservableModelDetails obModelDetails = (ObservableModelDetails) o;
			// the connected device details have changed,
			// update the statistics and histograms drop-down with appropriate number of channels
			
			//TODO enable channel selection box based on device model
			// reset the combo boxes
			//channelBox.removeAllItems();
			//channelBox.setBackground(Color.WHITE);
			
			String[] channelFormats = obModelDetails.getChannelFormats();
			int channelNum = 0;
			for(int i=0; i < channelFormats.length; i++) {
				// choice of voltage was arbitrary
				if(channelFormats[i].equals("V") && 
						dataModel.getElectricalDataType() != ElectricalDataTypes.TEMPERATURE
						|| channelFormats[i].equals("T") && 
						dataModel.getElectricalDataType() == ElectricalDataTypes.TEMPERATURE) {
					//channelBox.addItem("Channel " + channelNum);
					channelNum++;
				}
			}
			//System.out.println(channelNum);
		}
	}

	@Override
	public void seriesChanged(SeriesChangeEvent arg0) {
		//System.out.println("Series change event");
		
		/* Update the histogram and statistics table where appropriate */
		//updateHistogram(dataModel.getHistogramData(channelBox.getSelectedIndex()));
		updateHistogram(dataModel.getHistogramData(0));
		updateStatsTablePanel();
		updateLongTermStatsTablePanel();
	}
	
	@Override
	public void markerChanged(MarkerChangeEvent event) {
		// update the statistics histogram, cursor text and stats table to correspond to cursors 
		//updateHistogram(dataModel.getHistogramData(channelBox.getSelectedIndex()));
		updateHistogram(dataModel.getHistogramData(0));
		alignStatTextToCursors();
		updateStatsTablePanel();
	}
	

	@Override
	public void actionPerformed(ActionEvent event) {
		if(event.getSource() instanceof JRadioButtonMenuItem) {
			//updateHistogram(dataModel.getHistogramData(channelBox.getSelectedIndex()));
			updateHistogram(dataModel.getHistogramData(0));
			alignStatTextToCursors();
			updateStatsTablePanel();
		} else if (event.getSource().equals(channelBox)) {
			updateHistogram(dataModel.getHistogramData(channelBox.getSelectedIndex()));
		}
	}
	
	private void alignStatTextToCursors() {
		/* Update the cursor position text */
		if(cursorEnableButtons[0].isSelected()) {
			//cursorLabels[0].setText(dataModel.getMarker0String(channelBox.getSelectedIndex()));
			cursorLabels[0].setText(dataModel.getMarker0String(0));
		} else {
			cursorLabels[0].setText("Inactive");
		}
		
		if(cursorEnableButtons[1].isSelected()) {
			//cursorLabels[1].setText(dataModel.getMarker1String(channelBox.getSelectedIndex()));
			cursorLabels[1].setText(dataModel.getMarker1String(0));
		} else {
			cursorLabels[1].setText("Inactive");
		}
	}
	
	private void updateStatsTablePanel() {
		JPanel statPanel;
		
		statsTablePanel.removeAll();
		//for(Statistic value : dataModel.getFormattedStatistics(channelBox.getSelectedIndex()).values()) {
		for(Statistic value : dataModel.getFormattedStatistics(0).values()) {
			if(!value.getTitleStr().equals("Sum")) {
				statPanel = createStatisticPanel(value);
				statsTablePanel.add(statPanel);
			}
		}
		
		statsTablePanel.revalidate();
		statsTablePanel.repaint();
	}
	
	private void updateLongTermStatsTablePanel() {
		JPanel statPanel;
		
		longTermStatPanel.removeAll();
		//for(Statistic value : dataModel.getFormattedStatistics(channelBox.getSelectedIndex()).values()) {
		for(Statistic value : dataModel.getLongTermStatistics(0).values()) {
			if(value.getTitleStr().equals("Min")||value.getTitleStr().equals("Max")||value.getTitleStr().equals("Mean")) {
				statPanel = createStatisticPanel(value);
				longTermStatPanel.add(statPanel);
			}
		}
		
		longTermStatPanel.revalidate();
		longTermStatPanel.repaint();
	}
	
	private void updateHistogram(double[] data) {
		HistogramDataset histDataset = new HistogramDataset();
		
		if (data.length > 0)
			histDataset.addSeries("H1", data, NUMBER_OF_HIST_BINS);
		
			
		((XYPlot) histogram.getPlot()).setDataset(histDataset);
		((XYPlot) histogram.getPlot()).getRenderer().setSeriesPaint(0, 
				new Color(dataModel.getElectricalColour().getRed(),
						dataModel.getElectricalColour().getGreen(),
						dataModel.getElectricalColour().getBlue(),
						150));
		
	}

	
}
