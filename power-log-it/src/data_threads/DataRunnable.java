package data_threads;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.MenuElement;

import model.ElectricalDataTypes;
import model.ObservableModelDetails;
import model.TimeDataPoint;

public abstract class DataRunnable implements Runnable, ActionListener, Observer {
	/** Lists of thread safe buffers for storing channel data */
	ArrayList<LinkedBlockingQueue<TimeDataPoint>> voltageBuffers;
	ArrayList<LinkedBlockingQueue<TimeDataPoint>> currentBuffers;
	ArrayList<LinkedBlockingQueue<TimeDataPoint>> powerBuffers;
	ArrayList<LinkedBlockingQueue<TimeDataPoint>> resistanceBuffers;
	ArrayList<LinkedBlockingQueue<TimeDataPoint>> powerBuffersCopy;
	ArrayList<LinkedBlockingQueue<TimeDataPoint>> temperatureBuffers;
	
	/** Device model number (null when no device is connected) */
	ObservableModelDetails obModelDetails;
	
	/** Used to disable the sampling rates which the device does NOT support */
	JRadioButtonMenuItem[] samplingButtons;
	
    /** Indicates whether data is being captured by thread */
    boolean captureData;
	
	public DataRunnable(ObservableModelDetails obModelDetails, JMenuBar menuBar) {
		
		// initialise the lists of channel data buffers
    	voltageBuffers = new ArrayList<>();
    	currentBuffers = new ArrayList<>();
    	powerBuffers = new ArrayList<>();
    	resistanceBuffers = new ArrayList<>();
    	powerBuffersCopy = new ArrayList<>();
    	temperatureBuffers = new ArrayList<>();
    	
    	// set the observable model details
    	this.obModelDetails = obModelDetails;
    	obModelDetails.addObserver(this);
    	
    	// Get the sampling rate menu options and add this class as a listener
		JMenu toolsMenu = menuBar.getMenu(1);
		JMenu samplingMenu = (JMenu) toolsMenu.getSubElements()[0].getSubElements()[2];
		
		MenuElement[] samplingButtons = samplingMenu.getSubElements()[0].getSubElements();
		this.samplingButtons = new JRadioButtonMenuItem[samplingButtons.length];
		
		for (int i=0; i < samplingButtons.length; i++) {
			JRadioButtonMenuItem currItem = (JRadioButtonMenuItem) samplingButtons[i];
			
			// add the current sampling rate button to the list 
			this.samplingButtons[i] = currItem;
			
			// register the serial thread as a listener for sampling events 
			currItem.addActionListener(this);
		} 
		
		// Initially threads should be capturing data
    	captureData = true;
	}
	
	@Override
	public void update(Observable o, Object arg) {
    	// remove the pre-existing channel buffers
		removeAllBuffers();
		
		// Create the channel buffers to store each channels data
		ObservableModelDetails obModelDetails = (ObservableModelDetails) o;
		String[] channelFormats = obModelDetails.getChannelFormats();
		
    	for(String channelFormat : channelFormats) {
    		if(channelFormat.equals("V"))
    			voltageBuffers.add(new LinkedBlockingQueue<TimeDataPoint>());
    		else if (channelFormat.equals("I"))
    			currentBuffers.add(new LinkedBlockingQueue<TimeDataPoint>());
    		else if (channelFormat.equals("T"))
    			temperatureBuffers.add(new LinkedBlockingQueue<TimeDataPoint>());
    	}
    	
    	if(voltageBuffers.size() != currentBuffers.size()) {
    		System.err.println("Number of Voltage and Current Buffers should be equal!");
    		System.exit(1);
    	}
    	
    	// Add the buffers associated with the current and voltage channels
    	for(int i=0; i<voltageBuffers.size(); i++) {
    		powerBuffers.add(new LinkedBlockingQueue<TimeDataPoint>());
    		resistanceBuffers.add(new LinkedBlockingQueue<TimeDataPoint>());
    		powerBuffersCopy.add(new LinkedBlockingQueue<TimeDataPoint>());
    	}
    	
    	//TODO Debugging
    	//System.out.println("Num voltage buffers: " + voltageBuffers.size());
    	//System.out.println("Num current buffers: " + currentBuffers.size());
    	//System.out.println();
	}
	
    private synchronized void removeAllBuffers() {
    	voltageBuffers.clear();
        currentBuffers.clear();
        powerBuffers.clear();
    	resistanceBuffers.clear();
    	powerBuffersCopy.clear();
        temperatureBuffers.clear();
    }
    
