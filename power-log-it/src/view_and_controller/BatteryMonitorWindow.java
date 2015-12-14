package view_and_controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.apache.commons.io.FileUtils;

import model.BatteryType;
import model.ElectricalDataModel;
import model.ObservableDate;
import model.Statistic;

@SuppressWarnings("serial")
public class BatteryMonitorWindow extends JFrame implements Observer, ActionListener, PropertyChangeListener {
	/** Used to get the mAh rating for a battery located within the .csv */
	private final static int MAH_INDEX_IN_CSV = 3;
	
	/** ArrayList storing information concerning battery models */
	private ArrayList<Battery> primaryBatteries;
	private ArrayList<Battery> secondaryBatteries;
	
	/** Combo-box models used swapped in and out depending on user battery type selection */
	private ArrayList<String> primaryBatteryStrs;
	private ArrayList<String> secondaryBatteryStrs;
	
	// Used to get the current statistics for calculating the battery life
	private ElectricalDataModel currentModel;
	
	// the main panel which populates the entire frame
	private JPanel batteryPanel;
	
	// information required to do the battery calculations
	
	// the time difference between the cursors
	private long deltaT;
	
	// the average current used
	private Statistic mean;
	
	// The time panel
	private JLabel cursorStartTimeLbl;
	private JLabel cursorEndTimeLbl;
	private JLabel timeDiffLbl;
	
	// current panel
	private JLabel totalCurrentLbl;
	private JLabel totalCurrentUnit;
	
	private JLabel avgCurrentLbl;
	private JLabel avgCurrentUnit;
	
	// battery choice panel
	private JComboBox<String> batterySelectionsBox;
	private JFormattedTextField customMAhField;
	private JRadioButton primaryBtn;
	private JRadioButton secondaryBtn;
	
	// battery de-rating panel
	private JFormattedTextField cycleDischargeField;
	private JRadioButton enableHighDisBtn;
	
	// battery life label
	private JLabel batteryLifeLbl;
	
	@Override
	/**
	 * Start or end time for statistics has changed.
	 */
	public void update(Observable o, Object arg) {
		if(o instanceof ObservableDate) {
			ObservableDate obDate = (ObservableDate) o;
			String timePoint = new SimpleDateFormat("H:mm:ss").format(obDate.getDate());
			
			if(obDate.isStartTime()) {
				// change the start time label
				cursorStartTimeLbl.setText(timePoint);
			} else {
				// change the end time label
				cursorEndTimeLbl.setText(timePoint);
			}
			
			// update the delta t
			Time cursor0Time = Time.valueOf(cursorStartTimeLbl.getText());
			Time cursor1Time = Time.valueOf(cursorEndTimeLbl.getText());
			deltaT = cursor1Time.getTime() - cursor0Time.getTime();
			deltaT = Math.abs(deltaT);
			
			SimpleDateFormat isoFormat = new SimpleDateFormat("H:mm:ss");
			isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			timePoint = isoFormat.format(new Date(deltaT));
			timeDiffLbl.setText(timePoint);
			
			// update the total current values
			Statistic sum = currentModel.getFormattedStatistics().get("Sum");
			totalCurrentLbl.setText(sum.getDataStr());
			totalCurrentUnit.setText(sum.getUnitStr());
			
			// update the average current values
			mean = currentModel.getFormattedStatistics().get("Mean");
			avgCurrentLbl.setText(mean.getDataStr());
			avgCurrentUnit.setText(mean.getUnitStr());
			
			// update the estimated battery life
			updateEstimatedBatteryLife();
			
		}
		
	}
	
	@Override
	/**
	 * Change the battery life based on the cycle-discharge rate or
	 * the battery storage capacity changing.
	 */
	public void propertyChange(PropertyChangeEvent e) {
		updateEstimatedBatteryLife();
	}
	
