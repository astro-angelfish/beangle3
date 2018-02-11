/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.struts2.view.component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.struts2.StrutsException;
import org.apache.struts2.util.FastByteArrayOutputStream;
import org.apache.struts2.views.util.ContextUtil;

import com.opensymphony.xwork2.util.TextParseUtil;
import com.opensymphony.xwork2.util.ValueStack;

/**
 * <li>remove actionMapper\determineActionURL\determineNamespace</li> <li>remove copy parameter</li>
 * 
 * @author chaostone
 */
public class Component {
  public static final String COMPONENT_STACK = "b_component_stack";
  protected ValueStack stack;
  protected Map<String, Object> parameters;

  /**
   * Constructor.
   * 
   * @param stack
   *          OGNL value stack.
   */
  public Component(ValueStack stack) {
    this.stack = stack;
    this.parameters = new LinkedHashMap<String, Object>();
    getComponentStack().push(this);
  }

  /**
   * Gets the name of this component.
   * 
   * @return the name of this component.
   */
  private String getComponentName() {
    Class<?> c = getClass();
    String name = c.getName();
    int dot = name.lastIndexOf('.');
    return name.substring(dot + 1).toLowerCase();
  }

  /**
   * Gets the component stack of this component.
   * 
   * @return the component stack of this component, never <tt>null</tt>.
   */
  protected Stack<Component> getComponentStack() {
    @SuppressWarnings("unchecked")
    Stack<Component> componentStack = (Stack<Component>) stack.getContext().get(COMPONENT_STACK);
    if (componentStack == null) {
      componentStack = new Stack<Component>();
      stack.getContext().put(COMPONENT_STACK, componentStack);
    }
    return componentStack;
  }

  /**
   * Callback for the start tag of this component. Should the body be
   * evaluated?
   * 
   * @param writer
   *          the output writer.
   * @return true if the body should be evaluated
   */
  public boolean start(Writer writer) {
    return true;
  }

  /**
   * Callback for the end tag of this component. Should the body be evaluated
   * again?
   * <p/>
   * <b>NOTE:</b> will pop component stack.
   * 
   * @param writer the output writer.
   * @param body the rendered body.
   * @return true if the body should be evaluated again
   */
  public boolean end(Writer writer, String body) {
    return end(writer, body, true);
  }

  /**
   * Callback for the start tag of this component. Should the body be
   * evaluated again?
   * <p/>
   * <b>NOTE:</b> has a parameter to determine to pop the component stack.
   * 
   * @param writer the output writer.
   * @param body the rendered body.
   * @param popComponentStack
   *          should the component stack be popped?
   * @return true if the body should be evaluated again
   */
  protected boolean end(Writer writer, String body, boolean popComponentStack) {
    assert (body != null);
    try {
      writer.write(body);
    } catch (IOException e) {
      throw new StrutsException("IOError while writing the body: " + e.getMessage(), e);
    }
    if (popComponentStack) popComponentStack();
    return false;
  }

  /**
   * Pops the component stack.
   */
  protected void popComponentStack() {
    getComponentStack().pop();
  }

  /**
   * Finds the nearest ancestor of this component stack.
   * 
   * @param clazz
   *          the class to look for, or if assignable from.
   * @return the component if found, <tt>null</tt> if not.
   */
  @SuppressWarnings("unchecked")
  protected <T extends Component> T findAncestor(Class<T> clazz) {
    Stack<? extends Component> componentStack = getComponentStack();
    for (int i = componentStack.size() - 2; i >= 0; i--) {
      Component component = componentStack.get(i);
      if (clazz.equals(component.getClass())) return (T) component;
    }
    return null;
  }

  /**
   * Evaluates the OGNL stack to find a String value.
   * 
   * @param expr
   *          OGNL expression.
   * @return the String value found.
   */
  protected String findString(String expr) {
    return (String) findValue(expr, String.class);
  }

