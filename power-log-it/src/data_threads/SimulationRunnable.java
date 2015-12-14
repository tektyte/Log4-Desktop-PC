package data_threads;

import java.awt.Button;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;

import org.jfree.data.time.Millisecond;

import model.ObservableModelDetails;
import model.TimeDataPoint;

public class SimulationRunnable extends DataRunnable {
	
	/** The frequency of the voltage and current source (rad/s) */
	private static final double FREQ = 2*Math.PI*10;
	
	/** The rate at which simulation points are generated (ms)*/
	private int samplingPeriod;
	
	/** Thread used for generating the simulation data */
	private Thread simulationThread;
	
	
	public SimulationRunnable(ObservableModelDetails obModelDetails, JMenuBar menuBar) {
		// start the simulation thread on GUI start up
		super(obModelDetails, menuBar);
	}
	
	@Override
	public void run() {
		// change the model details to the USB Power Logger device
	    obModelDetails.changeModel(0);
		
	    // set the sampling period to the currently selected rate
		samplingPeriod = getSamplingPeriod();
		
		System.out.println("sampling period: " + samplingPeriod);
		
		while (!Thread.interrupted()) {
			// generate another sine wave point
			if(captureData)
				generateDataPoint();
			
			try {
				Thread.sleep(samplingPeriod);
			} catch (InterruptedException e) {
				return;
			}
		}
	}
	
	private void generateDataPoint() {
		// use system time for time value of data point
		Millisecond time = new Millisecond(new Date());
		double t = time.getMiddleMillisecond() / 1e5;
		
		//System.out.println(t);
		
		// 4V pk-pk, 2.5V DC offset
		double voltage = 2 * Math.sin(FREQ*t) + 2.5;
		
		// 200mA pk-pk,  150mA offset
		double current = 0.1 * Math.sin(FREQ*t) + 0.15;
		
		//System.out.println(voltage);
		
		voltageBuffers.get(0).offer(new TimeDataPoint(time, voltage));
		currentBuffers.get(0).offer(new TimeDataPoint(time, current));
		powerBuffers.get(0).offer(new TimeDataPoint(time, voltage*current));
		powerBuffersCopy.get(0).offer(new TimeDataPoint(time, voltage*current));
		resistanceBuffers.get(0).offer(new TimeDataPoint(time, voltage/current));
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			/* Play or stop capture button was pressed */
			handleCaptureButtonClick((JButton) e.getSource());
		} else if (e.getSource() instanceof JComboBox<?>) {
			/* Connection or Disconnection selection was made */
			handleComboBoxSelection((JComboBox<String>) e.getSource());
		}

	}
	
	public void startSimulationThread() {
		simulationThread = new Thread(this);
		simulationThread.start();
	}
	
	public void close() {
		if(simulationThread != null)
			simulationThread.interrupt();
	}
	
	/* Inherited abstract methods */
	
	public boolean isRunning() {
		if(simulationThread != null)
			return simulationThread.isAlive();
		return false;
	}
	
	void handleCaptureButtonClick(JButton button) {
		if(button.getActionCommand().equals("Stop Capture")) {
			captureData = false;
		} else if (button.getActionCommand().equals("Start-Pause Capture")) {
			captureData = true;
		}
	}
	
	void handleComboBoxSelection(JComboBox<String> comboBox) {
		if(comboBox.getSelectedIndex() == 0) {
			/* connection selection was made, therefore end simulation mode */
			close();
		}
	}
	
	
	

}
