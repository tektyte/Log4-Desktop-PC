package view_and_controller;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.*;

import javax.swing.*;

import data_threads.SerialCommsRunnable;
import data_threads.SimulationRunnable;
import model.*;

@SuppressWarnings("serial")
public class ContentPanel extends JPanel implements ActionListener, WindowListener {
	/** The data models for the different electrical data types */
	private Map<ElectricalDataTypes,ElectricalDataModel> dataModels;
	
	/** Used to start/stop simulation mode */
	private SerialCommsRunnable serialCommsRunner;
	private SimulationRunnable simulateRunner;
	
	/** Used for selecting and saving the .csv file which contains the data. */
	private final JFileChooser fileChooser;
	
	/** Instantiated here and used for disconnecting slave when .csv file is opened  */
	private StatusBar statusBar;
	
	/** Menu option for opening a previously logged .csv file */
	private JMenuItem openFile;
	
	/** Menu option for saving a .csv file of the data */
	private JMenuItem saveFile;
	
	/** Used for reading and writing the .csv file (need the number of data channels)*/
	private ObservableModelDetails obModelDetails;
	
	/** Used for writing of the .csv file */
	private ElectricalDataTypes minDataLengthKey;
	private int minDataLength;
	
	/** Menu option for opening the battery monitor sub-window */
	private JMenuItem batteryMenuButton;
	
	/** Menu options concerning the device */
	private JMenuItem deviceMenuButton;
	
	/** Menu option for enabling and disabling simulation mode */
	private JRadioButtonMenuItem simulateMenuButton;
	
	public ContentPanel(Map<ElectricalDataTypes,ElectricalDataModel> dataModels, 
			SerialCommsRunnable serialCommsRunner, SimulationRunnable simulateRunner, 
			ObservableModelDetails observableModelDetails,
			JMenuBar menuBar) {
		
		this.serialCommsRunner = serialCommsRunner;
		this.simulateRunner = simulateRunner;
		
		setLayout(new BorderLayout());
		
		// Add the bottom bar, which offers capture tools and slave device config options
		statusBar = new StatusBar(serialCommsRunner, simulateRunner, dataModels, menuBar);
		add(statusBar, BorderLayout.PAGE_END);
		
		// Add the center panel: tabbed side bar, main chart and statistic table
		add(new MonitorPanel(dataModels, observableModelDetails, menuBar), BorderLayout.CENTER);
		
		// Set up object used in the saving and opening files
		this.dataModels = dataModels;
		
		// Used for the saving of the data to and the opening of .csv files
		fileChooser = new JFileChooser();
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.addChoosableFileFilter(new CSVFilter());
		
		// Get the file menu for saving the recorded data and loading the recorded data
		JMenu fileMenu = menuBar.getMenu(0);
		openFile = (JMenuItem) fileMenu.getSubElements()[0].getSubElements()[0];
		saveFile = (JMenuItem) fileMenu.getSubElements()[0].getSubElements()[1];
		
		// Get the tools menu for displaying the battery monitor
		JMenu toolsMenu = menuBar.getMenu(1);
		batteryMenuButton = (JMenuItem) toolsMenu.getSubElements()[0].getSubElements()[0];
    	
		// Get the tools menu options concerning the connected device
		deviceMenuButton = (JMenuItem) toolsMenu.getSubElements()[0].getSubElements()[1];
		
		// Get the menu button for enabling and disabling simulation mode
		simulateMenuButton = (JRadioButtonMenuItem) toolsMenu.getSubElements()[0].getSubElements()[4];
		
		// Register the connected models details object
		this.obModelDetails = observableModelDetails;
		
		// register this panel with the menu bar options
		simulateMenuButton.addActionListener(this);
		batteryMenuButton.addActionListener(this);
		deviceMenuButton.addActionListener(this);
		openFile.addActionListener(this);
		saveFile.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		// Handle a file save and file open event
		if(event.getSource().equals(openFile)) {			
			// give user the option to open a file to be displayed
			int openSelection = fileChooser.showOpenDialog(this);
			
			if(openSelection == JFileChooser.APPROVE_OPTION &&
					checkUserOpenConfimation()) {
				
				// disconnect from the device
				statusBar.disconnectFromSlavedDevice();
				
				// open the file
				readCSVFile();
			}
		} else if (event.getSource().equals(saveFile)) {
			// give user the option to save the file to be displayed, no matter the state
			int saveSelection = fileChooser.showSaveDialog(this);
			
			if(saveSelection == JFileChooser.APPROVE_OPTION) {
				// save the file
				writeCSVFile();
			}
		} else if (event.getSource().equals(batteryMenuButton)) {
			// open the battery monitor window and disable the associated menu option
			batteryMenuButton.setEnabled(false);
			(new BatteryMonitorWindow(
					dataModels.get(ElectricalDataTypes.CURRENT), "Battery Monitor")
					).addWindowListener(this);
			
		} else if (event.getSource().equals(deviceMenuButton)) {
			new DeviceDetailsWindow(obModelDetails, serialCommsRunner);
		} else if (event.getSource().equals(simulateMenuButton)) {
			if(simulateMenuButton.isSelected()) {
				// start the simulation thread, and kill the serial comms and serial comms timer thread
				
				System.out.println("Start simulation thread, kill serial communications thread");
				
				if (checkUserSimulationModeConfirmation()) {
					serialCommsRunner.close();
					simulateRunner.startSimulationThread();
				} else {
					simulateMenuButton.setSelected(false);
				}
				
			} else {
				// start the serial comms thread, and kill the simulation thread
				
				System.out.println("Kill simulation thread, start serial communications thread");
				
				if(!serialCommsRunner.isRunning()) {
					simulateRunner.close();
					serialCommsRunner.startConnectionThread();
				}
			}
		}
	}
	
