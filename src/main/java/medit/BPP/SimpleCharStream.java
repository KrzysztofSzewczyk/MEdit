/* Generated By:JavaCC: Do not edit this line. SimpleCharStream.java Version 5.0 */
/* JavaCCOptions:STATIC=false,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package medit.BPP;

/**
 * An implementation of interface CharStream, where the stream is assumed to
 * contain only ASCII characters (without unicode processing).
 */

public class SimpleCharStream {
	/** Whether parser is static. */
	public static final boolean staticFlag = false;
	int available;
	protected int bufcolumn[];
	protected char[] buffer;
	protected int bufline[];
	/** Position in buffer. */
	public int bufpos = -1;
	int bufsize;

	protected int column = 0;
	protected int inBuf = 0;

	protected java.io.Reader inputStream;
	protected int line = 1;

	protected int maxNextCharInd = 0;

	protected boolean prevCharIsCR = false;
	protected boolean prevCharIsLF = false;
	protected int tabSize = 8;
	int tokenBegin;

	/** Constructor. */
	public SimpleCharStream(final java.io.InputStream dstream) {
		this(dstream, 1, 1, 4096);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.InputStream dstream, final int startline, final int startcolumn) {
		this(dstream, startline, startcolumn, 4096);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.InputStream dstream, final int startline, final int startcolumn,
			final int buffersize) {
		this(new java.io.InputStreamReader(dstream), startline, startcolumn, buffersize);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.InputStream dstream, final String encoding)
			throws java.io.UnsupportedEncodingException {
		this(dstream, encoding, 1, 1, 4096);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.InputStream dstream, final String encoding, final int startline,
			final int startcolumn) throws java.io.UnsupportedEncodingException {
		this(dstream, encoding, startline, startcolumn, 4096);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.InputStream dstream, final String encoding, final int startline,
			final int startcolumn, final int buffersize) throws java.io.UnsupportedEncodingException {
		this(encoding == null ? new java.io.InputStreamReader(dstream)
				: new java.io.InputStreamReader(dstream, encoding), startline, startcolumn, buffersize);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.Reader dstream) {
		this(dstream, 1, 1, 4096);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.Reader dstream, final int startline, final int startcolumn) {
		this(dstream, startline, startcolumn, 4096);
	}

	/** Constructor. */
	public SimpleCharStream(final java.io.Reader dstream, final int startline, final int startcolumn,
			final int buffersize) {
		this.inputStream = dstream;
		this.line = startline;
		this.column = startcolumn - 1;

		this.available = this.bufsize = buffersize;
		this.buffer = new char[buffersize];
		this.bufline = new int[buffersize];
		this.bufcolumn = new int[buffersize];
	}

	/**
	 * Method to adjust line and column numbers for the start of a token.
	 */
	public void adjustBeginLineColumn(int newLine, final int newCol) {
		int start = this.tokenBegin;
		int len;

		if (this.bufpos >= this.tokenBegin)
			len = this.bufpos - this.tokenBegin + this.inBuf + 1;
		else
			len = this.bufsize - this.tokenBegin + this.bufpos + 1 + this.inBuf;

		int i = 0, j = 0, k = 0;
		int nextColDiff = 0, columnDiff = 0;

		while (i < len && this.bufline[j = start % this.bufsize] == this.bufline[k = ++start % this.bufsize]) {
			this.bufline[j] = newLine;
			nextColDiff = columnDiff + this.bufcolumn[k] - this.bufcolumn[j];
			this.bufcolumn[j] = newCol + columnDiff;
			columnDiff = nextColDiff;
			i++;
		}

		if (i < len) {
			this.bufline[j] = newLine++;
			this.bufcolumn[j] = newCol + columnDiff;

			while (i++ < len)
				if (this.bufline[j = start % this.bufsize] != this.bufline[++start % this.bufsize])
					this.bufline[j] = newLine++;
				else
					this.bufline[j] = newLine;
		}

		this.line = this.bufline[j];
		this.column = this.bufcolumn[j];
	}

	/** Backup a number of characters. */
	public void backup(final int amount) {

		this.inBuf += amount;
		if ((this.bufpos -= amount) < 0)
			this.bufpos += this.bufsize;
	}

	/** Start. */
	public char BeginToken() throws java.io.IOException {
		this.tokenBegin = -1;
		final char c = this.readChar();
		this.tokenBegin = this.bufpos;

		return c;
	}

