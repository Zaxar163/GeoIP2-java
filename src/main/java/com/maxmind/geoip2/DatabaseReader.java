package com.maxmind.geoip2;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maxmind.db.InvalidDatabaseException;
import com.maxmind.db.Metadata;
import com.maxmind.db.NoCache;
import com.maxmind.db.NodeCache;
import com.maxmind.db.Reader;
import com.maxmind.db.Reader.FileMode;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AnonymousIpResponse;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.ConnectionTypeResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.model.DomainResponse;
import com.maxmind.geoip2.model.EnterpriseResponse;
import com.maxmind.geoip2.model.IspResponse;

/**
 * <p>
 * The class {@code DatabaseReader} provides a reader for the GeoIP2 database
 * format.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * To use the database API, you must create a new {@code DatabaseReader} using
 * the {@code DatabaseReader.Builder}. You must provide the {@code Builder}
 * constructor either an {@code InputStream} or {@code File} for your GeoIP2
 * database. You may also specify the {@code fileMode} and the {@code locales}
 * fallback order using the methods on the {@code Builder} object. After you
 * have created the {@code DatabaseReader}, you may then call the appropriate
 * method (e.g., {@code city}) for your database, passing it the IP address you
 * want to look up.
 * </p>
 * <p>
 * If the lookup succeeds, the method call will return a response class for the
 * GeoIP2 lookup. The class in turn contains multiple record classes, each of
 * which represents part of the data returned by the database.
 * </p>
 * <p>
 * We recommend reusing the {@code DatabaseReader} object rather than creating a
 * new one for each lookup. The creation of this object is relatively expensive
 * as it must read in metadata for the file. It is safe to share the object
 * across threads.
 * </p>
 * <h4>Caching</h4>
 * <p>
 * The database API supports pluggable caching (by default, no caching is
 * performed). A simple implementation is provided by
 * {@code com.maxmind.db.CHMCache}. Using this cache, lookup performance is
 * significantly improved at the cost of a small (~2MB) memory overhead.
 * </p>
 */
public class DatabaseReader implements DatabaseProvider, Closeable {

	/**
	 * <p>
	 * Constructs a Builder for the {@code DatabaseReader}. The file passed to it
	 * must be a valid GeoIP2 database file.
	 * </p>
	 * <p>
	 * {@code Builder} creates instances of {@code DatabaseReader} from values set
	 * by the methods.
	 * </p>
	 * <p>
	 * Only the values set in the {@code Builder} constructor are required.
	 * </p>
	 */
	public static final class Builder {
		final File database;
		final InputStream stream;

		List<String> locales = Collections.singletonList("en");
		FileMode mode = FileMode.MEMORY_MAPPED;
		NodeCache cache = NoCache.getInstance();
		boolean lazyInitialise;
		boolean reload;

		/**
		 * @param database the GeoIP2 database file to use.
		 */
		public Builder(File database) {
			this.database = database;
			stream = null;
		}

		/**
		 * @param stream the stream containing the GeoIP2 database to use.
		 */
		public Builder(InputStream stream) {
			this.stream = stream;
			database = null;
		}

		/**
		 * @return an instance of {@code DatabaseProvider} created from the fields set
		 *         on this builder.
		 * @throws IOException if there is an error reading the database
		 */
		public DatabaseProvider build() throws IOException {
			if (reload) {
				return new ReloadableDatabaseProvider(this, database, lazyInitialise);
			}
			return new DatabaseReader(this);
		}

		/**
		 * @param val The file mode used to open the GeoIP2 database
		 * @return Builder object
		 * @throws java.lang.IllegalArgumentException if you initialized the Builder
		 *         with a URL, which uses {@link FileMode#MEMORY}, but you provided a
		 *         different FileMode to this method.
		 */
		public Builder fileMode(FileMode val) {
			if (stream != null && FileMode.MEMORY != val) {
				throw new IllegalArgumentException("Only FileMode.MEMORY is supported when using an InputStream.");
			}
			mode = val;
			return this;
		}

		/**
		 * @param val List of locale codes to use in name property from most preferred
		 *            to least preferred.
		 * @return Builder object
		 */
		public Builder locales(List<String> val) {
			locales = val;
			return this;
		}

		/**
		 * @param cache backing cache instance
		 * @return Builder object
		 */
		public Builder withCache(NodeCache cache) {
			this.cache = cache;
			return this;
		}

		/**
		 * Enables the ability for this reader to be reloaded when updates are
		 * available.
		 *
		 * @return Builder object
		 */
		public Builder withReload() {
			return withReload(false);
		}

		/**
		 * Enables the ability for this reader to be reloaded when updates are
		 * available. Lazy initialisation can also be enabled, allowing a client to
		 * start an application with no database file whilst this can be downloading in
		 * another thread.
		 *
		 * @param lazyInitialise
		 * @return Builder object
		 */
		public Builder withReload(boolean lazyInitialise) {
			if (database == null) {
				throw new IllegalArgumentException("Only File based mode is supported for reloading.");
			}
			reload = true;
			this.lazyInitialise = lazyInitialise;
			return this;
		}
	}

	private final Reader reader;

	private final ObjectMapper om;

	private final List<String> locales;