	@Override
	/**
	 * Re-enable the battery monitor menu option, as the battery monitor frame 
	 * has been closed.
	 */
	public void windowClosed(WindowEvent e) { 
		batteryMenuButton.setEnabled(true);
	}
	
	
	/**
	 * Used to alert the user that they are about to loose their unsaved data.
	 * @return true if the user wants to open the .csv despite the loss of data, else false
	 */
	private boolean checkUserOpenConfimation() {
		// check if there is stored graph data (arbitrary choice of power)
		if(dataModels.get(ElectricalDataTypes.POWER).isEmpty()) {
			// no need to prompt the user as there is no data to loose
			return true;
		}
		
		// prompt the user as they are about to loose the current logged data
		int n = JOptionPane.showConfirmDialog(
				this, // the parent component
				"Are you sure you want to open the .csv file, clear the current logged data " +
				"and disconnect from the slaved device? All unsaved data will we lost.", 
				"Clear data and open .csv file?", 		// title of the window
				JOptionPane.YES_NO_OPTION); 			// 2 types of options
		
		return n == JOptionPane.YES_OPTION;
	}
	
	private boolean checkUserSimulationModeConfirmation() {
		// check if there is stored graph data (arbitrary choice of power)
		if(dataModels.get(ElectricalDataTypes.POWER).isEmpty()) {
			// no need to prompt the user as there is no data to loose
			return true;
		}
		
		// prompt the user as they are about to loose the current logged data
		int n = JOptionPane.showConfirmDialog(
				this, // the parent component
				"Are you sure you want to start simulation mode, clear the current logged data " +
				"and disconnect from the slaved device? All unsaved data will we lost.", 
				"Clear data and start simulation mode?", 		// title of the window
				JOptionPane.YES_NO_OPTION); 			// 2 types of options
		
		return n == JOptionPane.YES_OPTION;
	}
	
