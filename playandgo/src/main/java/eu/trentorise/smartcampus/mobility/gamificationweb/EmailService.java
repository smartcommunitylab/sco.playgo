package eu.trentorise.smartcampus.mobility.gamificationweb;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.trentorise.smartcampus.mobility.gamificationweb.model.BadgesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.ChallengesData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.MailImage;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.Summary;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekConfData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekPrizeData;
import eu.trentorise.smartcampus.mobility.gamificationweb.model.WeekWinnersData;

@Service
public class EmailService {

    @Autowired 
    private JavaMailSender mailSender;
    
    @Autowired 
    private TemplateEngine templateEngine;
    
    @Autowired
    @Value("${mail.from}")
    private String mailFrom;
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    public void sendMailGamification(final String recipientName, final Integer point_green, final Integer point_green_w, WeekConfData weekConfData, final String week_theme,  
            final List<BadgesData> badges,
            final List<ChallengesData> challenges,
            final List<ChallengesData> last_week_challenges,
            final List<WeekPrizeData> prizes,
            final List<MailImage> standardImages,
            final String recipientEmail, final String greengame_url, String surveyLink, String unsubscribtionLink, final Locale locale)
            throws MessagingException {
        
    	logger.debug(String.format("Gamification Mail Prepare for %s - OK", recipientName));
    	
    	List<ChallengesData> winChallenges = Lists.newArrayList();
    	if(last_week_challenges != null){
	    	last_week_challenges.stream().filter(x -> x.getSuccess()).collect(Collectors.toList());
    	}
    	
    	String challengesStartingTime = "";
    	String challengesEndingTime = "";
    	String challengesStartingDate = "";
    	String challengesEndingDate = "";
    	Date ch_startTime = null;
    	Date ch_endTime = null;
    	SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    	if(challenges != null && challenges.size() > 0){
    		long startTime = challenges.get(0).getStartDate();
    		long endTime = challenges.get(0).getEndDate();
    		ch_startTime = new Date(startTime);
    		ch_endTime = new Date(endTime);
    		String challStartingAll = dt.format(ch_startTime);
    		String challEndingAll = dt.format(ch_endTime);
    		String[] completeStart = challStartingAll.split(" ");
    		String[] completeEnd = challEndingAll.split(" ");
    		challengesStartingDate = completeStart[0];
    		challengesStartingTime = completeStart[1];
    		challengesEndingDate = completeEnd[0];
    		challengesEndingTime = completeEnd[1];
    	}
    	
    	boolean isLastWeek = false;
    	if("Last".equals(week_theme)){
    	//if(week_theme.compareTo("Batti i tuoi record") == 0){
    		isLastWeek = true;
    	}
    	
        // Prepare the evaluation context
        final Context ctx = new Context(locale);
        ctx.setVariable("lang", locale.getLanguage());
        ctx.setVariable("name", recipientName);
        ctx.setVariable("g_point", point_green);
        ctx.setVariable("g_point_w", point_green_w);
        ctx.setVariable("n_badges", badges);
        ctx.setVariable("next_week_num", weekConfData.getWeekNum());
        ctx.setVariable("next_week_theme", week_theme);
        ctx.setVariable("show_last_week", isLastWeek);
        ctx.setVariable("week_num", weekConfData.getWeekNum() - 1);
        ctx.setVariable("n_challenges", challenges);
        ctx.setVariable("n_lw_challenges", last_week_challenges);
        ctx.setVariable("n_lw_win_challenges", winChallenges);
        ctx.setVariable("chs_start_date", challengesStartingDate);
        ctx.setVariable("chs_start_time", challengesStartingTime);
        ctx.setVariable("chs_end_date", challengesEndingDate);
        ctx.setVariable("chs_end_time", challengesEndingTime);
        ctx.setVariable("n_prizes", prizes);
        ctx.setVariable("are_prizes", weekConfData.isPrizes());
        ctx.setVariable("are_prizes_last", weekConfData.isPrizesLast());
        ctx.setVariable("are_challenges", weekConfData.isChallenges());
        ctx.setVariable("greengame_url", greengame_url);
        ctx.setVariable("unsubscribtionLink", unsubscribtionLink);
        ctx.setVariable("surveyLink", surveyLink);
        ctx.setVariable("imageRNFoglie03", standardImages.get(0).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNFoglie04", standardImages.get(1).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNGreenScore", standardImages.get(2).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNHealthScore", standardImages.get(3).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNPrScore", standardImages.get(4).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageResourceName", standardImages.get(5).getImageName()); // so that we can reference it from HTML
        
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject("Play&Go - Notifica");
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);

        // Create the HTML body using Thymeleaf
        String htmlContent = "";
//        if(isFirstMail){
//        	htmlContent = (locale == Locale.ITALIAN) ? this.templateEngine.process("mail/email-gamification2016-startgame-tn", ctx) : this.templateEngine.process("mail/email-gamification2016-startgame-tn-eng", ctx);
//        } else {
        	htmlContent = (locale == Locale.ITALIAN) ? this.templateEngine.process("mail/email-gamification2018-tn", ctx) : this.templateEngine.process("mail/email-gamification2018-tn-eng", ctx);
//        }
        message.setText(htmlContent, true /* isHtml */);
        
        final InputStreamSource imageSourceFoglia03 = new ByteArrayResource(standardImages.get(0).getImageByte());
        message.addInline(standardImages.get(0).getImageName(), imageSourceFoglia03, standardImages.get(0).getImageType());
        final InputStreamSource imageSourceFoglia04 = new ByteArrayResource(standardImages.get(1).getImageByte());
        message.addInline(standardImages.get(1).getImageName(), imageSourceFoglia04, standardImages.get(1).getImageType());
        
        final InputStreamSource imageSourceGreen = new ByteArrayResource(standardImages.get(2).getImageByte());
        message.addInline(standardImages.get(2).getImageName(), imageSourceGreen, standardImages.get(2).getImageType());
        
        Set<String> badgeImages = Sets.newHashSet();
        if(badges != null){
        	// Add the inline images for badges
	        for(BadgesData bd: badges){
	        	String imgName = bd.getImageName();
	        	if (badgeImages.contains(imgName)) {
	        		continue;
	        	}
	        	final InputStreamSource tmp = new ByteArrayResource(bd.getImageByte());
	            message.addInline(imgName, tmp, bd.getImageType());
	            badgeImages.add(imgName);
	        }
        }
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.info(String.format("Gamification Mail Sent to %s - OK", recipientName));
        
    }
    
