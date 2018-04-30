/* ==================================================================
 *  Eniware Open sorce:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.eniware.central.biz.EniwareEdgeMetadataBiz;
import org.eniware.central.domain.EniwareEdgeMetadataFilter;
import org.eniware.central.security.AuthorizationException;
import org.eniware.central.security.SecurityPolicy;
import org.eniware.central.security.SecurityPolicyMetadataType;
import org.eniware.central.user.dao.UserNodeDao;
import org.eniware.central.user.support.AuthorizationSupport;
import org.springframework.util.AntPathMatcher;

/**
 * Security AOP support for {@link EniwareEdgeMetadataBiz}.
 * @version 1.0
 */
@Aspect
public class EniwareEdgeMetadataSecurityAspect extends AuthorizationSupport {

	/**
	 * Constructor.
	 * 
	 * @param userNodeDao
	 *        the UserNodeDao to use
	 */
	public EniwareEdgeMetadataSecurityAspect(UserNodeDao userNodeDao) {
		super(userNodeDao);
		AntPathMatcher antMatch = new AntPathMatcher();
		antMatch.setCachePatterns(false);
		antMatch.setCaseSensitive(true);
		setPathMatcher(antMatch);
	}

	@Pointcut("bean(aop*) && execution(* org.eniware.central.biz.EniwareEdgeMetadata*.addEniwareEdge*(..)) && args(nodeId,..)")
	public void addMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* org.eniware.central.biz.EniwareEdgeMetadata*.storeEniwareEdge*(..)) && args(nodeId,..)")
	public void storeMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* org.eniware.central.biz.EniwareEdgeMetadata*.removeEniwareEdge*(..)) && args(nodeId)")
	public void removeMetadata(Long nodeId) {
	}

	@Pointcut("bean(aop*) && execution(* org.eniware.central.biz.EniwareEdgeMetadata*.findEniwareEdge*(..)) && args(filter,..)")
	public void findMetadata(EniwareEdgeMetadataFilter filter) {
	}

	/**
	 * Check access to modifying node metadata.
	 * 
	 * @param nodeId
	 *        the ID of the node to verify
	 */
	@Before("addMetadata(nodeId) || storeMetadata(nodeId) || removeMetadata(nodeId)")
	public void updateMetadataCheck(Long nodeId) {
		requireNodeWriteAccess(nodeId);
	}

	/**
	 * Check access to reading node metadata.
	 * 
	 * @param pjp
	 *        the join point
	 * @param filter
	 *        the filter to verify
	 */
	@Around("findMetadata(filter)")
	public Object readMetadataCheck(ProceedingJoinPoint pjp, EniwareEdgeMetadataFilter filter)
			throws Throwable {
		Long[] nodeIds = (filter == null ? null : filter.getNodeIds());
		if ( nodeIds == null ) {
			log.warn("Access DENIED to node metadata without node ID filter");
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
		for ( Long nodeId : nodeIds ) {
			requireNodeReadAccess(nodeId);
		}

		// node ID passes, execute query and then filter based on security policy if necessary
		Object result = pjp.proceed();

		SecurityPolicy policy = getActiveSecurityPolicy();
		if ( policy == null || policy.getNodeMetadataPaths() == null
				|| policy.getNodeMetadataPaths().isEmpty() ) {
			return result;
		}

		return policyEnforcerCheck(result, SecurityPolicyMetadataType.Node);
	}

}
