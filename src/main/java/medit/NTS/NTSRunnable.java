package medit.NTS;

public abstract class NTSRunnable implements Runnable {
	public String exeName, code;
	public NTSRunnable(String nExeName, String nCode) {
		exeName = nExeName;
		code = nCode;
	}
}