  /**
   * Evaluates the OGNL stack to find a String value.
   * <p/>
   * If the given expression is <tt>null</tt/> a error is logged and a <code>RuntimeException</code>
   * is thrown constructed with a messaged based on the given field and errorMsg paramter.
   * 
   * @param expr
   *          OGNL expression.
   * @param field
   *          field name used when throwing <code>RuntimeException</code>.
   * @param errorMsg
   *          error message used when throwing <code>RuntimeException</code> .
   * @return the String value found.
   * @throws StrutsException
   *           is thrown in case of expression is <tt>null</tt>.
   */
  protected String findString(String expr, String field, String errorMsg) {
    if (expr == null) {
      throw fieldError(field, errorMsg, null);
    } else {
      return findString(expr);
    }
  }

  /**
   * Constructs a <code>RuntimeException</code> based on the given
   * information.
   * <p/>
   * A message is constructed and logged at ERROR level before being returned as a
   * <code>RuntimeException</code>.
   * 
   * @param field
   *          field name used when throwing <code>RuntimeException</code>.
   * @param errorMsg
   *          error message used when throwing <code>RuntimeException</code> .
   * @param e
   *          the caused exception, can be <tt>null</tt>.
   * @return the constructed <code>StrutsException</code>.
   */
  protected StrutsException fieldError(String field, String errorMsg, Exception e) {
    String msg = "tag '" + getComponentName() + "', field '" + field
        + (parameters != null && parameters.containsKey("name") ? "', name '" + parameters.get("name") : "")
        + "': " + errorMsg;
    throw new StrutsException(msg, e);
  }

  /**
   * Finds a value from the OGNL stack based on the given expression. Will
   * always evaluate <code>expr</code> against stack except when <code>expr</code> is null. If
   * altsyntax (%{...}) is applied, simply strip
   * it off.
   * 
   * @param expr
   *          the expression. Returns <tt>null</tt> if expr is null.
   * @return the value, <tt>null</tt> if not found.
   */
  protected Object findValue(String expr) {
    if (expr == null) { return null; }
    expr = stripExpressionIfAltSyntax(expr);
    return stack.findValue(expr, false);
  }

  /**
   * If altsyntax (%{...}) is applied, simply strip the "%{" and "}" off.
   * 
   * @param expr
   *          the expression (must be not null)
   * @return the stripped expression if altSyntax is enabled. Otherwise the
   *         parameter expression is returned as is.
   */
  protected String stripExpressionIfAltSyntax(String expr) {
    return stripExpressionIfAltSyntax(stack, expr);
  }

  /**
   * If altsyntax (%{...}) is applied, simply strip the "%{" and "}" off.
   * 
   * @param stack
   *          the ValueStack where the context value is searched for.
   * @param expr
   *          the expression (must be not null)
   * @return the stripped expression if altSyntax is enabled. Otherwise the
   *         parameter expression is returned as is.
   */
  public static String stripExpressionIfAltSyntax(ValueStack stack, String expr) {
    if (altSyntax(stack)) {
      // does the expression start with %{ and end with }? if so, just cut
      // it off!
      if (expr.startsWith("%{") && expr.endsWith("}")) { return expr.substring(2, expr.length() - 1); }
    }
    return expr;
  }

  /**
   * Is the altSyntax enabled? [TRUE]
   * <p/>
   * 
   * @param stack
   *          the ValueStack where the context value is searched for.
   * @return true if altSyntax is activated. False otherwise. See <code>struts.properties</code>
   *         where the altSyntax flag is
   *         defined.
   */
  public static boolean altSyntax(ValueStack stack) {
    return ContextUtil.isUseAltSyntax(stack.getContext());
  }

  /**
   * Is the altSyntax enabled? [TRUE]
   * <p/>
   * See <code>struts.properties</code> where the altSyntax flag is defined.
   */
  public boolean altSyntax() {
    return altSyntax(stack);
  }

