/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.web.filter;

import javax.servlet.http.HttpServletRequest;

import org.beangle.commons.http.HttpMethod;
import org.beangle.commons.lang.Assert;
import org.beangle.commons.lang.Strings;
import org.springframework.util.AntPathMatcher;

/**
 * Matcher which compares a pre-defined ant-style pattern against the URL (
 * {@code servletPath + pathInfo}) of an {@code HttpServletRequest}. The query
 * string of the URL is ignored and matching is case-insensitive.
 * 
 * @see AntPathMatcher
 */
public final class AntPathRequestMatcher implements RequestMatcher {

  private static final AntPathMatcher antMatcher = new AntPathMatcher();

  private final String pattern;
  private final HttpMethod httpMethod;

  /**
   * Creates a matcher with the specific pattern which will match all HTTP
   * methods.
   * 
   * @param pattern
   *          the ant pattern to use for matching
   */
  public AntPathRequestMatcher(String pattern) {
    this(pattern, null);
  }

  /**
   * Creates a matcher with the supplied pattern which will match all HTTP
   * methods.
   * 
   * @param pattern
   *          the ant pattern to use for matching
   * @param httpMethod
   *          the HTTP method. The {@code matches} method will return false
   *          if the incoming request doesn't have the same method.
   */
  public AntPathRequestMatcher(String pattern, String httpMethod) {
    Assert.notEmpty(pattern, "Pattern cannot be null or empty");
    this.pattern = pattern.toLowerCase();
    this.httpMethod = Strings.isNotEmpty(httpMethod) ? HttpMethod.valueOf(httpMethod) : null;
  }

  /**
   * Returns true if the configured pattern (and HTTP-Method) match those of
   * the supplied request.
   * 
   * @param request
   *          the request to match against. The ant pattern will be matched
   *          against the {@code servletPath} + {@code pathInfo} of the
   *          request.
   */
  public boolean matches(HttpServletRequest request) {
    if (httpMethod != null && httpMethod != HttpMethod.valueOf(request.getMethod())) { return false; }
    String url = request.getServletPath();
    if (request.getPathInfo() != null) url += request.getPathInfo();
    url = url.toLowerCase();
    // TODO: Optimise, since the pattern is fixed.
    return antMatcher.match(pattern, url);
  }

  public String getPattern() {
    return pattern;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof AntPathRequestMatcher)) { return false; }
    AntPathRequestMatcher other = (AntPathRequestMatcher) obj;
    return this.pattern.equals(other.pattern) && this.httpMethod == other.httpMethod;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Ant [pattern='").append(pattern).append("'");

    if (httpMethod != null) {
      sb.append(", " + httpMethod);
    }

    sb.append("]");

    return sb.toString();
  }
}
