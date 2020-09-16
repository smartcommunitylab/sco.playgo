package eu.trentorise.smartcampus.mobility.controller.rest;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import eu.trentorise.smartcampus.mobility.model.Announcement;
import eu.trentorise.smartcampus.mobility.model.Announcements;
import eu.trentorise.smartcampus.mobility.security.AppDetails;
import eu.trentorise.smartcampus.mobility.service.NotificationHelper;
import eu.trentorise.smartcampus.mobility.storage.DomainStorage;
import eu.trentorise.smartcampus.mobility.util.AnnouncementsHelper;

@Controller
@RequestMapping(value = "/web/notification")
public class NotificationController {

	@Autowired
	private NotificationHelper notifier;		
	
	@Autowired
	private DomainStorage storage;
	
	@Autowired
	private AnnouncementsHelper announcementsHelper;	
	
	@RequestMapping(method = RequestMethod.GET)
	public String notify(HttpSession session) {
		return "notification";
	}	

	@RequestMapping(method = RequestMethod.POST, value = "/notify")
	public @ResponseBody Map<String, String> notify(@RequestBody(required=false) Announcement announcement, HttpServletResponse response) throws Exception {
		Map<String, String> result = Maps.newTreeMap();

		try {

			String messagingAppId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getMessagingAppId();
			String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
			
			if (announcement.getNotification() != null && announcement.getNotification().booleanValue()) {
				notifier.notifyAnnouncement(announcement, messagingAppId);
			}
			announcement.setAppId(appId);
			announcement.setTimestamp(System.currentTimeMillis());
			storage.saveNews(announcement);

			result.put("message", "Message \"" + announcement.getTitle() + "\" sent @ " + new Date());
		} catch (Exception e) {
			result.put("error", "Exception @ " + new Date() + ": " + e.toString());
		}

		return result;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/announcements/news", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNews(@RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		return announcementsHelper.getAnnouncement("news", appId, skip, limit);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/announcements/notifications", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNotifications(@RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		return announcementsHelper.getAnnouncement("notification", appId, skip, limit);
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/announcements/appId", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody String getAppId(HttpServletResponse response) throws Exception {
		String appId = ((AppDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getApp().getAppId();
		return StringUtils.capitalize(appId);
	}	
	

}