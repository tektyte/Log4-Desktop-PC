package model;

import java.util.Date;
import java.util.Observable;

public class ObservableDate extends Observable {
	private Date date;
	private boolean startTime;
	
	public ObservableDate(Date date, boolean startTime) {
		this.date = date;
		this.startTime = startTime;
	}
	
	public boolean isStartTime() {
		return startTime;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		if(this.date == null || date.getTime() != this.date.getTime()) {	
			this.date = date;
			setChanged();
			notifyObservers();
		}
	}
	
}