	/** Reset buffer when finished. */
	public void Done() {
		this.buffer = null;
		this.bufline = null;
		this.bufcolumn = null;
	}

	protected void ExpandBuff(final boolean wrapAround) {
		final char[] newbuffer = new char[this.bufsize + 2048];
		final int newbufline[] = new int[this.bufsize + 2048];
		final int newbufcolumn[] = new int[this.bufsize + 2048];

		try {
			if (wrapAround) {
				System.arraycopy(this.buffer, this.tokenBegin, newbuffer, 0, this.bufsize - this.tokenBegin);
				System.arraycopy(this.buffer, 0, newbuffer, this.bufsize - this.tokenBegin, this.bufpos);
				this.buffer = newbuffer;

				System.arraycopy(this.bufline, this.tokenBegin, newbufline, 0, this.bufsize - this.tokenBegin);
				System.arraycopy(this.bufline, 0, newbufline, this.bufsize - this.tokenBegin, this.bufpos);
				this.bufline = newbufline;

				System.arraycopy(this.bufcolumn, this.tokenBegin, newbufcolumn, 0, this.bufsize - this.tokenBegin);
				System.arraycopy(this.bufcolumn, 0, newbufcolumn, this.bufsize - this.tokenBegin, this.bufpos);
				this.bufcolumn = newbufcolumn;

				this.maxNextCharInd = this.bufpos += this.bufsize - this.tokenBegin;
			} else {
				System.arraycopy(this.buffer, this.tokenBegin, newbuffer, 0, this.bufsize - this.tokenBegin);
				this.buffer = newbuffer;

				System.arraycopy(this.bufline, this.tokenBegin, newbufline, 0, this.bufsize - this.tokenBegin);
				this.bufline = newbufline;

				System.arraycopy(this.bufcolumn, this.tokenBegin, newbufcolumn, 0, this.bufsize - this.tokenBegin);
				this.bufcolumn = newbufcolumn;

				this.maxNextCharInd = this.bufpos -= this.tokenBegin;
			}
		} catch (final Throwable t) {
			throw new Error(t.getMessage());
		}

		this.bufsize += 2048;
		this.available = this.bufsize;
		this.tokenBegin = 0;
	}

	protected void FillBuff() throws java.io.IOException {
		if (this.maxNextCharInd == this.available)
			if (this.available == this.bufsize) {
				if (this.tokenBegin > 2048) {
					this.bufpos = this.maxNextCharInd = 0;
					this.available = this.tokenBegin;
				} else if (this.tokenBegin < 0)
					this.bufpos = this.maxNextCharInd = 0;
				else
					this.ExpandBuff(false);
			} else if (this.available > this.tokenBegin)
				this.available = this.bufsize;
			else if (this.tokenBegin - this.available < 2048)
				this.ExpandBuff(true);
			else
				this.available = this.tokenBegin;

		int i;
		try {
			if ((i = this.inputStream.read(this.buffer, this.maxNextCharInd,
					this.available - this.maxNextCharInd)) == -1) {
				this.inputStream.close();
				throw new java.io.IOException();
			} else
				this.maxNextCharInd += i;
			return;
		} catch (final java.io.IOException e) {
			--this.bufpos;
			this.backup(0);
			if (this.tokenBegin == -1)
				this.tokenBegin = this.bufpos;
			throw e;
		}
	}

	/** Get token beginning column number. */
	public int getBeginColumn() {
		return this.bufcolumn[this.tokenBegin];
	}

	/** Get token beginning line number. */
	public int getBeginLine() {
		return this.bufline[this.tokenBegin];
	}

	@Deprecated
	/**
	 * @deprecated
	 * @see #getEndColumn
	 */

	public int getColumn() {
		return this.bufcolumn[this.bufpos];
	}

	/** Get token end column number. */
	public int getEndColumn() {
		return this.bufcolumn[this.bufpos];
	}

	/** Get token end line number. */
	public int getEndLine() {
		return this.bufline[this.bufpos];
	}

	/** Get token literal value. */
	public String GetImage() {
		if (this.bufpos >= this.tokenBegin)
			return new String(this.buffer, this.tokenBegin, this.bufpos - this.tokenBegin + 1);
		else
			return new String(this.buffer, this.tokenBegin, this.bufsize - this.tokenBegin)
					+ new String(this.buffer, 0, this.bufpos + 1);
	}

