package picocli;

import hapi.app.AppInfo;

/**
 * Collection of utility methods and definitions associated with the
 * {@link CommandLine}.
 *
 * @author lopeznr1
 */
public class CommandLineUtil {

	/**
	 * Iterates through the arguments and if the values '-h' or '--help' are
	 * specified then will automatically display the help message and exit the
	 * application.
	 */
	public static void showHelpAndExitIfRequested(AppInfo aAppInfo, CommandLine aCommandLine, String[] aArgArr) {
		boolean showVersion = false;

		int helpLev = 0;
		for (var aArg : aArgArr) {
			if ("--version".equals(aArg) == true)
				showVersion = true;
			else if ("--help".equals(aArg) == true)
				helpLev++;
			else if (aArg.startsWith("-h") == true) {
				for (int c1 = 0; c1 < aArg.length(); c1++) {
					char tmpChar = aArg.charAt(c1);
					if (tmpChar == 'h' || tmpChar == 'v')
						helpLev++;
				}
			}
		}

		// Show version and exit
		if (helpLev == 0 && showVersion == true) {
			System.out.println(aAppInfo.name() + " - Version: " + aAppInfo.version());
			System.exit(0);
		}

		// Help is not requested, bail
		if (helpLev == 0)
			return;

		// Help is requested an show the appropriate level
		if (helpLev >= 1)
			aCommandLine.usage(System.out);

		System.exit(0);
	}

}