    /**
     * Drains and returns the points in the specified data buffer in ArrayList form.
     * @param elecDataType specifies the data buffer to be drained.
     * @return the drained data buffer
     */
    public ArrayList<ArrayList<TimeDataPoint>> drainDataBuffers(ElectricalDataTypes elecDataType) {
    	ArrayList<ArrayList<TimeDataPoint>> dataBufferCopies = new ArrayList<ArrayList<TimeDataPoint>>();
    	
    	if (elecDataType == ElectricalDataTypes.VOLTAGE)
    		for(LinkedBlockingQueue<TimeDataPoint> voltageBuffer : voltageBuffers) {
    			dataBufferCopies.add(new ArrayList<TimeDataPoint>());
    			voltageBuffer.drainTo(dataBufferCopies.get(dataBufferCopies.size()-1));
    		}
    	else if (elecDataType == ElectricalDataTypes.CURRENT)
    		for(LinkedBlockingQueue<TimeDataPoint> currentBuffer : currentBuffers) {
    			dataBufferCopies.add(new ArrayList<TimeDataPoint>());
    			currentBuffer.drainTo(dataBufferCopies.get(dataBufferCopies.size()-1));
    		}
    	else if(elecDataType == ElectricalDataTypes.POWER)
    		for(LinkedBlockingQueue<TimeDataPoint> powerBuffer : powerBuffers) {
    			dataBufferCopies.add(new ArrayList<TimeDataPoint>());
    			powerBuffer.drainTo(dataBufferCopies.get(dataBufferCopies.size()-1));
    		}
    	else if(elecDataType == ElectricalDataTypes.RESISTANCE)
    		for(LinkedBlockingQueue<TimeDataPoint> resistanceBuffer : resistanceBuffers) {
    			dataBufferCopies.add(new ArrayList<TimeDataPoint>());
    			resistanceBuffer.drainTo(dataBufferCopies.get(dataBufferCopies.size()-1));
    		}
    	else if(elecDataType == ElectricalDataTypes.ENERGY)
    		for(LinkedBlockingQueue<TimeDataPoint> powerBufferCopy : powerBuffersCopy) {
    			dataBufferCopies.add(new ArrayList<TimeDataPoint>());
    			powerBufferCopy.drainTo(dataBufferCopies.get(dataBufferCopies.size()-1));
    		}
    	else if(elecDataType == ElectricalDataTypes.TEMPERATURE)
    		for(LinkedBlockingQueue<TimeDataPoint> temperatureBuffer : temperatureBuffers) {
    			dataBufferCopies.add(new ArrayList<TimeDataPoint>());
    			temperatureBuffer.drainTo(dataBufferCopies.get(dataBufferCopies.size()-1));
    		}
    		
    	
    	return dataBufferCopies;
    }
    
    public int getSamplingPeriod() {
    	// Default sampling period is 100ms
    	int samplingPeriod = 100;
    
    	for(JRadioButtonMenuItem samplingButton : samplingButtons) {
    		if(samplingButton.isSelected()) {
    			String [] samplingButtonSelection = samplingButton.getText().split(" ");	
    				if(samplingButtonSelection[1].equals("ms")){
    				// sampling period specified in ms
    				samplingPeriod=Integer.parseInt(samplingButtonSelection[0]);
    			} else if (samplingButtonSelection[1].equals("sec")) {
    				// sampling period specifies in seconds
    				samplingPeriod=Integer.parseInt(samplingButtonSelection[0])*1000;
    			} else if (samplingButtonSelection[1].equals("min")) {
    				// sampling period specifies in minutes
    				samplingPeriod=Integer.parseInt(samplingButtonSelection[0])*1000*60;
    			} else if (samplingButtonSelection[1].equals("hr")) {
    				// sampling period specifies in hours
    				samplingPeriod=Integer.parseInt(samplingButtonSelection[0])*1000*60*60;
    			}
    			
    			break;
    		}
    	}
    	
    	return samplingPeriod;
    }
    
    /***
     * Used to clear data in the buffers when:
     * 		-User clicks the clear data button
     */
    public void clearData() {
    	clearDataInBuffer(voltageBuffers);
    	clearDataInBuffer(currentBuffers);
    	clearDataInBuffer(powerBuffers);
    	clearDataInBuffer(powerBuffersCopy);
    	clearDataInBuffer(resistanceBuffers);
    	clearDataInBuffer(temperatureBuffers);
    }
    
    private void clearDataInBuffer(ArrayList<LinkedBlockingQueue<TimeDataPoint>> buffer) {
    	for(LinkedBlockingQueue<TimeDataPoint> channelDataPoints : buffer) {
    		channelDataPoints.clear();
    	}
    }
    
    /* Abstract Methods */
    
    public abstract boolean isRunning();
    
    /* Handle a start or stop capture button click */
    abstract void handleCaptureButtonClick(JButton button);
    
    /* Handle a connection or disconnection combo-box event */
    abstract void handleComboBoxSelection(JComboBox<String> comboBox);
    
	
}
