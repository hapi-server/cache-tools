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
	 * Input strings will be parsed with the following specification: An integer followed by a unit char (d, h, m, s)
	 * which corresponds to (days, hours, minutes, seconds).
	 * <p>
	 * On failure null will be returned.
	 */
	public static Duration parseAsDuration(String aInputStr)
	{
		if (aInputStr == null || aInputStr.length() < 2)
			return null;

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