	/**
	 * Open the .csv file for viewing
	 */
	//TODO reading in a .csv file will fail if the .csv file is nnot in the same format as the connected device
	private void readCSVFile() {
		// Use the file chooser to get the .csv file
		File file = fileChooser.getSelectedFile();
		
		// Used for reading the .csv file
		Scanner scanner;
		
		
		try {
			//numLines = (int) readNumberOfCSVFileLines(file);
			scanner = new Scanner(file);
			
			// read in the model number that produced this .csv file
			int modelNumber = Integer.parseInt(scanner.nextLine().split(",")[1]);
			
			// skip over the lines that don't contain series data (including column headings)
			scanner.nextLine();
			while(!scanner.nextLine().contains("TIME")) ;
			
			// clear both of the time series maps
			for(ElectricalDataModel dataModel : dataModels.values()) {
				dataModel.removeAllDataAndDataSeries();
			}
			
			
			// update the classes with the model details
			obModelDetails.changeModel(modelNumber);
			
			// read the remaining lines of the .csv file into the data models
			int flag=0;
			while(scanner.hasNextLine()) {
				String[] channelStrings = scanner.nextLine().split(" ");
				
				// the time point associated with the data
				long time = Long.parseLong(channelStrings[0].replaceAll(",", ""));
				if (flag==0){
					for(ElectricalDataModel dataModel : dataModels.values()) {
						dataModel.setLogStartTime(time);
						dataModel.stopUpdateTimer();
					}
					flag=1;
				}
				
				for(int i=1; i<channelStrings.length; i++) {
					String[] valueStrings = channelStrings[i].split(",");
					
					if(valueStrings.length == 6) {
						// the channel stores electrical data
						dataModels.get(ElectricalDataTypes.POWER).addChartData(i-1, time,Double.parseDouble(valueStrings[1]));
						dataModels.get(ElectricalDataTypes.VOLTAGE).addChartData(i-1, time,Double.parseDouble(valueStrings[2]));
						dataModels.get(ElectricalDataTypes.CURRENT).addChartData(i-1, time,Double.parseDouble(valueStrings[3]));
						dataModels.get(ElectricalDataTypes.RESISTANCE).addChartData(i-1, time,Double.parseDouble(valueStrings[4]));
						dataModels.get(ElectricalDataTypes.ENERGY).addChartData(i-1, time,Double.parseDouble(valueStrings[5]));
					} else if (valueStrings.length == 2) {
						// the channel stores temperature data
						dataModels.get(ElectricalDataTypes.TEMPERATURE).addChartData(i-1, time,Double.parseDouble(valueStrings[1]));
					}
					
				}		

			}
			
			scanner.close();
		} catch (FileNotFoundException e) {
			System.err.println("Failed to read the input file.");
		}
	}
	
	/**
	 * Write the current data to a .csv file
	 */
	private void writeCSVFile() {
		// the file to write to
		File file = fileChooser.getSelectedFile();
		
		// used to handle annoying extension naming
		String path = file.getAbsolutePath();
		String extension = ".csv";
		
		try {
			// Add the appropriate .csv extension if the path does not exist 
			if(!path.endsWith(extension)) {
				file.delete();
				file = new File(path + extension);
				
				// If the file exists overwrite it
				if(file.exists())
					file.delete();	
					
				file.createNewFile();
			}
			
			FileWriter fileWriter = new FileWriter(file);
			
			// update the minimum data length and associated electrical data type
			findMinDataLength();
			
			// write the table header (including the cursor times)
			writeTableHeader(fileWriter);
			
			// write the data statistics to file
			writeDataStatisticsToFile(fileWriter);
			
			// write the data points to file
			writeDataPointsToFile(fileWriter);
			
			fileWriter.close();
		} catch (IOException e) {
			System.err.println("Failed to create or write the file.");
		}
	}
	
