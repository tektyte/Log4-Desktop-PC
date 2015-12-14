package view_and_controller;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.Observer;


import javax.swing.*;
import javax.swing.border.*;


import model.ObservableModelDetails;
import data_threads.SerialCommsRunnable;


@SuppressWarnings("serial")
public class DeviceDetailsWindow extends JFrame implements ActionListener,  Observer{
	/** The standard fonts used within this panel */
	private static final String STD_FONT_STR = "Planer";
	private JButton updateFirmwareBtn;
	private SerialCommsRunnable serialCommsRunner;
	private JLabel firmwareLbl;
	
	
	public DeviceDetailsWindow(ObservableModelDetails observableModelDetails, SerialCommsRunnable serialCommsRunner)   {
		this.serialCommsRunner = serialCommsRunner;
		observableModelDetails.addObserver(this);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 373, 350);
		
		JPanel window = new JPanel();
		window.setBackground(Color.WHITE);
		window.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(window);
		window.setLayout(new GridLayout(1, 0, 0, 0));
		
		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(Color.WHITE);
		window.add(mainPanel);
		mainPanel.setLayout(null);
		
		/** Used for updating the firmware */
			
		
		// Contains the Power-Log-IT series name
		JPanel titlePanel = new JPanel();
		titlePanel.setBorder(new MatteBorder(0, 0, 1, 0, (Color) Color.LIGHT_GRAY));
		titlePanel.setBackground(Color.WHITE);
		titlePanel.setBounds(0, 11, 374, 41);
		mainPanel.add(titlePanel);
		titlePanel.setLayout(null);
		Integer x=1;
		
		if (observableModelDetails.hasModelDetails()) {
			titlePanel.add(constructTitleLbl("Log4 Analyser"));
			createDeviceGUIComponents(observableModelDetails, mainPanel);
		} else {
			titlePanel.add(constructTitleLbl("No Device Connected"));
		}
		
