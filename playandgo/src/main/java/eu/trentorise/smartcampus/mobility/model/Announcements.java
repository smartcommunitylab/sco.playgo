package eu.trentorise.smartcampus.mobility.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement (name = "annoucements")

public class Announcements {

	private List<Announcement> announcement;

	public List<Announcement> getAnnouncement() {
		return announcement;
	}

	public void setAnnouncement(List<Announcement> announcement) {
		this.announcement = announcement;
	}
	
}