	private void writeDataPointsToFile(FileWriter fileWriter) throws IOException {
		// write the time column
		fileWriter.write("TIME\n");
		
		// write the data points to file
		int numChannels = obModelDetails.getChannels().size();
		
		// get the channels
		ArrayList<ElectricalDataTypes> channels = obModelDetails.getChannels();
		boolean flag=true;
		Scanner powerScanner = new Scanner(dataModels.get(ElectricalDataTypes.POWER).getFile());
		Scanner voltageScanner = new Scanner(dataModels.get(ElectricalDataTypes.VOLTAGE).getFile());
		Scanner currentScanner = new Scanner(dataModels.get(ElectricalDataTypes.CURRENT).getFile());
		Scanner resistanceScanner = new Scanner(dataModels.get(ElectricalDataTypes.RESISTANCE).getFile());
		Scanner energyScanner = new Scanner(dataModels.get(ElectricalDataTypes.ENERGY).getFile());
		while(flag){
			String[] inputString = powerScanner.nextLine().split(",");
			fileWriter.write(inputString[0]);
			fileWriter.write(", ,"+inputString[1]);	
			inputString = voltageScanner.nextLine().split(",");
			fileWriter.write(","+inputString[1]);	
			inputString = currentScanner.nextLine().split(",");
			fileWriter.write(","+inputString[1]);
			inputString = resistanceScanner.nextLine().split(",");
			flag=resistanceScanner.hasNextLine();
			fileWriter.write(","+inputString[1]);
			inputString = energyScanner.nextLine().split(",");
			fileWriter.write(","+inputString[1]);
			fileWriter.write("\n");
		}
		powerScanner.close();
		voltageScanner.close();
		currentScanner.close();
		resistanceScanner.close();
		energyScanner.close();
		
//		for(int i=0; i < minDataLength; i++) {
//			// write the time value for the channels
//			if(dataModels.containsKey(ElectricalDataTypes.POWER))
//				fileWriter.write("" + dataModels.get(ElectricalDataTypes.POWER).getXValue(0, i));
//			else if (dataModels.containsKey(ElectricalDataTypes.TEMPERATURE))
//				fileWriter.write("" + dataModels.get(ElectricalDataTypes.TEMPERATURE).getXValue(0, i));
//			
//			// for each channel stream (power or temperature)
//			for(int j=0; j < numChannels; j++) {
//				
//				if(channels.get(j) == ElectricalDataTypes.POWER) {
//					// choice of power as the time reference was arbitrary
//					
//					fileWriter.write(", ,"+dataModels.get(ElectricalDataTypes.POWER).getYValue(j, i));
//					fileWriter.write(","+dataModels.get(ElectricalDataTypes.VOLTAGE).getYValue(j, i));
//					fileWriter.write(","+dataModels.get(ElectricalDataTypes.CURRENT).getYValue(j, i));
//					fileWriter.write(","+dataModels.get(ElectricalDataTypes.RESISTANCE).getYValue(j, i));
//					fileWriter.write(","+dataModels.get(ElectricalDataTypes.ENERGY).getYValue(j, i));
//				} else {
//					fileWriter.write(", ," + dataModels.get(ElectricalDataTypes.TEMPERATURE).getYValue(j, i));
//				}
//				
//			}
//			
//			fileWriter.write("\n");
//		}
		
	}
	
	/**
	 * Writes the electrical data types statistics to a .csv file
	 * @param fileWriter used to write the statistics
	 * @throws IOException thrown by the file writer if it fails to write the staistics
	 */
	private void writeDataStatisticsToFile(FileWriter fileWriter) throws IOException {

		
		//String[] statTypes = {"Min", "Max", "Mean", "Median", "Std", "Var"};
		String[] statTypes = {"Min", "Max", "Mean"};
		// write the data statistics to file
		int numChannels = obModelDetails.getChannels().size();
		ArrayList<ElectricalDataTypes> channels = obModelDetails.getChannels();
		
		for(String statType : statTypes) {
			// write the statistic header
			fileWriter.write(statType);
			
			// for each channel stream (power or temperature)
			for(int i=0; i < numChannels; i++) {
				if(channels.get(i) == ElectricalDataTypes.POWER) {
					fileWriter.write(",,"+dataModels.get(ElectricalDataTypes.POWER).getLongTermStatistics(i).get(statType).getDataWithUnit());
					fileWriter.write(","+dataModels.get(ElectricalDataTypes.VOLTAGE).getLongTermStatistics(i).get(statType).getDataWithUnit());
					fileWriter.write(","+dataModels.get(ElectricalDataTypes.CURRENT).getLongTermStatistics(i).get(statType).getDataWithUnit());
					fileWriter.write(","+dataModels.get(ElectricalDataTypes.RESISTANCE).getLongTermStatistics(i).get(statType).getDataWithUnit());
					fileWriter.write(","+dataModels.get(ElectricalDataTypes.ENERGY).getLongTermStatistics(i).get(statType).getDataWithUnit());
				} else if (channels.get(i) == ElectricalDataTypes.TEMPERATURE) {
					fileWriter.write(",,"+dataModels.get(ElectricalDataTypes.TEMPERATURE).getLongTermStatistics(i).get(statType).getDataWithUnit());
				}
			}
			fileWriter.write("\n");
		}
		fileWriter.write("\n\n");
	}
	
