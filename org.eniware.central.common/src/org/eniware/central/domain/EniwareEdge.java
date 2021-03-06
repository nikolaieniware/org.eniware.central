/* ===================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ===================================================================
 */

package org.eniware.central.domain;

import java.io.Serializable;
import java.util.TimeZone;

import org.eniware.util.SerializeIgnore;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Domain object for Edge related info.
 * @version 1.1
 */
public class EniwareEdge extends BaseEntity implements Cloneable, Serializable, EdgeIdentity {

	private static final long serialVersionUID = -1478837853706836739L;

	private String name = null;
	private Long locationId = null; // the location ID
	private Long weatherLocationId = null; // the weather location ID

	@SerializeIgnore
	@JsonIgnore
	private EniwareLocation location;

	/**
	 * Default constructor.
	 */
	public EniwareEdge() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param id
	 *        the ID
	 * @param locationId
	 *        the location ID
	 * @param timeZoneId
	 *        the time zone ID
	 */
	public EniwareEdge(Long id, Long locationId) {
		super();
		setId(id);
		setCreated(new DateTime());
		setLocationId(locationId);
	}

	@Override
	public String toString() {
		return "EniwareEdge{id=" + getId() + ",locationId=" + this.locationId + '}';
	}

	/**
	 * Get a {@link TimeZone} instance for this Edge's
	 * {@link EniwareLocation#getTimeZoneId()}.
	 * 
	 * @return the TimeZone
	 */
	public TimeZone getTimeZone() {
		return (this.location != null && this.location.getTimeZoneId() != null ? TimeZone
				.getTimeZone(this.location.getTimeZoneId()) : null);
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public Long getWeatherLocationId() {
		return weatherLocationId;
	}

	public void setWeatherLocationId(Long weatherLocationId) {
		this.weatherLocationId = weatherLocationId;
	}

	@JsonIgnore
	public EniwareLocation getLocation() {
		return location;
	}

	public void setLocation(EniwareLocation location) {
		this.location = location;
		if ( location != null && location.getId() != null ) {
			this.locationId = location.getId();
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
