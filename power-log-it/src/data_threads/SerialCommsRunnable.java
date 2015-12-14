package data_threads;

import jssc.*;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

import model.ObservableModelDetails;
import model.TimeDataPoint;

import org.jfree.data.time.Millisecond;


public class SerialCommsRunnable extends DataRunnable implements SerialPortEventListener {
	/** The thread the searches for a serial connection */
	private Thread connectionThread;
	
	/** Locks for the communication, closing and opening on serial port */
	private static final Object CLOSE_LOCK = new Object();
	private static final Object INITIALISE_LOCK = new Object();
	private static final Object COMMUNICATION_LOCK = new Object();
	
	/** Commands for in-line power monitor */
	private static final byte CMD_ERROR = 0x00;
	private static final byte GET_ID = 0x01;
	private static final byte KEEP_ALIVE_CMD = 0x02;
	private static final byte GET_CHANNELS = 0x03;
	private static final byte SET_BAUD_RATE = 0x04;
	private static final byte GET_BAUD_RATES = 0x05;
	private static final byte SET_SAMPLING = 0x06;
	private static final byte GET_SAMPLING = 0x07;
	private static final byte SET_DATE_TIME =  0x08;
	private static final byte GET_DATE_TIME =  0x09;
	private static final byte SET_SLEEP =  0x0A;
	private static final int SLAVE_DATA = 0x0B;
	private static final byte GET_FRAMEBUFFER =  0x0C;
	private static final byte FORCE_EVENT =  0x0D;
	
	/** Generic Error Codes*/
	private static final byte ERR_INVALID_DATA = 0x01;
	private static final byte ERR_INVALID_CMD = 0x02;
	private static final byte ERR_TIME_OUT = 0x03;
	private static final byte ERR_INVALID_COUNT = 0x04;
	private static final byte ERR_BUSY = 0x05;
	private static final byte ERR_INVALID_CRC = 0x06;
	private static final byte ERR_PACKET_TOO_LARGE = 0x07;
	private static final byte ERR_SLAVE_DEBUG_MSG = 0x08;
	
	/** Specific Error Codes*/
	private static final byte ERR_INVALID_CHAN = 0x21;
	private static final byte ERR_INVALID_SAMPLE_RATE = 0x22;
	
	
	/** Object used for serial communication */
	private SerialPort serialPort;
    
    /** Buffered input stream from the port */
    private InputStream input;
    
    //TODO port time out should not be hard coded (Milliseconds)
    private static final int PORT_TIME_OUT = 5000;
    
    //TODO Data time out should NOT be hard coded (Milliseconds)
    private static final int DATA_TIME_OUT = 2000;
    
    // TODO KEEP_ALIVE_TIME_OUT should not be hard coded (Milliseconds)
    private static final int KEEP_ALIVE_TIME_OUT = 1600;
    
    /** Default bits per second for COM port. */
    private int dataRate = 115200;
    
    /** The input data buffer. */
    private String inputBuffer="";
 
    /** A list of the connected COM PORTs */
    private String[] portNames;
    
    /** Thread used for executing serial comms timer events */
    private ScheduledExecutorService ex;
    
    /** Handler used for terminating a scheduled timer */
    private ScheduledFuture<?> portTimeoutTask;
    private ScheduledFuture<?> dataTimeoutTask;
    private ScheduledFuture<?> keepAliveTimeoutTask;
    
    /** Connection combo-box */
    private JComboBox<StringBuffer> connectComboBox;
    
    /** Baud combo-box */
    private JComboBox<String> baudComboBox;
    
    /** Indicates status of the connection selected by the user.
     * True: connected selected, False: disconnected selected */
    private boolean connectedSelected;
    
	/** Boolean indicates whether the connection or serial communications 
	 * thread is running */
	private boolean running;
    
    /** Used to determine which combo box fired the event */
	private static final String CONNECT_ACTION_STR = "connect";
	private static final String BAUD_ACTION_STR = "baud";
	private static final String COM_ACTION_STR = "com";
	
	private StringBuffer[] connectStringBuffers;
    
    //TODO debugging purposes
    int droppedBytes;
    long startTime;
    
    //TODO remove hard coded COM PORT
    private String portString = "COM18";
    
