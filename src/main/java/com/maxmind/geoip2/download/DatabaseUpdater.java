package com.maxmind.geoip2.download;

import java.io.IOException;

/**
 * Responsible for downloading updated versions of the database when they are
 * created.
 */
public interface DatabaseUpdater {

	/**
	 * Implementation of {@link DatabaseUpdater} that always returns <tt>false</tt>
	 * to refresh requests.
	 */
	class NoopDatabaseUpdater implements DatabaseUpdater {

		@Override
		public boolean refresh() {
			return false;
		}
	}

	/**
	 * Verifies the current available version and if a newer one exists, downloads
	 * it replacing the current version.
	 *
	 * @return
	 * @throws IOException
	 */
	boolean refresh() throws IOException;
}