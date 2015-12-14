package model;

public class Statistic {
	private static double FEMENTO = 1e-15;
	private static double PICO    = 1e-12;
	private static double NANO    = 1e-9;
	private static double MICRO   = 1e-6;
	private static double MILLI   = 1e-3;
	
	private static int KILO = (int) 1e3;
	private static int MEGA = (int) 1e6;
	private static int GIGA = (int) 1e9;
	private static int TERA = (int) 1e12;
	private static int PETA = (int) 1e15;
	
	
	private String title;
	private String data;
	private String unit;
	
	private double baseData;
	
	public Statistic(String title, String unit) {
		this.title = title;

		this.unit = unit;
		this.data = "0 ";
	
		this.baseData = 0;
	}
	
	public String getTitleStr() {
		return title;
	}
	
	public String getDataStr() {
		return data;
	}
	
	
	
	public String getUnitStr() {
		return unit;
	}
	
	public String getDataWithUnit() {
		return data + unit;
	}
	
	public double getBaseData() {
		return baseData;
	}
	public void setDataStr(double newData) {
		baseData = newData;
		double absData = Math.abs(newData);
		if (baseData==0){
			data = String.format("%5.3f", baseData);
			unit = " " + unit.charAt(unit.length()-1);
		} else if (absData <= PICO) {
			data = String.format("%5.3f", baseData * 1.0/FEMENTO);
			unit = "f" + unit.charAt(unit.length()-1);
		} else if (PICO < absData && absData <= NANO) {
			data = String.format("%5.3f", baseData * 1.0/PICO);
			unit = "p" + unit.charAt(unit.length()-1);
		} else if (NANO < absData && absData <= MICRO) {
			data = String.format("%5.3f", baseData * 1.0/NANO);
			unit = "n" + unit.charAt(unit.length()-1);
		} else if (MICRO < absData && absData <= MILLI) {
			data = String.format("%5.3f", baseData * 1.0/MICRO);
			unit = "u" + unit.charAt(unit.length()-1);
		} else if (MILLI < absData && absData <= 1) {
			data = String.format("%5.3f", baseData * 1.0/MILLI);
			unit = "m" + unit.charAt(unit.length()-1);
		} else if (1 < absData && absData <= KILO) {
			data = String.format("%5.3f", baseData);
			unit = " " + unit.charAt(unit.length()-1);
		} else if (KILO < absData && absData <= MEGA) {
			data = String.format("%5.3f", baseData * 1.0/KILO);
			unit = "k" + unit.charAt(unit.length()-1);
		} else if (MEGA < absData && absData <= GIGA) {
			data = String.format("%5.3f", baseData * 1.0/MEGA);
			unit = "M" + unit.charAt(unit.length()-1);
		} else if (GIGA < absData && absData <= TERA) {
			data = String.format("%5.3f", baseData * 1.0/GIGA);
			unit = "G" + unit.charAt(unit.length()-1);
		} else if (TERA < absData && absData <= PETA) {
			data = String.format("%5.3f", baseData * 1.0/TERA);
			unit = "T" + unit.charAt(unit.length()-1);
		} else if (absData > PETA) {
			data = String.format("%5.3f", baseData * 1.0/PETA);
			unit = "P" + unit.charAt(unit.length()-1);			
		}
		
	}
	
}
