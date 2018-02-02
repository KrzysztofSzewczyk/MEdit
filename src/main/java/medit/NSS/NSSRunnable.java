package medit.NSS;

public abstract class NSSRunnable implements Runnable {
	public String codefn;

	public NSSRunnable(final String nCodeFN) {
		this.codefn = nCodeFN;
	}
}
