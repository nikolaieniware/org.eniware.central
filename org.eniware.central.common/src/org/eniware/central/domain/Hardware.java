/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.domain;


/**
 * A hardware element on a Edge.
 * 
 * <p>This entity is designed to hold information about hardware so that hardware
 * can be compared against other hardware.</p>
 * @version $Revision$
 */
public class Hardware extends BaseEntity implements EntityMatch {

	private static final long serialVersionUID = 7522632952888186387L;

	private String manufacturer;
	private String model;
	private Integer revision;
	
	public String getManufacturer() {
		return manufacturer;
	}
	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}
	public String getModel() {
		return model;
	}
	public void setModel(String model) {
		this.model = model;
	}
	public Integer getRevision() {
		return revision;
	}
	public void setRevision(Integer revision) {
		this.revision = revision;
	}

}
