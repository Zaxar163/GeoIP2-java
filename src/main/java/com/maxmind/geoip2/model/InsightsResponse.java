package com.maxmind.geoip2.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.MaxMind;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.RepresentedCountry;
import com.maxmind.geoip2.record.Subdivision;
import com.maxmind.geoip2.record.Traits;

/**
 * <p>
 * This class provides a model for the data returned by the GeoIP2 Precision:
 * Insights end point.
 * </p>
 * <p>
 * The only difference between the City and Insights model classes is which
 * fields in each record may be populated.
 * </p>
 * <p>
 * 
 * @see <a href="http://dev.maxmind.com/geoip/geoip2/web-services">GeoIP2 Web
 *      Services</a>
 *      </p>
 */
public class InsightsResponse extends AbstractCityResponse {

	InsightsResponse() {
		this(null, null, null, null, null, null, null, null, null, null);
	}

	public InsightsResponse(@JsonProperty("city") City city, @JsonProperty("continent") Continent continent,
			@JsonProperty("country") Country country, @JsonProperty("location") Location location,
			@JsonProperty("maxmind") MaxMind maxmind, @JsonProperty("postal") Postal postal,
			@JsonProperty("registered_country") Country registeredCountry,
			@JsonProperty("represented_country") RepresentedCountry representedCountry,
			@JsonProperty("subdivisions") List<Subdivision> subdivisions,
			@JacksonInject("traits") @JsonProperty("traits") Traits traits) {
		super(city, continent, country, location, maxmind, postal, registeredCountry, representedCountry, subdivisions,
				traits);
	}
}