	@Override
	/**
	 * A radio button has been selected/deselected.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(primaryBtn) || e.getSource().equals(secondaryBtn)) {
			// change the list to contain only primary batteries
			ArrayList<String> selections = new ArrayList<>();
			
			if (primaryBtn.isSelected())
				selections.addAll(primaryBatteryStrs);
			if (secondaryBtn.isSelected())
				selections.addAll(secondaryBatteryStrs);
			
			batterySelectionsBox.setModel(new DefaultComboBoxModel<String>(
					selections.toArray(new String[selections.size()])));
			
			// Set the custom mAh text field to the selected combox battery
			String selection = (String) batterySelectionsBox.getSelectedItem();
			if(selection != null)
				customMAhField.setValue(Double.parseDouble(
						selection.split(",")[MAH_INDEX_IN_CSV].substring(1)));
		} else if (e.getSource().equals(enableHighDisBtn)) {
			
		} else if (e.getSource().equals(batterySelectionsBox)) {
			// Set the custom mAh text field to the selected combox battery
			String selection = (String) batterySelectionsBox.getSelectedItem();
			if(selection != null)
				customMAhField.setValue(Double.parseDouble(
						selection.split(",")[MAH_INDEX_IN_CSV].substring(1)));
		}
	}
	
	
	private void updateEstimatedBatteryLife() {
		try {
			double batteryStorage  = ((Number)customMAhField.getValue()).doubleValue();
			//NumberFormat.getNumberInstance(Locale.US).parse(customMAhField.getText()).doubleValue();
			double batteryDerating = ((Number)cycleDischargeField.getValue()).doubleValue(); 
			//Double.parseDouble(cycleDischargeField.getText());
			
			batteryStorage -= batteryDerating/100.0 *batteryStorage;
			
			// battery life in fractional hours
			String batteryLifeText = "";
			double batteryLife = (batteryStorage / (1000 * mean.getBaseData()));
			
			int bLHours = (int) batteryLife;
			int bLMinutes = (int) (60 * (batteryLife - bLHours));
			int bLSeconds = (int) Math.round( 60 * ((60 * batteryLife) - Math.floor((60 * batteryLife))));
			
			if(bLHours > 24) {
				batteryLifeText += (bLHours / 24) + "d ";
				bLHours = bLHours - 24 * (bLHours/24);
			}
			batteryLifeText += bLHours+"h "+bLMinutes+"m "+bLSeconds+"s";
			
			batteryLifeLbl.setText(batteryLifeText);
		} catch (Exception e) {
			//System.err.println("Failed to parse the battery storage and/or the battery derating");
		}
	}
	
	/**
	 * Launch the application.
	 */
	public BatteryMonitorWindow(ElectricalDataModel currentModel, String frameTitle) { 
		// used to access the time window in which to assess the battery life
		this.currentModel = currentModel;
		currentModel.addStartTimeAndEndTimeObservers(this);
		
		// Initialise the window parameters
		setResizable(false);		
		setTitle(frameTitle);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		setBounds(100, 100, 676, 505);
		batteryPanel = new JPanel();
		batteryPanel.setBackground(Color.WHITE);
		batteryPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(batteryPanel);
		batteryPanel.setLayout(new GridLayout(1, 0, 0, 0));
		
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		batteryPanel.add(panel);
		panel.setLayout(null);
		
		JPanel titlePanel = new JPanel();
		titlePanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		titlePanel.setBackground(Color.WHITE);
		titlePanel.setBounds(0, 11, 650, 42);
		panel.add(titlePanel);
		titlePanel.setLayout(null);
		
		JLabel title = new JLabel("Battery Life Estimator Tool");
		title.setBounds(10, 0, 294, 31);
		titlePanel.add(title);
		title.setFont(new Font("Tahoma", Font.PLAIN, 25));
		
		JComboBox<String> channelSelectionBox = new JComboBox<String>(new String[] {"Channel 0"});
		channelSelectionBox.setBounds(500, 4, 150, 22);
		channelSelectionBox.setFont(new Font("Tahoma", Font.PLAIN, 12));
		channelSelectionBox.setEnabled(false);
		titlePanel.add(channelSelectionBox);
		
		// time panel section
		JPanel timePanel = initialiseTimePanel();
		panel.add(timePanel);
		
		// current panel section
		JPanel currentPanel = initialiseCurrrentPanel();
		panel.add(currentPanel);
		
		// battery types panel section
		JPanel batteryTypePanel = initialiseBatteryTypePanel();
		panel.add(batteryTypePanel);
		
		// De-rating panel
		JPanel deratingPanel = initialiseDeratingPanel();
		panel.add(deratingPanel);
		
		// Setup Large Battery Life Labels
		JLabel batteryLifeTitle = new JLabel("Estimated Battery Life:");
		batteryLifeTitle.setFont(new Font("Tahoma", Font.PLAIN, 18));
		batteryLifeTitle.setBounds(96, 407, 192, 23);
		panel.add(batteryLifeTitle);
		
		batteryLifeLbl = new JLabel("2h 24m 5s");
		batteryLifeLbl.setFont(new Font("Tahoma", Font.PLAIN, 42));
		batteryLifeLbl.setBounds(310, 395, 350, 51);
		panel.add(batteryLifeLbl);
		
		// Set the frame visible
		setVisible(true);
	}
	