	@Deprecated
	/**
	 * @deprecated
	 * @see #getEndLine
	 */

	public int getLine() {
		return this.bufline[this.bufpos];
	}

	/** Get the suffix. */
	public char[] GetSuffix(final int len) {
		final char[] ret = new char[len];

		if (this.bufpos + 1 >= len)
			System.arraycopy(this.buffer, this.bufpos - len + 1, ret, 0, len);
		else {
			System.arraycopy(this.buffer, this.bufsize - (len - this.bufpos - 1), ret, 0, len - this.bufpos - 1);
			System.arraycopy(this.buffer, 0, ret, len - this.bufpos - 1, this.bufpos + 1);
		}

		return ret;
	}

	protected int getTabSize(final int i) {
		return this.tabSize;
	}

	/** Read a character. */
	public char readChar() throws java.io.IOException {
		if (this.inBuf > 0) {
			--this.inBuf;

			if (++this.bufpos == this.bufsize)
				this.bufpos = 0;

			return this.buffer[this.bufpos];
		}

		if (++this.bufpos >= this.maxNextCharInd)
			this.FillBuff();

		final char c = this.buffer[this.bufpos];

		this.UpdateLineColumn(c);
		return c;
	}

	/** Reinitialise. */
	public void ReInit(final java.io.InputStream dstream) {
		this.ReInit(dstream, 1, 1, 4096);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.InputStream dstream, final int startline, final int startcolumn) {
		this.ReInit(dstream, startline, startcolumn, 4096);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.InputStream dstream, final int startline, final int startcolumn,
			final int buffersize) {
		this.ReInit(new java.io.InputStreamReader(dstream), startline, startcolumn, buffersize);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.InputStream dstream, final String encoding)
			throws java.io.UnsupportedEncodingException {
		this.ReInit(dstream, encoding, 1, 1, 4096);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.InputStream dstream, final String encoding, final int startline,
			final int startcolumn) throws java.io.UnsupportedEncodingException {
		this.ReInit(dstream, encoding, startline, startcolumn, 4096);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.InputStream dstream, final String encoding, final int startline,
			final int startcolumn, final int buffersize) throws java.io.UnsupportedEncodingException {
		this.ReInit(encoding == null ? new java.io.InputStreamReader(dstream)
				: new java.io.InputStreamReader(dstream, encoding), startline, startcolumn, buffersize);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.Reader dstream) {
		this.ReInit(dstream, 1, 1, 4096);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.Reader dstream, final int startline, final int startcolumn) {
		this.ReInit(dstream, startline, startcolumn, 4096);
	}

	/** Reinitialise. */
	public void ReInit(final java.io.Reader dstream, final int startline, final int startcolumn, final int buffersize) {
		this.inputStream = dstream;
		this.line = startline;
		this.column = startcolumn - 1;

		if (this.buffer == null || buffersize != this.buffer.length) {
			this.available = this.bufsize = buffersize;
			this.buffer = new char[buffersize];
			this.bufline = new int[buffersize];
			this.bufcolumn = new int[buffersize];
		}
		this.prevCharIsLF = this.prevCharIsCR = false;
		this.tokenBegin = this.inBuf = this.maxNextCharInd = 0;
		this.bufpos = -1;
	}

	protected void setTabSize(final int i) {
		this.tabSize = i;
	}

	protected void UpdateLineColumn(final char c) {
		this.column++;

		if (this.prevCharIsLF) {
			this.prevCharIsLF = false;
			this.line += this.column = 1;
		} else if (this.prevCharIsCR) {
			this.prevCharIsCR = false;
			if (c == '\n')
				this.prevCharIsLF = true;
			else
				this.line += this.column = 1;
		}

		switch (c) {
		case '\r':
			this.prevCharIsCR = true;
			break;
		case '\n':
			this.prevCharIsLF = true;
			break;
		case '\t':
			this.column--;
			this.column += this.tabSize - this.column % this.tabSize;
			break;
		default:
			break;
		}

		this.bufline[this.bufpos] = this.line;
		this.bufcolumn[this.bufpos] = this.column;
	}

}
/*
 * JavaCC - OriginalChecksum=c635bae29996595a597407cbe46ea1af (do not edit this
 * line)
 */
