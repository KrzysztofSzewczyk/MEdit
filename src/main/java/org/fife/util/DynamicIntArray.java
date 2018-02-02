/*
 * 03/26/2004
 *
 * DynamicIntArray.java - Similar to an ArrayList, but holds ints instead
 * of Objects.
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Similar to a <code>java.util.ArrayList</code>, but specifically for
 * <code>int</code>s. This is basically an array of integers that resizes itself
 * (if necessary) when adding new elements.
 *
 * @author Robert Futrell
 * @version 0.8
 */
public class DynamicIntArray implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The actual data.
	 */
	private int[] data;

	/**
	 * The number of values in the array. Note that this is NOT the capacity of the
	 * array; rather, <code>size &lt;= capacity</code>.
	 */
	private int size;

	/**
	 * Constructs a new array object with an initial capacity of 10.
	 */
	public DynamicIntArray() {
		this(10);
	}

	/**
	 * Constructs a new array object with a given initial capacity.
	 *
	 * @param initialCapacity
	 *            The initial capacity.
	 * @throws IllegalArgumentException
	 *             If <code>initialCapacity</code> is negative.
	 */
	public DynamicIntArray(final int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal initialCapacity: " + initialCapacity);
		this.data = new int[initialCapacity];
		this.size = 0;
	}

	/**
	 * Constructs a new array object from the given int array. The resulting
	 * <code>DynamicIntArray</code> will have an initial capacity of 110% the size
	 * of the array.
	 *
	 * @param intArray
	 *            Initial data for the array object.
	 * @throws NullPointerException
	 *             If <code>intArray</code> is <code>null</code>.
	 */
	public DynamicIntArray(final int[] intArray) {
		this.size = intArray.length;
		final int capacity = (int) Math.min(this.size * 110L / 100, Integer.MAX_VALUE);
		this.data = new int[capacity];
		System.arraycopy(intArray, 0, this.data, 0, this.size); // source, dest, length.
	}

	/**
	 * Appends the specified <code>int</code> to the end of this array.
	 *
	 * @param value
	 *            The <code>int</code> to be appended to this array.
	 */
	public void add(final int value) {
		this.ensureCapacity(this.size + 1);
		this.data[this.size++] = value;
	}

	/**
	 * Inserts the specified <code>int</code> at the specified position in this
	 * array. Shifts the <code>int</code> currently at that position (if any) and
	 * any subsequent <code>int</code>s to the right (adds one to their indices).
	 *
	 * @param index
	 *            The index at which the specified integer is to be inserted.
	 * @param value
	 *            The <code>int</code> to be inserted.
	 * @throws IndexOutOfBoundsException
	 *             If <code>index</code> is less than zero or greater than
	 *             <code>getSize()</code>.
	 */
	public void add(final int index, final int value) {
		if (index > this.size)
			this.throwException2(index);
		this.ensureCapacity(this.size + 1);
		System.arraycopy(this.data, index, this.data, index + 1, this.size - index);
		this.data[index] = value;
		this.size++;
	}

	/**
	 * Inserts all <code>int</code>s in the specified array into this array object
	 * at the specified location. Shifts the <code>int</code> currently at that
	 * position (if any) and any subsequent <code>int</code>s to the right (adds one
	 * to their indices).
	 *
	 * @param index
	 *            The index at which the specified integer is to be inserted.
	 * @param intArray
	 *            The array of <code>int</code>s to insert.
	 * @throws IndexOutOfBoundsException
	 *             If <code>index</code> is less than zero or greater than
	 *             <code>getSize()</code>.
	 * @throws NullPointerException
	 *             If <code>intArray</code> is <code>null</code>.
	 */
	public void add(final int index, final int[] intArray) {
		if (index > this.size)
			this.throwException2(index);
		final int addCount = intArray.length;
		this.ensureCapacity(this.size + addCount);
		final int moveCount = this.size - index;
		if (moveCount > 0)
			System.arraycopy(this.data, index, this.data, index + addCount, moveCount);
		System.arraycopy(intArray, 0, this.data, index, addCount);
		this.size += addCount;
	}

	/**
	 * Removes all values from this array object. Capacity will remain the same.
	 */
	public void clear() {
		this.size = 0;
	}

	/**
	 * Returns whether this array contains a given integer. This method performs a
	 * linear search, so it is not optimized for performance.
	 *
	 * @param integer
	 *            The <code>int</code> for which to search.
	 * @return Whether the given integer is contained in this array.
	 */
	public boolean contains(final int integer) {
		for (int i = 0; i < this.size; i++)
			if (this.data[i] == integer)
				return true;
		return false;
	}

	/**
	 * Decrements all values in the array in the specified range.
	 *
	 * @param from
	 *            The range start offset (inclusive).
	 * @param to
	 *            The range end offset (exclusive).
	 * @see #increment(int, int)
	 */
	public void decrement(final int from, final int to) {
		for (int i = from; i < to; i++)
			this.data[i]--;
	}

	/**
	 * Makes sure that this <code>DynamicIntArray</code> instance can hold at least
	 * the number of elements specified. If it can't, then the capacity is
	 * increased.
	 *
	 * @param minCapacity
	 *            The desired minimum capacity.
	 */
	private void ensureCapacity(final int minCapacity) {
		final int oldCapacity = this.data.length;
		if (minCapacity > oldCapacity) {
			final int[] oldData = this.data;
			// Ensures we don't just keep increasing capacity by some small
			// number like 1...
			int newCapacity = oldCapacity * 3 / 2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			this.data = new int[newCapacity];
			System.arraycopy(oldData, 0, this.data, 0, this.size);
		}
	}

	/**
	 * Sets the value of all entries in this array to the specified value.
	 *
	 * @param value
	 *            The new value for all elements in the array.
	 */
	public void fill(final int value) {
		Arrays.fill(this.data, value);
	}

	/**
	 * Returns the <code>int</code> at the specified position in this array object.
	 *
	 * @param index
	 *            The index of the <code>int</code> to return.
	 * @return The <code>int</code> at the specified position in this array.
	 * @throws IndexOutOfBoundsException
	 *             If <code>index</code> is less than zero or greater than or equal
	 *             to <code>getSize()</code>.
	 */
	public int get(final int index) {
		// Small enough to be inlined, and throwException() is rarely called.
		if (index >= this.size)
			this.throwException(index);
		return this.data[index];
	}

	/**
	 * Returns the number of <code>int</code>s in this array object.
	 *
	 * @return The number of <code>int</code>s in this array object.
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Returns the <code>int</code> at the specified position in this array object,
	 * without doing any bounds checking. You really should use {@link #get(int)}
	 * instead of this method.
	 *
	 * @param index
	 *            The index of the <code>int</code> to return.
	 * @return The <code>int</code> at the specified position in this array.
	 */
	public int getUnsafe(final int index) {
		// Small enough to be inlined.
		return this.data[index];
	}

	/**
	 * Increments all values in the array in the specified range.
	 *
	 * @param from
	 *            The range start offset (inclusive).
	 * @param to
	 *            The range end offset (exclusive).
	 * @see #decrement(int, int)
	 */
	public void increment(final int from, final int to) {
		for (int i = from; i < to; i++)
			this.data[i]++;
	}

	public void insertRange(final int offs, final int count, final int value) {
		if (offs > this.size)
			this.throwException2(offs);
		this.ensureCapacity(this.size + count);
		System.arraycopy(this.data, offs, this.data, offs + count, this.size - offs);
		if (value != 0)
			Arrays.fill(this.data, offs, offs + count, value);
		this.size += count;
	}

	/**
	 * Returns whether or not this array object is empty.
	 *
	 * @return Whether or not this array object contains no elements.
	 */
	public boolean isEmpty() {
		return this.size == 0;
	}

	/**
	 * Removes the <code>int</code> at the specified location from this array
	 * object.
	 *
	 * @param index
	 *            The index of the <code>int</code> to remove.
	 * @throws IndexOutOfBoundsException
	 *             If <code>index</code> is less than zero or greater than or equal
	 *             to <code>getSize()</code>.
	 */
	public void remove(final int index) {
		if (index >= this.size)
			this.throwException(index);
		final int toMove = this.size - index - 1;
		if (toMove > 0)
			System.arraycopy(this.data, index + 1, this.data, index, toMove);
		--this.size;
	}

	/**
	 * Removes the <code>int</code>s in the specified range from this array object.
	 *
	 * @param fromIndex
	 *            The index of the first <code>int</code> to remove.
	 * @param toIndex
	 *            The index AFTER the last <code>int</code> to remove.
	 * @throws IndexOutOfBoundsException
	 *             If either of <code>fromIndex</code> or <code>toIndex</code> is
	 *             less than zero or greater than or equal to
	 *             <code>getSize()</code>.
	 */
	public void removeRange(final int fromIndex, final int toIndex) {
		if (fromIndex >= this.size || toIndex > this.size)
			this.throwException3(fromIndex, toIndex);
		final int moveCount = this.size - toIndex;
		System.arraycopy(this.data, toIndex, this.data, fromIndex, moveCount);
		this.size -= toIndex - fromIndex;
	}

	/**
	 * Sets the <code>int</code> value at the specified position in this array
	 * object.
	 *
	 * @param index
	 *            The index of the <code>int</code> to set
	 * @param value
	 *            The value to set it to.
	 * @throws IndexOutOfBoundsException
	 *             If <code>index</code> is less than zero or greater than or equal
	 *             to <code>getSize()</code>.
	 */
	public void set(final int index, final int value) {
		// Small enough to be inlined, and throwException() is rarely called.
		if (index >= this.size)
			this.throwException(index);
		this.data[index] = value;
	}

	/**
	 * Sets the <code>int</code> value at the specified position in this array
	 * object, without doing any bounds checking. You should use
	 * {@link #set(int, int)} instead of this method.
	 *
	 * @param index
	 *            The index of the <code>int</code> to set
	 * @param value
	 *            The value to set it to.
	 */
	public void setUnsafe(final int index, final int value) {
		// Small enough to be inlined.
		this.data[index] = value;
	}

	/**
	 * Throws an exception. This method isolates error-handling code from the
	 * error-checking code, so that callers (e.g. {@link #get} and {@link #set}) can
	 * be both small enough to be inlined, as well as not usually make any expensive
	 * method calls (since their callers will usually not pass illegal arguments to
	 * them).
	 *
	 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956">
	 * this Sun bug report</a> for more information.
	 *
	 * @param index
	 *            The invalid index.
	 * @throws IndexOutOfBoundsException
	 *             Always.
	 */
	private void throwException(final int index) {
		throw new IndexOutOfBoundsException("Index " + index + " not in valid range [0-" + (this.size - 1) + "]");
	}

	/**
	 * Throws an exception. This method isolates error-handling code from the
	 * error-checking code, so that callers can be both small enough to be inlined,
	 * as well as not usually make any expensive method calls (since their callers
	 * will usually not pass illegal arguments to them).
	 *
	 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956">
	 * this Sun bug report</a> for more information.
	 *
	 * @param index
	 *            The invalid index.
	 * @throws IndexOutOfBoundsException
	 *             Always.
	 */
	private void throwException2(final int index) {
		throw new IndexOutOfBoundsException("Index " + index + ", not in range [0-" + this.size + "]");
	}

	/**
	 * Throws an exception. This method isolates error-handling code from the
	 * error-checking code, so that callers can be both small enough to be inlined,
	 * as well as not usually make any expensive method calls (since their callers
	 * will usually not pass illegal arguments to them).
	 *
	 * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5103956">
	 * this Sun bug report</a> for more information.
	 *
	 * @param index
	 *            The invalid index.
	 * @throws IndexOutOfBoundsException
	 *             Always.
	 */
	private void throwException3(final int fromIndex, final int toIndex) {
		throw new IndexOutOfBoundsException(
				"Index range [" + fromIndex + ", " + toIndex + "] not in valid range [0-" + (this.size - 1) + "]");
	}

}