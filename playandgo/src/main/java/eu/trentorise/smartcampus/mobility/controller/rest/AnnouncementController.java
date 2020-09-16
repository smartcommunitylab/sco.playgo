package eu.trentorise.smartcampus.mobility.controller.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;

import eu.trentorise.smartcampus.mobility.model.Announcements;
import eu.trentorise.smartcampus.mobility.util.AnnouncementsHelper;

@Controller
public class AnnouncementController {

	@Autowired
	private AnnouncementsHelper helper;
	
	@Value("${feed.entries:50}")
	private Integer feedEntries = null;	

	
	@RequestMapping(method = RequestMethod.GET, value = "/announcements/news/{appId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNews(@PathVariable String appId, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		return helper.getAnnouncement("news", appId, skip, limit);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/announcements/notifications/{appId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
	public @ResponseBody Announcements getNotifications(@PathVariable String appId, @RequestParam(required = false) Integer skip, @RequestParam(required = false) Integer limit, HttpServletResponse response) throws Exception {
		return helper.getAnnouncement("notification", appId, skip, limit);
	}	
	
	@RequestMapping(method = RequestMethod.GET, value = "/announcements/rss/{appId}")
	public void getRSS(@PathVariable String appId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String url = request.getRequestURL().toString();
		
		Document doc = helper.generateRSS(helper.getAnnouncement("news", appId, 0, feedEntries).getAnnouncement(), appId, url);

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult console = new StreamResult(response.getOutputStream());
		transformer.transform(source, console);
		
		response.setHeader("Content-Type", "application/rss+xml");
	}

}