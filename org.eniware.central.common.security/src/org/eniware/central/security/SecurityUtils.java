/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.security;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Security helper methods.
 
 * @version 1.3
 */
public class SecurityUtils {

	private static final Logger LOG = LoggerFactory.getLogger(SecurityUtils.class);

	/**
	 * Authenticate a user.
	 * 
	 * @param authenticationManager
	 *        the {@link AuthenticationManager}
	 * @param user
	 *        the user to authenticate
	 */
	public static void authenticate(AuthenticationManager authenticationManager, Object username,
			Object password) {
		try {
			UsernamePasswordAuthenticationToken usernameAndPassword = new UsernamePasswordAuthenticationToken(
					username, password);
			Authentication auth = authenticationManager.authenticate(usernameAndPassword);
			SecurityContextHolder.getContext().setAuthentication(auth);
		} catch ( AuthenticationException e ) {
			SecurityContextHolder.getContext().setAuthentication(null);
			throw e;
		}
	}

	/**
	 * Become a user with a {@code RUN_AS_ROLE_USER} authority.
	 * 
	 * @param username
	 *        the username (email) to use
	 * @param name
	 *        the name
	 * @param userId
	 *        the user ID
	 * @since 1.1
	 */
	public static void becomeUser(String username, String name, Long userId) {
		User userDetails = new User(username, "", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, userId, name, false);
		Collection<GrantedAuthority> authorities = Collections
				.singleton((GrantedAuthority) new SimpleGrantedAuthority("RUN_AS_ROLE_USER"));
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, "",
				authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	/**
	 * Require any one of a set of roles for the current actor. The actor's
	 * roles are converted to upper case before testing for inclusion in the
	 * {@code roles} argument.
	 * 
	 * @param roles
	 * @since 1.2
	 */
	public static void requireAnyRole(final Set<String> roles) {
		Authentication auth = getCurrentAuthentication();
		if ( auth == null || auth.isAuthenticated() == false ) {
			throw new AuthorizationException(AuthorizationException.Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		for ( GrantedAuthority role : auth.getAuthorities() ) {
			if ( roles.contains(role.getAuthority().toUpperCase()) ) {
				return;
			}
		}
		throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
	}

	/**
	 * Require any one of a set of roles for the current actor. The actor's
	 * roles are converted to upper case before testing for inclusion in the
	 * {@code roles} argument.
	 * 
	 * @param roles
	 * @since 1.2
	 */
	public static void requireAllRoles(final Set<String> roles) {
		Authentication auth = getCurrentAuthentication();
		if ( auth == null || auth.isAuthenticated() == false ) {
			throw new AuthorizationException(AuthorizationException.Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		Set<String> rolesCopy = new HashSet<String>(roles);
		for ( GrantedAuthority role : auth.getAuthorities() ) {
			if ( rolesCopy.remove(role.getAuthority().toUpperCase()) == false ) {
				throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
			}
			if ( rolesCopy.size() < 1 ) {
				return;
			}
		}
		if ( rolesCopy.size() > 0 ) {
			throw new AuthorizationException(AuthorizationException.Reason.ACCESS_DENIED, null);
		}
	}

	/**
	 * Get the current active authentication.
	 * 
	 * @return the active Authentication, or <em>null</em> if none available
	 */
	public static Authentication getCurrentAuthentication() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if ( auth == null ) {
			LOG.debug("No Authentication available, cannot tell current user");
			return null;
		}
		return auth;
	}

	/**
	 * Get the current {@link SecurityActor}.
	 * 
	 * @return the current actor, never <em>null</em>
	 * @throws SecurityException
	 *         if the actor is not available
	 */
	public static SecurityActor getCurrentActor() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityActor ) {
			return (SecurityActor) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityActor ) {
			return (SecurityActor) auth.getDetails();
		}
		throw new SecurityException("Actor not available");
	}

	/**
	 * Get the current {@link SecurityActor}'s {@code userId}.
	 * 
	 * @return The user ID of the current {@link SecurityActor} (never
	 *         <em>null</em>).
	 * @throws SecurityException
	 *         If the actor is not available.
	 * @since 1.3
	 */
	public static Long getCurrentActorUserId() throws SecurityException {
		SecurityActor actor = getCurrentActor();
		Long userId = null;
		if ( actor instanceof SecurityUser ) {
			userId = ((SecurityUser) actor).getUserId();
		} else if ( actor instanceof SecurityToken ) {
			userId = ((SecurityToken) actor).getUserId();
		}
		if ( userId != null ) {
			return userId;
		}
		throw new SecurityException("User not available");
	}

	/**
	 * Get the current {@link SecurityToken}.
	 * 
	 * @return the current actor, never <em>null</em>
	 * @throws SecurityException
	 *         if the actor is not available
	 */
	public static SecurityToken getCurrentToken() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityToken ) {
			return (SecurityToken) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityToken ) {
			return (SecurityToken) auth.getDetails();
		}
		throw new SecurityException("Token not available");
	}

	/**
	 * Get the current {@link SecurityUser}.
	 * 
	 * @return the current user, never <em>null</em>
	 * @throws SecurityException
	 *         if the user is not available
	 */
	public static SecurityUser getCurrentUser() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityUser ) {
			return (SecurityUser) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityUser ) {
			return (SecurityUser) auth.getDetails();
		}
		throw new SecurityException("User not available");
	}

	/**
	 * Get the current {@link SecurityEdge}.
	 * 
	 * @return the current Edge, never <em>null</em>
	 * @throws SecurityException
	 *         if the Edge is not available
	 */
	public static SecurityEdge getCurrentEdge() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityEdge ) {
			return (SecurityEdge) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityEdge ) {
			return (SecurityEdge) auth.getDetails();
		}
		throw new SecurityException("Edge not available");
	}

}
