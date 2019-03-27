package eu.trentorise.smartcampus.mobility.gamificationweb.model;

public class OtherAttendeeData {
	private int status = 0;
	private double row_status = 0L;

	private String playerId;
	private String nickname;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public double getRow_status() {
		return row_status;
	}

	public void setRow_status(double row_status) {
		this.row_status = row_status;
	}

	public String getPlayerId() {
		return playerId;
	}

	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

}	