    /**
     * Constructor for the thread concerned with reading the serial communications.
     * @param dataModels the dataModels that have to be updated with the incoming serial communications.
     */
    public SerialCommsRunnable(ObservableModelDetails obModelDetails, JMenuBar menuBar) {
    	// initially the GUI is in simulation mode
    	super(obModelDetails, menuBar);
    	
    	// Initialise the timer thread
    	ex = Executors.newSingleThreadScheduledExecutor();
    	
    	// Start looking for a connection by default
//    	connectionThread = new Thread(this);
//    	connectionThread.start();
    }
    
    @Override
    /***
     * Method is called when the user wants to connect to the slaved device
     */
	public void run() {
    	// connection thread is running
    	running = true;
    	
		// No COM ports have been enumerated yet 
		portNames = null;
		
		// Initially the connected option is selected on the combo-box. 
		connectedSelected = true;
		
		initialize();
		
		
		
		
        
        
	}
    
    /**
     * Set up the connection between the usb power monitor and this process.
     */
    private void initialize() {
    	// ensure that only one thread can ever call the initialize method
    	synchronized (INITIALISE_LOCK) {
        	// we have not yet found the connected device's serial port
        	serialPort = null;
        	
        	// default is to capture data being streamed
        	captureData = true;
        	
        	obModelDetails.changeModel(0);
			obModelDetails.setFirmwareVersion(0);
			
            // iterate through, looking for the port
    	    while (serialPort == null && !Thread.interrupted()) {
    	    	portNames = SerialPortList.getPortNames();
    	    		// Check whether the child is on this port
    	    		//TODO remove hard coded COM PORT/ Done, how do we deal with conntecting to a device that isn't a Log4?
            		for(int currPortId=0;currPortId<portNames.length;currPortId++){
            			// restart the port timer
            			restartPortTimeout();
    	            
            		
            			System.out.println("Found " + portNames[currPortId]);
            			// TODO read the device model number and use it here to get JSON device details
            			serialPort=openConnection(portNames[currPortId]);
            			
            			
            			getID();
            		
            		
    	            if(serialPort != null)
    	            	break;
    	            
    	        }
    	    	
    	    	try {
    				Thread.sleep(100);
    			} catch (InterruptedException e) {
    				//e.printStackTrace();
    				System.out.println("Connection thread sleep interrupted");
    				return;
    			}
    	    	
    	    	System.out.println("Looking for serial port");
    	    	
    	    }
    	    
    	    if (serialPort == null) {
    	    	System.out.println("Connection thread interrupt");
    	    	return;
    	    }
    	  //get compatible device baudrates
            //getBaudRates();
    	    
    	    
    	    // restart all timer tasks
    	    restartPortTimeout();
    	    restartDataTimeout();
    	    restartKeepaliveTimeout();
    	    
            // add event listeners for detecting serial data available
            try {
            	int mask=SerialPort.MASK_RXCHAR;
    			serialPort.addEventListener(this);
    			serialPort.setEventsMask(mask);
    		} catch (Exception e){
    			e.printStackTrace();
    		}
            
            
            
         // change the combo-box string to connected
            connectComboBoxes("Connected");
            System.out.println("Child found");
            
          //set device date to current system time
    	    setDate();

		}
    }
    
