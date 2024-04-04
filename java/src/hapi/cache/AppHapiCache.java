package hapi.cache;

import java.io.IOException;

import hapi.SpecVersion;
import hapi.app.AppInfo;
import hapi.app.cli.ActionMixin;
import hapi.app.cli.CacheDirectiveMixin;
import hapi.app.cli.FetchQueryMixin;
import hapi.app.cli.IntroMixin;
import hapi.app.cli.PicocliParseUtil;
import hapi.server.ServerUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLineUtil;

/**
 * Class provides the main entry point into the HAPI cache (hapi-cache) application.
 *
 * @author lopeznr1
 */
@Command(name = "hapi-cache", sortOptions = false, usageHelpWidth = 120, //
		description = """
                              Application to interact with a HAPI cache. The following is provided:
                               - Log details of the local HAPI cache
                               - Pull remote content into the local HAPI cache
                               - Send to stdout a HAPI stream (from the local HAPI cache or a remote HAPI server)
                               - Expire stale data in the local HAPI cache
                              """
        )
public class AppHapiCache
{
	// Constants
	/** Defines the formal hapi-cache details */
	private static final AppInfo HapiCacheAppInfo = new AppInfo("HAPI-Cache", "0.0.2");

	// Picocli Arguments
	@Mixin
	private IntroMixin argIntroMixin;

	@Option(names = { "--dry-run" }, //
			description = "Perform a trial run with no changes made to the cache (or calling to the remote server).")
	private boolean isDryRun;

	@Option(names = { "--specVer" }, paramLabel = "<aSpecVer>", converter = PicocliParseUtil.ConverterSpecVersion.class, //
			description = "Defines the HAPI specification version. Supported values: [2, 3]. Default is: 2.\n")
	private SpecVersion argSpecVersion = SpecVersion.Version2;

	@Mixin
	ActionMixin argActionMixin;

	@Mixin
	private FetchQueryMixin argFetchQueryMixin;

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

			// Validate the action args
			app.argActionMixin.validate(tmpCL);

			// Validate the parameter args if action == fetchOnce
			if (app.argActionMixin.fetchOnce == true)
			{
				if (app.argFetchQueryMixin != null)
					app.argFetchQueryMixin.validate(tmpCL, app.argSpecVersion);
			}

		}
		catch (ParameterException aExp)
		{
			System.err.println(aExp.getMessage());
			System.exit(-1);
		}

		// Take the appropriate action
		var cacheDirective = app.argCacheDirectiveMixin.getCacheDirective();
		if (app.argActionMixin.fetchOnce == true)
			fetchOnce(cacheDirective, app.argSpecVersion, app.argFetchQueryMixin, app.isDryRun);
		else if (app.argActionMixin.proxyAttr != null)
			ServerUtil.startProxy(cacheDirective, app.argActionMixin.proxyAttr);
	}

	/**
	 * Utility helper method that will fetch the HAPI data (either from the cache or remote source) and return the HAPI
	 * stream on stdout.
	 */
	private static void fetchOnce(CacheDirective aCacheDirective, SpecVersion aSpecVersion,
			FetchQueryMixin aFetchQueryMixin, boolean aIsDryRun)
	{
		var tmpUrl = aFetchQueryMixin.getHapiUrl(aSpecVersion);

		// Log the action to be taken and bail
		if (aIsDryRun == true)
		{
			System.err.println("\n[dry-run] Action ---> fetchOnce: " + tmpUrl + "\n");
			return;
		}

		// TODO: Eventually we may want to pass the HapiCache2024 in rather than create it here...
		// Create the HapiCache2024 object.
		var hapiCache2024 = new HapiCache2024(aCacheDirective);

		// Fetch the content from the URL and return via stdout
		var outStream = System.out;
		try (var aInStream = hapiCache2024.getInputStream(tmpUrl))
		{
			aInStream.transferTo(outStream);
		}
		catch (IOException aExp)
		{
			aExp.printStackTrace();
		}
	}

}
