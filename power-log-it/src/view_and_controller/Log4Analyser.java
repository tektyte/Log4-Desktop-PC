package view_and_controller;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.*;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import data_threads.SerialCommsRunnable;
import data_threads.SimulationRunnable;
import model.*;





/**
 * 
 * 
 * Java Version: 1.7
 * 
 * 
 * @author M.J
 *
 */
public class Log4Analyser implements ActionListener {
	
	private JFrame window;
	
	private JFrame splashScreen;
	
	public Log4Analyser() {
		// Initialise the look and feel of the UI to be consistent with OS (e.g. windows, MAC, etc...)
		try {
			UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
		} catch (Exception e) {
			System.out.println("Cannot set look and feel!");
		}
		
		showSplashScreen();
		
		// Open the main window for the in-line power monitor GUI
		window = new JFrame("Log4 Analyser");
		window.setMinimumSize(new Dimension(800,600));
		
		// Create the menu bar
		JMenuBar menuBar = createMenuBar();
		
		// Observable Model Details
		ObservableModelDetails obModelDetails = new ObservableModelDetails();
		
		// Initialise the thread used for serial communication 
		SerialCommsRunnable serialComms = new SerialCommsRunnable(obModelDetails, menuBar);
		
		// Initialise the thread used for simulation
		SimulationRunnable simulateRunner = new SimulationRunnable(obModelDetails, menuBar);
		
		// Initialise the data stores on the GUI thread (this thread)
		Map<ElectricalDataTypes,ElectricalDataModel> dataModels = new HashMap<ElectricalDataTypes, ElectricalDataModel>();
		dataModels.put(ElectricalDataTypes.VOLTAGE, 
				new ElectricalDataModel(serialComms, simulateRunner, obModelDetails, ElectricalDataTypes.VOLTAGE, menuBar));
		dataModels.put(ElectricalDataTypes.CURRENT, 
				new ElectricalDataModel(serialComms, simulateRunner, obModelDetails, ElectricalDataTypes.CURRENT, menuBar));
		dataModels.put(ElectricalDataTypes.POWER, 
				new ElectricalDataModel(serialComms, simulateRunner, obModelDetails, ElectricalDataTypes.POWER, menuBar));
		dataModels.put(ElectricalDataTypes.RESISTANCE, 
				new ElectricalDataModel(serialComms, simulateRunner, obModelDetails, ElectricalDataTypes.RESISTANCE, menuBar));
		dataModels.put(ElectricalDataTypes.ENERGY, 
				new ElectricalDataModel(serialComms, simulateRunner, obModelDetails, ElectricalDataTypes.ENERGY, menuBar));
		
		// Contains all of the GUI components, excluding the menu bar
		ContentPanel content = new ContentPanel(dataModels, serialComms, simulateRunner, 
				obModelDetails, menuBar);
		
		// Set up the GUI window
		window.setJMenuBar(menuBar);
		window.getContentPane().setLayout(new BorderLayout());
		window.getContentPane().add(content, BorderLayout.CENTER);
		window.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		window.setResizable(true);
		window.setSize(1024,768);
		
		// ensure GUI is displayed in the middle of the screen
		int[] xy = getXYStartPositionOfWindow(window.getSize().width / 2, 
											  window.getSize().height / 2);
		window.setLocation(xy[0], xy[1]);
		
		// Start the thread concerned with Serial Communication
		//new Thread(serialComms).start();
		serialComms.startConnectionThread();
	}
	
	/**
	 * Entry point for the in-line power monitor GUI
	 * @param args command line arguments. NOT Used.
	 */
	public static void main(String args[]) {
		new Log4Analyser();
	}
	
	/**
	 * Creates the main menu bar for the in-line power GUI.
	 * @return the menu bar for the power GUI
	 */
	public static JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		JMenu toolsMenu = new JMenu("Tools");
		JMenu viewMenu = new JMenu("View");
		JMenu helpMenu = new JMenu("Help");
		
