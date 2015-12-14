package model;

import org.jfree.data.time.Millisecond;

public class TimeDataPoint {
	private Millisecond time;
	private double valu;
	
	public TimeDataPoint(Millisecond time, double valu) {
		this.time = time;
		this.valu = valu;
	}
	
	public Millisecond getTime() {
		return time;
	}
	
	public double getValu() {
		return valu;
	}
}
