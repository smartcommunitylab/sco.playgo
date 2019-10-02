package eu.trentorise.smartcampus.mobility.gamification.model;

public class PlayerLevel {
    private String levelName;
    private String levelValue;
    private String pointConcept;
    private double startLevelScore;
    private double endLevelScore;
    private double toNextLevel;

    public PlayerLevel() {
    }
    
    public PlayerLevel(String levelName, String pointConcept, String levelValue,
            Double toNextLevel, Double startLevelScore, Double endLevelScore) {
        this.levelName = levelName;
        this.pointConcept = pointConcept;
        this.levelValue = levelValue;

        // check if value is persisted or give a valid one
        this.toNextLevel = toNextLevel != null ? toNextLevel : 0;
        this.startLevelScore = startLevelScore != null ? startLevelScore : 0;
        this.endLevelScore = endLevelScore != null ? endLevelScore : 0;
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
}
