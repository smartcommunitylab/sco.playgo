package eu.trentorise.smartcampus.mobility.gamification.model;

public class ClassificationPosition implements Comparable<ClassificationPosition> {
	private double score;
	private String playerId;
	private int position;

	public ClassificationPosition() {
	}
	
	public ClassificationPosition(double score, String playerId) {
		this.score = score;
		this.playerId = playerId;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public int compareTo(ClassificationPosition o) {
		return (position - o.position);
	}
	
	@Override
	public String toString() {
		return position + ":" + score;
	}

}
