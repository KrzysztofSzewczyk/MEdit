/*
 * 12/12/2008
 *
 * TokenMakerFactory.java - A factory for TokenMakers.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.Set;

import org.fife.ui.rsyntaxtextarea.modes.PlainTextTokenMaker;

/**
 * A factory that maps syntax styles to {@link TokenMaker}s capable of splitting
 * text into tokens for those syntax styles.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class TokenMakerFactory {

	/**
	 * The singleton default <code>TokenMakerFactory</code> instance.
	 */
	private static TokenMakerFactory DEFAULT_INSTANCE;

	/**
	 * If this system property is set, a custom <code>TokenMakerFactory</code> of
	 * the specified class will be used as the default token maker factory.
	 */
	public static final String PROPERTY_DEFAULT_TOKEN_MAKER_FACTORY = "TokenMakerFactory";

	/**
	 * Returns the default <code>TokenMakerFactory</code> instance. This is the
	 * factory used by all {@link RSyntaxDocument}s by default.
	 *
	 * @return The factory.
	 * @see #setDefaultInstance(TokenMakerFactory)
	 */
	public static synchronized TokenMakerFactory getDefaultInstance() {
		if (TokenMakerFactory.DEFAULT_INSTANCE == null) {
			String clazz = null;
			try {
				clazz = System.getProperty(TokenMakerFactory.PROPERTY_DEFAULT_TOKEN_MAKER_FACTORY);
			} catch (final java.security.AccessControlException ace) {
				clazz = null; // We're in an applet; take default.
			}
			if (clazz == null)
				clazz = "org.fife.ui.rsyntaxtextarea.DefaultTokenMakerFactory";
			try {
				TokenMakerFactory.DEFAULT_INSTANCE = (TokenMakerFactory) Class.forName(clazz).newInstance();
			} catch (final RuntimeException re) { // FindBugs
				throw re;
			} catch (final Exception e) {
				e.printStackTrace();
				throw new InternalError("Cannot find TokenMakerFactory: " + clazz);
			}
		}
		return TokenMakerFactory.DEFAULT_INSTANCE;
	}

	/**
	 * Sets the default <code>TokenMakerFactory</code> instance. This is the factory
	 * used by all future {@link RSyntaxDocument}s by default.
	 * <code>RSyntaxDocument</code>s that have already been created are not
	 * affected.
	 *
	 * @param tmf
	 *            The factory.
	 * @throws IllegalArgumentException
	 *             If <code>tmf</code> is <code>null</code>.
	 * @see #getDefaultInstance()
	 */
	public static synchronized void setDefaultInstance(final TokenMakerFactory tmf) {
		if (tmf == null)
			throw new IllegalArgumentException("tmf cannot be null");
		TokenMakerFactory.DEFAULT_INSTANCE = tmf;
	}

	/**
	 * Returns a {@link TokenMaker} for the specified key.
	 *
	 * @param key
	 *            The key.
	 * @return The corresponding <code>TokenMaker</code>, or
	 *         {@link PlainTextTokenMaker} if none matches the specified key.
	 */
	public final TokenMaker getTokenMaker(final String key) {
		TokenMaker tm = this.getTokenMakerImpl(key);
		if (tm == null)
			tm = new PlainTextTokenMaker();
		return tm;
	}

	/**
	 * Returns a {@link TokenMaker} for the specified key.
	 *
	 * @param key
	 *            The key.
	 * @return The corresponding <code>TokenMaker</code>, or <code>null</code> if
	 *         none matches the specified key.
	 */
	protected abstract TokenMaker getTokenMakerImpl(String key);

	/**
	 * Returns the set of keys that this factory maps to token makers.
	 *
	 * @return The set of keys.
	 */
	public abstract Set<String> keySet();

}