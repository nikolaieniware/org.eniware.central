/* ===================================================================
 *  Eniware Open sorce:Nikolai Manchev
 *  Apache License 2.0
 * ===================================================================
 */

package org.eniware.central.dao;

import org.eniware.central.domain.EniwareEdge;

/**
 * DAO API for EniwareEdge data.
 * @version $Revision$ $Date$
 */
public interface EniwareEdgeDao extends GenericDao<EniwareEdge, Long> {

	/**
	 * Get an unused node ID value.
	 * 
	 * <p>Once an ID has been returned by this method, that ID will
	 * never be returned again.</p>
	 * 
	 * @return an unused node ID
	 */
	Long getUnusedNodeId();
	
}
