/*
 * Copyright (c) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.auth.oauth2;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;

/**
 * OAuth 2.0 Web Server flow as specified in <a
 * href="http://tools.ietf.org/html/draft-ietf-oauth-v2-06#section-2.5">Web
 * Server Flow</a>.
 * <p>
 * Sample usage:
 * 
 * <pre>
 * <code>@Override
 * public void doGet(HttpServletRequest request, HttpServletResponse response)
 *     throws IOException {
 *   PrintWriter writer = response.getWriter();
 *   HttpTransport transport = new HttpTransport();
 *   StringBuffer fullUrlBuf = request.getRequestURL();
 *   if (request.getQueryString() != null) {
 *     fullUrlBuf.append('?').append(request.getQueryString());
 *   }
 *   String fullUrl = fullUrlBuf.toString();
 *   AuthorizationResponseUrl responseUrl =
 *       new AuthorizationResponseUrl(fullUrl);
 *   // check for user-denied error
 *   if (responseUrl.error != null) {
 *     writer.println("Authorization denied.");
 *     return;
 *   }
 *   // if no user-granted code yet, then redirect to user auth page
 *   if (responseUrl.code == null) {
 *     AuthorizationUrl authorizeUrl = new AuthorizationUrl(AUTHORIZE_URL);
 *     authorizeUrl.clientId = CLIENT_ID;
 *     authorizeUrl.redirectUri = request.getRequestURL().toString();
 *     authorizeUrl.scope = SCOPE;
 *     response.sendRedirect(authorizeUrl.build());
 *     return;
 *   }
 *   // have user-granted code, so request access token
 *   AccessTokenRequest tokenRequest =
 *       new AccessTokenRequest(ACCESS_TOKEN_URL);
 *   tokenRequest.clientId = CLIENT_ID;
 *   tokenRequest.redirectUri = request.getRequestURL().toString();
 *   tokenRequest.clientSecret = CLIENT_SECRET;
 *   tokenRequest.code = responseUrl.code;
 *   AccessTokenResponse tokenResponse = tokenRequest.execute();
 *   tokenResponse.authorize(transport);
 *   // ... run HTTP code using transport ...
 * }
 * </code>
 * </pre>
 * 
 * @since 2.3
 * @author Yaniv Inbar
 */
public final class WebServerFlow {

  /**
   * URL builder for an authorization web page to allow the end user to
   * authorize the application to access their protected resources.
   * <p>
   * The most commonly-set fields are {@link #clientId} and {@link #redirectUri}
   * , and possibly {@link #scope}. After the end-user grants or denies the
   * request, they will be redirected to the {@link #redirectUri} with query
   * parameters set by the authorization server. Use
   * {@link AuthorizationResponseUrl} to parse the redirect URL.
   */
  public static class AuthorizationUrl extends GenericUrl {

    /**
     * @param encodedAuthorizationServerUrl encoded authorization server URL
     */
    public AuthorizationUrl(String encodedAuthorizationServerUrl) {
      super(encodedAuthorizationServerUrl);
    }

    /** (REQUIRED) The parameter value MUST be set to "web_server". */
    @Key
    public final String type = "web_server";

    /** (REQUIRED) The client identifier. */
    @Key("client_id")
    public String clientId;

    /**
     * REQUIRED unless a redirection URI has been established between the client
     * and authorization server via other means. An absolute URI to which the
     * authorization server will redirect the user-agent to when the end-user
     * authorization step is completed. The authorization server MAY require the
     * client to pre-register their redirection URI. Authorization servers MAY
     * restrict the redirection URI to not include a query component as defined
     * by [RFC3986] section 3.
     */
    @Key("redirect_uri")
    public String redirectUri;

    /**
     * (OPTIONAL) An opaque value used by the client to maintain state between
     * the request and callback. The authorization server includes this value
     * when redirecting the user-agent back to the client.
     */
    @Key
    public String state;

    /**
     * (OPTIONAL) The scope of the access request expressed as a list of
     * space-delimited strings. The value of the "scope" parameter is defined by
     * the authorization server. If the value contains multiple space-delimited
     * strings, their order does not matter, and each string adds an additional
     * access range to the requested scope.
     */
    @Key
    public String scope;

    /**
     * (OPTIONAL) The parameter value must be set to "true" or "false". If set
     * to "true", the authorization server MUST NOT prompt the end-user to
     * authenticate or approve access. Instead, the authorization server
     * attempts to establish the end-user's identity via other means (e.g.
     * browser cookies) and checks if the end-user has previously approved an
     * identical access request by the same client and if that access grant is
     * still active. If the authorization server does not support an immediate
     * check or if it is unable to establish the end-user's identity or approval
     * status, it MUST deny the request without prompting the end-user. Defaults
     * to "false" if omitted.
     */
    @Key
    public Boolean immediate;
  }

  /**
   * Parses the redirect URL after end user grants or denies authorization.
   * <p>
   * To check if the end user grants authorization, check if {@link #error} is
   * {@code null}. If the end user grants authorization, the next step is to
   * request an access token using {@link AccessTokenRequest}. Use the
   * {@link #code} in {@link AccessTokenRequest#code}.
   */
  public static class AuthorizationResponseUrl extends GenericUrl {

    /**
     * (REQUIRED if the end user denies authorization) MUST be set to
     * "user_denied".
     */
    @Key
    public String error;

    /**
     * (REQUIRED if the end user grants authorization) The verification code
     * generated by the authorization server.
     */
    @Key
    public String code;

    /**
     * REQUIRED if the "state" parameter was present in the client authorization
     * request. Set to the exact value received from the client.
     */
    @Key
    public String state;

    /**
     * @param encodedRedirectUrl encoded redirect URL
     */
    public AuthorizationResponseUrl(String encodedRedirectUrl) {
      super(encodedRedirectUrl);
    }
  }

  /**
   * Request an access token.
   * <p>
   * The most commonly set fields are {@link #clientId}, {@link #clientSecret},
   * {@link #code}, and {@link #redirectUri}. Call {@link #execute()} to execute
   * the request.
   */
  public static class AccessTokenRequest extends AbstractAccessTokenRequest {

    /**
     * (REQUIRED) The verification code received from the authorization server.
     */
    @Key
    public String code;

    /** (REQUIRED) The redirection URI used in the initial request. */
    @Key("redirect_uri")
    public String redirectUri;

    /**
     * @param encodedAuthorizationServerUrl encoded authorization server URL
     */
    public AccessTokenRequest(String encodedAuthorizationServerUrl) {
      super(encodedAuthorizationServerUrl, "web_server");
    }
  }

  private WebServerFlow() {
  }
}
