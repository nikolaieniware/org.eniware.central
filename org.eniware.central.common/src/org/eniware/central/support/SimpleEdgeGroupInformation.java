/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.support;

import org.eniware.central.domain.BaseIdentity;
import org.eniware.central.domain.Location;
import org.eniware.central.domain.EdgeGroupInformation;
import org.eniware.central.domain.EniwareCapability;
import org.eniware.central.domain.EniwareEdgeGroupCapability;

/**
 * Simple implementation of {@link EdgeGroupInformation}.
 * @version $Revision$
 */
public class SimpleEdgeGroupInformation extends BaseIdentity implements EdgeGroupInformation {

	private static final long serialVersionUID = -1983417976743765775L;

	private String name;
	private Location location;
	private EniwareCapability capability;

	/**
	 * Default constructor.
	 */
	public SimpleEdgeGroupInformation() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param group the group to copy values from.
	 * @param location the location
	 */
	public SimpleEdgeGroupInformation(String name, EniwareEdgeGroupCapability capability, 
			Location location) {
		setId(capability.getGroupId());
		this.name = name;
		this.capability = capability;
		this.location = location;
	}
	
	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Increment the generation capacity.
	 * @param amount the amount to add
	 */
	public void addGenerationCapacityWatts(Long amount) {
		capability.setGenerationCapacityWatts(
				capability.getGenerationCapacityWatts() + amount);
	}
	
	/**
	 * Increment the storage capacity.
	 * @param amount the amount to add
	 */
	public void addStorageCapacityWattHours(Long amount) {
		capability.setStorageCapacityWattHours(
				capability.getStorageCapacityWattHours() + amount);
	}
	
	/**
	 * @return the generationCapacityWatts
	 */
	public Long getGenerationCapacityWatts() {
		return capability.getGenerationCapacityWatts();
	}

	/**
	 * @return the storageCapacityWattHours
	 */
	public Long getStorageCapacityWattHours() {
		return capability.getStorageCapacityWattHours();
	}

}
