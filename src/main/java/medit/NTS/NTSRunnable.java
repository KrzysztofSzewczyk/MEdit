package medit.NTS;

/**
 * This page is extending runnable type to suit my needs.
 * @author Krzysztof Szewczyk
 *
 */

public abstract class NTSRunnable implements Runnable {
	public String exeName, code;

	public NTSRunnable(final String nExeName, final String nCode) {
		this.exeName = nExeName;
		this.code = nCode;
	}
}
