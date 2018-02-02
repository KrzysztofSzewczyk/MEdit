package medit.NTS;

public abstract class NTSRunnable implements Runnable {
	public String exeName, code;

	public NTSRunnable(final String nExeName, final String nCode) {
		this.exeName = nExeName;
		this.code = nCode;
	}
}