	private JPanel initialiseTimePanel() {
		JPanel timePanel = new JPanel();
		timePanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		timePanel.setBackground(Color.WHITE);
		timePanel.setBounds(0, 63, 650, 71);
		timePanel.setLayout(null);
		
		// Time Panel Title
		JLabel timeTitle = new JLabel("TIME");
		timeTitle.setBounds(10, 0, 28, 15);
		timePanel.add(timeTitle);
		timeTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		// Cursor End Time
		JLabel cursorEndLabel = new JLabel("End Cursor");
		cursorEndLabel.setBounds(54, 21, 65, 14);
		timePanel.add(cursorEndLabel);
		cursorEndLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		String endTime = new 
				SimpleDateFormat("H:mm:ss").format(new Date(currentModel.getStatisticsEndTime()));
		cursorEndTimeLbl = new JLabel(endTime);
		cursorEndTimeLbl.setBounds(54, 46, 65, 14);
		timePanel.add(cursorEndTimeLbl);
		cursorEndTimeLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		// Cursor Start Time
		JLabel cursorStartLabel = new JLabel("Start Cursor");
		cursorStartLabel.setBounds(214, 21, 65, 14);
		timePanel.add(cursorStartLabel);
		cursorStartLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));

		String startTime = new 
				SimpleDateFormat("H:mm:ss").format(new Date(currentModel.getStatisticsStartTime()));		
		cursorStartTimeLbl = new JLabel(startTime);
		cursorStartTimeLbl.setBounds(214, 46, 65, 14);
		timePanel.add(cursorStartTimeLbl);
		cursorStartTimeLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		// Minus symbol
		JLabel minusLbl = new JLabel("-");
		minusLbl.setFont(new Font("Tahoma", Font.BOLD, 25));
		minusLbl.setBounds(145, 46, 19, 14);
		timePanel.add(minusLbl);
		
		// Time difference between the cursors
		JLabel timeDiffTitle = new JLabel("Time Delta");
		timeDiffTitle.setBounds(358, 21, 76, 14);
		timePanel.add(timeDiffTitle);
		timeDiffTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		SimpleDateFormat isoFormat = new SimpleDateFormat("H:mm:ss");
		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date deltaDate = new Date(currentModel.getStatisticsEndTime()-
				currentModel.getStatisticsStartTime());
		String deltaTime = isoFormat.format(deltaDate);
		timeDiffLbl = new JLabel(deltaTime);
		timeDiffLbl.setBounds(358, 46, 65, 14);
		timePanel.add(timeDiffLbl);
		timeDiffLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		JLabel timeDiffFormatLbl = new JLabel("hh:mm:ss");
		timeDiffFormatLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		timeDiffFormatLbl.setBounds(443, 47, 65, 14);
		timePanel.add(timeDiffFormatLbl);
		
		return timePanel;
	}
	
	private JPanel initialiseCurrrentPanel() {
		JPanel currentPanel = new JPanel();
		currentPanel.setLayout(null);
		currentPanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		currentPanel.setBackground(Color.WHITE);
		currentPanel.setBounds(0, 136, 650, 71);
		
		// title of the current panel
		JLabel currentTitle = new JLabel("CURRENT");
		currentTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		currentTitle.setBounds(10, 0, 65, 15);
		currentPanel.add(currentTitle);
		
		// the summation of the current between the cursors
		JLabel totalCurrentTitle = new JLabel("Total Current");
		totalCurrentTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		totalCurrentTitle.setBounds(55, 23, 81, 14);
		currentPanel.add(totalCurrentTitle);
		
		Statistic sum = currentModel.getFormattedStatistics().get("Sum");
		totalCurrentLbl = new JLabel(sum.getDataStr());
		totalCurrentLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		totalCurrentLbl.setBounds(55, 48, 65, 14);
		currentPanel.add(totalCurrentLbl);
		
		totalCurrentUnit = new JLabel(sum.getUnitStr());
		totalCurrentUnit.setFont(new Font("Tahoma", Font.PLAIN, 12));
		totalCurrentUnit.setBounds(130, 48, 33, 14);
		currentPanel.add(totalCurrentUnit);
		
		// the average current based on the time window and summed current
		JLabel avgCurrentTitle = new JLabel("Average Current");
		avgCurrentTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		avgCurrentTitle.setBounds(213, 23, 100, 14);
		currentPanel.add(avgCurrentTitle);
		
		Statistic mean = currentModel.getFormattedStatistics().get("Mean");
		avgCurrentLbl = new JLabel(mean.getDataStr());
		avgCurrentLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		avgCurrentLbl.setBounds(213, 48, 65, 14);
		currentPanel.add(avgCurrentLbl);
		
		avgCurrentUnit = new JLabel(mean.getUnitStr());
		avgCurrentUnit.setFont(new Font("Tahoma", Font.PLAIN, 12));
		avgCurrentUnit.setBounds(306, 48, 33, 14);
		currentPanel.add(avgCurrentUnit);
		
		return currentPanel;
	}
	
	private JPanel initialiseBatteryTypePanel() {
		JPanel batteryTypePanel = new JPanel();
		batteryTypePanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		batteryTypePanel.setBackground(Color.WHITE);
		batteryTypePanel.setBounds(0, 205, 650, 102);
		batteryTypePanel.setLayout(null);
		
		// read in the list of batteries
		readInBatteries();
		
		// battery type panel title
		JLabel batteryTitle = new JLabel("BATTERY TYPE");
		batteryTitle.setBounds(10, 11, 90, 14);
		batteryTypePanel.add(batteryTitle);
		batteryTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		// battery type panel radio buttons for selecting whether the battery is primary or secondary
		primaryBtn = new JRadioButton("Primary");
		primaryBtn.setBounds(53, 44, 109, 23);
		batteryTypePanel.add(primaryBtn);
		primaryBtn.setBackground(Color.WHITE);
		primaryBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		primaryBtn.addActionListener(this);
		primaryBtn.setSelected(true);
		
		secondaryBtn = new JRadioButton("Secondary (Rechargeable)");
		secondaryBtn.setBounds(53, 70, 189, 23);
		batteryTypePanel.add(secondaryBtn);
		secondaryBtn.setBackground(Color.WHITE);
		secondaryBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		secondaryBtn.addActionListener(this);
		secondaryBtn.setSelected(true);
		
		// Drop down box for selecting a battery model
		JLabel batterySelectTitle = new JLabel("Model Selection");
		batterySelectTitle.setBounds(286, 26, 88, 14);
		batteryTypePanel.add(batterySelectTitle);
		batterySelectTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		batterySelectionsBox = new JComboBox<String>();
		batterySelectionsBox.setBounds(286, 45, 340, 22);
		initBatteryBoxWithValues(batterySelectionsBox);
		batteryTypePanel.add(batterySelectionsBox);
		batterySelectionsBox.addActionListener(this);
		
		// Option to enter a custom value for a batteries mAh rating
		JLabel customMAhTitle = new JLabel("Custom Value");
		customMAhTitle.setBounds(286, 74, 88, 14);
		batteryTypePanel.add(customMAhTitle);
		customMAhTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		customMAhField = new JFormattedTextField(NumberFormat.getNumberInstance());
		// Initialise the custom mAh text box with the first combo-box option
		customMAhField.setValue(new Double(
				((String) batterySelectionsBox.getSelectedItem()).
				split(",")[MAH_INDEX_IN_CSV].substring(1)));
		customMAhField.setBounds(371, 72, 60, 20);
		batteryTypePanel.add(customMAhField);
		customMAhField.setFont(new Font("Tahoma", Font.PLAIN, 12));
		customMAhField.addPropertyChangeListener("value",this);
		
		JLabel customMAhUnit = new JLabel("mAh");
		customMAhUnit.setBounds(438, 74, 46, 14);
		batteryTypePanel.add(customMAhUnit);
		customMAhUnit.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		return batteryTypePanel;
	}
	
	private JPanel initialiseDeratingPanel() {
		JPanel deratingPanel = new JPanel();
		deratingPanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		deratingPanel.setBackground(Color.WHITE);
		deratingPanel.setBounds(0, 307, 650, 65);
		deratingPanel.setLayout(null);
		
		JLabel deratingTitle = new JLabel("DERATING");
		deratingTitle.setBounds(10, 11, 76, 14);
		deratingPanel.add(deratingTitle);
		deratingTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		JLabel cycleDischargeTitle = new JLabel("Cycle Discharge Limit");
		cycleDischargeTitle.setBounds(56, 36, 144, 14);
		deratingPanel.add(cycleDischargeTitle);
		cycleDischargeTitle.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		cycleDischargeField = new JFormattedTextField(NumberFormat.getNumberInstance());
		cycleDischargeField.setValue(new Integer(10));
		cycleDischargeField.setBounds(210, 34, 30, 18);
		deratingPanel.add(cycleDischargeField);
		cycleDischargeField.setFont(new Font("Tahoma", Font.PLAIN, 12));
		cycleDischargeField.addPropertyChangeListener("value", this);
		
		JLabel percentLbl = new JLabel("%");
		percentLbl.setBounds(246, 34, 15, 16);
		deratingPanel.add(percentLbl);
		percentLbl.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		// Enable High Discharge
		enableHighDisBtn = new JRadioButton("Enable High Discharge Rate Compensastion");
		enableHighDisBtn.setBounds(359, 32, 268, 23);
		//deratingPanel.add(enableHighDisLbl);
		enableHighDisBtn.setBackground(Color.WHITE);
		enableHighDisBtn.setFont(new Font("Tahoma", Font.PLAIN, 12));
		
		
		return deratingPanel;
	}

	private void readInBatteries() {
		// intialise the list of battery objects
		primaryBatteries = new ArrayList<>();
		secondaryBatteries = new ArrayList<>();
		primaryBatteryStrs = new ArrayList<>();
		secondaryBatteryStrs = new ArrayList<>();
		
		// initialise variables required for reading in .csv file of batteries
		File file = new File("db/batteries.csv");
		String[] lines;
		Battery battery;
		try {
			lines = FileUtils.readFileToString(file).split("\n");
			
			for(int i=1; i < lines.length; i++) {
				battery = new Battery(lines[i]);
				if(battery.getBatteryType() == BatteryType.PRIMARY) {
					primaryBatteries.add(0, battery);
					primaryBatteryStrs.add(battery.getComboBoxString());
				} else {
					secondaryBatteries.add(battery);
					secondaryBatteryStrs.add(battery.getComboBoxString());
				}
				
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void initBatteryBoxWithValues(JComboBox<String> combo) {
		ArrayList<String> initSelections = new ArrayList<>();
		initSelections.addAll(primaryBatteryStrs);
		initSelections.addAll(secondaryBatteryStrs);
		batterySelectionsBox.setModel(new DefaultComboBoxModel<String>(
				initSelections.toArray(new String[initSelections.size()])));
	}
	
}

class Battery {
	/** Used to get the mAh rating for a battery located within the .csv */
	private final static int MAH_INDEX_IN_CSV = 3;
	
	private String name;
	private BatteryType type;
	private String typeStr;
	private String chemistry;
	private int mAh;
	
	
	public Battery(String csvLine) {
		String[] values = csvLine.split(",");
		
		name = values[0];
		typeStr = values[1];
		
		if(typeStr.equals("Primary"))
			type = BatteryType.PRIMARY;
		else
			type = BatteryType.SECONDARY;
		
		chemistry = values[2];
		mAh = Integer.parseInt(values[MAH_INDEX_IN_CSV].substring(0, 
				values[MAH_INDEX_IN_CSV].length() - 1));
	}
	
	public String getComboBoxString() {
		return name + ", " + typeStr + ", " + chemistry + ", " + mAh;
	}
	
	public BatteryType getBatteryType() {
		return type;
	}
	
}