		setVisible(true);
	}
	
	private JLabel constructTitleLbl(String titleStr) {
		JLabel title = new JLabel(titleStr);
		title.setBounds(71, 0, 217, 31);
		title.setHorizontalAlignment( SwingConstants.CENTER );
		title.setFont(new Font(STD_FONT_STR, Font.PLAIN, 25));
		
		return title;
	}
	
	private void createDeviceGUIComponents(ObservableModelDetails obModelDetails, JPanel mainPanel) {
		
		// Device details panel
		JPanel detailsPanel = new JPanel();
		detailsPanel.setBorder(null);
		detailsPanel.setBackground(Color.WHITE);
		detailsPanel.setBounds(0, 63, 374, 300);
		mainPanel.add(detailsPanel);
		detailsPanel.setLayout(null);
		
		JLabel deviceDetailsTitle = new JLabel("Device Details");
		deviceDetailsTitle.setBounds(10, 0, 105, 22);
		detailsPanel.add(deviceDetailsTitle);
		deviceDetailsTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		
		// The name of this specific device
		JLabel deviceNameTitle = new JLabel("Name:");
		deviceNameTitle.setBounds(20, 26, 125, 22);
		detailsPanel.add(deviceNameTitle);
		deviceNameTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		
		JLabel deviceNameLbl = new JLabel("Power-Log-IT! " + obModelDetails.getName());
		deviceNameLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		deviceNameLbl.setBounds(200, 26, 150, 22);
		detailsPanel.add(deviceNameLbl);
		
		// The model number of this device
		JLabel modelNumberTitle = new JLabel("Model Number:");
		modelNumberTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		modelNumberTitle.setBounds(20, 54, 125, 22);
		detailsPanel.add(modelNumberTitle);
		
		JLabel modelNumLbl = new JLabel("" + obModelDetails.getModelNumber());
		modelNumLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		modelNumLbl.setBounds(200, 54, 150, 23);
		detailsPanel.add(modelNumLbl);
		
		// The firmware version of this device
		JLabel firmwareTitle = new JLabel("Firmware:");
		firmwareTitle.setBounds(20, 80, 125, 22);
		detailsPanel.add(firmwareTitle);
		firmwareTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		
		firmwareLbl = new JLabel("" + obModelDetails.getFirmwareVersion());
		firmwareLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		firmwareLbl.setBounds(200, 80, 150, 22);
		detailsPanel.add(firmwareLbl);
		
		// The maximum baud rate of this device
		JLabel maxBaudTitle = new JLabel("Max Baud Rate:");
		maxBaudTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		maxBaudTitle.setBounds(20, 110, 125, 22);
		detailsPanel.add(maxBaudTitle);
		
		JLabel maxBaudLbl = new JLabel(obModelDetails.getMaxBaudRate() + " bps");
		maxBaudLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		maxBaudLbl.setBounds(200, 110, 150, 22);
		detailsPanel.add(maxBaudLbl);
		
		// The device's maximum sampling rate
		JLabel maxSampleRateTitle = new JLabel("Max Sampling Rate");
		maxSampleRateTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		maxSampleRateTitle.setBounds(20, 139, 125, 22);
		detailsPanel.add(maxSampleRateTitle);
		
		JLabel maxSampleRateLbl = new JLabel(obModelDetails.getMaxSamplingRate() + " sample/second");
		maxSampleRateLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		maxSampleRateLbl.setBounds(200, 139, 150, 22);
		detailsPanel.add(maxSampleRateLbl);
		
		// The device's data channels
		JLabel dataChannelsTitle = new JLabel("Data Channels:");
		dataChannelsTitle.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		dataChannelsTitle.setBounds(20, 169, 125, 22);
		detailsPanel.add(dataChannelsTitle);
		
		String dataChannelStr = "";
		for(String abbrev : obModelDetails.getChannelFormats()) {
			if(abbrev.equals("V")) {
				dataChannelStr += "Voltage, ";
			} else if (abbrev.equals("I")) {
				dataChannelStr += "Current, ";
			} else if (abbrev.equals("T")) {
				dataChannelStr += "Temperature, ";
			}
		}
		
		JLabel dataChannelLbl = new JLabel(dataChannelStr.substring(0, dataChannelStr.length() - 2));
		dataChannelLbl.setFont(new Font(STD_FONT_STR, Font.PLAIN, 14));
		dataChannelLbl.setBounds(200, 169, 150, 22);
		detailsPanel.add(dataChannelLbl);
		
		// button for updating firmware on device
		updateFirmwareBtn = new JButton("Update Firmware");
		updateFirmwareBtn.setActionCommand("Update Firmware");
		updateFirmwareBtn.addActionListener(this);
		updateFirmwareBtn.setBounds(70, 200, 200, 30);
		detailsPanel.add(updateFirmwareBtn);
		
	}
	
	public void actionPerformed(ActionEvent event){
		if(event.getSource().equals(updateFirmwareBtn)){
			updateFirmwareBtn.setText("Updating..."); 
			
//			try {
//				Runtime rt = Runtime.getRuntime();
//				Process pr = rt.exec("ipconfig");
//				BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//				 
//                String line=null;
// 
//                while((line=input.readLine()) != null) {
//                    System.out.println(line);
//                }
// 
//                int exitVal = pr.waitFor();
//                System.out.println("Exited with error code "+exitVal);
//			} catch (IOException e) {
//				System.out.println("File not found");
//			} catch (InterruptedException e) {
//				System.out.println("Process Error");
//			}
//			serialCommsRunner.getID(); //request ID and firmware version from device
			updateFirmwareBtn.setText("Firmware Up To Date");
			
		}
	}
	
	public void update(Observable o, Object arg) {
		ObservableModelDetails obModelDetails = (ObservableModelDetails) o;
		firmwareLbl.setText("" +  obModelDetails.getFirmwareVersion()); //Update firmware version label
	}
	
}