	DatabaseReader(Builder builder) throws IOException {
		if (builder.stream != null) {
			this.reader = new Reader(builder.stream, builder.cache);
		} else if (builder.database != null) {
			this.reader = new Reader(builder.database, builder.mode, builder.cache);
		} else {
			// This should never happen. If it does, review the Builder class
			// constructors for errors.
			throw new IllegalArgumentException("Unsupported Builder configuration: expected either File or URL");
		}
		this.om = new ObjectMapper();
		this.om.configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, false);
		this.om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		this.om.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
		this.locales = builder.locales;
	}

	/**
	 * Look up an IP address in a GeoIP2 Anonymous IP.
	 *
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return a AnonymousIpResponse for the requested IP address.
	 * @throws GeoIp2Exception if there is an error looking up the IP
	 * @throws IOException     if there is an IO error
	 */
	@Override
	public AnonymousIpResponse anonymousIp(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, AnonymousIpResponse.class, "GeoIP2-Anonymous-IP");
	}

	/**
	 * Look up an IP address in a GeoLite2 ASN database.
	 *
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return an AsnResponse for the requested IP address.
	 * @throws GeoIp2Exception if there is an error looking up the IP
	 * @throws IOException     if there is an IO error
	 */
	@Override
	public AsnResponse asn(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, AsnResponse.class, "GeoLite2-ASN");
	}

	@Override
	public CityResponse city(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, CityResponse.class, "City");
	}

	/**
	 * <p>
	 * Closes the database.
	 * </p>
	 * <p>
	 * If you are using {@code FileMode.MEMORY_MAPPED}, this will <em>not</em> unmap
	 * the underlying file due to a limitation in Java's {@code MappedByteBuffer}.
	 * It will however set the reference to the buffer to {@code null}, allowing the
	 * garbage collector to collect it.
	 * </p>
	 *
	 * @throws IOException if an I/O error occurs.
	 */
	@Override
	public void close() throws IOException {
		reader.close();
	}

	/**
	 * Look up an IP address in a GeoIP2 Connection Type database.
	 *
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return a ConnectTypeResponse for the requested IP address.
	 * @throws GeoIp2Exception if there is an error looking up the IP
	 * @throws IOException     if there is an IO error
	 */
	@Override
	public ConnectionTypeResponse connectionType(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, ConnectionTypeResponse.class, "GeoIP2-Connection-Type");
	}

	@Override
	public CountryResponse country(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, CountryResponse.class, "Country");
	}

	/**
	 * Look up an IP address in a GeoIP2 Domain database.
	 *
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return a DomainResponse for the requested IP address.
	 * @throws GeoIp2Exception if there is an error looking up the IP
	 * @throws IOException     if there is an IO error
	 */
	@Override
	public DomainResponse domain(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, DomainResponse.class, "GeoIP2-Domain");
	}

	/**
	 * Look up an IP address in a GeoIP2 Enterprise database.
	 *
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return an EnterpriseResponse for the requested IP address.
	 * @throws GeoIp2Exception if there is an error looking up the IP
	 * @throws IOException     if there is an IO error
	 */
	@Override
	public EnterpriseResponse enterprise(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, EnterpriseResponse.class, "Enterprise");
	}

	/**
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return A <T> object with the data for the IP address
	 * @throws IOException              if there is an error opening or reading from
	 *                                  the file.
	 * @throws AddressNotFoundException if the IP address is not in our database
	 */
	private <T> T get(InetAddress ipAddress, Class<T> cls, String type) throws IOException, AddressNotFoundException {

		String databaseType = getMetadata().getDatabaseType();
		if (!databaseType.contains(type)) {
			String caller = Thread.currentThread().getStackTrace()[2].getMethodName();
			throw new UnsupportedOperationException(
					"Invalid attempt to open a " + databaseType + " database using the " + caller + " method");
		}

		ObjectNode node = jsonNodeToObjectNode(reader.get(ipAddress));

		// We throw the same exception as the web service when an IP is not in
		// the database
		if (node == null) {
			throw new AddressNotFoundException(
					"The address " + ipAddress.getHostAddress() + " is not in the database.");
		}

		InjectableValues inject = new JsonInjector(locales, ipAddress.getHostAddress());

		return om.reader(inject).treeToValue(node, cls);
	}

	/**
	 * @return the metadata for the open MaxMind DB file.
	 */
	@Override
	public Metadata getMetadata() {
		return reader.getMetadata();
	}

	/**
	 * Look up an IP address in a GeoIP2 ISP database.
	 *
	 * @param ipAddress IPv4 or IPv6 address to lookup.
	 * @return an IspResponse for the requested IP address.
	 * @throws GeoIp2Exception if there is an error looking up the IP
	 * @throws IOException     if there is an IO error
	 */
	@Override
	public IspResponse isp(InetAddress ipAddress) throws IOException, GeoIp2Exception {
		return this.get(ipAddress, IspResponse.class, "GeoIP2-ISP");
	}

	private ObjectNode jsonNodeToObjectNode(JsonNode node) throws InvalidDatabaseException {
		if (node == null || node instanceof ObjectNode) {
			return (ObjectNode) node;
		}
		throw new InvalidDatabaseException("Unexpected data type returned. The GeoIP2 database may be corrupt.");
	}
}