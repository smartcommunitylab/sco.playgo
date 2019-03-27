package eu.trentorise.smartcampus.mobility.gamification.model;

import java.util.List;

public class ClassificationBoard {
	
	private String pointConceptName;
	private ClassificationType type;
	private List<ClassificationPosition> board;
	
	private long updateTime;

	public String getPointConceptName() {
		return pointConceptName;
	}

	public void setPointConceptName(String pointConceptName) {
		this.pointConceptName = pointConceptName;
	}

	public ClassificationType getType() {
		return type;
	}

	public void setType(ClassificationType type) {
		this.type = type;
	}

	public List<ClassificationPosition> getBoard() {
		return board;
	}

	public void setBoard(List<ClassificationPosition> board) {
		this.board = board;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	
	@Override
	public String toString() {
		return board.toString();
	}

}
