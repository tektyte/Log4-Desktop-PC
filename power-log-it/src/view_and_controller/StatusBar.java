package view_and_controller;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import data_threads.SerialCommsRunnable;
import data_threads.SimulationRunnable;
import jssc.SerialPortList;
import model.ElectricalDataModel;
import model.ElectricalDataTypes;

@SuppressWarnings("serial")
public class StatusBar extends JPanel implements PopupMenuListener, ActionListener  {
	/** Used to clear data, and handle play, pause, stop button selection */
	private SerialCommsRunnable serialCommsRunner;
	private SimulationRunnable simulateRunner;
	
	/** Used for clearing data  */
	Map<ElectricalDataTypes,ElectricalDataModel> dataModels;
	
	/** Used for connecting to and from the slaved device*/
	private JComboBox<StringBuffer> connectComboBox;
	
	/** Used for selecting the baud rate of the slave device*/
	private JComboBox<String> baudComboBox;
	
	/** Used for changing the selected COM Port */
	private JComboBox<String> comComboBox;
	
	/** The action commands associated with the combo boxes */
	private static final String CONNECT_ACTION_STR = "connect";
	private static final String BAUD_ACTION_STR = "baud";
	private static final String COM_ACTION_STR = "com";
	
	/** The strings associate with the connected combo box */
	private static final StringBuffer[] CONNECT_STRING_BUFFERS = { new StringBuffer("Connecting"), new StringBuffer("Disconnect") };
	
	/** Capture button variable handling */
	private JButton startPauseCaptureBtn;
	private JButton stopCaptureBtn;
	private JButton clearCaptureBtn;
	
	/** Start-Pause Capture Button Images */
	private ImageIcon startImageIcon;
	private ImageIcon pauseImageIcon;
	
	/** Disabled when the user connects to the device via a combo-box.*/
	private JRadioButtonMenuItem simulateMenuButton; 
	
