package medit.NSS;

/**
 * This page is extending runnable type to suit my needs.
 * @author Krzysztof Szewczyk
 *
 */

public abstract class NSSRunnable implements Runnable {
	public String codefn;

	public NSSRunnable(final String nCodeFN) {
		this.codefn = nCodeFN;
	}
}
