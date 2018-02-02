package medit.NTS;

public abstract class NTSEntry {
	String code;
	String exeName;
	String name;

	public NTSEntry(final String name, final String code, final String exeName) {
		this.name = name;
		this.code = code;
		this.exeName = exeName;
	}

	public abstract String getCode();

	public abstract String getExeName();

	public abstract String getName();
}
