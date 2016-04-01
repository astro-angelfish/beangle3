/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2016, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.security.access.intercept;

import org.beangle.commons.bean.Initializing;
import org.beangle.commons.lang.Assert;
import org.beangle.security.access.AccessDeniedException;
import org.beangle.security.access.AuthorityManager;
import org.beangle.security.auth.AuthenticationManager;
import org.beangle.security.core.Authentication;
import org.beangle.security.core.AuthenticationException;
import org.beangle.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that implements security interception for secure objects.
 * <p>
 * The <code>AbstractSecurityInterceptor</code> will ensure the proper startup configuration of the
 * security interceptor. It will also implement the proper handling of secure object invocations,
 * namely:
 * <ol>
 * <li>Obtain the {@link Authentication} object from the {@link SecurityContextHolder}.</li>
 * <li>Determine if the request relates to a secured or public invocation by looking up the secure
 * object request against the ObjectDefinitionSource.</li>
 * <li>For an invocation that is secured (there is a <code>ConfigAttributeDefinition</code> for the
 * secure object invocation):
 * <ol type="a">
 * <li>If either the {@link org.beangle.security.core.Authentication#isAuthenticated()} returns
 * <code>false</code>, or the {@link #alwaysReauthenticate} is <code>true</code> , authenticate the
 * request against the configured {@link AuthenticationManager}. When authenticated, replace the
 * <code>Authentication</code> object on the <code>SecurityContextHolder</code> with the returned
 * value.</li>
 * <li>Authorize the request against the configured {@link AuthorityManager}.</li>
 * <li>Perform any run-as replacement via the configured RunAsManager.</li>
 * <li>Pass control back to the concrete subclass, which will actually proceed with executing the
 * object. A {@link InterceptorStatusToken} is returned so that after the subclass has finished
 * proceeding with execution of the object, its finally clause can ensure the
 * <code>AbstractSecurityInterceptor</code> is re-called and tidies up correctly.</li>
 * <li>The concrete subclass will re-call the <code>AbstractSecurityInterceptor</code> via the
 * {@link #afterInvocation(InterceptorStatusToken, Object)} method.</li>
 * <li>If the <code>RunAsManager</code> replaced the <code>Authentication</code> object, return the
 * <code>SecurityContextHolder</code> to the object that existed after the call to
 * <code>AuthenticationManager</code>.</li>
 * <li>If an <code>AfterInvocationManager</code> is defined, invoke the invocation manager and allow
 * it to replace the object due to be returned to the caller.</li>
 * </ol>
 * </li>
 * <li>For an invocation that is public (there is no <code>ConfigAttributeDefinition</code> for the
 * secure object invocation):
 * <ol type="a">
 * <li>As described above, the concrete subclass will be returned an
 * <code>InterceptorStatusToken</code> which is subsequently re-presented to the
 * <code>AbstractSecurityInterceptor</code> after the secure object has been executed. The
 * <code>AbstractSecurityInterceptor</code> will take no further action when its
 * {@link #afterInvocation(InterceptorStatusToken, Object)} is called.</li>
 * </ol>
 * </li>
 * <li>Control again returns to the concrete subclass, along with the <code>Object</code> that
 * should be returned to the caller. The subclass will then return that result or exception to the
 * original caller.</li>
 * </ol>
 */
public abstract class AbstractSecurityInterceptor implements Initializing {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractSecurityInterceptor.class);

  private AuthorityManager authorityManager;
  private AuthenticationManager authenticationManager;

  private boolean alwaysReauthenticate = false;
  private boolean rejectPublicInvocations = false;
  private boolean validateConfigAttributes = true;

  /**
   * Completes the work of the <tt>AbstractSecurityInterceptor</tt> after the
   * secure object invocation has been completed.
   * 
   * @param token as returned by the {@link #beforeInvocation(Object)} method
   * @param returnedObject any object returned from the secure object invocation (may be
   *          <tt>null</tt>)
   * @return the object the secure object invocation should ultimately return
   *         to its caller (may be <tt>null</tt>)
   */
  protected Object afterInvocation(InterceptorStatusToken token, Object returnedObject) {
    if (token == null) return returnedObject;
    if (token.isContextHolderRefreshRequired()) {
      logger.debug("Reverting to original Authentication: {}", token.getAuthentication());
      SecurityContextHolder.getContext().setAuthentication(token.getAuthentication());
    }
    return returnedObject;
  }

  public void init() throws Exception {
    Assert.notNull(getSecureObjectClass(),
        "Subclass must provide a non-null response to getSecureObjectClass()");
    Assert.notNull(this.authenticationManager, "An AuthenticationManager is required");
    Assert.notNull(this.authorityManager, "An AuthorityManager is required");
  }

  protected InterceptorStatusToken beforeInvocation(Object object) {
    Assert.notNull(object, "Object was null");
    if (!getSecureObjectClass().isAssignableFrom(object.getClass())) { throw new IllegalArgumentException(
        "Security invocation attempted for object " + object.getClass().getName()
            + " but AbstractSecurityInterceptor only configured to support secure objects of type: "
            + getSecureObjectClass()); }
    Authentication authenticated = authenticateIfRequired();

    // Attempt authorization
    if (!authorityManager.isAuthorized(authenticated, object)) { throw new AccessDeniedException(object,
        "access denied"); }
    logger.debug("Authorization successful");

    // AuthorizedEvent event = new AuthorizedEvent(object, attr,
    // authenticated);
    // publishEvent(event);

    // Attempt to run as a different user
    // Authentication runAs = this.runAsManager.buildRunAs(authenticated,
    // object, attr);

    return new InterceptorStatusToken(authenticated, false, object);

    // if (runAs == null) {
    // logger.debug("RunAsManager did not change Authentication object");
    //
    // // no further work post-invocation
    // return new InterceptorStatusToken(authenticated, false, attr,
    // object);
    // } else {
    // logger.debug("Switching to RunAs Authentication: {}" , runAs);
    // SecurityContextHolder.getContext().setAuthentication(runAs);
    // // revert to token.Authenticated post-invocation
    // return new InterceptorStatusToken(authenticated, true, attr, object);
    // }
  }

  /**
   * Checks the current authentication token and passes it to the
   * AuthenticationManager if {@link org.beangle.security.core.Authentication#isAuthenticated()}
   * returns false or the property <tt>alwaysReauthenticate</tt> has been set
   * to true.
   * 
   * @return an authenticated <tt>Authentication</tt> object.
   */
  private Authentication authenticateIfRequired() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (null == authentication) { throw new AuthenticationException(); }
    if (authentication.isAuthenticated() && !alwaysReauthenticate) {
      logger.debug("Previously Authenticated: {}", authentication);
      return authentication;
    }
    authentication = authenticationManager.authenticate(authentication);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    return authentication;
  }

  public AuthorityManager getAuthorityManager() {
    return authorityManager;
  }

  public AuthenticationManager getAuthenticationManager() {
    return this.authenticationManager;
  }

  /**
   * Indicates the type of secure objects the subclass will be presenting to
   * the abstract parent for processing. This is used to ensure collaborators
   * wired to the <code>AbstractSecurityInterceptor</code> all support the
   * indicated secure object class.
   * 
   * @return the type of secure object the subclass provides services for
   */
  public abstract Class<?> getSecureObjectClass();

  public boolean isAlwaysReauthenticate() {
    return alwaysReauthenticate;
  }

  public boolean isRejectPublicInvocations() {
    return rejectPublicInvocations;
  }

  public boolean isValidateConfigAttributes() {
    return validateConfigAttributes;
  }

  public void setAuthorityManager(AuthorityManager authorityManager) {
    this.authorityManager = authorityManager;
  }

  /**
   * Indicates whether the <code>AbstractSecurityInterceptor</code> should
   * ignore the {@link Authentication#isAuthenticated()} property. Defaults to <code>false</code>,
   * meaning by default the <code>Authentication.isAuthenticated()</code> property is trusted and
   * re-authentication will not occur if the principal has already been
   * authenticated.
   * 
   * @param alwaysReauthenticate
   *          <code>true</code> to force <code>AbstractSecurityInterceptor</code> to disregard
   *          the
   *          value of <code>Authentication.isAuthenticated()</code> and
   *          always re-authenticate the request (defaults to <code>false</code>).
   */
  public void setAlwaysReauthenticate(boolean alwaysReauthenticate) {
    this.alwaysReauthenticate = alwaysReauthenticate;
  }

  public void setAuthenticationManager(AuthenticationManager newManager) {
    this.authenticationManager = newManager;
  }

  /**
   * By rejecting public invocations (and setting this property to <tt>true</tt>), essentially you
   * are ensuring that every secure object
   * invocation advised by <code>AbstractSecurityInterceptor</code> has a
   * configuration attribute defined. This is useful to ensure a "fail safe"
   * mode where undeclared secure objects will be rejected and configuration
   * omissions detected early. An <tt>IllegalArgumentException</tt> will be
   * thrown by the <tt>AbstractSecurityInterceptor</tt> if you set this
   * property to <tt>true</tt> and an attempt is made to invoke a secure
   * object that has no configuration attributes.
   * 
   * @param rejectPublicInvocations
   *          set to <code>true</code> to reject invocations of secure
   *          objects that have no configuration attributes (by default it
   *          is <code>false</code> which treats undeclared secure objects
   *          as "public" or unauthorized).
   */
  public void setRejectPublicInvocations(boolean rejectPublicInvocations) {
    this.rejectPublicInvocations = rejectPublicInvocations;
  }

  public void setValidateConfigAttributes(boolean validateConfigAttributes) {
    this.validateConfigAttributes = validateConfigAttributes;
  }
}
