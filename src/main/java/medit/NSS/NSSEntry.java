package medit.NSS;

/**
 * This class is 'entry' of new script system. It's representing script.
 *
 * @author Krzysztof Szewczyk
 *
 */

public abstract class NSSEntry {
	String codefn;
	String name;

	public NSSEntry(final String name, final String code) {
		this.name = name;
		this.codefn = code;
	}

	public abstract String getCodeFN();

	public abstract String getName();
}
