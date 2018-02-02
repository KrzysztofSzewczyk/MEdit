package medit.NSS;

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
