package hapi.app.cli;

import java.io.File;
import java.time.Duration;

import picocli.CommandLine.Option;

/**
 * Object which defines the (Picocli) arguments that allow specification of HAPI cache directives.
 * <p>
 * The following arguments are provided: --cache-dir, --read-cache, --write-cache, --expire-after
 * 
 * @author lopeznr1
 */
public class CacheDirectiveMixin
{
	@Option(names = { "--cache-dir" }, paramLabel = "<path>", //
			description = "Path to the top level HAPI cache", required = true)
	private File pathCacheDir;

	@Option(names = { "--read-cache" }, //
			description = "Use cache if there")
	private boolean readCache;

	@Option(names = { "--write-cache" }, //
			description = "Write cache if there")
	private boolean writeCache;

	@Option(names = { "--expire-after" }, paramLabel = "<time>", //
			description = "Don't use cache if written before the specified delta time."
					+ " Delta time input: N{y, d, h, m, s} where N is number of time units and unit is one of 'year, day, hour, min, sec'")
	private Duration expireAfterDur;

	@Option(names = { "--cache-exact" }, paramLabel = "aBool", //
			description = "Only cache exact request; Will lead to less cache hits but fast " //
					+ "cache response if exact request is made again.")
	private boolean cacheExact;

}
