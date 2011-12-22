package org.sqlite.android;

/**
 * Class for SQLite related exceptions.
 */

public class SQLiteException extends java.lang.Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2697488517148897221L;

	/**
	 * Constructs a new SQLite exception.
	 * 
	 * @param string
	 *            error message
	 */

	public SQLiteException(String string) {
		super(string);
	}
}