    private SerialPort openConnection(String portName) {
    	SerialPort serialPort = new SerialPort(portName);
    	
        try {
            // open serial port
        	serialPort.openPort();
            // set port parameters
            serialPort.setParams(dataRate, // baud rate
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            
            // check if the device responded appropriately, therefore is the slave
            if (queryChild())
            	return serialPort;

        	serialPort.closePort();
            return null;
        } catch (Exception e) {
            System.err.println("Failed to connect to COM PORT: " + portName);
            return null;
        }
        
    }
    
    private boolean queryChild() {
    	//sendCommand(GET_CMD, null);
//    	int childID = childResponded();
//    	if (childID >= 0) {
//    		obModelDetails.changeModel(childID);
//    		return true;
//    	}
//    	return false;
    	return true;
    }
    
    /**
     * Method reads the response of the child and checks to see whether it is the
     * desired slave based on the response.
     * @return -1 if the connected device is not from the Power-Log-IT series,
     * else return the model number of the connected device
     */
    private int childResponded() throws SerialPortException {
    	byte[] childIDBytes = new byte[4];
        
    		try {
				if(!readHeader() || GET_ID != input.read())
					return -1;
				 
		    	// get the number of available data bytes at the input 
		    	int available = input.read();
		    	for(int i=0;i<available;i++)
		    		childIDBytes[i] = (byte) input.read();
		        
		    	int childID = ByteBuffer.wrap(childIDBytes).getInt();
		    	
		    	// Read and IGNORE checksum
		    	input.read();
		    	input.read();
		    	input.read();
		    	input.read();
		    	
		    	// Read terminating character
		    	System.out.println(childID);
		    	if(input.read() != '\n' || childID <=0)
		    		return -1;
		    	
		    	return childID;
    		} catch (IOException e) {
				e.printStackTrace();
				return -1;
			} 
    }

    /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public void close() {
    	synchronized (CLOSE_LOCK) {
    		// thread is no longer running
    		running = false;
    		
    		// if the connection thread is searching for a connection interrupt it
    		if (connectionThread != null && connectionThread.isAlive()) {
    			connectionThread.interrupt();
    			System.out.println("Connection interrupt close");
    		}
    		
        	// close the connection with the device and stop the timers
    		// cancel the queued up port, data and keep-alive events
    		cancelAndRemoveDataTask(true);
    		cancelAndRemoveKeepAliveTask(true);
    		cancelAndRemovePortTask(true);
    			
        	// change the connection status on the combo-box from 'Disconnecting' 
    		// to 'Disconnected' and make sure it is selected
        	disconnectComboBoxes("Disconnected");
            
        	// remove the serial port event listeners and close the port
        	if (serialPort != null) {
                try{
                serialPort.removeEventListener();
                serialPort.closePort();
                serialPort = null;
                } catch (Exception e){
        			e.printStackTrace();
        		}
            }
		}
    }

    /**
     * Method sends the specified command and data to the child in the correct format.
     */
    private void sendCommand(byte cmd, byte[] data){        
    	// Only one thread can read or write to the serial port
    	// at one time
    	synchronized (COMMUNICATION_LOCK) {
        	// the header of the message to send
        	byte[] header = new byte[] {
            		(byte) ':', // Initialising character
            		(byte) 0x00 // Device Address (unused)
            	};
            
        	// the footer of the message to send
            byte[] footer = new byte[] {
            		(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, // Checksum
            		(byte) '\n'   										// Terminating Character
            	};
            
            // the message to be sent
            byte[] bytes_to_send;
            
            // intialise the size of the message to be sent
            if(data!=null)
            	// 2 bytes: storing the data length and the cmd byte
            	bytes_to_send = new byte[header.length + 2 + data.length + footer.length];
            else 
            	bytes_to_send = new byte[header.length + 2 + footer.length];
            
            int i=0;
            // add the header
            for(; i<header.length; i++)
            	bytes_to_send[i] = header[i];
            
            // add the cmd
            bytes_to_send[i] = cmd;
            System.out.println("snd " + cmd);
            
            // add the data length
            i++;
            if(data!=null)
            	bytes_to_send[i] = (byte) data.length;
            else
            	bytes_to_send[i] = (byte) 0x00;
            
            // add the data
            i++;
            if (data != null) {	
            	for(int j=0; j < data.length; i++, j++) {
            		bytes_to_send[i] = data[j];
            		
            	}
            }
            
            // add the footer
            for(int j=0; j < footer.length; i++, j++) {
            	bytes_to_send[i] = footer[j];
            }
            
        	try {						
                //output.write(bytes_to_send);//write it to the serial
                //output.flush();				//refresh the serial
        		serialPort.writeBytes(bytes_to_send);
                //for (int j=0; j<bytes_to_send.length;j++)
                	//System.out.println(bytes_to_send[j]);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
		}
    }
 
    /**
     * This Method is called when serial data is received.
     * 
     * Currently only handles the SLAVE_DATA command, all other commands are discarded.
     */
    public void serialEvent(SerialPortEvent oEvent) {
    	// Only one thread can read or write to the serial port
    	// at one time
    	synchronized (COMMUNICATION_LOCK) {
//        	if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
        	if (oEvent.isRXCHAR()) {
        		// restart the port timer as the connection is still alive
        		restartPortTimeout();
        		
        		try {            	
                	// get the number of available data bytes at the input 
                	int available = oEvent.getEventValue();
                	if(captureData) {
                		for(int i=0; i < available; i += readInput(available-i)) {
                			// restart the timer as the connection is still active
                			restartDataTimeout();
                			restartPortTimeout();
                		}
                	} else {
                		//flushEntireInputBuffer(available);
                		serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
                	}
                } catch (Exception e) {
                	//TODO implement fix when device is unplugged
                    System.err.println(e.toString());
                }
            }
		}
    }
    
    
    /**
     * Reads a single DATA_SLAVE command. This command contains a 
     * time-stamp, voltage and current reading.
     * @return the number of bytes read
     * @throws IOException if it fails to read the serial data
     */
    private int readInput(int available) throws SerialPortException {
    	int numDataBytes;
    	
    	// Read the header and the command type
    	if (!readHeader()) {
    		droppedBytes++;
    		System.out.println("Failed to Read Header " + droppedBytes + " " 
    				+ (System.currentTimeMillis()-startTime)/1e3);
    		return 2 + available;
    	}
    	byte[] buffer=serialPort.readBytes(2);
    	int command = buffer[0];
    	System.out.println("rcv " + command);
//		 read the number of data bytes to be read
    	numDataBytes = buffer[1];
    	switch (command) {
    	case CMD_ERROR:
    		fillInputBuffer(numDataBytes);
    		buffer= serialPort.readBytes(1);
    		if((char) buffer[0] == '\n') {
    			byte error=inputBuffer.getBytes()[0];
    			errorHandler(error);
    			if (numDataBytes>1)
    				System.err.println(inputBuffer.substring(1));
        	}
    		// reset the inputBuffer
        	inputBuffer = "";
    		return 9 + numDataBytes;
    		
    	case SLAVE_DATA: 
    		fillInputBuffer(numDataBytes);
        	
        	// read the terminating character
    		buffer = serialPort.readBytes(1);
    		
    		if((char) buffer[0] == '\n') {
        		
        		writeBufferToDataModels();
        		
        	}
        	
        	// reset the inputBuffer
        	inputBuffer = "";
        	
        	return 9 + numDataBytes;
        	
    	case GET_ID:
        	byte[] idBuffer=serialPort.readBytes(numDataBytes);
    		// read the checksum and terminating character
        	buffer = serialPort.readBytes(5);
    		if((char) buffer[4] == '\n') {	
    			ByteBuffer temp = ByteBuffer.wrap(idBuffer);
    			temp.order(ByteOrder.LITTLE_ENDIAN);
        		int model=temp.getInt();
//        		int version=temp.getInt(4);
        		obModelDetails.changeModel(model);
        		obModelDetails.setFirmwareVersion(model);
        	}
    		return 9 + numDataBytes;
    		
    		//TODO change the below case to GET_BAUD_RATES when the firmware is fixed
    	case 0x03:
    		
    		fillInputBuffer(numDataBytes);
    		buffer = serialPort.readBytes(1);
    		if((char) buffer[0] == '\n') {	
	        	String[] baudRates = new String[numDataBytes];
	        	baudRates = inputBuffer.split(",");
	        	int[] baud = new int[numDataBytes];
//	        	int count = baudComboBox.getItemCount();
	        	for (int i=0; i<baudRates.length; i++){
	        		baudRates[i]=baudRates[i].trim();
	        		baud[i] = Integer.parseInt(baudRates[i]);
	        		System.out.println(baud[i]);
//		        	baudComboBox.addItem(baudRates[i]);
	        	}
	        	baudComboBox = new JComboBox<String>(baudRates);
//	        	for (int j=count-1; j>=0; j--){
//	        		baudComboBox.removeItemAt(j);
//	        		System.out.println(j);
//	        	}
///	        	baudComboBox.setSelectedItem(Integer.toString(dataRate));
        	}
        	// reset the inputBuffer
        	inputBuffer = "";
        	return 9 + numDataBytes;
    		
    	case KEEP_ALIVE_CMD:
    		serialPort.readBytes(5);
    		return 9;
    	
    	default:
    		// reset the inputBuffer
        	inputBuffer = "";
        	System.out.println("help");
    		return 4 + available;
    	}
    }
    
    private void fillInputBuffer(int numDataBytes) throws SerialPortException {
    	byte[] buffer=serialPort.readBytes(numDataBytes);
    	for(int i=0; i < numDataBytes; i++)
    		inputBuffer += (char) buffer[i];
    	//read and ignore the checksum
    	serialPort.readBytes(4);
    }
    
    /**
     * Called when a DATA_SLAVE command has been successfully read.
     * 
     * Assumes that if the device has 4 channels it is sending 4 channels
     * of data!!!
     */
    @SuppressWarnings("deprecation")
	private void writeBufferToDataModels() {
    	// Contains the values for each channel in order
    	String[] dataStrings;
    	
    	// The system time for when this value was received
    	Millisecond time;
    	
    	// Arrays of voltage and current values for one point in time (multiple channels)
    	ArrayList<Double> newVoltages = new ArrayList<>();
    	ArrayList<Double> newCurrents = new ArrayList<>();
    	
    	try {
    		// used to associate electrical data type to a value
    		String[] channelFormats = obModelDetails.getChannelFormats();
    		// data strings time stamps (2), channel data (X)
    		dataStrings = inputBuffer.split(",");
    		
    		// use system time for time value of data point
    		//time = new Millisecond(new Date());
    		
    		// use time stamp for time value
    		String[] currDate=dataStrings[0].split("-");
    		String[] currTime=dataStrings[1].split(":");
    		int year=Integer.parseInt(currDate[0]);
    		int month=Integer.parseInt(currDate[1]);
    		int day=Integer.parseInt(currDate[2]);
    		int hrs=Integer.parseInt(currTime[0]);
    		int min=Integer.parseInt(currTime[1]);
    		int sec=Integer.parseInt(currTime[2]);
    		
    		time = new Millisecond(new Date(year,month,day,hrs,min,sec) );
    		// start i=2 because first 2 values are time stamps
    		int offset = 2;
    		for(int i=offset ; i < channelFormats.length + offset; i++) {
    			
    			double data = Double.parseDouble(dataStrings[i]);
    			
    			if(channelFormats[i-offset].equals("V")) {
    				// handle addition of voltage data point
    				newVoltages.add(data / 1e3);
    			} else if (channelFormats[i-offset].equals("I")) {
    				// handle addition of current data point
    				newCurrents.add(data / 1e6);
    			} else if (channelFormats[i-offset].equals("T")) {
    				// handle addition of temperature data point
    				addDataPointToBuffer(temperatureBuffers, new TimeDataPoint(time, data));
    			}
    		}
	    	
    		// Check to ensure the voltage and current buffer sizes are the same.
    		if(newVoltages.size() != newCurrents.size() && newVoltages.size() == voltageBuffers.size()) {
    			System.err.println("Failed to read the same number of voltage and current points");
    			System.exit(1);
    		}
    		
    		for(int i=0; i<newVoltages.size(); i++) {
    			double voltage = newVoltages.get(i);
    			double current = newCurrents.get(i);
    			
    			voltageBuffers.get(i).offer(new TimeDataPoint(time, voltage));
    			currentBuffers.get(i).offer(new TimeDataPoint(time, current));
    			powerBuffers.get(i).offer(new TimeDataPoint(time, Math.abs(voltage*current)));
    			powerBuffersCopy.get(i).offer(new TimeDataPoint(time, Math.abs(voltage*current)));
    			if(current !=0){
    			resistanceBuffers.get(i).offer(new TimeDataPoint(time, Math.abs(voltage/current)));
    		 	}
    			else{
    				resistanceBuffers.get(i).offer(new TimeDataPoint(time, 0));
    			}
    		}
    	
    	} catch (NumberFormatException e) {
    		//System.err.println("Failed to parse current and/or voltage.");
    		//e.printStackTrace();
    	} catch (ArrayIndexOutOfBoundsException e) {
    		//System.err.println("Serial input is NOT correct length.");
    	} catch (NullPointerException e){
    		e.printStackTrace();
    	}
    }
    
    /**
     * Adds a data point to the channel buffers in order.
     * Assumes that data is always being received for all channels!!
     * 
     * @param channelBuffers contains a list of channel buffers
     * @param dataPoint the data point to be added
     */
    private void addDataPointToBuffer(ArrayList<LinkedBlockingQueue<TimeDataPoint>> channelBuffers,
    		TimeDataPoint dataPoint) {
    	int i;
		for(i=0; i < channelBuffers.size(); i++)
			if(i == channelBuffers.get(i).size() - 1 ||
					channelBuffers.get(i).size() == channelBuffers.get(i+1).size()) {
				channelBuffers.get(i).offer(dataPoint);
				break;
			}
    }
    
    /*
     * 
     * @param dmy has format yyyy-mm-dd
     * @param hms has format hh:mm:ss
     * @return
     
    private synchronized Second parseTimeStamp(String dmy, String hms) {
    	int year, month, day;
    	int hour, minute, second;
    	
    	year = Integer.parseInt(dmy.split("-")[0]);
    	month = Integer.parseInt(dmy.split("-")[1]);
    	day = Integer.parseInt(dmy.split("-")[2]);
    	
    	hour = Integer.parseInt(hms.split(":")[0]);
    	minute = Integer.parseInt(hms.split(":")[1]);
    	second = Integer.parseInt(hms.split(":")[2]);
    	
    	return new Second(second,minute,hour,day,month,year);
    } */
    
    /**
     * The header is standard for all devices.
     * @return
     * @throws IOException
     */
    private boolean readHeader() throws SerialPortException {
    	byte[] buffer=null;
		buffer = serialPort.readBytes(2);
    	int startByte = buffer[0];
    	int addrByte = buffer[1];
    	
    	//System.out.println((char) startByte + " " + (char) addrByte);
    	return ':' == startByte && addrByte == 1;
    }
    
    
    public void flushInputBuffer() {
    	try {
			//flushInputBuffer(input.available());
    		flushEntireInputBuffer();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * 
     * @param available the number of bytes available at the serial as of last check
     * @return the number of bytes flushed from the input buffer.
     */
    private int flushInputBuffer(int available) {
    	synchronized (COMMUNICATION_LOCK) {
        	int count = 0;
        	try {
    			while(available>0 && input.read()!='\n') {
    				available--;
    				count++;
    			}
    		} catch (IOException e) {
    			System.err.println("Failed to flush input buffer.");
    		}
        	
        	return count;
		}
    }
    
    private void flushEntireInputBuffer() throws SerialPortException {
    	serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
    }
    
    /**
     * getID requests the ID and firmware version of the connected device
     */
    
    public void getID(){
    	sendCommand((byte) GET_ID,null);
    }
    
    /** 
     * errorHandler responds to errors that are received from the connected device
     */
    
    private void errorHandler(byte error){
    	switch (error){
    	case ERR_INVALID_DATA:
    		System.err.println("The previous packet contained invalid data for the command");
    		break;
    	case ERR_INVALID_CMD:
    		System.err.println("The previous command  was not recognized as valid");
    		break;
    	case ERR_TIME_OUT:
    		System.err.println("The previous packet took too long to complete");
    		break;
    	case ERR_INVALID_COUNT:
    		System.err.println("The data count did not match the amount of data bytes received");
    		break;
    	case ERR_BUSY:
    		System.err.println("Unable to process request at the moment");
    		break;
    	case ERR_INVALID_CRC:
    		System.err.println("The CRC did not match the data");
    		break;
    	case ERR_PACKET_TOO_LARGE:
    		System.err.println("The last received packet was too large to buffer");
    		break;
    	case ERR_SLAVE_DEBUG_MSG:
    		System.err.println("The slave encountered internal error");
    		break;
    	case ERR_INVALID_CHAN:
    		System.err.println("The channel you are addressing doesn�t exist on this device");
    		break;
    	case ERR_INVALID_SAMPLE_RATE:
    		System.err.println("The sample rate was unable to be selected");
    		break;
    	}
    }
    
    /**
     * setDate sets the date and time of the connected device to the system date and time
     */
    public void setDate(){
    	byte [] date_array = new byte[7];
    	date_array[1]=(byte) ((Calendar.getInstance().get(Calendar.YEAR))>>8);
    	date_array[0]=(byte) (Calendar.getInstance().get(Calendar.YEAR));
    	date_array[2]=(byte) (Calendar.getInstance().get(Calendar.MONTH)+1);
    	date_array[3]=(byte) (Calendar.getInstance().get(Calendar.DATE));
    	date_array[4]=(byte) (Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    	date_array[5]=(byte) (Calendar.getInstance().get(Calendar.MINUTE));
    	date_array[6]=(byte) (Calendar.getInstance().get(Calendar.SECOND));
    	sendCommand((byte) SET_DATE_TIME,date_array);
    	
    }
    
    /**
     * getBaudRates requests the connected device to list all of its compatible data rates
     */
    public void getBaudRates(){
    	sendCommand( (byte) GET_BAUD_RATES,null);
    }
    
    /**
     * getBaudRates requests the connected device to list all of its compatible data rates
     */
    public void getSampling(){
    	sendCommand( (byte) GET_SAMPLING,null);
    }
    
    /* Listener methods */
    
	/**
	 * Event fires when the timer times out or a drop down box selection is made.
	 */
    @Override
	public void actionPerformed(ActionEvent event) {

    	if (event.getSource() instanceof JComboBox<?>) {
    		handleComboBoxSelection((JComboBox<String>) event.getSource());
    	}
    	// check if display start/stop capture has been triggered
    	else if (event.getSource() instanceof JButton) {
    		handleCaptureButtonClick((JButton) event.getSource());
    	} else if (event.getSource() instanceof JRadioButtonMenuItem) {
    		// sampling rate menu option was selected
    		JRadioButtonMenuItem buttonSelected = (JRadioButtonMenuItem) event.getSource();
    		
    		// ensure the button selected is selected (do NOT allow de-selections)
    		buttonSelected.setSelected(true);
    		
    		// check the other sampling rate buttons
    		boolean samplingChange = false;
    		for(int i=0; i<samplingButtons.length; i++) {
    			if(samplingButtons[i].isSelected() && !buttonSelected.equals(samplingButtons[i])) {
    				samplingButtons[i].setSelected(false);
    				samplingChange = true;
    			}
    		}
    		
    		if(samplingChange) {
    			// array of data bytes
    			byte[] data = new byte[14];
    			
    			// sampling period (in ms)
    			int samplingPeriod = getSamplingPeriod();
    			
    			// Continuous sampling
    			data[0] = 0x01;
    			
    			// Sampling rate
    			for(int i=4, j=1 ; i>=1 ; i--, j++) {
    				data[i] = (byte)(samplingPeriod >>> 8*(i-1)); 
    				
    			}
    			
    			System.out.println(data[1] + " " + data[2] + " " + data[3] + " " + data[4] );
    			// Alarm type (no alarm)
    			data[5] = 0x00;
    			
    			// Number of samples sent after alarm triggered (does NOT matter)
    			data[6] = 0x00; data[7] = 0x00;
    			data[8] = 0x00; data[9] = 0x00;
    			
    			// The alarm mask (all alarms are off)
    			data[10] = 0x00; data[11] = 0x00;
    			data[12] = 0x00; data[13] = 0x00;
    			
    			// send a command to the slaved device to change the sampling rate
    			sendCommand(SET_SAMPLING, data);
    		}
    	}
	}
    
    /* Listener Helpers */
    
    /**
     * Toggles flag indicating whether the USB buffered input data is
     * captured or flushed.
     * 
     * @param button the start or stop capture button that was pressed
     */
    void handleCaptureButtonClick(JButton button) {
    	if(button.getActionCommand().equals("Stop Capture")) {
    		captureData = false;
    		
    		// stop the data timer task as the capture of data has been halted
    		cancelAndRemoveDataTask(false);
    	} else if(button.getActionCommand().equals("Start-Pause Capture")) {
			captureData = true;
			
			// data is being captured again so start the data timer
			restartDataTimeout();
    	}

    }
    
    /**
     * Determines which combo-box was selected, and calls appropriate
     * helper method.
     * @param comboBox
     */
    void handleComboBoxSelection(JComboBox<String> comboBox) {
    	if(comboBox.getActionCommand().equals(CONNECT_ACTION_STR)) {
    		handleConnectionSelection((StringBuffer) comboBox.getSelectedItem());
    	} else if (comboBox.getActionCommand().equals(COM_ACTION_STR)) {
    		handleCOMSelection((String) comboBox.getSelectedItem());
    	} else if (comboBox.getActionCommand().equals(BAUD_ACTION_STR)){
    		handleBaudSelection((String) comboBox.getSelectedItem());
    	}
    }
    
    /**
     * Close the connection if disconnect is selected and it previously was NOT selected.
     * Open the connection if connect is selected and it previously was NOT selected.
     * @param selection
     */
    private void handleConnectionSelection(StringBuffer selection) {
    	if (selection.equals(connectStringBuffers[1]) && connectedSelected) {
    		// disconnect option selected and connect option was previously selected
    		disconnectComboBoxes("Disconnecting...");
    		
    		connectedSelected = false;
    		
    		close();
    	} else if (selection.equals(connectStringBuffers[0]) && !connectedSelected) {
    		// start the thread that searches for serial device connections
    		startConnectionThread();
    	}
    }
    
    /**
     * Change the port string to match the users selection.
     * @param selection
     */
    private void handleCOMSelection(String selection) {
    	portString = selection;
    }
    
    
    /**
     * Change the baud rate to match the users selection.
     * @param selection
     */
    private void handleBaudSelection(String selection) {
    	dataRate = Integer.parseInt(selection);
    	byte[] data = new byte[4];
    	for(int i=0; i<=3 ; i++) {
			data[i] = (byte)(dataRate >>> 8*(i)); 
		}
    	
    	sendCommand(SET_BAUD_RATE, data); 
    	close();
    }
    
    private void disconnectComboBoxes(String newString) {
    	connectComboBox.setSelectedIndex(1);
    	StringBuffer selectedItem = (StringBuffer) connectComboBox.getSelectedItem();
    	selectedItem.delete(0, selectedItem.length());
    	selectedItem.append(newString);
    }
    
    private void connectComboBoxes(String newString) {
    	connectComboBox.setSelectedIndex(0);
    	StringBuffer selectedItem = (StringBuffer) connectComboBox.getSelectedItem();
    	selectedItem.delete(0, selectedItem.length());
    	selectedItem.append(newString);
    	connectComboBox.repaint();
    }
    
    public void startConnectionThread() {
    	connectionThread = new Thread(this);
    	connectionThread.start();
    }
    
    public boolean isRunning() {
    	return running;
    }
    
    /* Functions used to send commands to the slaved device */
    
    public void sendKeepAliveCMD() {
    	sendCommand(KEEP_ALIVE_CMD, null);
    }
    
    /* Functions used to handle the timers */

    private void cancelAndRemovePortTask(boolean interrupt) {
    	if(portTimeoutTask != null)
    		portTimeoutTask.cancel(interrupt);
    }
    
    private void cancelAndRemoveDataTask(boolean interrupt) {
    	if(dataTimeoutTask != null)
    		dataTimeoutTask.cancel(interrupt);
    }
    
    private void cancelAndRemoveKeepAliveTask(boolean interrupt) {
    	if(keepAliveTimeoutTask != null)
    		keepAliveTimeoutTask.cancel(interrupt);
    }
    
    private void restartPortTimeout() {
    	if(portTimeoutTask != null)
    		portTimeoutTask.cancel(true);
    	
    	
    	portTimeoutTask = ex.scheduleAtFixedRate(new PortTimeoutRunnable(this),
    			PORT_TIME_OUT, PORT_TIME_OUT, TimeUnit.MILLISECONDS);
    }
    
    private void restartDataTimeout() {
    	if(dataTimeoutTask != null)
    		dataTimeoutTask.cancel(true);
    	
    	dataTimeoutTask = ex.scheduleAtFixedRate(new DataTimeoutRunnable(this),
    			DATA_TIME_OUT, DATA_TIME_OUT, TimeUnit.MILLISECONDS);
	}
    
    private void restartKeepaliveTimeout() {
    	if(keepAliveTimeoutTask != null)
    		keepAliveTimeoutTask.cancel(true);
    	
    	keepAliveTimeoutTask = ex.scheduleAtFixedRate(new KeepAliveTimeoutRunnable(this),
    			KEEP_ALIVE_TIME_OUT, KEEP_ALIVE_TIME_OUT, TimeUnit.MILLISECONDS);
	}
    
    /* Getters and Setters */
    
    /**
     * 
     * @param connectStrings the connect and disconnect JComboBox options 
     */
    public synchronized void setComboBoxChoices(StringBuffer[] connectStrings) {
    	this.connectStringBuffers = connectStrings;
    }
    
    /**
     * Used for changing combo-box string based on connection status
     * @param connectComboBox
     */
    public synchronized void setConnectionComboBox(JComboBox<StringBuffer> connectComboBox) {
    	this.connectComboBox = connectComboBox;
    }
    
    /**
     * Used for changing combo-box string based on baud rates status
     * @param baudComboBox
     */
    public synchronized void setBaudComboBox(JComboBox<String> baudComboBox) {
    	this.baudComboBox = baudComboBox;
    }
}


class PortTimeoutRunnable implements Runnable {
	
	private SerialCommsRunnable serialCommsRunner;

	public PortTimeoutRunnable(SerialCommsRunnable serialCommsRunner) {
		this.serialCommsRunner = serialCommsRunner;
	}
	
	@Override
	public void run() {
		serialCommsRunner.close();
		serialCommsRunner.startConnectionThread();
	}

}

class DataTimeoutRunnable implements Runnable {

	private SerialCommsRunnable serialCommsRunner;

	public DataTimeoutRunnable(SerialCommsRunnable serialCommsRunner) {
		this.serialCommsRunner = serialCommsRunner;
	}
	
	@Override
	public void run() {
		serialCommsRunner.flushInputBuffer();
		
	}
	
}


class KeepAliveTimeoutRunnable implements Runnable {
	
	private SerialCommsRunnable serialCommsRunner;

	public KeepAliveTimeoutRunnable(SerialCommsRunnable serialCommsRunner) {
		this.serialCommsRunner = serialCommsRunner;
	}
	
	@Override
	public void run() {
		serialCommsRunner.sendKeepAliveCMD();
	}
	
}
