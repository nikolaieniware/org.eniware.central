/* ==================================================================
 * SimpleInstructionFilter.java - Sep 30, 2011 9:31:17 AM
 * 
 * Copyright 2007-2011 EniwareNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package org.eniware.central.instructor.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eniware.central.instructor.domain.InstructionFilter;
import org.eniware.central.instructor.domain.InstructionState;
import org.eniware.util.SerializeIgnore;

/**
 * Simple implementation of {@link InstructionFilter}.
 * 
 * @version 1.1
 */
public class SimpleInstructionFilter implements InstructionFilter {

	private Long[] EdgeIds;
	private Long[] instructionIds;
	private List<InstructionState> states;

	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> f = new LinkedHashMap<String, Object>(2);
		if ( EdgeIds != null && EdgeIds.length > 0 ) {
			f.put("EdgeId", EdgeIds[0]); // backwards compatibility
			f.put("EdgeIds", EdgeIds);
		}
		if ( instructionIds != null && instructionIds.length > 0 ) {
			f.put("instructionIds", instructionIds);
		}
		if ( states != null && states.isEmpty() == false ) {
			f.put("state", states.iterator().next().toString());
			if ( states.size() > 1 ) {
				f.put("states", states.toArray(new InstructionState[states.size()]));
			}
		}
		return f;
	}

	/**
	 * Set a single Edge ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single Edge ID at a
	 * time. The Edge ID is still stored on the {@code EdgeIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code EdgeIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param EdgeId
	 *        the ID of the Edge
	 */
	public void setEdgeId(Long EdgeId) {
		this.EdgeIds = new Long[] { EdgeId };
	}

	/**
	 * Get the first Edge ID.
	 * 
	 * <p>
	 * This returns the first available Edge ID from the {@code EdgeIds} array,
	 * or <em>null</em> if not available.
	 * </p>
	 * 
	 * @return the first Edge ID
	 */
	@Override
	public Long getEdgeId() {
		return this.EdgeIds == null || this.EdgeIds.length < 1 ? null : this.EdgeIds[0];
	}

	@Override
	public Long[] getEdgeIds() {
		return EdgeIds;
	}

	public void setEdgeIds(Long[] EdgeIds) {
		this.EdgeIds = EdgeIds;
	}

	@Override
	public InstructionState getState() {
		return (states != null && states.isEmpty() == false ? states.iterator().next() : null);
	}

	public void setState(InstructionState state) {
		if ( state == null ) {
			states = null;
		} else {
			states = Arrays.asList(state);
		}
	}

	@Override
	public List<InstructionState> getStates() {
		return states;
	}

	/**
	 * Set the {@code states} property via a Set. This is useful when using an
	 * {@link EnumSet}.
	 * 
	 * @param stateSet
	 *        the Set to convert to a List of {@link InstructionState} values
	 *        for the {@code states} property
	 */
	public void setStateSet(Set<InstructionState> stateSet) {
		if ( stateSet == null ) {
			this.states = null;
		} else {
			this.states = new ArrayList<InstructionState>(stateSet);
		}
	}

	public void setStates(List<InstructionState> states) {
		if ( states == null ) {
			this.states = null;
		} else {
			// filter out duplicates
			Set<InstructionState> set = EnumSet.copyOf(states);
			this.states = new ArrayList<InstructionState>(set);
		}
	}

	/**
	 * Get a set of instruction IDs.
	 * 
	 * @since 1.1
	 */
	@Override
	public Long[] getInstructionIds() {
		return instructionIds;
	}

	/**
	 * Set an instruction IDs list.
	 * 
	 * @param instructionIds
	 *        the IDs to set
	 * @since 1.1
	 */
	public void setInstructionIds(Long[] instructionIds) {
		this.instructionIds = instructionIds;
	}

}
