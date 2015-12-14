package model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 *
 * 
 * @author M.J
 *
 */
public class ObservableModelDetails extends Observable {
	
	private String name;
	
	private boolean hasModelDetails;
	
	private Integer modelNumber;
	
	private Integer firmWareVersion;
	
	private String latestFirmWareVersion;
	
	private String[] channelFormats;
	
	private ArrayList<ElectricalDataTypes> channels;
	
	private boolean checkSum;
	
	private Integer[] baudRates;
	
	private int maxSamplingRate;
	
	public ObservableModelDetails() {    		
	}
	
	public void changeModel(Integer modelNumber) {
		// The model has been changed
		setChanged();
		
		this.hasModelDetails = true;
		
		this.modelNumber = modelNumber;
		
		
    	String jsonText = null;
    	try {
    		InputStream is = this.getClass().getResourceAsStream("/db/"+modelNumber+".json");
    		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    		StringBuilder out = new StringBuilder();
    		
			//File file = new File(url.getPath());
    		while ((jsonText = reader.readLine()) !=null){
    			out.append(jsonText);
    		}
    		jsonText=out.toString();
    		reader.close();
		} catch (IOException e) { 
		System.err.println("JSON IOException");
		e.printStackTrace();
		}
    	
    	// Create the JSON Objects
    	JSONObject deviceJSON = new JSONObject(jsonText);
    	JSONObject devicePropertiesJSON = deviceJSON.getJSONObject("properties");
    	
    	name = deviceJSON.getString("name");
    	
    	// Get the latest firmware version
    	latestFirmWareVersion = devicePropertiesJSON.getString("latest_firmware_version");
    	
    	// Get the channel formats
    	channelFormats = devicePropertiesJSON.getString("channel_data_format").split(",");
    	
    	// Initialise the list of channels
    	channels = new ArrayList<>();
    	for(int i=0; i<channelFormats.length; i++) {
    		if(channelFormats[i].equals("V")) {
    			channels.add(ElectricalDataTypes.POWER);
    		} else if (channelFormats[i].equals("T")) {
    			channels.add(ElectricalDataTypes.TEMPERATURE);
    		}
    	}
    	
    	// Find out whether the device uses a check sum
    	checkSum = devicePropertiesJSON.getBoolean("check_sum");
    	
    	// get the maximum sampling rate of the device
    	maxSamplingRate = devicePropertiesJSON.getInt("max_sampling");
    	
    	// Read in the array of baud rates
    	JSONArray jsonBaudRates = devicePropertiesJSON.getJSONArray("baud_rates");
    	baudRates = new Integer[jsonBaudRates.length()];
    	for(int i=0; i<jsonBaudRates.length(); i++)
    		baudRates[i] = jsonBaudRates.getInt(i);
    	
    	// Notify the observers of the change
    	notifyObservers();
	}
	
	public void setFirmwareVersion(int version){
		setChanged();
		firmWareVersion=version;
		notifyObservers();
	}
	
	public boolean hasModelDetails() {
		return hasModelDetails;
	}
	
	public Integer getModelNumber() {
		return modelNumber;
	}
	
	public String[] getChannelFormats() {
		return channelFormats;
	}
	
	public ArrayList<ElectricalDataTypes> getChannels() {
		return channels;
	}
	
	public int getMaxSamplingRate() {
		return maxSamplingRate;
	}
	
	public String getLatestFirmwareVersion() {
		return latestFirmWareVersion;
	}
	
	public int getFirmwareVersion() {
		return firmWareVersion;
	}
	
	public int getMaxBaudRate() {
		return baudRates[baudRates.length-1];
	}
	
	public String getName() {
		return name;
	}
	
}
