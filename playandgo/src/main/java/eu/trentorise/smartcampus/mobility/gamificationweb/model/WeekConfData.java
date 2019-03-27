package eu.trentorise.smartcampus.mobility.gamificationweb.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.lang3.time.FastDateFormat;

public class WeekConfData {

	private int weekNum = 0;
	private String weekTheme = "";
	private String weekThemeEng = "";
	private boolean challenges = false;
	private boolean prizes = false;
	private boolean prizesLast = false;
	private String weekStart, weekEnd;
	
	private static final FastDateFormat SDF_WEEK_DATE = FastDateFormat.getInstance("yyyy-MM-dd");
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	public int getWeekNum() {
		return weekNum;
	}

	public String getWeekTheme() {
		return weekTheme;
	}

	public boolean isChallenges() {
		return challenges;
	}

	public boolean isPrizes() {
		return prizes;
	}

	public boolean isPrizesLast() {
		return prizesLast;
	}

	public void setWeekNum(int weekNum) {
		this.weekNum = weekNum;
	}

	public void setWeekTheme(String weekTheme) {
		this.weekTheme = weekTheme;
	}

	public void setChallenges(boolean challenges) {
		this.challenges = challenges;
	}

	public void setPrizes(boolean prizes) {
		this.prizes = prizes;
	}

	public void setPrizesLast(boolean prizesLast) {
		this.prizesLast = prizesLast;
	}

	public String getWeekThemeEng() {
		return weekThemeEng;
	}

	public void setWeekThemeEng(String weekThemeEng) {
		this.weekThemeEng = weekThemeEng;
	}

	public WeekConfData() {
		// TODO Auto-generated constructor stub
	}

	public WeekConfData(int weekNum, String weekTheme, String weekThemeEng, boolean challenges, boolean prizes, boolean prizesLast,
			String weekStart, String weekEnd) {
		super();
		this.weekNum = weekNum;
		this.weekTheme = weekTheme;
		this.weekThemeEng = weekThemeEng;
		this.challenges = challenges;
		this.prizes = prizes;
		this.prizesLast = prizesLast;
		this.weekStart = weekStart;
		this.weekEnd = weekEnd;
	}

	public String getWeekStart() {
		return weekStart;
	}

	public void setWeekStart(String weekStart) {
		this.weekStart = weekStart;
	}

	public String getWeekEnd() {
		return weekEnd;
	}

	public void setWeekEnd(String weekEnd) {
		this.weekEnd = weekEnd;
	}

	@Override
	public String toString() {
		return "WeekConfData [weekNum=" + weekNum + ", weekTheme=" + weekTheme + ", challenges=" + challenges
				+ ", prizes=" + prizes + ", prizesLast=" + prizesLast + ", weekStart=" + weekStart + ", weekEnd=" + weekEnd + "]";
	}

	public boolean isWeek(long timestamp) {
		String currDate = SDF_WEEK_DATE.format(new Date(timestamp));
		return currDate.compareTo(weekEnd) <= 0 && currDate.compareTo(weekStart) >= 0;
	}	
	
	public boolean currentWeek() {
		String currDate = SDF_WEEK_DATE.format(new Date());
		return currDate.compareTo(weekEnd) <= 0 && currDate.compareTo(weekStart) >= 0;
	}

	public boolean previousWeek() {
		String currDate = SDF_WEEK_DATE.format(new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 7));
		return currDate.compareTo(weekEnd) <= 0 && currDate.compareTo(weekStart) >= 0;
	}
	
	public boolean nextWeek() {
		String currDate = SDF_WEEK_DATE.format(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7));
		return currDate.compareTo(weekEnd) <= 0 && currDate.compareTo(weekStart) >= 0;
	}	
	

	public static WeekConfData buildDummyCurrentWeek() {
		return buildDummytWeek(0);
	}
	
	public static WeekConfData buildDummyPrevioustWeek() {
		return buildDummytWeek(1);
	}	
	
	private static WeekConfData buildDummytWeek(int deltaWeeks) {
		WeekConfData current = new WeekConfData();

		LocalDate now = LocalDate.now().minusWeeks(deltaWeeks);
		TemporalField tf = WeekFields.of(Locale.ITALY).dayOfWeek();
		
		int day = now.getDayOfWeek().getValue(); 
		LocalDate start = null;
		LocalDate end = null;
		if (day >= 6) {
			start = now.with(DayOfWeek.SATURDAY);
			end = start.plusDays(6);
		} else {
			end = now.with(DayOfWeek.FRIDAY);
			start = end.minusDays(6);
		}
		current.setWeekStart(dtf.format(start));
		current.setWeekEnd(dtf.format(end));
		current.setWeekNum(-deltaWeeks);
		
//		System.err.println(current);
		
		return current;
	}

	
}
