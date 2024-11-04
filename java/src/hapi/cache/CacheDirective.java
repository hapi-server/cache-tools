package hapi.cache;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

import hapi.app.cli.ArgDuraUtil;

/**
 * Defines the directives that specific to an actual cache.
 * <p>
 * The following attributes are supported:
 * <ul>
 * <li>rootCacheDir: The path to the top level folder of the HAPI cache.
 * <li>staleAfter: Defines how long the content in the cached should be utilized before considering it stale.
 * <li>useStaleIfErr: Defines if the (stale) cache should be utilized, if an attempt to update results in failure.
 * </ul>
 *
 * @author lopeznr1
 */
public record CacheDirective(File rootCacheDir, String staleAfter, boolean useStaleIfErr)
{
	/**
	 * Returns the staleAfter attribute as a {@link Duration}. Returns null if the attribute was not expressed as a
	 * {@link Duration}.
	 */
	public Duration getStaleAfterAsDuration()
	{
		// Delegate
		return ArgDuraUtil.parseAsDuration(staleAfter);
	}

	/**
	 * Returns the staleAfter attribute as a {@link LocalDateTime}. Returns null if the attribute was not expressed as a
	 * {@link LocalDateTime}.
	 */
	public LocalDateTime getStaleAfterAsLocalDateTime()
	{
		// Delegate
		return ArgDuraUtil.parseAsLocalDateTime(staleAfter);
	}

}
