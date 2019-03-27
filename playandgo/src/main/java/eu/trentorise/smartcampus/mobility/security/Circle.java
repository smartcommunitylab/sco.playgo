package eu.trentorise.smartcampus.mobility.security;

import eu.trentorise.smartcampus.mobility.util.GamificationHelper;

public class Circle extends Shape {

	private double[] center;
	private double radius;

	public Circle() {
	}
	
	public double[] getCenter() {
		return center;
	}

	public void setCenter(double[] center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	@Override
	public boolean inside(double lat, double lon) {
		return GamificationHelper.harvesineDistance(center[0], center[1], lat, lon) <= radius;
	}

}