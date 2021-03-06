/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.security;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eniware.central.domain.Aggregation;
import org.eniware.domain.GeneralDatumMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.PathMatcher;

/**
 * Support for enforcing a {@link SecurityPolicy} on domain objects.
 
 * @version 1.2
 * @since 1.12
 */
public class SecurityPolicyEnforcer implements InvocationHandler {

	private final Object delegate;
	private final SecurityPolicy policy;
	private final Object principal;
	private final PathMatcher pathMatcher;
	private final SecurityPolicyMetadataType metadataType;

	private Long[] cachedEdgeIds;
	private String[] cachedSourceIds;
	private GeneralDatumMetadata cachedMetadata;

	private static final Logger LOG = LoggerFactory.getLogger(SecurityPolicyEnforcer.class);

	/**
	 * Construct a new enforcer.
	 * 
	 * @param policy
	 *        The policy to enforce.
	 * @param principal
	 *        The active principal.
	 * @param delegate
	 *        The domain object to enforce the policy on.
	 */
	public SecurityPolicyEnforcer(SecurityPolicy policy, Object principal, Object delegate) {
		this(policy, principal, delegate, (PathMatcher) null);
	}

	/**
	 * Construct a new enforcer with patch matching support.
	 * 
	 * @param policy
	 *        The policy to enforce.
	 * @param principal
	 *        The active principal.
	 * @param delegate
	 *        The domain object to enforce the policy on.
	 * @param pathMatcher
	 *        The path matcher to use.
	 * @since 1.1
	 */
	public SecurityPolicyEnforcer(SecurityPolicy policy, Object principal, Object delegate,
			PathMatcher pathMatcher) {
		this(policy, principal, delegate, pathMatcher, null);
	}

	/**
	 * Construct a new enforcer with patch matching support.
	 * 
	 * @param policy
	 *        The policy to enforce.
	 * @param principal
	 *        The active principal.
	 * @param delegate
	 *        The domain object to enforce the policy on.
	 * @param pathMatcher
	 *        The path matcher to use.
	 * @param metadataType
	 *        The type of metadata associated with {@code delegate}, or
	 *        {@code null}.
	 * @since 1.2
	 */
	public SecurityPolicyEnforcer(SecurityPolicy policy, Object principal, Object delegate,
			PathMatcher pathMatcher, SecurityPolicyMetadataType metadataType) {
		super();
		this.delegate = delegate;
		this.policy = policy;
		this.principal = principal;
		this.pathMatcher = pathMatcher;
		this.metadataType = metadataType;
	}

	/**
	 * Wrap an object with a {@link SecurityPolicyEnforcer}, enforcing policy
	 * properties.
	 * 
	 * This will return a proxy object that implements all interfaces on the
	 * provided enforder's {@code delegate} property.
	 * 
	 * @param enforcer
	 *        The policy enforcer.
	 * @return A new wrapped object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T createSecurityPolicyProxy(SecurityPolicyEnforcer enforcer) {
		Class<?>[] interfaces = ClassUtils.getAllInterfaces(enforcer.getDelgate());
		return (T) Proxy.newProxyInstance(enforcer.getDelgate().getClass().getClassLoader(), interfaces,
				enforcer);
	}

	/**
	 * Verify the security policy on all supported properties immediately.
	 * 
	 * @throws AuthorizationException
	 *         if any policy fails
	 */
	public void verify() {
		String[] getters = new String[] { "getEdgeIds", "getSourceIds", "getAggregation",
				"getMetadata" };
		for ( String methodName : getters ) {
			try {
				Method m = delegate.getClass().getMethod(methodName);
				invoke(null, m, null);
			} catch ( AuthorizationException e ) {
				throw e;
			} catch ( Throwable e ) {
				// ignore this
			}
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		final Object delegateResult = method.invoke(delegate, args);
		if ( "getEdgeIds".equals(methodName) || "getEdgeId".equals(methodName) ) {
			Long[] EdgeIds;
			if ( methodName.endsWith("s") ) {
				EdgeIds = (Long[]) delegateResult;
			} else {
				EdgeIds = (delegateResult != null ? new Long[] { (Long) delegateResult } : null);
			}
			Long[] result = verifyEdgeIds(EdgeIds);
			if ( result == null || result.length < 1 || methodName.endsWith("s") ) {
				return result;
			}
			return result[0];
		} else if ( "getSourceIds".equals(methodName) || "getSourceId".equals(methodName) ) {
			String[] sourceIds;
			if ( methodName.endsWith("s") ) {
				sourceIds = (String[]) delegateResult;
			} else {
				sourceIds = (delegateResult != null ? new String[] { (String) delegateResult } : null);
			}
			String[] result = verifySourceIds(sourceIds, true);
			if ( result == null || result.length < 1 || methodName.endsWith("s") ) {
				return result;
			}
			return result[0];
		} else if ( "getAggregation".equals(methodName) ) {
			Aggregation agg = (Aggregation) delegateResult;
			return verifyAggregation(agg);
		} else if ( "getMetadata".equals(methodName) ) {
			GeneralDatumMetadata meta = (GeneralDatumMetadata) delegateResult;
			return verifyMetadata(meta, true);
		}
		return delegateResult;
	}

	/**
	 * Verify an arbitrary list of Edge IDs against the configured policy.
	 * 
	 * @param EdgeIds
	 *        The Edge IDs to verify.
	 * @return The allowed Edge IDs.
	 * @throws AuthorizationException
	 *         if no Edge IDs are allowed
	 */
	public Long[] verifyEdgeIds(Long[] EdgeIds) {
		Set<Long> policyEdgeIds = policy.getEdgeIds();
		// verify source IDs
		if ( policyEdgeIds == null || policyEdgeIds.isEmpty() ) {
			return EdgeIds;
		}
		if ( cachedEdgeIds != null ) {
			return (cachedEdgeIds.length == 0 ? null : cachedEdgeIds);
		}
		if ( EdgeIds != null && EdgeIds.length > 0 ) {
			// remove any source IDs not in the policy
			Set<Long> EdgeIdsSet = new LinkedHashSet<Long>(Arrays.asList(EdgeIds));
			for ( Iterator<Long> itr = EdgeIdsSet.iterator(); itr.hasNext(); ) {
				Long EdgeId = itr.next();
				if ( !policyEdgeIds.contains(EdgeId) ) {
					LOG.warn("Access DENIED to Edge {} for {}: policy restriction", EdgeId, principal);
					itr.remove();
				}
			}
			if ( EdgeIdsSet.size() < 1 ) {
				LOG.warn("Access DENIED to Edges {} for {}", EdgeIds, principal);
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, EdgeIds);
			} else if ( EdgeIdsSet.size() < EdgeIds.length ) {
				EdgeIds = EdgeIdsSet.toArray(new Long[EdgeIdsSet.size()]);
			}
		} else if ( EdgeIds == null || EdgeIds.length < 1 ) {
			// no source IDs provided, set to policy source IDs
			LOG.info("Access RESTRICTED to Edges {} for {}", policyEdgeIds, principal);
			EdgeIds = policyEdgeIds.toArray(new Long[policyEdgeIds.size()]);
		}
		cachedEdgeIds = (EdgeIds == null ? new Long[0] : EdgeIds);
		return EdgeIds;
	}

