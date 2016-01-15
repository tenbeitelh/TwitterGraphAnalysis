package de.hs.osnabrueck.tenbeitel.mr.extendedgraph.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
	private static final String TWITTER_DATE_PATTERN = "eee MMM dd HH:mm:ss ZZZZ yyyy";

	public static Date convertTwitterDateStringToDate(String dateString) throws ParseException {
		SimpleDateFormat sf = new SimpleDateFormat(TWITTER_DATE_PATTERN, Locale.ENGLISH);
		return sf.parse(dateString);
	}
}
