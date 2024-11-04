package picocli;

import java.io.PrintStream;

import hapi.app.AppInfo;
import picocli.CommandLine.Help.Ansi;

/**
 * Collection of utility methods and definitions associated with the {@link CommandLine}.
 *
 * @author lopeznr1
 */
public class CommandLineUtil
{
	/**
	 * Iterates through the command arguments and if a help or version option is specified then the relevant text will be
	 * sent to stdout and the application will exit.
	 */
	public static void showHelpAndExitIfRequested(AppInfo aAppInfo, CommandLine aCommandLine, String[] aArgArr)
	{
		var showHelpArg = false;
		var showHelpReg = false;
		var showVersion = false;

		for (var aArg : aArgArr)
		{
			if ("--help-arg".equals(aArg) == true)
				showHelpArg = true;
			else if ("--help".equals(aArg) == true)
				showHelpReg = true;
			else if (aArg.matches("\\-[A-z]+") == true && aArg.contains("h") == true)
				showHelpReg = true;
			else if ("--version".equals(aArg) == true)
				showVersion = true;
		}

		// Show version and exit
		if (showHelpReg == false && showHelpArg == false && showVersion == true)
		{
			System.out.println(aAppInfo.name() + " - Version: " + aAppInfo.version());
			System.exit(0);
		}

		// Bail if help has not been requested
		if (showHelpReg == false && showHelpArg == false)
			return;

		// Show the relevant help and exit
		if (showHelpReg == true)
			aCommandLine.usage(System.out);
		if (showHelpArg == true)
			showHelpOnArgs(System.out);

		System.exit(0);
	}

	/**
	 * Displays user help information on various option arguments.
	 */
	public static void showHelpOnArgs(PrintStream aPrintStream)
	{
		var fullMsg = "Listed below are details on select option arguments:\n\n";

		var tmpMsg = """
				@|yellow aDura|@: Parameter defines a time duration.
				----------------------------------------------------------------------------
				The value associated with this parameter can be expressed as one of the
				following:
				[1] A relative duration such that:
				        N{d,h,m,s}
				    where N represents a number and the unit character {d,h,m,s}
				    corresponds to (days, hours, minutes, seconds).

				[2] A relative duration expressed as an ISO-8601 string. Example:
				        P2DT-3H4M
				        PT15M

				[3] A duration since an explicit time such as:
				        yyyy-MM-dd
				        yyyy-MM-ddThh:mm:ss


				@|yellow aTime|@: Parameter defines an explicit time epoch.
				----------------------------------------------------------------------------
				The value associated with this parameter can be expressed as::
				        yyyy-MM-dd
				        yyyy-MM-dd hh:mm:ss\n""";
		tmpMsg = "   " + tmpMsg.replaceAll("\n", "\n   ").trim();
		fullMsg += tmpMsg;

		aPrintStream.println("\n" + Ansi.AUTO.string(fullMsg) + "\n");
	}

}