	private boolean matchesPattern(Set<String> patterns, String value) {
		for ( String pattern : patterns ) {
			if ( pathMatcher.match(pattern, value) ) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesPatternStart(Set<String> patterns, String value) {
		for ( String pattern : patterns ) {
			if ( pathMatcher.matchStart(pattern, value) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verify an arbitrary list of source IDs against the configured policy.
	 * 
	 * @param sourceIds
	 *        The source IDs to verify.
	 * @return The allowed source IDs.
	 * @throws AuthorizationException
	 *         if no source IDs are allowed
	 */
	public String[] verifySourceIds(String[] sourceIds) {
		return verifySourceIds(sourceIds, false);
	}

	private String[] verifySourceIds(String[] sourceIds, final boolean cacheResults) {
		Set<String> policySourceIds = policy.getSourceIds();

		// verify source IDs
		if ( policySourceIds == null || policySourceIds.isEmpty() ) {
			return sourceIds;
		}
		if ( cacheResults && cachedSourceIds != null ) {
			return (cachedSourceIds.length == 0 ? null : cachedSourceIds);
		}

		if ( sourceIds != null && sourceIds.length > 0 ) {
			// extract policy source ID patterns
			Set<String> policySourceIdPatterns = null;
			if ( pathMatcher != null ) {
				for ( String policySourceId : policySourceIds ) {
					if ( pathMatcher.isPattern(policySourceId) ) {
						if ( policySourceIdPatterns == null ) {
							policySourceIdPatterns = new LinkedHashSet<String>(policySourceIds.size());
						}
						policySourceIdPatterns.add(policySourceId);
					}
				}
				if ( policySourceIdPatterns != null ) {
					Set<String> mutablePolicySourceIds = new LinkedHashSet<String>(policySourceIds);
					mutablePolicySourceIds.removeAll(policySourceIdPatterns);
					policySourceIds = mutablePolicySourceIds;
				}
			}

			// remove any source IDs not in the policy (or not matching a pattern)
			Set<String> sourceIdsSet = new LinkedHashSet<String>(Arrays.asList(sourceIds));
			for ( Iterator<String> itr = sourceIdsSet.iterator(); itr.hasNext(); ) {
				String sourceId = itr.next();
				if ( policySourceIds.contains(sourceId) ) {
					continue;
				}
				if ( policySourceIdPatterns != null
						&& matchesPattern(policySourceIdPatterns, sourceId) ) {
					continue;
				}
				LOG.warn("Access DENIED to source {} for {}: policy restriction", sourceId, principal);
				itr.remove();
			}
			if ( sourceIdsSet.size() < 1 ) {
				LOG.warn("Access DENIED to sources {} for {}", sourceIds, principal);
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, sourceIds);
			} else if ( sourceIdsSet.size() < sourceIds.length ) {
				sourceIds = sourceIdsSet.toArray(new String[sourceIdsSet.size()]);
			}
		} else if ( sourceIds == null || sourceIds.length < 1 ) {
			// no source IDs provided, set to policy source IDs
			LOG.info("Access RESTRICTED to sources {} for {}", policySourceIds, principal);
			sourceIds = policySourceIds.toArray(new String[policySourceIds.size()]);
		}
		if ( cacheResults ) {
			cachedSourceIds = (sourceIds == null ? new String[0] : sourceIds);
		}
		return sourceIds;
	}

	private Aggregation verifyAggregation(Aggregation agg) {
		final Aggregation min = policy.getMinAggregation();
		if ( min != null ) {
			if ( agg == null || agg.compareLevel(min) < 0 ) {
				LOG.info("Access RESTRICTED from aggregation {} to {} for {}", agg, min, principal);
				return min;
			}
			return agg;
		}
		final Set<Aggregation> allowed = policy.getAggregations();
		if ( allowed == null || allowed.isEmpty() || allowed.contains(agg) ) {
			return agg;
		}
		LOG.warn("Access DENIED to aggregation {} for {}", agg, principal);
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, agg);
	}

	/**
	 * Verify an arbitrary metadata instance against the configured policy.
	 * 
	 * @param metadata
	 *        The metadata to verify.
	 * @return The allowed metadata.
	 * @throws AuthorizationException
	 *         if no metadata access is allowed
	 */
	public GeneralDatumMetadata verifyMetadata(GeneralDatumMetadata metadata) {
		return verifyMetadata(metadata, false);
	}

	private GeneralDatumMetadata verifyMetadata(final GeneralDatumMetadata meta,
			final boolean cacheResults) {
		final Set<String> policyMetadataPaths;
		switch (metadataType) {
			case Edge:
				policyMetadataPaths = policy.getEdgeMetadataPaths();
				break;

			case User:
				policyMetadataPaths = policy.getUserMetadataPaths();
				break;

			default:
				policyMetadataPaths = Collections.emptySet();
				break;
		}

		// verify metadata
		if ( meta == null || policyMetadataPaths == null || policyMetadataPaths.isEmpty() ) {
			return meta;
		}
		if ( cacheResults && cachedMetadata != null ) {
			return cachedMetadata;
		}

		Map<String, Object> infoMap = null;
		if ( meta.getInfo() != null ) {
			infoMap = enforceMetadataPaths(policyMetadataPaths, meta.getInfo(), "/m");
		}

		Map<String, Object> propMap = null;
		if ( meta.getPropertyInfo() != null ) {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Map<String, Object> pm = (Map) meta.getPropertyInfo();
			propMap = enforceMetadataPaths(policyMetadataPaths, pm, "/pm");
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, Map<String, Object>> pm = (Map) propMap;
		GeneralDatumMetadata result = new GeneralDatumMetadata(infoMap, pm);
		result.setTags(meta.getTags());
		if ( result.equals(meta) ) {
			return meta;
		}

		if ( infoMap == null && propMap == null ) {
			// no metadata matches any path, so throw exception
			LOG.warn("Access DENIED to metadata {} on {} for {}", meta, delegate, principal);
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, meta);
		}

		if ( cacheResults ) {
			cachedMetadata = result;
		}
		return result;
	}

	private Map<String, Object> enforceMetadataPaths(Set<String> policyPaths, Map<String, Object> meta,
			String path) {
		if ( meta == null ) {
			return null;
		}
		Map<String, Object> result = null;
		for ( Map.Entry<String, Object> me : meta.entrySet() ) {
			String entryPath = path + "/" + me.getKey();
			Object val = me.getValue();
			if ( val instanceof Map ) {
				// object Edge; try to remove entire trees from checking if the path start doesn't match
				if ( !matchesPatternStart(policyPaths, entryPath) ) {
					continue;
				}
				// descend into map path for verification
				@SuppressWarnings("unchecked")
				Map<String, Object> mapVal = (Map<String, Object>) val;
				mapVal = enforceMetadataPaths(policyPaths, mapVal, entryPath);
				if ( mapVal != null ) {
					if ( result == null ) {
						result = new LinkedHashMap<String, Object>(meta.size());
					}
					result.put(me.getKey(), mapVal);
				}
			} else {
				// leaf Edge
				if ( matchesPattern(policyPaths, entryPath) ) {
					if ( result == null ) {
						result = new LinkedHashMap<String, Object>(meta.size());
					}
					result.put(me.getKey(), val);
				}
			}
		}
		if ( result == null || result.isEmpty() ) {
			return null;
		}
		return result;
	}

	public Object getDelgate() {
		return delegate;
	}

}
