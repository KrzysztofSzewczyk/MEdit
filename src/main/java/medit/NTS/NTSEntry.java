package medit.NTS;

public abstract class NTSEntry {
	String name;
	String code;
	String exeName;

	public NTSEntry(String name, String code, String exeName) {
		this.name = name;
		this.code = code;
		this.exeName = exeName;
	}

	public abstract String getName();

	public abstract String getCode();
	
	public abstract String getExeName();
}
