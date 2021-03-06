/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.domain;

/**
 * Filter API for Hardware.
 * @version $Revision$
 */
public interface HardwareFilter extends Filter {

	/**
	 * Get a specific hardware ID to filter on.
	 * @return
	 */
	Long getHardwareId();
	
	/**
	 * Get a general name filter (manufacturer, model, etc).
	 * 
	 * @return the name
	 */
	String getName();

}
