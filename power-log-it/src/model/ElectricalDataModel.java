package model;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;
import javax.swing.Timer;

import org.jfree.chart.event.MarkerChangeEvent;
import org.jfree.chart.event.MarkerChangeListener;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesChangeListener;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.*;
import org.apache.commons.math3.stat.descriptive.*;

import data_threads.SerialCommsRunnable;
import data_threads.SimulationRunnable;
import view_and_controller.BatteryMonitorWindow;
import view_and_controller.MyValueMarker;


public class ElectricalDataModel implements ActionListener,
		MarkerChangeListener, SeriesChangeListener, Observer {
	// Stores the thread runner that reads and buffers the serial data.
	private SerialCommsRunnable serialRunner;
	
	private File temp;
	
	// Stores the thread runner that simulates a connected device
	private SimulationRunnable simulateRunner;
	
	// Update the graph at a rate of once per 1 second
	private static final int UPDATE_PERIOD = 1000;
	private Timer updateTimer;

	// Objects store the current data displayed in the histogram and line charts
	private ObTimeSeriesCollection obDataCollection;
	private ObTimeSeriesCollection obLineDataCollection;
	
	// Objects contain statistics (Max, Min, Mean, Median, Std and Var)
	private ArrayList<Map<String, Statistic>> channelFormattedStats;
	private ArrayList<double[]> channelStatAndHistData;
	
	
	private ArrayList<Map<String, Statistic>> longTermStats;
	private double longTermStatData=0;
	private int dataSize=0;

	// Object indicates what electrical data this model stores (e.g. Power,
	// Voltage...)
	private ElectricalDataTypes elecDataType;

	// Used for recording the region of time in which the statistics represent
	// used by the battery calculator window
	private ObservableDate startTime;
	private ObservableDate endTime;

	private Millisecond logStartTime;
	// menu bar buttons which contain cursor status
	JRadioButtonMenuItem[] cursorEnableButtons;
	ArrayList<MyValueMarker> myMarkers;

	/** Menu options that involve serial communication with the power logger */
	private JMenuItem deviceDetailsItem;

	// contains the Y value for the specified cursor
	private ArrayList<Statistic> marker0Ys;
	private ArrayList<Statistic> marker1Ys;
	
	// maximum number of points to display
	public static int MAX_ITEM_AGE = 120000;
	
	// time difference for buffer
	long timeDiff=0;

	public ElectricalDataModel(SerialCommsRunnable serialRunner, SimulationRunnable simulateRunner,
			ObservableModelDetails obModelDetails, ElectricalDataTypes elecDataType, JMenuBar menuBar) {
		this.serialRunner = serialRunner;
		this.simulateRunner = simulateRunner;
		this.elecDataType = elecDataType;
		logStartTime= new Millisecond(new Date(System.currentTimeMillis()));
		// initially data capture is disabled
		updateTimer = new Timer(UPDATE_PERIOD, this);
		updateTimer.stop();
		//updateTimer.start();
		

		// register this component with the cursor activation menu buttons
		JMenu viewMenu = menuBar.getMenu(2);
		JMenu cursorMenu = (JMenu) viewMenu.getSubElements()[0]
				.getSubElements()[0];
		MenuElement[] cursorEnableButtons = cursorMenu.getSubElements()[0]
				.getSubElements();

		this.cursorEnableButtons = new JRadioButtonMenuItem[cursorEnableButtons.length];

		for (int i = 0; i < cursorEnableButtons.length; i++) {
			JRadioButtonMenuItem currItem = (JRadioButtonMenuItem) cursorEnableButtons[i];
			this.cursorEnableButtons[i] = currItem;
			currItem.addActionListener(this);
		}

		// initialise the marker statistics
		marker0Ys = new ArrayList<>();
		marker1Ys = new ArrayList<>();

		// initialise the collections which store the channel data
		obDataCollection = new ObTimeSeriesCollection();
		obLineDataCollection = new ObTimeSeriesCollection();
		
		// intialise the statistics representing the data stores
		channelFormattedStats = new ArrayList<>();
		longTermStats = new ArrayList<>();

		// initialise the observable start and end times
		startTime = new ObservableDate(null, true);
		endTime = new ObservableDate(null, false);

		// Menu options which involve serial communication with the device
		JMenu toolsMenu = menuBar.getMenu(1);
		deviceDetailsItem = (JMenuItem) toolsMenu.getSubElements()[0]
				.getSubElements()[1];
		deviceDetailsItem.addActionListener(this);

		// listen for changes in the model number
		obModelDetails.addObserver(this);

		// used for updating the histogram data
		addDataCollectionListener(this);
	}

	/**
	 * Used to update the histogram when no cursors are displayed on the screen.
	 * 
	 * @return values to be used in the histogram
	 */
	private void updateHistogramDataAndStats() {
		
		channelStatAndHistData = new ArrayList<>();
		for(int i=0 ; i < obDataCollection.getSeriesCount(); i++) {
			TimeSeries series = obDataCollection.getSeries(i);
			
			
			// update the times which the statistics are recorded between
			if (series.getItemCount() != 0) {
				startTime.setDate(new Date(logStartTime.getMiddleMillisecond()));
				endTime.setDate(new Date(series.getTimePeriod(
						series.getItemCount() - 1).getMiddleMillisecond()));
			}
			int k=0;
			for (k = 0; k<series.getItemCount(); k++){
				if(new Date(series.getDataItem(k).getPeriod().getMiddleMillisecond()).before(startTime.getDate())){
				}
				else{
					break;
				}
			}
			double[] statAndHistData = new double[series.getItemCount()-k];
			/* Histogram is revalidated with new data. */
			for (int j = 0; j < statAndHistData.length; j++) {
				
				statAndHistData[j] = series.getDataItem(j+k).getValue().doubleValue();
			}
			
			
			// add the array of histogram data to the list
			channelStatAndHistData.add(statAndHistData);
			// update the formatted statistics for channel i
			updateTableStatistics(statAndHistData, channelFormattedStats.get(i));
			updateLongTermStats(longTermStatData,longTermStats.get(i));
		}
	}

	/**
	 * Function is called to get the histogram data set when both cursors are
	 * displayed on the screen.
	 * 
	 * @param minTime
	 *            the minimum cursor time value
	 * @param maxTime
	 *            the maximum cursor time value
	 * @return values to be used in the histogram that fall between minTime and
	 *         maxTime
	 */
	private void updateHistogramDataAndStats(Date minTime, Date maxTime) {
		channelStatAndHistData = new ArrayList<>();
		
		// update the times which the statistics are recorded between
		startTime.setDate(minTime);
		endTime.setDate(maxTime);
		
		for(int i=0 ; i < obDataCollection.getSeriesCount(); i++) {
			TimeSeries series = obDataCollection.getSeries(i);
			ArrayList<Double> dataList = new ArrayList<Double>();
			
			for (int j = 0; j < series.getItemCount(); j++) {
				Date time = series.getDataItem(j).getPeriod().getStart();
				if (time.after(minTime) && time.before(maxTime))
					dataList.add(series.getDataItem(j).getValue().doubleValue());
			}
			
			double[] statAndHistData = new double[dataList.size()];
			for (int j = 0; j < dataList.size(); j++) {
				statAndHistData[j] = dataList.get(j);
			}
			
			channelStatAndHistData.add(statAndHistData);
			updateTableStatistics(statAndHistData, channelFormattedStats.get(i));
			updateLongTermStats(longTermStatData,longTermStats.get(i));
		}
	}

	/**
	 * Used to update the histogram data set when one cursor is displayed on the
	 * screen.
	 * 
	 * @param cutOffTime
	 *            the cursor time value
	 * @param before
	 *            indicates whether to display histogram for data below cursors
	 *            (true) or above cursors (false)
	 * @return data set for histogram
	 */
	private void updateHistogramDataAndStats(Date cutOffTime, boolean before) {
		channelStatAndHistData = new ArrayList<>();
		
		for(int i=0 ; i < obDataCollection.getSeriesCount(); i++) {
			TimeSeries series = obDataCollection.getSeries(i);
			ArrayList<Double> dataList = new ArrayList<Double>();
			
			// update the times the statistics are recorded between
			// for this particular channel
			if (before) {
				startTime.setDate(new Date(series.getTimePeriod(0)
						.getMiddleMillisecond()));
				endTime.setDate(cutOffTime);
			} else {
				startTime.setDate(cutOffTime);
				endTime.setDate(new Date(series.getTimePeriod(
						series.getItemCount() - 1).getMiddleMillisecond()));
			}
			
			// get the relevant channel data that fall within the time window
			for (int j = 0; j < series.getItemCount(); j++) {
				Date time = series.getDataItem(j).getPeriod().getStart();
				if (before && time.before(cutOffTime))
					dataList.add(series.getDataItem(j).getValue().doubleValue());
				else if (!before && time.after(cutOffTime))
					dataList.add(series.getDataItem(j).getValue().doubleValue());
			}
			
			// update the statistics used in the histogram
			double[] statAndHistData = new double[dataList.size()];
			for (int j = 0; j < dataList.size(); j++) {
				statAndHistData[j] = dataList.get(j);
				
			}
			
			// store the histogram data in the list and update the associated formatted statistics
			channelStatAndHistData.add(statAndHistData);
			updateTableStatistics(statAndHistData, channelFormattedStats.get(i));
			updateLongTermStats(longTermStatData,longTermStats.get(i));
		}
	}

	/**
	 * 
	 * @param data
	 *            the buffered data to be used in updating the statistics
	 */
	private void updateTableStatistics(double[] statAndHistData, Map<String, Statistic> formatDataStats) {
		DescriptiveStatistics tableStats = new DescriptiveStatistics();
		
		
		for (int i = 0; i < statAndHistData.length; i++) {
			tableStats.addValue(statAndHistData[i]);
		}
		formatDataStats.get("Min").setDataStr(tableStats.getMin());
		formatDataStats.get("Max").setDataStr(tableStats.getMax());
		formatDataStats.get("Median").setDataStr(
				tableStats.getPercentile(50));
		formatDataStats.get("Mean").setDataStr(tableStats.getMean());
		formatDataStats.get("Std").setDataStr(
				tableStats.getStandardDeviation());
		formatDataStats.get("Var").setDataStr(tableStats.getVariance());
		formatDataStats.get("Sum").setDataStr(tableStats.getSum());
		
	}
	
	private void updateLongTermStats (double longTermStatData, Map<String, Statistic> longTermStats){
		dataSize++;
		if (dataSize==1)
			longTermStats.get("Min").setDataStr(longTermStatData);
		else if(longTermStatData<longTermStats.get("Min").getBaseData()){
			longTermStats.get("Min").setDataStr(longTermStatData);
		}
		if(longTermStatData>longTermStats.get("Max").getBaseData()){
			longTermStats.get("Max").setDataStr(longTermStatData);
		}
		double sum = longTermStats.get("Sum").getBaseData() + longTermStatData;
		longTermStats.get("Sum").setDataStr(sum);
		longTermStats.get("Mean").setDataStr(sum/(dataSize));	



	}

	/* Listener methods */
	
	@Override
	public void seriesChanged(SeriesChangeEvent arg0) {
		/* Update the histogram and statistics table where appropriate */
		if (!cursorEnableButtons[0].isSelected()
				&& !cursorEnableButtons[1].isSelected()) {
			// both of the cursors are inactive
			updateHistogramDataAndStats();
		} else if (!cursorEnableButtons[0].isSelected()
				&& cursorEnableButtons[1].isSelected()) {
			// cursor 1 is the only active cursor
			Date date = new Date((long) myMarkers.get(1).getValueMarker()
					.getValue());
			updateHistogramDataAndStats(date, false);
		}
	}

	@Override
	public void markerChanged(MarkerChangeEvent event) {
		updateHistogramAndStatsToCursors();
	}

	private void updateHistogramAndStatsToCursors() {
		try {

			/* Update the histogram and the y points associated with the markers */
			if (cursorEnableButtons[0].isSelected()
					&& cursorEnableButtons[1].isSelected()) {
				// cursor 0 and cursor 1 are both active
				Date date0, date1;
				date0 = new Date((long) myMarkers.get(0).getValueMarker()
						.getValue());
				date1 = new Date((long) myMarkers.get(1).getValueMarker()
						.getValue());

				if (date0.before(date1)) {
					// cursor 0 is before cursor 1
					updateHistogramDataAndStats(date0, date1);
					for(int i=0 ; i<marker0Ys.size(); i++) {
						marker0Ys.get(i).setDataStr(channelStatAndHistData.get(i)[0]);
						marker1Ys.get(i).setDataStr(channelStatAndHistData.get(i)
							[channelStatAndHistData.get(i).length - 1]);
					}
				} else {
					// cursor 1 is before cursor 0
					updateHistogramDataAndStats(date1, date0);
					for(int i=0 ; i<marker0Ys.size(); i++) {
						marker0Ys.get(i).setDataStr(channelStatAndHistData.get(i)
							[channelStatAndHistData.get(i).length - 1]);
						marker1Ys.get(i).setDataStr(channelStatAndHistData.get(i)[0]);
					}
				}
			} else if (cursorEnableButtons[0].isSelected()) {
				// cursor 0 is the only active cursor
				Date date = new Date((long) myMarkers.get(0).getValueMarker()
						.getValue());
				updateHistogramDataAndStats(date, true);
				
				// update the cursor 0 y-values
				for(int i=0 ; i<marker0Ys.size(); i++) {
					marker0Ys.get(i).setDataStr(channelStatAndHistData.get(i)
						[channelStatAndHistData.get(i).length - 1]);
				}

			} else if (cursorEnableButtons[1].isSelected()) {
				// cursor 1 is the only active cursor
				Date date = new Date((long) myMarkers.get(1).getValueMarker()
						.getValue());
				updateHistogramDataAndStats(date, false);
				
				// update the cursor 1 y-values
				for(int i=0 ; i<marker0Ys.size(); i++) {
					marker1Ys.get(i).setDataStr(channelStatAndHistData.get(i)[0]);
				}

			} else {
				// both of the cursors are inactive
				updateHistogramDataAndStats();
			}
		} catch (IndexOutOfBoundsException e) {
			System.err.println("Histogram data set is empty.");
		}
	}
	
	
	/**
	 * Used by the statistics panels and the tab panels to stay up to date with
	 * the statistics when the data changes. Also used by the MonitorChartPanel
	 * to ensure that when 1 cursor is displayed on the graph the area extends
	 * to the far
	 * 
	 * @param listener
	 */
	public void addDataCollectionListener(Observer listener) {
		obDataCollection.addObserver(listener);
	}

	public void removeDataCollectionListener(Observer listener) {
		obDataCollection.deleteObserver(listener);
	}

	public void addSeriesChangeListener(SeriesChangeListener listener,
			int seriesNumber) {
		// remove the listener from the serious it was previously listening to
		for (int i = 0; i < obDataCollection.getSeriesCount(); i++) {
			TimeSeries series = obDataCollection.getSeries(i);
			series.removeChangeListener(listener);
		}

		// add the listener to the desired series
		if (seriesNumber < obDataCollection.getSeriesCount())
			obDataCollection.getSeries(seriesNumber).addChangeListener(listener);
	}

	/**
	 * Used by the battery monitor window to display the times which the battery
	 * statistics are being calculated over
	 * 
	 * @param batteryMonitorWindow
	 */
	public void addStartTimeAndEndTimeObservers(
			BatteryMonitorWindow batteryMonitorWindow) {
		// System.out.println("Observers added");
		startTime.addObserver(batteryMonitorWindow);
		endTime.addObserver(batteryMonitorWindow);
	}

	@Override
	public void update(Observable o, Object arg) {
		if(o instanceof ObservableModelDetails) {
			handleObModelDetailsEvent((ObservableModelDetails) o);
		} else if (o instanceof ObTimeSeriesCollection) {
			// A channel has been added or removed from the model
			// Set the default statistics listeners to the first channel
			addSeriesChangeListener(this, 0);
		}
	}
	
	private void handleObModelDetailsEvent(ObservableModelDetails obModelDetails) {
		// remove all pre-existing series
		removeAllDataAndDataSeries();

		// Create the channel buffers to store each channels data
		String[] channelFormats = obModelDetails.getChannelFormats();

		// check if this data model represents temperature or not
		if (elecDataType != ElectricalDataTypes.TEMPERATURE) {
			// choice of voltage or current is arbitrary
			for (String channelFormat : channelFormats) {
				if (channelFormat.equals("V")) {
					obDataCollection.addSeries(new TimeSeries(""));	
					obLineDataCollection.addSeries(new TimeSeries(""));
					// initialise structure for handling the histogram and table statistics
					channelFormattedStats.add(initialiseFormattedStatistics());
					longTermStats.add(initialiseFormattedStatistics());
					
					// Store the cursor y values for each channel
					marker0Ys.add(new Statistic(getElectricalDataTitle(), getBaseDataUnitString()));
					marker1Ys.add(new Statistic(getElectricalDataTitle(), getBaseDataUnitString()));
				}
			}
		} else {
			for (String channelFormat : channelFormats) {
				if (channelFormat.equals("T")) {
					obDataCollection.addSeries(new TimeSeries(""));
					obLineDataCollection.addSeries(new TimeSeries(""));
					
					// initialise structure for handling the histogram and table statistics
					longTermStats.add(initialiseFormattedStatistics());
					
					// Store the cursor y values for each channel
					marker0Ys.add(new Statistic(getElectricalDataTitle(), getBaseDataUnitString()));
					marker1Ys.add(new Statistic(getElectricalDataTitle(), getBaseDataUnitString()));
				}
			}
		}
		try {
			temp = File.createTempFile(elecDataType.toString(), ".tmp");
			temp.deleteOnExit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		updateTimer.restart();
	}
	
	/**
	 * Initialise the formatted statistics hash map with entries containing the
	 * relevant statistic title and unit.
	 */
	private Map<String, Statistic> initialiseFormattedStatistics() {
		Map<String, Statistic> channelFormatStats = new TreeMap<String, Statistic>();
		
		channelFormatStats.put("Min", new Statistic("Min",
				getBaseDataUnitString()));
		channelFormatStats.put("Max", new Statistic("Max",
				getBaseDataUnitString()));
		channelFormatStats.put("Median", new Statistic("Median",
				getBaseDataUnitString()));
		channelFormatStats.put("Mean", new Statistic("Mean",
				getBaseDataUnitString()));
		channelFormatStats.put("Std", new Statistic("Std",
				getBaseDataUnitString()));
		channelFormatStats.put("Var", new Statistic("Var",
				getBaseDataUnitString()));
		channelFormatStats.put("Sum", new Statistic("Sum",
				getBaseDataUnitString()));
		
		return channelFormatStats;
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		ArrayList<ArrayList<TimeDataPoint>> dataBuffers = null;

		if (e.getSource().equals(updateTimer)) {

			// Update the display if capture enabled and sufficient time has passed
			//TODO simulation thread stuff
			
			if(serialRunner.isRunning()) {
				dataBuffers = serialRunner.drainDataBuffers(elecDataType);
			} else if (simulateRunner.isRunning()) {
				dataBuffers = simulateRunner.drainDataBuffers(elecDataType);
			}
			
			/* If neither of the simulation or serial threads are running
			 * do nothing */
			if(dataBuffers == null)
				return;
			
			// Updating energy series involves an integration of power
			if (elecDataType == ElectricalDataTypes.ENERGY) {
				updateEnergySeries(dataBuffers);
			} else {
				updateRegularSeries(dataBuffers);
			}

		} else if (e.getSource() instanceof JButton) {
			// start, pause or stop capture buttons has been pressed
			if ((e.getActionCommand().equals("Start-Pause Capture") && updateTimer.isRunning())
					|| e.getActionCommand().equals("Stop Capture")) {
				// pause or stop button was pressed
				updateTimer.stop();
			} else if (e.getActionCommand().equals("Start-Pause Capture")
					&& !updateTimer.isRunning()) {
				// start button was pressed
				updateTimer.restart();
			}
		} else if (e.getSource().equals(deviceDetailsItem)) {
			
		} else if(e.getSource() instanceof JComboBox<?>) {
			// Connection combo-box was selected
			JComboBox<?> connectBox = (JComboBox<?>)  e.getSource();
			
			if(connectBox.getSelectedIndex() == 0) {
				// connection is being attempted
				updateTimer.restart();
			} else if (connectBox.getSelectedIndex() == 1) {
				// disconnection is being attempted
				//updateTimer.stop();
			}
			
		}
	}

	/* Listener helper methods */

	/**
	 * Drain the buffered data into this data model.
	 * 
	 * @param dataPoints
	 *            the buffered data to be drained into this data model.
	 */
	private void updateRegularSeries(
			ArrayList<ArrayList<TimeDataPoint>> dataBuffers) {
		if (dataBuffers.size() != obDataCollection.getSeriesCount()
				|| dataBuffers.size() != obLineDataCollection.getSeriesCount()) {
			System.err.println("Error: Databuffers Size = " + dataBuffers.size() + ", "
					+ "Datacollection Size = " + obDataCollection.getSeriesCount());
			return;
		}
try{
			FileWriter fileWriter = new FileWriter(temp,true);
		for (int i = 0; i < dataBuffers.size(); i++) {
			ArrayList<TimeDataPoint> dataPoints = dataBuffers.get(i);
			TimeSeries seriesData = obDataCollection.getSeries(i);
			TimeSeries lineSeriesData = obLineDataCollection.getSeries(i);
			while (dataPoints.size() > 0) {
				TimeDataPoint currDataPoint = dataPoints.remove(0);
				if (obDataCollection.getSeries(i).getItemCount()==0){
					obDataCollection.getSeries(0).setMaximumItemAge(MAX_ITEM_AGE);
					obLineDataCollection.getSeries(0).setMaximumItemAge(MAX_ITEM_AGE);
					for(int j=MAX_ITEM_AGE;j>0;j-=1000){
					seriesData.add(new Millisecond( new Date(currDataPoint.getTime().getMiddleMillisecond()-j)), 0.0);
					lineSeriesData.add(seriesData.getDataItem(seriesData.getItemCount() - 1));
					}
					logStartTime = currDataPoint.getTime();
					dataSize=0;
				}
				
				try {
					longTermStatData=currDataPoint.getValu();
					seriesData.add(currDataPoint.getTime(),
							currDataPoint.getValu());
					lineSeriesData.add(seriesData.getDataItem(seriesData
							.getItemCount() - 1));
					fileWriter.write(currDataPoint.getTime().getMiddleMillisecond() + "," + currDataPoint.getValu()+"\n");
					
					
				} catch (SeriesException exception) {
					System.err.println("Duplicate Data Points");
				}
				
			}
			
			
		}
			fileWriter.flush();
			fileWriter.close();
			} catch (IOException e){
				e.printStackTrace();
			}
	}

	/**
	 * Converts the buffered power data into energy data and drains it into this
	 * data model.
	 * 
	 * @param powerPoints
	 *            the buffered power data.
	 */
	private void updateEnergySeries(
			ArrayList<ArrayList<TimeDataPoint>> powerBuffers) {
		try{
			FileWriter fileWriter = new FileWriter(temp,true);
			// iterate through the energy buffers, storing the new data points in
			// each
			for (int i = 0; i < powerBuffers.size(); i++) {
				ArrayList<TimeDataPoint> powerPoints = powerBuffers.get(i);
				TimeSeries seriesData = obDataCollection.getSeries(i);
				TimeSeries lineSeriesData = obLineDataCollection.getSeries(i);

				while (powerPoints.size() > 0) {
					// the current power data point to be converted and added
					TimeDataPoint powerDataPoint = powerPoints.remove(0);
					if (obDataCollection.getSeries(i).getItemCount()==0){
						obDataCollection.getSeries(0).setMaximumItemAge(MAX_ITEM_AGE);
						obLineDataCollection.getSeries(0).setMaximumItemAge(MAX_ITEM_AGE);
						for(int j=MAX_ITEM_AGE;j>0;j-=1000){
						seriesData.add(new Millisecond( new Date(powerDataPoint.getTime().getMiddleMillisecond()-j)), 0.0);
						lineSeriesData.add(seriesData.getDataItem(seriesData
								.getItemCount() - 1));
						}
					logStartTime =powerDataPoint.getTime();
					dataSize=0;
					}
					
	
					// the time difference between two points in seconds
					double dt;
					// catches exception associated with duplicate time values
					try {
						if (seriesData.getItemCount() > 0) {
							dt = powerDataPoint.getTime().getMiddleMillisecond()
									- seriesData.getTimePeriod(
											seriesData.getItemCount() - 1)
											.getMiddleMillisecond();
							dt = dt / 1e3;
	
							// new energy value is the old energy value plus dt*(new
							// power value)
							longTermStatData=seriesData.getValue(seriesData.getItemCount() - 1).doubleValue()
									+ dt * powerDataPoint.getValu();
							seriesData.add(powerDataPoint.getTime(), seriesData
									.getValue(seriesData.getItemCount() - 1)
									.doubleValue()
									+ dt * powerDataPoint.getValu());
							fileWriter.write(powerDataPoint.getTime().getMiddleMillisecond() + "," + (seriesData
									.getValue(seriesData.getItemCount() - 1)
									.doubleValue()
									+ dt * powerDataPoint.getValu()+"\n"));
							
						} else {
							// first data point: initially no energy stored
							longTermStatData=0;
							seriesData.add(powerDataPoint.getTime(), 0);
							fileWriter.write(powerDataPoint.getTime().getMiddleMillisecond() +",0\n");
							
						}
						// update the line series
						lineSeriesData.add(seriesData.getDataItem(seriesData
								.getItemCount() - 1));
						
					} catch (SeriesException exception) {
						System.err.println("Duplicate Energy Data Points");
					}
				}
			}
			
			
			fileWriter.flush();
			fileWriter.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	/* Getters and Setters */

	public void setMyMarkers(ArrayList<MyValueMarker> myMarkers) {
		this.myMarkers = myMarkers;
	}
	
	/**
	 * Used by MonitorChartPanel cursors to set cursor x-values
	 * @return
	 */
	public long getSeriesXLowerBound() {
		long min=0;
		for(int i=0; i < obDataCollection.getSeriesCount(); i++) {
			if(i==0 || min > obDataCollection.getSeries(i).getDataItem(0).
					getPeriod().getMiddleMillisecond()) {
				min = obDataCollection.getSeries(i).getDataItem(0).
						getPeriod().getMiddleMillisecond();
			}
		}
		return min; 
	}
	
	/**
	 * Used by MonitorChartPanel cursors to set cursor x-values
	 * @return
	 */
	public long getSeriesXUpperBound() {
		long max=0;
		for(int i=0; i < obDataCollection.getSeriesCount(); i++) {
			TimeSeries series = obDataCollection.getSeries(i);
			if((i==0 || max < series.getDataItem(series.getItemCount()-1).
					getPeriod().getMiddleMillisecond()) && series.getItemCount()!=0) {
				max = obDataCollection.getSeries(i).getDataItem(series.getItemCount()-1).
						getPeriod().getMiddleMillisecond();
			}
		}
		return max; 
	}
	
	public long getTimeDiff(){
		long diff = timeDiff;
		timeDiff=0;
		return diff;
	}
	
	/**
	 * Used by the MonitorStatsPanel after a series or marker or action event
	 * @return
	 */
	public double[] getHistogramData(int channelNum) {
		if(channelStatAndHistData != null && channelNum < channelStatAndHistData.size() 
				&& channelNum >= 0)
			return channelStatAndHistData.get(channelNum);
		else {
			return new double[] {};
		}
	}

	public String getMarker0String(int channelNumber) {
		Date xPoint = new Date((long) myMarkers.get(0).getValueMarker()
				.getValue());

		return "(" + new SimpleDateFormat("H:mm:ss").format(xPoint) + ", "
				+ marker0Ys.get(channelNumber).getDataWithUnit() + ")";
	}

	public String getMarker1String(int channelNumber) {
		Date xPoint = new Date((long) myMarkers.get(1).getValueMarker()
				.getValue());

		return "(" + new SimpleDateFormat("H:mm:ss").format(xPoint) + ", "
				+ marker1Ys.get(channelNumber).getDataWithUnit() + ")";
	}

	/**
	 * Used when writing the .csv file and updating the battery window
	 * 
	 * @return
	 */
	public long getStatisticsStartTime() {
		if (startTime.getDate() == null)
			return 0;
		return startTime.getDate().getTime();
	}

	/**
	 * Used when writing the .csv file and updating the battery window
	 * 
	 * @return
	 */
	public long getStatisticsEndTime() {
		if (endTime.getDate() == null)
			return 0;
		return endTime.getDate().getTime();
	}
	
	/**
	 * Used by the tab panels to get the channel 0 statisitcs.
	 * @return
	 */
	public Map<String, Statistic> getFormattedStatistics() {
		if(channelFormattedStats.size() > 0)	
			return channelFormattedStats.get(0);
		else
			return initialiseFormattedStatistics();
		
	}
	/**
	 * Used by the tab panels to get the channel 0 long term statisitcs.
	 * @return
	 */
	public Map<String, Statistic> getLongTermStatistics() {
		if(longTermStats.size() > 0)	
			return longTermStats.get(0);
		else
			return initialiseFormattedStatistics();
		
	}
	
	/**
	 * Used by the the statistics panel to get the appropriate channel statistics
	 * @param channelIndex
	 * @return
	 */
	public Map<String, Statistic> getFormattedStatistics(int channelIndex) {
		if(channelFormattedStats.size() > 0 && channelIndex < channelFormattedStats.size())
			return channelFormattedStats.get(channelIndex);
		else if(channelFormattedStats.size() > 0)	
			return channelFormattedStats.get(0);
		else
			return initialiseFormattedStatistics();
	}
	
	/**
	 * Used by the the statistics panel to get the appropriate long Term statistics
	 * @param channelIndex
	 * @return
	 */
	public Map<String, Statistic> getLongTermStatistics(int channelIndex) {
		if(longTermStats.size() > 0 && channelIndex < longTermStats.size())
			return longTermStats.get(channelIndex);
		else if(longTermStats.size() > 0)	
			return longTermStats.get(0);
		else
			return initialiseFormattedStatistics();
	}

	public TimeSeriesCollection getDataCollection() {
		return obDataCollection.getCollection();
	}

	public TimeSeriesCollection getLineDataCollection() {
		return obLineDataCollection.getCollection();
	}

	public ElectricalDataTypes getElectricalDataType() {
		return elecDataType;
	}
	
	/**
	 * 
	 */
	public void removeAllDataAndDataSeries() {
		// must occur before the series and line data is cleared
		//statAndHistogramData = new double[] {};
		channelStatAndHistData = new ArrayList<>();
		// initialiseFormattedStatistics();
		channelFormattedStats = new ArrayList<>();
		longTermStats = new ArrayList<>();
		
		// remove the markers from the list
		marker0Ys.clear();
		marker1Ys.clear();
		
		updateTimer.stop();
		obDataCollection.removeAllSeries();
		obLineDataCollection.removeAllSeries();
		if (temp!=null)
		temp.delete();
		
	}

	/**
	 * Used to clear the current data before reading a csv file, or when the
	 * user click the clear button
	 */
	public void clearData() {
		// must occur before the series and line data is cleared
		channelStatAndHistData = new ArrayList<>();
		// store the number of channels
		int numChannels = channelFormattedStats.size();
		channelFormattedStats = new ArrayList<>();
		longTermStats = new ArrayList<>();
		dataSize=0;
		for(int i=0; i < numChannels ; i++) {
			channelFormattedStats.add(initialiseFormattedStatistics());
			longTermStats.add(initialiseFormattedStatistics());
		}
		longTermStatData=0;
		for (int i = 0; i < obDataCollection.getSeriesCount(); i++) {
			obDataCollection.getSeries(i).clear();
			obLineDataCollection.getSeries(i).clear();
		}
		temp.delete();
		
	}

	/**
	 * Used for reading of the csv file (static analysis mode)
	 * 
	 * @param time
	 * @param value
	 */
	public void addChartData(int channelNumber, long time, double value) {
		Millisecond time2 = new Millisecond(new Date(time));
		obDataCollection.getSeries(channelNumber).add(time2, value);
		obLineDataCollection.getSeries(channelNumber).add(time2, value);
	}
	
	public void setLogStartTime(long time){
		logStartTime=new Millisecond(new Date(time));
	}
	
	public void stopUpdateTimer(){
		updateTimer.stop();
	}

	/**
	 * Used for writing a csv file
	 * 
	 * @return
	 */
	public int getMinimumItemCount() {
		int min=0;
		for(int i=0; i < obDataCollection.getSeriesCount(); i++) {
			if(i==0 || min > obDataCollection.getSeries(i).getItemCount()) {
				min = obDataCollection.getSeries(i).getItemCount();
			}
		}
		return min;
	}

	/**
	 * Used for writing data to a csv file
	 * 
	 * @param index
	 * @return
	 */
	public Number getYValue(int seriesIndex, int dataIndex) {
		if(seriesIndex < obDataCollection.getSeriesCount())
			return obDataCollection.getSeries(seriesIndex).getValue(dataIndex);
		System.out.println(".csv channel out of bounds");
		return 0;
	}

	/**
	 * Used for writing data to a csv file
	 * 
	 * @param index
	 * @return
	 */
	public long getXValue(int seriesIndex, int dataIndex) {
		if(seriesIndex < obDataCollection.getSeriesCount())
			return obDataCollection.getSeries(seriesIndex).getTimePeriod(dataIndex).getMiddleMillisecond();
		System.out.println(".csv channel out of bounds");
		return 0;
	}
	
	/**
	 * Used for reading from temp file
	 * @return file
	 */
	public File getFile(){
		return temp;
	}

	/**
	 * 
	 * @return false if there is currently any series data within this data
	 *         model, else true
	 */
	public boolean isEmpty() {
		// choice of data collection or line data collection is arbitrary
		for (int i = 0; i < obDataCollection.getSeriesCount(); i++) {
			if (!obDataCollection.getSeries(0).isEmpty())
				return false;
		}

		return true;
	}

	public String getElectricalDataTitle() {
		if (elecDataType == ElectricalDataTypes.VOLTAGE)
			return "Voltage";
		else if (elecDataType == ElectricalDataTypes.CURRENT)
			return "Current";
		else if (elecDataType == ElectricalDataTypes.POWER)
			return "Power";
		else if (elecDataType == ElectricalDataTypes.RESISTANCE)
			return "Resistance";
		else if (elecDataType == ElectricalDataTypes.ENERGY)
			return "Energy";
		else
			return "";
	}

	public String getBaseDataUnitString() {
		if (elecDataType == ElectricalDataTypes.VOLTAGE)
			return "V";
		else if (elecDataType == ElectricalDataTypes.CURRENT)
			return "A";
		else if (elecDataType == ElectricalDataTypes.POWER)
			return "W";
		else if (elecDataType == ElectricalDataTypes.RESISTANCE)
			return "\u03A9";
		else if (elecDataType == ElectricalDataTypes.ENERGY)
			return "J";
		else
			return "";
	}

	public Color getElectricalColour() {
		if (elecDataType == ElectricalDataTypes.VOLTAGE)
			return Color.GREEN;
		else if (elecDataType == ElectricalDataTypes.CURRENT)
			return Color.RED;
		else if (elecDataType == ElectricalDataTypes.POWER)
			return Color.BLUE;
		else if (elecDataType == ElectricalDataTypes.RESISTANCE)
			return new Color(102, 0, 204); // purple
		else if (elecDataType == ElectricalDataTypes.ENERGY)
			return new Color(255, 128, 0); // yellow 2
		else
			return Color.BLACK;
	}
}
