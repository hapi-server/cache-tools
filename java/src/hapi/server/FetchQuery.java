package hapi.server;

import java.util.Collection;

// import org.hapiserver.TimeUtil;

/**
 * Defines the attributes specific to a HAPI data fetch.
 * <p>
 * The following attributes are supported:
 * <ul>
 * <li>The HAPI dataset of interest.
 * <li>The start time for the HAPI data.
 * <li>The stop time for the HAPI data.
 * <li>The list of parameters of interest. If this is null or empty, then all parameters should be returned.
 * <li>A flag that defines if various headers should be returned.
 * </ul>
 * Note the following:</br>
 * - The times string pattern is equivalent to that found in {@link TimeUtil}</br>
 * - This record does not reference the actual HAPI data endpoint (URL).
 *
 * @author lopeznr1
 */
public record FetchQuery(String dataset, String timeBeg, String timeEnd, Collection<String> parameterL,
		boolean includeHeader)
{

}