    public void sendMailGamificationForWinners(
            final String recipientName, final String point_green, final String point_health, final String point_pr, final String badge,
            final String position, final Integer week_number, final String week_theme, final Integer last_week_number, final Boolean are_challenges, final Boolean are_prizes, final Boolean are_last_week_prizes, 
            final List<BadgesData> badges,
            final List<ChallengesData> challenges,
            final List<ChallengesData> last_week_challenges,
            final List<WeekPrizeData> prizes,
            final List<WeekWinnersData> winners,
            final List<MailImage> standardImages,
            final String recipientEmail, final String greengame_url, String surveyLink, String unsubscribtionLink, final Locale locale)
            throws MessagingException {
        
    	logger.debug(String.format("Gamification Mail Prepare for %s - OK", recipientName));
    	
    	// Correct the winners:
    	List<WeekWinnersData> last_week_winners = Lists.newArrayList();
    	for(int i = 0; i < winners.size(); i++){
    		if(last_week_number != null && winners.get(i).getWeekNum() == last_week_number){
    			last_week_winners.add(winners.get(i));
    		}
    	}
    	// Correct the win challenges
    	List<ChallengesData> winChallenges = Lists.newArrayList();
    	for(int i = 0; i < last_week_challenges.size(); i++){
    		if(last_week_challenges.get(i).getSuccess()){
    			winChallenges.add(last_week_challenges.get(i));
    		}
    	}
    	
    	String challengesStartingTime = "";
    	String challengesEndingTime = "";
    	String challengesStartingDate = "";
    	String challengesEndingDate = "";
    	Date ch_startTime = null;
    	Date ch_endTime = null;
    	SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    	if(challenges != null && challenges.size() > 0){
    		long startTime = challenges.get(0).getStartDate();
    		long endTime = challenges.get(0).getEndDate();
    		ch_startTime = new Date(startTime);
    		ch_endTime = new Date(endTime);
    		String challStartingAll = dt.format(ch_startTime);
    		String challEndingAll = dt.format(ch_endTime);
    		String[] completeStart = challStartingAll.split(" ");
    		String[] completeEnd = challEndingAll.split(" ");
    		challengesStartingDate = completeStart[0];
    		challengesStartingTime = completeStart[1];
    		challengesEndingDate = completeEnd[0];
    		challengesEndingTime = completeEnd[1];
    	}
    	
        // Prepare the evaluation context
        final Context ctx = new Context(locale);
        ctx.setVariable("name", recipientName);
        ctx.setVariable("g_point", point_green);
        ctx.setVariable("h_point", point_health);
        ctx.setVariable("p_point", point_pr);
        ctx.setVariable("n_badge", badge);
        ctx.setVariable("n_badges", badges);
        ctx.setVariable("next_week_num", week_number);
        ctx.setVariable("next_week_theme", week_theme);
        ctx.setVariable("week_num", last_week_number);
        ctx.setVariable("n_challenges", challenges);
        ctx.setVariable("n_lw_challenges", last_week_challenges);
        ctx.setVariable("n_lw_win_challenges", winChallenges);
        ctx.setVariable("chs_start_date", challengesStartingDate);
        ctx.setVariable("chs_start_time", challengesStartingTime);
        ctx.setVariable("chs_end_date", challengesEndingDate);
        ctx.setVariable("chs_end_time", challengesEndingTime);
        ctx.setVariable("n_prizes", prizes);
        ctx.setVariable("are_prizes", are_prizes);
        ctx.setVariable("are_prizes_last", are_last_week_prizes);
        ctx.setVariable("are_challenges", are_challenges);
        ctx.setVariable("n_winners", last_week_winners);
        ctx.setVariable("u_position", position);
        ctx.setVariable("greengame_url", greengame_url);
        ctx.setVariable("surveyLink", surveyLink);
        ctx.setVariable("unsubscribtionLink", unsubscribtionLink);
        ctx.setVariable("imageRNFoglie03", standardImages.get(0).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNFoglie04", standardImages.get(1).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNGreenScore", standardImages.get(2).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNHealthScore", standardImages.get(3).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNPrScore", standardImages.get(4).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageResourceName", standardImages.get(5).getImageName()); // so that we can reference it from HTML
        
        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject("Play&Go - Premiazione"); //Vincitori
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);

        // Create the HTML body using Thymeleaf
        final String htmlContent = (locale == Locale.ITALIAN) ? this.templateEngine.process("mail/email-gamification2016-winners-tn", ctx) : this.templateEngine.process("mail/email-gamification2016-winners-tn-eng", ctx);
        message.setText(htmlContent, true /* isHtml */);
        
        // Add the inline titles image, referenced from the HTML code as "cid:${imageResourceName}"
        final InputStreamSource imageSourceFoglia03 = new ByteArrayResource(standardImages.get(0).getImageByte());
        message.addInline(standardImages.get(0).getImageName(), imageSourceFoglia03, standardImages.get(0).getImageType());
        final InputStreamSource imageSourceFoglia04 = new ByteArrayResource(standardImages.get(1).getImageByte());
        message.addInline(standardImages.get(1).getImageName(), imageSourceFoglia04, standardImages.get(1).getImageType());
        
        // Add the inline score image, referenced from the HTML code as "cid:${imageResourceName}"
        /*final InputStreamSource imageSourceGreen = new ByteArrayResource(standardImages.get(2).getImageByte());
        message.addInline(standardImages.get(2).getImageName(), imageSourceGreen, standardImages.get(2).getImageType());*/
        /*final InputStreamSource imageSourceHealth = new ByteArrayResource(standardImages.get(3).getImageByte());
        message.addInline(standardImages.get(3).getImageName(), imageSourceHealth, standardImages.get(3).getImageType());
        final InputStreamSource imageSourcePr = new ByteArrayResource(standardImages.get(4).getImageByte());
        message.addInline(standardImages.get(4).getImageName(), imageSourcePr, standardImages.get(4).getImageType());*/
        
        // Add the inline footer image, referenced from the HTML code as "cid:${imageResourceName}"
        final InputStreamSource imageSourceFooter = new ByteArrayResource(standardImages.get(5).getImageByte());
        message.addInline(standardImages.get(5).getImageName(), imageSourceFooter, standardImages.get(5).getImageType());
        
        if(badges != null){
        	// Add the inline images for badges	-- commented to avoid attachment images in mail
	        //for(int i = 0; i < badges.size(); i++){
	        //	final InputStreamSource tmp = new ByteArrayResource(badges.get(i).getImageByte());
	        //   message.addInline(badges.get(i).getImageName(), tmp, badges.get(i).getImageType());
	        //}
        }
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.info(String.format("Gamification Mail Sent to %s - OK", recipientName));
        
    }
    
