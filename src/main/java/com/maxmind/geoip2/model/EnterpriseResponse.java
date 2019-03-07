package com.maxmind.geoip2.model;

import java.util.ArrayList;

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
 * This class provides a model for the data returned by the GeoIP2 Enterprise
 * database
 * </p>
 */
public final class EnterpriseResponse extends AbstractCityResponse {

	public EnterpriseResponse(@JsonProperty("city") City city, @JsonProperty("continent") Continent continent,
			@JsonProperty("country") Country country, @JsonProperty("location") Location location,
			@JsonProperty("maxmind") MaxMind maxmind, @JsonProperty("postal") Postal postal,
			@JsonProperty("registered_country") Country registeredCountry,
			@JsonProperty("represented_country") RepresentedCountry representedCountry,
			@JsonProperty("subdivisions") ArrayList<Subdivision> subdivisions,
			@JacksonInject("traits") @JsonProperty("traits") Traits traits) {
		super(city, continent, country, location, maxmind, postal, registeredCountry, representedCountry, subdivisions,
				traits);
	}
}
