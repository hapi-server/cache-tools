package hapi.cache;

import hapi.app.AppInfo;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParameterException;
import picocli.CommandLineUtil;

/**
 * Class provides the main entry point into the HAPI cache (hapi-cache) application.
 * 
 * @author lopeznr1
 */
@Command(name = "hapi-cache", sortOptions = false, usageHelpWidth = 120, //
		description = "\nApplication to interact with a HAPI cache. The following is provided:\n" + //
				" - Log details of the local HAPI cache\n" + //
				" - Pull remote content into the local HAPI cache\n" + //
				" - Send to stdout a HAPI stream (from the local HAPI cache or a remote HAPI server)\n" + //
				" - Expire stale data in the local HAPI cache\n")
public class AppHapiCache
{
	// Constants
	/** Defines the formal hapi-cache details */
	private static final AppInfo HapiCacheAppInfo = new AppInfo("HAPI-Cache", "0.0.1");

	// Picocli Arguments
	@Mixin
	private IntroMixin argIntroMixin;

	@Mixin
	private ServerMixin argServerMixin;

	@Mixin
	private CacheDirectiveMixin argCacheDirectiveMixin;

	/**
	 * Main entry point of application
	 */
	public static void main(String... aArgArr) throws Exception
	{
		var app = new AppHapiCache();

		// Parse the args
		try
		{
			var tmpCL = new CommandLine(app);

			// Show robust help or version (even if previous arguments are bogus)
			CommandLineUtil.showHelpAndExitIfRequested(HapiCacheAppInfo, tmpCL, aArgArr);

			tmpCL.parseArgs(aArgArr);

			// Validate the HAPI server specification
			if (app.argServerMixin != null)
				app.argServerMixin.validateHapiServerSpecification(tmpCL);

		}
		catch (ParameterException aExp)
		{
			System.err.println(aExp.getMessage());
			System.exit(-1);
		}

	}

}