    public void sendMailGamificationWithReport(
            final String recipientName,  
            File reportFile,

            final List<MailImage> standardImages,
            final String recipientEmail, final String greengame_url, final Boolean show_final_event, final Locale locale)
            throws MessagingException {
        
        // Prepare the evaluation context
        final Context ctx = new Context(locale);
        ctx.setVariable("name", recipientName);

        ctx.setVariable("imageRNFoglie03", standardImages.get(0).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNFoglie04", standardImages.get(1).getImageName()); // so that we can reference it from HTML
        
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject((locale == Locale.ITALIAN) ? "Play&Go - Attestato" : "Play&Go - Certificate"); //Vincitori
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);

        final String htmlContent = (locale == Locale.ITALIAN) ? this.templateEngine.process("mail/email-gamification2018-module-tn", ctx) : this.templateEngine.process("mail/email-gamification2018-module-tn-eng", ctx);
        message.setText(htmlContent, true /* isHtml */);
        
        final InputStreamSource imageSourceFoglia03 = new ByteArrayResource(standardImages.get(0).getImageByte());
        message.addInline(standardImages.get(0).getImageName(), imageSourceFoglia03, standardImages.get(0).getImageType());
        final InputStreamSource imageSourceFoglia04 = new ByteArrayResource(standardImages.get(1).getImageByte());
        message.addInline(standardImages.get(1).getImageName(), imageSourceFoglia04, standardImages.get(1).getImageType());
        
        message.addAttachment(reportFile.getName(), reportFile);
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.info(String.format("Gamification Mail Sent to %s - OK", recipientName));
        
    }
    
