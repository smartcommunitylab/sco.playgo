package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class WeekWinnersData {

	private int weekNum = 0;
	private String nickPlayer = "";
	private String prizesDesc = "";
	private String prizesTarget = "";
	
	public WeekWinnersData(){
		super();
	};

	public WeekWinnersData(int weekNum, String nickPlayer, String prizesDesc, String prizesTarget) {
		super();
		this.weekNum = weekNum;
		this.nickPlayer = nickPlayer;
		this.prizesDesc = prizesDesc;
		this.prizesTarget = prizesTarget;
	}

	public int getWeekNum() {
		return weekNum;
	}

	public String getNickPlayer() {
		return nickPlayer;
	}

	public String getPrizesDesc() {
		return prizesDesc;
	}

	public String getPrizesTarget() {
		return prizesTarget;
	}

	public void setWeekNum(int weekNum) {
		this.weekNum = weekNum;
	}

	public void setNickPlayer(String nickPlayer) {
		this.nickPlayer = nickPlayer;
	}

	public void setPrizesDesc(String prizesDesc) {
		this.prizesDesc = prizesDesc;
	}

	public void setPrizesTarget(String prizesTarget) {
		this.prizesTarget = prizesTarget;
	}

	@Override
	public String toString() {
		return "WeekWinnersData [weekNum=" + weekNum + ", nickPlayer=" + nickPlayer + ", prizesDesc=" + prizesDesc
				+ ", prizesTarget=" + prizesTarget + "]";
	}


}