  /**
   * Adds the sorrounding %{ } to the expression for proper processing.
   * 
   * @param expr
   *          the expression.
   * @return the modified expression if altSyntax is enabled, or the parameter
   *         expression otherwise.
   */
  protected String completeExpressionIfAltSyntax(String expr) {
    if (altSyntax()) { return "%{" + expr + "}"; }
    return expr;
  }

  /**
   * This check is needed for backwards compatibility with 2.1.x
   * 
   * @param expr
   *          the expression.
   * @return the found string if altSyntax is enabled. The parameter
   *         expression otherwise.
   */
  protected String findStringIfAltSyntax(String expr) {
    if (altSyntax()) { return findString(expr); }
    return expr;
  }

  /**
   * Evaluates the OGNL stack to find an Object value.
   * <p/>
   * Function just like <code>findValue(String)</code> except that if the given expression is
   * <tt>null</tt/> a error is logged and a <code>RuntimeException</code> is thrown constructed with
   * a messaged based on the given field and errorMsg paramter.
   * 
   * @param expr
   *          OGNL expression.
   * @param field
   *          field name used when throwing <code>RuntimeException</code>.
   * @param errorMsg
   *          error message used when throwing <code>RuntimeException</code> .
   * @return the Object found, is never <tt>null</tt>.
   * @throws StrutsException
   *           is thrown in case of not found in the OGNL stack, or
   *           expression is <tt>null</tt>.
   */
  protected Object findValue(String expr, String field, String errorMsg) {
    if (expr == null) {
      throw fieldError(field, errorMsg, null);
    } else {
      Object value = null;
      Exception problem = null;
      try {
        value = findValue(expr);
      } catch (Exception e) {
        problem = e;
      }

      if (value == null) { throw fieldError(field, errorMsg, problem); }

      return value;
    }
  }

  /**
   * Evaluates the OGNL stack to find an Object of the given type. Will
   * evaluate <code>expr</code> the portion wrapped with altSyntax (%{...})
   * against stack when altSyntax is on, else the whole <code>expr</code> is
   * evaluated against the stack.
   * <p/>
   * This method only supports the altSyntax. So this should be set to true.
   * 
   * @param expr
   *          OGNL expression.
   * @param toType
   *          the type expected to find.
   * @return the Object found, or <tt>null</tt> if not found.
   */
  protected Object findValue(String expr, Class<?> toType) {
    if (altSyntax() && toType == String.class) {
      return TextParseUtil.translateVariables('%', expr, stack);
    } else {
      expr = stripExpressionIfAltSyntax(expr);
      return stack.findValue(expr, toType, false);
    }
  }

  /**
   * Constructs a string representation of the given exception.
   * 
   * @param t
   *          the exception
   * @return the exception as a string.
   */
  protected String toString(Throwable t) {
    FastByteArrayOutputStream bout = new FastByteArrayOutputStream();
    PrintWriter wrt = new PrintWriter(bout);
    t.printStackTrace(wrt);
    wrt.close();
    return bout.toString();
  }

  /**
   * Gets the parameters.
   * 
   * @return the parameters. Is never <tt>null</tt>.
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * Adds all the given parameters to this component's own parameters.
   * 
   * @param params
   *          the parameters to add.
   */
  public void addAllParameters(Map<String, Object> params) {
    parameters.putAll(params);
  }

  /**
   * Adds the given key and value to this component's own parameter.
   * <p/>
   * If the provided key is <tt>null</tt> nothing happens. If the provided value is <tt>null</tt>
   * any existing parameter with the given key name is removed.
   * 
   * @param key
   *          the key of the new parameter to add.
   * @param value
   *          the value assoicated with the key.
   */
  public void addParameter(String key, Object value) {
    if (key != null) {
      Map<String, Object> params = getParameters();
      if (value == null) params.remove(key);
      else params.put(key, value);
    }
  }

  /**
   * Overwrite to set if body shold be used.
   * 
   * @return always false for this component.
   */
  public boolean usesBody() {
    return false;
  }
}
