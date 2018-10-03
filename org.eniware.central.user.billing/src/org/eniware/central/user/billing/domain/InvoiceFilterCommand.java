/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.user.billing.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eniware.central.domain.SortDescriptor;
import org.eniware.central.support.FilterSupport;
import org.eniware.central.support.MutableSortDescriptor;

/**
 * Filter support for invoice actions.
 * 
 * @version 1.0
 */
public class InvoiceFilterCommand extends FilterSupport implements InvoiceFilter {

	private static final long serialVersionUID = -6127206654609364990L;

	private List<MutableSortDescriptor> sorts;
	private Integer offset = 0;
	private Integer max;
	private Boolean unpaid;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((max == null) ? 0 : max.hashCode());
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
		result = prime * result + ((unpaid == null) ? 0 : unpaid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof InvoiceFilterCommand) ) {
			return false;
		}
		InvoiceFilterCommand other = (InvoiceFilterCommand) obj;
		if ( max == null ) {
			if ( other.max != null ) {
				return false;
			}
		} else if ( !max.equals(other.max) ) {
			return false;
		}
		if ( offset == null ) {
			if ( other.offset != null ) {
				return false;
			}
		} else if ( !offset.equals(other.offset) ) {
			return false;
		}
		if ( sorts == null ) {
			if ( other.sorts != null ) {
				return false;
			}
		} else if ( !sorts.equals(other.sorts) ) {
			return false;
		}
		if ( unpaid == null ) {
			if ( other.unpaid != null ) {
				return false;
			}
		} else if ( !unpaid.equals(other.unpaid) ) {
			return false;
		}
		return true;
	}

	/**
	 * Get the mutable sort descriptors.
	 * 
	 * @return the sort descriptors, or {@literal null}
	 */
	public List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	/**
	 * Set the mutable sort descriptors.
	 * 
	 * @param sorts
	 *        the sort descriptors to se
	 */
	public void setSorts(List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	/**
	 * Get the sort descriptors.
	 * 
	 * <p>
	 * This returns a copy of the {@code sorts} list or an empty list if that is
	 * {@literal null}.
	 * </p>
	 * 
	 * @return the sort descriptors, never {@code null}
	 */
	public List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return Collections.emptyList();
		}
		return new ArrayList<SortDescriptor>(sorts);
	}

	/**
	 * Get the result starting offset.
	 * 
	 * @return the starting offset, or {@literal null} for no offset
	 */
	public Integer getOffset() {
		return offset;
	}

	/**
	 * Set the result starting offset.
	 * 
	 * @param offset
	 *        the offset to set
	 */
	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	/**
	 * Get the maximum desired results.
	 * 
	 * @return the maximum desired result count
	 */
	public Integer getMax() {
		return max;
	}

	/**
	 * Set the maximum desired results.
	 * 
	 * @param max
	 *        the maximum desired result count, or {@literal null} for no limit
	 */
	public void setMax(Integer max) {
		this.max = max;
	}

	@Override
	public Boolean getUnpaid() {
		return unpaid;
	}

	/**
	 * Set the unpaid criteria.
	 * 
	 * @param unpaid
	 *        the unpaid value to limit the results to
	 */
	public void setUnpaid(Boolean unpaid) {
		this.unpaid = unpaid;
	}

}
