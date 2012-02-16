package fr.aliasource.funambol.utils;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps funambol
 * 
 * @author tom
 * 
 */
public class FunisHelper {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(FunisHelper.class);

	public static String removeQuotedPrintableFromVCalString(String vcal) {
		String cleaned = vcal.replace("\r\n", "\n");
		cleaned = cleaned.replace("\r", "");
		cleaned = cleaned.replace("\n\n", "\n");
		String[] lines = safeSplit(cleaned, '\n');
		StringBuffer noQuoted = new StringBuffer(lines.length * 76);
		boolean quotedMode = false;
		boolean concatNext = false;
		// concat quoted lines ending with =
		for (int i = 0; i < lines.length; i++) {
			String l = lines[i];

			if (l.contains("QUOTED-PRINTABLE")) {
				quotedMode = true;
			}
			if (quotedMode && l.endsWith("=")) {
				concatNext = true;
				l = l.substring(0, l.length() - 1);
			} else {
				concatNext = false;
				quotedMode = false;
			}
			if ((l.startsWith("DTSTART:") || l.startsWith("DESCRIPTION"))
					&& !noQuoted.toString().endsWith("\n")) {
				noQuoted.append('\n');
			}
			noQuoted.append(l);
			if (!concatNext && i < lines.length - 1
					&& !nextLineIsIndented(lines[i + 1])) {
				noQuoted.append('\n');
			}
		}

		String noq = noQuoted.toString();
		return noq;
	}

	private static boolean nextLineIsIndented(String l) {
		return l.startsWith(" ");
	}

	private static String[] safeSplit(String s, char sep) {
		ArrayList<String> al = new ArrayList<String>(25);
		StringBuffer cur = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == sep) {
				al.add(cur.toString());
				cur = new StringBuffer();
			} else {
				cur.append(s.charAt(i));
			}
		}
		al.add(cur.toString());

		String[] ret = al.toArray(new String[al.size()]);
		return ret;
	}

}
