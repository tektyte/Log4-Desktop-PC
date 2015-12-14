package model;

import java.util.List;
import java.util.Observable;

import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

public class ObTimeSeriesCollection extends Observable {
	private TimeSeriesCollection dataCollection;
	
	public ObTimeSeriesCollection() {
		dataCollection = new TimeSeriesCollection();
	}
	
	public TimeSeries getSeries(int i) {
		return dataCollection.getSeries(i);
	}
	
	public void addSeries(TimeSeries series) {
		setChanged();
		dataCollection.addSeries(series);
		notifyObservers();
	}
	
	public int getSeriesCount() {
		return dataCollection.getSeriesCount();
	}
	
	public void removeAllSeries() {
		setChanged();
		dataCollection.removeAllSeries();
		notifyObservers();
	}
	
	public TimeSeriesCollection getCollection() {
		return dataCollection;
	}

}