	public StatusBar(SerialCommsRunnable serialCommsRunner, SimulationRunnable simulateRunner,
			Map<ElectricalDataTypes, ElectricalDataModel> dataModels, JMenuBar menuBar) {
		/** Set up the layout and border of the status bar */
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBackground(Color.WHITE);
		setBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.BLACK));
		
		/* Store the runners for serial data and simulation data */
		this.serialCommsRunner = serialCommsRunner;
		this.simulateRunner = simulateRunner;
		
		/* Store the data model which is cleared of data on a clear data button press */
		this.dataModels = dataModels;

		// Connected status selection
		JLabel connectStatusLbl = new JLabel("Device Status:");
		
		// Order of the combo box strings matters!!
		connectComboBox = new JComboBox<StringBuffer>(CONNECT_STRING_BUFFERS);
		connectComboBox.setActionCommand(CONNECT_ACTION_STR);
		connectComboBox.setMaximumSize(connectComboBox.getPreferredSize());
		connectComboBox.addActionListener(simulateRunner);
		
		add(connectStatusLbl);
		add(connectComboBox);
		addSeperator();
		
		// Baud rate selection
		JLabel baudRateLbl = new JLabel("Baud Rate:");
		
		String[] baudStrings = {"921600", "460800","230400","115200", "57600", "38400","19200", "9600"};
		
		baudComboBox = new JComboBox<String>(baudStrings);
		baudComboBox.setActionCommand(BAUD_ACTION_STR);
		baudComboBox.setSelectedItem("115200");
		baudComboBox.setMaximumSize(baudComboBox.getPreferredSize());
		
		
		add(baudRateLbl);
		add(baudComboBox);
		addSeperator();
		
		// COM port selection
		JLabel comPortLabel = new JLabel("COM Port:");
		
		String[] comPortStrings = { "Auto" };
		comComboBox = new JComboBox<String>(comPortStrings);
		comComboBox.setActionCommand(COM_ACTION_STR);
		comComboBox.setMaximumSize(comComboBox.getPreferredSize());
		// can not change the COM port as the device is selected as connected
		comComboBox.setEnabled(false);
		
		//add(Box.createHorizontalGlue());
		add(comPortLabel);
		add(comComboBox);
		
		// Register the serial communications thread with the combo boxes
		serialCommsRunner.setComboBoxChoices(CONNECT_STRING_BUFFERS);
		serialCommsRunner.setConnectionComboBox(connectComboBox);
		serialCommsRunner.setBaudComboBox(baudComboBox);
		registerComboBoxListener(serialCommsRunner);
		
		/* Store the menu bar sampling mode enable button so it can be disabled after a 
		 * connection selection on the connect combo box */
		JMenu toolsMenu = menuBar.getMenu(1);
		simulateMenuButton = (JRadioButtonMenuItem) toolsMenu.getSubElements()[0].getSubElements()[4];
		
		comComboBox.addPopupMenuListener(this);
		
		
		// Add the start and stop capture buttons
		add(Box.createHorizontalGlue());
		
		startPauseCaptureBtn = new JButton();
		stopCaptureBtn = new JButton();
		
		// start capture button configuration
		startPauseCaptureBtn.setActionCommand("Start-Pause Capture");
		startPauseCaptureBtn.addActionListener(serialCommsRunner);
		startPauseCaptureBtn.addActionListener(simulateRunner);
		
		for(ElectricalDataModel dataModel : dataModels.values()) {
			startPauseCaptureBtn.addActionListener(dataModel);
		}
		startPauseCaptureBtn.addActionListener(this);
		readAndSetStartPauseButtonIcons();
		
		// stop capture button configuration
		stopCaptureBtn.setActionCommand("Stop Capture");
		stopCaptureBtn.addActionListener(serialCommsRunner);
		stopCaptureBtn.addActionListener(simulateRunner);
		for(ElectricalDataModel dataModel : dataModels.values()) {
			stopCaptureBtn.addActionListener(dataModel);
		}
		stopCaptureBtn.addActionListener(this);
		readAndSetStopImage(stopCaptureBtn, "stop");
		
		add(startPauseCaptureBtn);
		add(stopCaptureBtn);
		
		// add the clear capture button
		clearCaptureBtn = new JButton("Clear");
		clearCaptureBtn.setActionCommand("Clear Capture");
		clearCaptureBtn.addActionListener(this);
		
		add(clearCaptureBtn);
		
	}
	
	private void addSeperator() {
		// Separator between status bar elements
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		separator.setMaximumSize( new Dimension(1, Integer.MAX_VALUE) );
		
		add(Box.createHorizontalStrut(4));
		add(separator);
		add(Box.createHorizontalStrut(3));
	}
	
	private void readAndSetStartPauseButtonIcons() {
		try {
			Image img = ImageIO.read(StatusBar.class.getResourceAsStream("/assets/play.png"));
			startImageIcon = new ImageIcon(img);
			
			img = ImageIO.read(StatusBar.class.getResourceAsStream("/assets/pause.png"));
			pauseImageIcon = new ImageIcon(img);
			
			startPauseCaptureBtn.setMargin(new Insets(0, 0, 0, 0));
			startPauseCaptureBtn.setOpaque(true);
			startPauseCaptureBtn.setBorder(null);
			
			startPauseCaptureBtn.setIcon(pauseImageIcon);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void readAndSetStopImage(JButton button, String name) {
		name += ".png";
		try {			
			Image img = ImageIO.read(StatusBar.class.getResourceAsStream("/assets/"+name));
			
			button.setMargin(new Insets(0, 0, 0, 0));
			button.setOpaque(true);
			button.setBorder(null);
			
			button.setIcon(new ImageIcon(img));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void registerComboBoxListener(ActionListener listener) {
		for(ElectricalDataModel dataModel : dataModels.values()) {
			connectComboBox.addActionListener(dataModel);
			baudComboBox.addActionListener(dataModel);
		}
		
		connectComboBox.addActionListener(listener);
		baudComboBox.addActionListener(listener);
		comComboBox.addActionListener(listener);
		
		connectComboBox.addActionListener(this);
		
	}
	
	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
		// Update COM combo box with new selections
		String[] portIdentifiers = SerialPortList.getPortNames();;
		ArrayList<String> comOptions = new ArrayList<String>();
		
		for(int i=0; i<portIdentifiers.length; i++) {
			comOptions.add(portIdentifiers[i]);
		}
		
		comComboBox.removeAllItems();
		comComboBox.addItem("Auto");
		for(String option : comOptions) {
			comComboBox.addItem(option);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		// handle a connect combo-box selection
		if(event.getSource().equals(connectComboBox)) {
			if(connectComboBox.getSelectedItem().equals(CONNECT_STRING_BUFFERS[0])) {
				// connection
				if(checkUserConnectionConfirmation()) {
					/* Disable the COM port selection box */
					comComboBox.setEnabled(false);
					
					/* Enable the stop capture button */
					stopCaptureBtn.setEnabled(true);
					
					/* Ensure the simulation thread menu button is NOT selected */
					simulateMenuButton.setSelected(false);
					
					// clear the displayed data
					for(ElectricalDataModel dataModel : dataModels.values()) {
						//dataModel.clearData();
					}
					
					// update the capture buttons
					startPauseCaptureBtn.setEnabled(true);
					startPauseCaptureBtn.setIcon(pauseImageIcon);
					
					// change the strings in the connection ComboBox
					connectComboBoxes("Connecting...");
					disconnectComboBoxes("Disconnect");
				} else {
					// set disconnection option
					connectComboBox.setSelectedIndex(1);
				}
			} else {
				// disconnection
				comComboBox.setEnabled(true);
				
				//System.out.println(simulateRunner.isRunning());
				if(!simulateMenuButton.isSelected()) {
					/* If the simulation thread is NOT running, disable 
					 * the start and stop capture buttons */
					startPauseCaptureBtn.setEnabled(false);
					stopCaptureBtn.setEnabled(false);
				}
				// change the strings in the connection ComboBox
				connectComboBoxes("Connect");
				disconnectComboBoxes("Disconnecting...");
			}
		}
		
		// handle a capture button click
		else if(event.getSource().equals(startPauseCaptureBtn)) {
			stopCaptureBtn.setEnabled(true);
			if(startPauseCaptureBtn.getIcon().equals(startImageIcon))
				startPauseCaptureBtn.setIcon(pauseImageIcon);
			else 
				startPauseCaptureBtn.setIcon(startImageIcon);
		} else if(event.getSource().equals(stopCaptureBtn)) {
			startPauseCaptureBtn.setIcon(startImageIcon);
			startPauseCaptureBtn.setEnabled(true);
			stopCaptureBtn.setEnabled(false);
		}
		else if(event.getSource().equals(clearCaptureBtn)) {
			int n = JOptionPane.showConfirmDialog(
				getParent(), // the parent component
				"Are you sure you want to clear the data? All unsaved data will we lost", // message 
				"Clear Data?", 	// title of the window
				JOptionPane.YES_NO_OPTION); 			// 2 types of options
			
			if(n == JOptionPane.YES_OPTION) {
				for(ElectricalDataModel dataModel : dataModels.values()) {
					dataModel.clearData();
				}
				
				/* clear the data stored within the buffers */
				simulateRunner.clearData();
				serialCommsRunner.clearData();
			}

		}
		
	}
	
	private boolean checkUserConnectionConfirmation() {
		// check if there is stored graph data
		if(dataModels.get(ElectricalDataTypes.POWER).isEmpty()) {
			// no need to prompt the user as there is no data to loose
			return true;
		}
		
		// prompt the user as they are about to loose the .csv log display
		int n = JOptionPane.showConfirmDialog(
				getParent(), 						// the parent component
				"Are you sure you want to connect to the slaved device? All unsaved data will we lost.", 
				"Connect to device?", 				// title of the window
				JOptionPane.YES_NO_OPTION); // 2 types of options
		
		return n == JOptionPane.YES_OPTION;
	}
	
	/**
	 * Used for changing the combobox strings
	 * @param newString
	 */
    private void disconnectComboBoxes(String newString) {
    	CONNECT_STRING_BUFFERS[1].delete(0, CONNECT_STRING_BUFFERS[1].length());
    	CONNECT_STRING_BUFFERS[1].append(newString);
    }
    
    private void connectComboBoxes(String newString) {
    	CONNECT_STRING_BUFFERS[0].delete(0, CONNECT_STRING_BUFFERS[0].length());
    	CONNECT_STRING_BUFFERS[0].append(newString);
    }
	
	/* Getters and Setters */
	public void disconnectFromSlavedDevice() {
		// Fires the combo-box event and subsequent disconnection
		connectComboBox.setSelectedItem(connectComboBox.getItemAt(1));
	}
	
	/* Unused listener methods */
	
	@Override
	public void popupMenuCanceled(PopupMenuEvent e) { }

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
}
