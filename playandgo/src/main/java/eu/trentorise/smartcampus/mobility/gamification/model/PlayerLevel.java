package eu.trentorise.smartcampus.mobility.gamification.model;

public class PlayerLevel {
    private String levelName;
    private String levelValue;
    private String pointConcept;
    private double startLevelScore;
    private double endLevelScore;
    private double toNextLevel;
    private Integer levelIndex;

    public PlayerLevel() {
    }

    public String getLevelName() {
        return levelName;
    }

    public String getPointConcept() {
        return pointConcept;
    }

    public double getToNextLevel() {
        return toNextLevel;
    }

    public String getLevelValue() {
        return levelValue;
    }

    public double getStartLevelScore() {
        return startLevelScore;
    }

    public double getEndLevelScore() {
        return endLevelScore;
    }

	/**
	 * @return the levelIndex
	 */
	public Integer getLevelIndex() {
		return levelIndex;
	}

	/**
	 * @param levelIndex the levelIndex to set
	 */
	public void setLevelIndex(Integer levelIndex) {
		this.levelIndex = levelIndex;
	}
}
