package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class WeekPrizeData {

	private int weekNum = 0;
	private String prize = "";
	private String target = "";
	private String sponsor = "";
	

	public int getWeekNum() {
		return weekNum;
	}

	public void setWeekNum(int weekNum) {
		this.weekNum = weekNum;
	}

	public String getPrize() {
		return prize;
	}

	public String getTarget() {
		return target;
	}

	public String getSponsor() {
		return sponsor;
	}

	public void setPrize(String prize) {
		this.prize = prize;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setSponsor(String sponsor) {
		this.sponsor = sponsor;
	}

	public WeekPrizeData() {
		// TODO Auto-generated constructor stub
	}

	public WeekPrizeData(int weekNum, String prize, String target, String sponsor) {
		super();
		this.weekNum = weekNum;
		this.prize = prize;
		this.target = target;
		this.sponsor = sponsor;
	}

	@Override
	public String toString() {
		return "WeekPrizeData [weekNum=" + weekNum + ", prize=" + prize + ", target=" + target + ", sponsor=" + sponsor
				+ "]";
	}

}
