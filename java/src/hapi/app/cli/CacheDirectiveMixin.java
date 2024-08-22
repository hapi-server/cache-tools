package hapi.app.cli;

import java.io.File;

import hapi.cache.CacheDirective;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

/**
 * Object which defines the (Picocli) arguments that allow specification of HAPI cache directives.
 * <p>
 * The following arguments are provided: --cache-dir, --stale-after, --use-stale-if-error
 *
 * @author lopeznr1
 */
public class CacheDirectiveMixin
{
	// Constants: Error messages
	private static final String ERR_STALE_AFTER_INVALID_INPUT = "--stale-after: Invalid input. Please run with --help-arg for details on argument aDura";

	@Option(names = { "--cache-dir" }, paramLabel = "<aPath>", //
			description = "Path to the top level HAPI cache", required = true)
	private File pathCacheDir;

	@Option(names = { "--stale-after" }, paramLabel = "<aDura>", //
			description = "Don't use cache if written before the specified time duration.")
	private String staleAfter;

	@Option(names = { "--use-stale-if-error" }, //
			description = "Utilize the (stale) cache if an attempt to update results in failure.")
	private boolean useStaleIfErr;

	/**
	 * Returns the {@link CacheDirective} associated with this {@link CacheDirectiveMixin}.
	 */
	public CacheDirective getCacheDirective()
	{
		return new CacheDirective(pathCacheDir, staleAfter, useStaleIfErr);
	}

	/**
	 * Method to ensure that the {@link CacheDirective} attributes are properly specified.
	 * <p>
	 * Validation consists of:
	 * <ul>
	 * <li>if the --stale-after option is specified, then ensure it can be properly parsed.
	 * </ul>
	 * <p>
	 * On any failure this will throw a {@link ParameterException}.
	 */
	public void validate(CommandLine aCommandLine)
	{
		// Ensure the --stale-after option is valid (if it is defined)
		if (staleAfter != null)
		{
			if (ArgDuraUtil.parseAsDuration(staleAfter) == null && ArgDuraUtil.parseAsLocalDateTime(staleAfter) == null)
				throw new ParameterException(aCommandLine, ERR_STALE_AFTER_INVALID_INPUT);
		}

		// Nothing else to validate
		return;
	}

}
