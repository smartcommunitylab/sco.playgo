package eu.trentorise.smartcampus.mobility.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.model.Announcements;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;

@Component
public class AnnouncementsHelper {

	@Autowired
	private DomainStorage storage;	
	
	public Announcements getAnnouncement(String type, String appId, Integer skip, Integer limit) {
		Criteria criteria = new Criteria(type).is(true).and("appId").is(appId);
		Query query = new Query(criteria).with(new Sort(Sort.Direction.DESC, "timestamp"));
				if (skip != null) {
					query = query.skip(skip);
				}
				if (limit != null) {
					query = query.limit(limit);
				}

		List<Announcement> msgs = storage.searchDomainObjects(query, Announcement.class);
		
		Announcements result = new Announcements();
		result.setAnnouncement(msgs);
		return result;
	}	
}
