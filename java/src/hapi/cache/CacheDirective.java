package hapi.cache;

import java.io.File;
import java.time.Duration;

/**
 * Defines the directives that specific to an actual cache.
 * <p>
 * The following attributes are supported:
 * <ul>
 * <li>rootCacheDir: The path to the top level folder of the HAPI cache.
 * <li>readCache: Boolean that defines if the cache should be read (and utilized for request fulfillment).
 * <li>writeCache: Boolean that defines if the cache should be updated (with any remote HAPI response).
 * <li>expireAfterDur: Defines how long the content in the cached should be utilized before considering it expired.
 * </ul>
 *
 * @author lopeznr1
 */
public record CacheDirective(File rootCacheDir, boolean readCache, boolean writeCache, Duration expireAfterDur)
{

}
