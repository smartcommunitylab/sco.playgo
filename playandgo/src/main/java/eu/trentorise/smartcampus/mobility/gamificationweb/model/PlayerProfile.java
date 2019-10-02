package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Strings;

@JsonInclude(Include.NON_NULL)
public class PlayerProfile implements Comparable<PlayerProfile>{

	private String user_id;
	private String date_of_registration;
	private String gender;
	private String year_of_birth_min;
	private String year_of_birth_max;
	private String main_mode_pre_campaign;
	
	private String daily_travelled_distance_min;
	private String daily_travelled_distance_max;
	private String daily_travelled_distance_uom;	
	
	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public String getDate_of_registration() {
		return date_of_registration;
	}
	public void setDate_of_registration(String date_of_registration) {
		this.date_of_registration = date_of_registration;
	}
	public String getGender() {
		return gender;
	}
	public void setGender(String gender) {
		this.gender = gender;
	}
	public String getYear_of_birth_min() {
		return year_of_birth_min;
	}
	public void setYear_of_birth_min(String year_of_birth_min) {
		this.year_of_birth_min = year_of_birth_min;
	}
	public String getYear_of_birth_max() {
		return year_of_birth_max;
	}
	public void setYear_of_birth_max(String year_of_birth_max) {
		this.year_of_birth_max = year_of_birth_max;
	}
	public String getMain_mode_pre_campaign() {
		return main_mode_pre_campaign;
	}
	public void setMain_mode_pre_campaign(String main_mode_pre_campaign) {
		this.main_mode_pre_campaign = main_mode_pre_campaign;
	}
	public String getDaily_travelled_distance_min() {
		return daily_travelled_distance_min;
	}
	public void setDaily_travelled_distance_min(String daily_travelled_distance_min) {
		this.daily_travelled_distance_min = daily_travelled_distance_min;
	}
	public String getDaily_travelled_distance_max() {
		return daily_travelled_distance_max;
	}
	public void setDaily_travelled_distance_max(String daily_travelled_distance_max) {
		this.daily_travelled_distance_max = daily_travelled_distance_max;
	}
	public String getDaily_travelled_distance_uom() {
		return daily_travelled_distance_uom;
	}
	public void setDaily_travelled_distance_uom(String daily_travelled_distance_uom) {
		this.daily_travelled_distance_uom = daily_travelled_distance_uom;
	}
	@Override
	public int compareTo(PlayerProfile o) {
		return Strings.padStart(user_id, 6, '0').compareTo(Strings.padStart(o.user_id, 6, '0'));
	}
	
	
	
}
