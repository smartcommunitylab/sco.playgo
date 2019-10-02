package eu.trentorise.smartcampus.mobility.gamification.model;

public class UserDescriptor implements Comparable<UserDescriptor> {

	private String userId;
	private int valid;
	private int invalid;
	private int pending;
	private int total;
	private boolean banned;
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getValid() {
		return valid;
	}

	public void setValid(int valid) {
		this.valid = valid;
	}

	public int getInvalid() {
		return invalid;
	}

	public void setInvalid(int invalid) {
		this.invalid = invalid;
	}

	public int getPending() {
		return pending;
	}

	public void setPending(int pending) {
		this.pending = pending;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public boolean isBanned() {
		return banned;
	}

	public void setBanned(boolean banned) {
		this.banned = banned;
	}
	@Override
	public int compareTo(UserDescriptor o) {
		try {
			return Integer.parseInt(userId) - (Integer.parseInt(o.userId));
		} catch (Exception e) {
			return 0;
		}
	}
	
	
}