    public void sendMailSummary(
            final String recipientName, final String point_green, final String point_health, final String point_pr, 
            final List<Summary> summary,
            final List<MailImage> standardImages,
            final String recipientEmail, final Locale locale)
            throws MessagingException {
        
    	logger.debug(String.format("Gamification Mail Prepare for %s - OK", recipientName));
    	
        // Prepare the evaluation context
        final Context ctx = new Context(locale);
        ctx.setVariable("name", recipientName);
        ctx.setVariable("g_point", point_green);
        ctx.setVariable("h_point", point_health);
        ctx.setVariable("p_point", point_pr);
        ctx.setVariable("n_summ", summary);
        ctx.setVariable("imageRNFoglie03", standardImages.get(0).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNFoglie04", standardImages.get(1).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNGreenScore", standardImages.get(2).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNHealthScore", standardImages.get(3).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageRNPrScore", standardImages.get(4).getImageName()); // so that we can reference it from HTML
        ctx.setVariable("imageResourceName", standardImages.get(5).getImageName()); // so that we can reference it from HTML
        
        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject("Play&Go - Riepilogo");
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);

        // Create the HTML body using Thymeleaf
        final String htmlContent = this.templateEngine.process("mail/email-gamification-summary", ctx);
        message.setText(htmlContent, true /* isHtml */);
        
        // Add the inline titles image, referenced from the HTML code as "cid:${imageResourceName}"
        final InputStreamSource imageSourceFoglia03 = new ByteArrayResource(standardImages.get(0).getImageByte());
        message.addInline(standardImages.get(0).getImageName(), imageSourceFoglia03, standardImages.get(0).getImageType());
        final InputStreamSource imageSourceFoglia04 = new ByteArrayResource(standardImages.get(1).getImageByte());
        message.addInline(standardImages.get(1).getImageName(), imageSourceFoglia04, standardImages.get(1).getImageType());
        
        // Add the inline score image, referenced from the HTML code as "cid:${imageResourceName}"
        final InputStreamSource imageSourceGreen = new ByteArrayResource(standardImages.get(2).getImageByte());
        message.addInline(standardImages.get(2).getImageName(), imageSourceGreen, standardImages.get(2).getImageType());
        final InputStreamSource imageSourceHealth = new ByteArrayResource(standardImages.get(3).getImageByte());
        message.addInline(standardImages.get(3).getImageName(), imageSourceHealth, standardImages.get(3).getImageType());
        final InputStreamSource imageSourcePr = new ByteArrayResource(standardImages.get(4).getImageByte());
        message.addInline(standardImages.get(4).getImageName(), imageSourcePr, standardImages.get(4).getImageType());
        
        // Add the inline footer image, referenced from the HTML code as "cid:${imageResourceName}"
        final InputStreamSource imageSourceFooter = new ByteArrayResource(standardImages.get(5).getImageByte());
        message.addInline(standardImages.get(5).getImageName(), imageSourceFooter, standardImages.get(5).getImageType());
        
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.info(String.format("Gamification Mail Sent to %s - OK", recipientName));
        
    }

    public void sendGenericMail(String body, String subject, final String recipientName, final String recipientEmail, final Locale locale) throws MessagingException {
        
    	logger.debug(String.format("Gamification Generic Mail Prepare for %s - OK", recipientName));
    	
        // Prepare the evaluation context
        final Context ctx = new Context(locale);
        ctx.setVariable("name", recipientName);
        ctx.setVariable("body", body);
                
        // Prepare message using a Spring helper
        final MimeMessage mimeMessage = this.mailSender.createMimeMessage();
        final MimeMessageHelper message = 
                new MimeMessageHelper(mimeMessage, true /* multipart */, "UTF-8");
        message.setSubject(subject);
        message.setFrom(mailFrom);
        message.setTo(recipientEmail);

        // Create the HTML body using Thymeleaf
        final String htmlContent = this.templateEngine.process("mail/email-generic-template", ctx);
        message.setText(htmlContent, true /* isHtml */);        
        
        // Send mail
        this.mailSender.send(mimeMessage);
        logger.info(String.format("Gamification Generic Mail Sent to %s - OK", recipientName));
        
    }
    
}