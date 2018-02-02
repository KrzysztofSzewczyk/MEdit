/*
 * 12/14/08
 *
 * AbstractTokenMakerFactory.java - Base class for TokenMaker implementations.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for {@link TokenMakerFactory} implementations. A mapping from
 * language keys to the names of {@link TokenMaker} classes is stored.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractTokenMakerFactory extends TokenMakerFactory {

	/**
	 * Wrapper that handles the creation of TokenMaker instances.
	 */
	private static class TokenMakerCreator {

		private final ClassLoader cl;
		private final String className;

		public TokenMakerCreator(final String className, final ClassLoader cl) {
			this.className = className;
			this.cl = cl != null ? cl : this.getClass().getClassLoader();
		}

		public TokenMaker create() throws Exception {
			return (TokenMaker) Class.forName(this.className, true, this.cl).newInstance();
		}

	}

	/**
	 * A mapping from keys to the names of {@link TokenMaker} implementation class
	 * names. When {@link #getTokenMaker(String)} is called with a key defined in
	 * this map, a <code>TokenMaker</code> of the corresponding type is returned.
	 */
	private final Map<String, Object> tokenMakerMap;

	/**
	 * Constructor.
	 */
	protected AbstractTokenMakerFactory() {
		this.tokenMakerMap = new HashMap<>();
		this.initTokenMakerMap();
	}

	/**
	 * Returns a {@link TokenMaker} for the specified key.
	 *
	 * @param key
	 *            The key.
	 * @return The corresponding <code>TokenMaker</code>, or <code>null</code> if
	 *         none matches the specified key.
	 */
	@Override
	protected TokenMaker getTokenMakerImpl(final String key) {
		final TokenMakerCreator tmc = (TokenMakerCreator) this.tokenMakerMap.get(key);
		if (tmc != null)
			try {
				return tmc.create();
			} catch (final RuntimeException re) { // FindBugs
				throw re;
			} catch (final Exception e) {
				e.printStackTrace();
			}
		return null;
	}

	/**
	 * Populates the mapping from keys to instances of
	 * <code>TokenMakerCreator</code>s. Subclasses should override this method and
	 * call one of the <code>putMapping</code> overloads to register
	 * {@link TokenMaker}s for syntax constants.
	 *
	 * @see #putMapping(String, String)
	 * @see #putMapping(String, String, ClassLoader)
	 */
	protected abstract void initTokenMakerMap();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> keySet() {
		return this.tokenMakerMap.keySet();
	}

	/**
	 * Adds a mapping from a key to a <code>TokenMaker</code> implementation class
	 * name.
	 *
	 * @param key
	 *            The key.
	 * @param className
	 *            The <code>TokenMaker</code> class name.
	 * @see #putMapping(String, String, ClassLoader)
	 */
	public void putMapping(final String key, final String className) {
		this.putMapping(key, className, null);
	}

	/**
	 * Adds a mapping from a key to a <code>TokenMaker</code> implementation class
	 * name.
	 *
	 * @param key
	 *            The key.
	 * @param className
	 *            The <code>TokenMaker</code> class name.
	 * @param cl
	 *            The class loader to use when loading the class.
	 * @see #putMapping(String, String)
	 */
	public void putMapping(final String key, final String className, final ClassLoader cl) {
		this.tokenMakerMap.put(key, new TokenMakerCreator(className, cl));
	}

}