package eu.trentorise.smartcampus.mobility.security;

public class Polygon extends Shape {

	private double[][] points;

	private java.awt.Polygon polygon;

	public Polygon() {
	}

	public double[][] getPoints() {
		return points;
	}

	public void setPoints(double[][] points) {
		this.points = points;
		
		int x[] = new int[points.length];
		int y[] = new int[points.length];		
		for (int i = 0; i < points.length; i++) {
			x[i] = (int)(points[i][0] * 1E6);
			y[i] = (int)(points[i][1] * 1E6);
		}
		
		polygon = new java.awt.Polygon(x, y, points.length);	
	}        
        

	@Override
	public boolean inside(double lat, double lon) {
		return polygon.contains((int) (lat * 1E6), (int) (lon * 1E6));
	}

}