	/**
	 * Used to write the electrical data types in the table header and
	 * cursor times to the .csv
	 * @param fileWriter used to write the .csv file
	 * @throws IOException
	 */
	private void writeTableHeader(FileWriter fileWriter) throws IOException {
		// write the device model number
		fileWriter.write("Model Number," + obModelDetails.getModelNumber());
		fileWriter.write("\n\n");
		
		// write the time period the statistics are between
		long tstart, tend;
		
		// choice of power arbitrary for start value
		tstart = dataModels.get(ElectricalDataTypes.POWER).getStatisticsStartTime();
		tend = dataModels.get(minDataLengthKey).getStatisticsEndTime();
		
		fileWriter.write("Start Time, End Time, Time Diff.\n");
		fileWriter.write(tstart+","+tend+","+(tend-tstart)+"\n\n");
		
		// write the channel headings
		ArrayList<ElectricalDataTypes> channels = obModelDetails.getChannels();
		
		fileWriter.write(",");
		for(int i=0; i < channels.size(); i++) {
			ElectricalDataTypes channelType = channels.get(i);
			if (channelType == ElectricalDataTypes.POWER) {
				fileWriter.write(",,,Channel " + i + ",,,");
			} else if (channelType == ElectricalDataTypes.TEMPERATURE) {
				fileWriter.write(",Channel " + i + ",");
			}
		}
		fileWriter.write("\n\n");
		
		// write the electrical data type headings
		fileWriter.write(",");
		for(ElectricalDataTypes channelType : channels) {
			if (channelType == ElectricalDataTypes.POWER) {
				
				fileWriter.write(","+ElectricalDataTypes.POWER.toString());
				fileWriter.write(","+ElectricalDataTypes.VOLTAGE.toString());
				fileWriter.write(","+ElectricalDataTypes.CURRENT.toString());
				fileWriter.write(","+ElectricalDataTypes.RESISTANCE.toString());
				fileWriter.write(","+ElectricalDataTypes.ENERGY.toString());
				fileWriter.write(",");
			} else if (channelType == ElectricalDataTypes.TEMPERATURE) {
				fileWriter.write(","+ElectricalDataTypes.TEMPERATURE.toString());
				fileWriter.write(",");
			}
		}
		fileWriter.write("\n");

	}
	
	/**
	 * 
	 * @return the minimum item count for the list of electrical data type series
	 */
	private void findMinDataLength() {
		minDataLength = Integer.MAX_VALUE;
		
		for(ElectricalDataModel dataModel : dataModels.values()) {
			if (minDataLength > dataModel.getMinimumItemCount()) {
				minDataLength = dataModel.getMinimumItemCount();
				minDataLengthKey = dataModel.getElectricalDataType();
			}
		}
		
		
	}
	

	@Override
	public void windowActivated(WindowEvent e) { }

	@Override
	public void windowClosing(WindowEvent e) { }

	@Override
	public void windowDeactivated(WindowEvent e) { }

	@Override
	public void windowDeiconified(WindowEvent e) { }

	@Override
	public void windowIconified(WindowEvent e) { }

	@Override
	public void windowOpened(WindowEvent e) { }
}
