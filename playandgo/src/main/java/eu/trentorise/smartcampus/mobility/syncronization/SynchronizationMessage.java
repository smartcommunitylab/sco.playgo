package eu.trentorise.smartcampus.mobility.syncronization;

public class SynchronizationMessage {

	private String from;
	private long timestamp;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

}
