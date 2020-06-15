package eu.trentorise.smartcampus.mobility.controller.rest;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import eu.trentorise.smartcampus.mobility.model.Announcements;
import eu.trentorise.smartcampus.mobility.util.AnnouncementsHelper;

@Controller
public class AnnouncementController {

	@Autowired
	private AnnouncementsHelper helper;
	
	@RequestMapping(method = RequestMethod.GET, value = "/announcements/news/{appId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNews(@PathVariable String appId, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		return helper.getAnnouncement("news", appId, skip, limit);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/announcements/notifications/{appId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNotifications(@PathVariable String appId, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		return helper.getAnnouncement("notification", appId, skip, limit);
	}	
}