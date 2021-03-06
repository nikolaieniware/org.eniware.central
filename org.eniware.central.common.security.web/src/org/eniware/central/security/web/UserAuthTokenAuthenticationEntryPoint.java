/* ==================================================================
 *  Eniware Open Source:Nikolai Manchev
 *  Apache License 2.0
 * ==================================================================
 */

package org.eniware.central.security.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.eniware.web.security.AuthenticationScheme;
import org.eniware.web.security.WebConstants;

/**
 * Entry point for EniwareNetworkWS authentication.
 
 * @version 1.3
 */
public class UserAuthTokenAuthenticationEntryPoint implements AuthenticationEntryPoint, Ordered {

	private int order = Integer.MAX_VALUE;
	private Map<String, String> httpHeaders = defaultHttpHeaders();

	private static Map<String, String> defaultHttpHeaders() {
		Map<String, String> headers = new HashMap<String, String>(2);
		headers.put("Access-Control-Allow-Origin", "*");
		headers.put("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH");
		headers.put("Access-Control-Allow-Headers",
				"Authorization, Content-MD5, Content-Type, Digest, X-SN-Date");
		return headers;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		final String authHeaderValue = request.getHeader("Authorization");
		AuthenticationScheme authScheme = AuthenticationScheme.V2; // default to V2 unless a known V1 request
		if ( authHeaderValue != null
				&& authHeaderValue.startsWith(AuthenticationScheme.V1.getSchemeName()) ) {
			authScheme = AuthenticationScheme.V1;
		}
		response.addHeader("WWW-Authenticate", authScheme.getSchemeName());
		response.addHeader(WebConstants.HEADER_ERROR_MESSAGE, authException.getMessage());
		if ( httpHeaders != null ) {
			for ( Map.Entry<String, String> me : httpHeaders.entrySet() ) {
				response.addHeader(me.getKey(), me.getValue());
			}
		}
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Get the currently configured HTTP headers that are included in each
	 * response.
	 * 
	 * @return The HTTP headers to include in each response.
	 * @since 1.1
	 */
	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}

	/**
	 * Set additional HTTP headers to include in the response. By default the
	 * {@code Access-Control-Allow-Origin} header is set to {@code *} and
	 * {@code Access-Control-Allow-Headers} header is set to
	 * {@code Authorization, X-SN-Date}.
	 * 
	 * @param httpHeaders
	 *        The HTTP headers to include in each response.
	 * @since 1.1
	 */
	public void setHttpHeaders(Map<String, String> httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

}
