package hapi.app.cli;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Collection of utility methods for working with arguments of type "aDura".
 *
 * @author lopeznr1
 */
public class ArgDuraUtil
{
	/**
	 * Parses the specified input string and returns it as a function of {@link Duration}.
	 * <p>
	 * Input can be specified in one of 2 ways:
	 * <ul>
	 * <li>As an ISO-8601 duration
	 * <li>As a string N{d,h,m,s} where N represents a number and the unit char {d,h,m,s} corresponds to (days, hours,
	 * minutes, seconds)
	 * </ul>
	 * On failure null will be returned.
	 */
	public static Duration parseAsDuration(String aInputStr)
	{
		// Bail if the string is not valid or sufficient length
		if (aInputStr == null || aInputStr.length() < 2)
			return null;

		// Try parsing as ISO-8601
		try
		{
			return Duration.parse(aInputStr);
		}
		catch (DateTimeParseException aExp)
		{
			; // Nothing to do
		}

		// Try parsing as custom format: N{d,h,m,s}
		var lastIdx = aInputStr.length() - 1;

		var numVal = 0L;
		try
		{
			var numStr = aInputStr.substring(0, lastIdx);
			numVal = Long.parseLong(numStr);
		}
		catch (NumberFormatException aExp)
		{
			return null;
		}

		var lastChar = aInputStr.charAt(lastIdx);
		switch (lastChar)
		{
			case 'd':
				return Duration.ofDays(numVal);
			case 'h':
				return Duration.ofHours(numVal);
			case 'm':
				return Duration.ofMinutes(numVal);
			case 's':
				return Duration.ofSeconds(numVal);
			default:
				return null;
		}
	}

	/**
	 * Parses the specified input string and returns it as a function of {@link Duration}.
	 * <p>
	 * Input strings will be parsed with one of the following methods:
	 * <ul>
	 * <li>{@link LocalDate#parse(CharSequence)} (and the time set to {@link LocalTime#MIN}.
	 * <li>{@link LocalDateTime#parse(CharSequence)}
	 * </ul>
	 *
	 * On failure null will be returned.
	 */
	public static LocalDateTime parseAsLocalDateTime(String aInputStr)
	{
		// Try to parse as a date-time
		try
		{
			return LocalDateTime.parse(aInputStr);
		}
		catch (DateTimeParseException aExp)
		{
			; // Nothing to do
		}

		// Try to parse as a date
		try
		{
			var tmpDate = LocalDate.parse(aInputStr);
			return LocalDateTime.of(tmpDate, LocalTime.MIN);
		}
		catch (DateTimeParseException aExp)
		{
			; // Nothing to do
		}

		return null;
	}

}
