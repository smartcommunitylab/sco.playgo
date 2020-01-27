package eu.trentorise.smartcampus.mobility.security;

import java.io.Serializable;
import java.util.List;

public class GameInfo implements Serializable {
	private static final long serialVersionUID = -6374757914730418649L;
	
	private String id;
	private String user;
	private String password;
	private String start;
	private List<Shape> areas;
	private Boolean send;

	private List<Initiative> initiatives;
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public List<Shape> getAreas() {
		return areas;
	}

	public void setAreas(List<Shape> areas) {
		this.areas = areas;
	}

	public Boolean getSend() {
		return send;
	}

	public void setSend(Boolean sendEmail) {
		this.send = sendEmail;
	}
	
	public List<Initiative> getInitiatives() {
		return initiatives;
	}

	public void setInitiatives(List<Initiative> initiatives) {
		this.initiatives = initiatives;
	}



	public static class Initiative {
		private String initiative, accountMask, registrationDeadline, startDate;
		private Integer bonusThreshold, bonus;
		
		private List<String> fields;

		public String getInitiative() {
			return initiative;
		}

		public void setInitiative(String initiative) {
			this.initiative = initiative;
		}

		public String getAccountMask() {
			return accountMask;
		}

		public void setAccountMask(String accountMask) {
			this.accountMask = accountMask;
		}

		public String getRegistrationDeadline() {
			return registrationDeadline;
		}

		public void setRegistrationDeadline(String registrationDeadline) {
			this.registrationDeadline = registrationDeadline;
		}

		public List<String> getFields() {
			return fields;
		}

		public void setFields(List<String> fields) {
			this.fields = fields;
		}

		public String getStartDate() {
			return startDate;
		}

		public void setStartDate(String startDate) {
			this.startDate = startDate;
		}

		public Integer getBonusThreshold() {
			return bonusThreshold;
		}

		public void setBonusThreshold(Integer bonusThreshold) {
			this.bonusThreshold = bonusThreshold;
		}

		public Integer getBonus() {
			return bonus;
		}

		public void setBonus(Integer bonus) {
			this.bonus = bonus;
		}
	}

}
