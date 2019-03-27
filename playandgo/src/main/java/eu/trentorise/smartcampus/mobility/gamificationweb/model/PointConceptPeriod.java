package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class PointConceptPeriod {
	
	private double score;
	private long start;
	private long end;
	
	public double getScore() {
		return score;
	}

	public long getStart() {
		return start;
	}

	public long getEnd() {
		return end;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public void setEnd(long end) {
		this.end = end;
	}

	public PointConceptPeriod() {
		super();
	}

	public PointConceptPeriod(double score, long start, long end) {
		super();
		this.score = score;
		this.start = start;
		this.end = end;
	}

}