		menuBar.add(fileMenu);
		menuBar.add(toolsMenu);
		menuBar.add(viewMenu);
		menuBar.add(helpMenu);
		
		// File menu options
		JMenuItem openItem = new JMenuItem("Open");
		JMenuItem saveItem = new JMenuItem("Save");
		JMenuItem exitItem = new JMenuItem("Exit");
		
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(exitItem);
		
		// Tools menu options
		
		// Battery monitor
		JMenuItem batteryItem = new JMenuItem("Battery Monitor");
		toolsMenu.add(batteryItem);
		toolsMenu.addSeparator();
		
		// Used for getting the device name and version
		JMenuItem deviceDetailsItem = new JMenuItem("Device Details");
		toolsMenu.add(deviceDetailsItem);
		
		// Used to set the sampling rate of the slaved device
		JMenu samplingRateMenu = new JMenu("Sampling Rate");
		
		samplingRateMenu.add(new JRadioButtonMenuItem("1 ms"));
		samplingRateMenu.add(new JRadioButtonMenuItem("10 ms"));
		
		// Default is a sampling rate of 1 sample/ ms
		JRadioButtonMenuItem start = new JRadioButtonMenuItem("100 ms");
		start.setSelected(true);
		
		samplingRateMenu.add(start);
		samplingRateMenu.add(new JRadioButtonMenuItem("1 sec"));
		samplingRateMenu.add(new JRadioButtonMenuItem("1 min"));
		samplingRateMenu.add(new JRadioButtonMenuItem("1 hr"));
		samplingRateMenu.addSeparator();
		samplingRateMenu.add(new JRadioButtonMenuItem("Custom"));
		
		toolsMenu.add(samplingRateMenu);
		
		// Used to set the alarm thresholds and turn alarms on and off
		JMenuItem alarmItem = new JMenuItem("Alarm Settings");
		toolsMenu.add(alarmItem);
		
		// Used to engage simulation mode
		toolsMenu.addSeparator();
		toolsMenu.add(new JRadioButtonMenuItem("Simulation Mode"));
		
		// View menu options
		
		// Cursor sub menu
		JMenu cursorMenu = new JMenu("Cursors");
		JRadioButtonMenuItem cursor0 = new JRadioButtonMenuItem("Enable Cursor 0");
		JRadioButtonMenuItem cursor1 = new JRadioButtonMenuItem("Enable Cursor 1");
		
		cursor0.setActionCommand("Cursor 0");
		cursor1.setActionCommand("Cursor 1");
		
		cursorMenu.add(cursor0);
		cursorMenu.add(cursor1);
		
		viewMenu.add(cursorMenu);
		
