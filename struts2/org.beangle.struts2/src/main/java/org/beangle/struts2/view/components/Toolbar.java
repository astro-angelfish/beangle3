/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.struts2.view.components;

import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.xwork.StringUtils;

import com.opensymphony.xwork2.util.ValueStack;

public class Toolbar extends ClosingUIBean {
	final private static transient Random RANDOM = new Random();

	public Toolbar(ValueStack stack, HttpServletRequest req, HttpServletResponse res) {
		super(stack, req, res);
	}
	
	public void evaluateExtraParams() {
		if (StringUtils.isEmpty(this.id)) {
			int nextInt = RANDOM.nextInt();
			nextInt = (nextInt == Integer.MIN_VALUE) ? Integer.MAX_VALUE : Math.abs(nextInt);
			this.id = "toolbar_" + String.valueOf(nextInt);
		}
	}

}
