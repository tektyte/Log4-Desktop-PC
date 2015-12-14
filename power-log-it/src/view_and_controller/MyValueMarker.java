package view_and_controller;

import model.Statistic;

import org.jfree.chart.plot.ValueMarker;

public class MyValueMarker {
	
	private boolean dragging;
	private ValueMarker marker;
	private double offset;
	
	public MyValueMarker(ValueMarker marker){
		this.marker = marker;
	}
	
	public void setValueMarker(ValueMarker marker) {
		this.marker = marker;
	}
	
	public ValueMarker getValueMarker() {
		return marker;
	}
	
	public boolean isDragging() {
		return dragging;
	}
	
	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}
	
	public double getOffset(){
		return this.offset;
	}
	
	public void setOffset(double offset){
		this.offset = offset;
	}
	
	/*
	public void setYValue(double yValue) {
		this.yValue.setDataStr(yValue);
	}
	
	public String getYValueStr() {
		return yValue.getDataStr() + yValue.getUnitStr();
	} */
}