		return menuBar;
	}
	
	/**
	 * Method initialises the splash screen and the splash screen timer.
	 */
	private void showSplashScreen() {
		// start the splash screen timer
		int splashScreenTime = 2000; // 2 seconds
		Timer splashScreenTimer = new Timer(splashScreenTime, this);
		splashScreenTimer.setActionCommand("timer");
		splashScreenTimer.restart();
		
		splashScreen = new JFrame();
		
		// Load in the splash screen images
		ImageIcon companyImage = new ImageIcon(SplashScreen.class.getResource("/assets/TektyteLogoSplash.png"));
		
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(Color.WHITE);
		mainPanel.setBorder(new LineBorder(new Color(0, 0, 0), 3));
		splashScreen.getContentPane().add(mainPanel, BorderLayout.CENTER);
		
		// label for the company panel
		JPanel titlePanel = new JPanel();
		titlePanel.setBounds(13, 25, 190, 207);
		titlePanel.setLayout(null);
		titlePanel.setBorder(new MatteBorder(0, 0, 0, 2, (Color) new Color(0, 0, 0)));
		titlePanel.setBackground(Color.WHITE);
		
		// label for the company name; 'Tektyte'
		JLabel companyLbl = new JLabel(companyImage);
		companyLbl.setBounds(0, 63, 180, 75);
		titlePanel.add(companyLbl);
		
		// panel stores everything besides the company name
		JPanel detailsPanel = new JPanel();
		detailsPanel.setBounds(213, 25, 227, 207);
		detailsPanel.setLayout(null);
		detailsPanel.setBackground(Color.WHITE);
		
		// label for the application name
		JLabel applicationLbl = new JLabel("Log4 Analyser");
		applicationLbl.setFont(new Font("Planer", Font.PLAIN, 28));
		applicationLbl.setBounds(10, 11, 196, 59);
		detailsPanel.add(applicationLbl);
		
		// label for the release dates
		JLabel releaseDateLbl = new JLabel("2015 Release");
		releaseDateLbl.setFont(new Font("Planer", Font.PLAIN, 12));
		releaseDateLbl.setBounds(10, 65, 89, 26);
		detailsPanel.add(releaseDateLbl);
		
		// label for stating that app. belongs to Tekt
		JLabel copyWriteLbl = new JLabel("\u00A9 2015 Tekt Industries and its licensors.");
		copyWriteLbl.setFont(new Font("Planer", Font.PLAIN, 12));
		copyWriteLbl.setBounds(10, 139, 207, 14);
		detailsPanel.add(copyWriteLbl);
		
		// Label for stating 'all rights reserved'
		JLabel rightsLbl = new JLabel("All rights reserved.");
		rightsLbl.setFont(new Font("Planer", Font.PLAIN, 10));
		rightsLbl.setBounds(10, 154, 197, 14);
		detailsPanel.add(rightsLbl);
		
		// Label for thanking the contributors
		//JLabel withThanksLbl = new JLabel("With thanks to Mathew Adams");
		JLabel withThanksLbl = new JLabel("");
		withThanksLbl.setFont(new Font("Planer", Font.PLAIN, 12));
		withThanksLbl.setBounds(10, 102, 169, 26);
		detailsPanel.add(withThanksLbl);
		mainPanel.setLayout(null);
		mainPanel.add(titlePanel);
		mainPanel.add(detailsPanel);
		
		// Initialise the splash screen frame (setting the dimensions, main panel and
		// removing the decoration...)
		splashScreen.getContentPane().setBackground(Color.WHITE);
		splashScreen.getContentPane().setLayout(new BorderLayout(0, 0));
		splashScreen.setBounds(100, 100, 450, 255);
		splashScreen.setUndecorated(true);
		splashScreen.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		splashScreen.getContentPane().add(mainPanel, BorderLayout.CENTER);
		
		// get the middle of the monitor
		int[] xy = getXYStartPositionOfWindow(splashScreen.getSize().width / 2, 
								   splashScreen.getSize().height / 2);
		
		// ensure the splash screen appears in the middle of the screen
		splashScreen.setLocation(xy[0], xy[1]);
		splashScreen.setVisible(true);
	}
	
	/**
	 * Method is used to get the x and y location the left corner of the window
	 * should be located at to ensure it is centered in the screen
	 * @param xOffset half the width of the window to be centered
	 * @param yOffset half the height of the window to be centered
	 * @return the x,y coordinates of the left corner of the window
	 */
	private int[] getXYStartPositionOfWindow(int xOffset, int yOffset) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		Rectangle mainMonitorBounds = gd[0].getDefaultConfiguration().getBounds();
		
		int xLocation = mainMonitorBounds.width / 2 - xOffset;
		int yLocation = mainMonitorBounds.height / 2 - yOffset;
		
		if (xLocation < 0)
			xLocation = 0;
		if (yLocation < 0)
			yLocation = 0;
		
		return new int[] {xLocation, yLocation};
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("timer")) {
			// remove the splash screen
			splashScreen.dispose();
			
			
			// set the application window viewable
			window.setVisible(true);
			
			// remove the timer
			Timer splashScreenTimer = ((Timer) e.getSource());
			splashScreenTimer.stop();
		}
		
	}
	
}
