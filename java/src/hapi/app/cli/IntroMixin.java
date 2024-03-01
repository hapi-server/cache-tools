package hapi.app.cli;

import picocli.CommandLine.Option;

/**
 * Object which defines the (Picocli) arguments that allow specification of the standard introductory arguments.
 * <p>
 * The following arguments are provided: -h, --help, -v, --verbose, --version
 * 
 * @author lopeznr1
 */
public class IntroMixin
{
	@Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help message.")
	private boolean showHelp;

	@Option(names = { "-v", "--verbose" }, description = "Increase verbose info.")
	private boolean[] verboseLev;

	@Option(names = { "--version" }, description = "Display the version.\n", versionHelp = true)
	private boolean showVersion;

}
