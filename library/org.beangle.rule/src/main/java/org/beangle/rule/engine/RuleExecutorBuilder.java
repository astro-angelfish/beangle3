/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.rule.engine;

import java.util.List;

import org.beangle.rule.Rule;
import org.beangle.rule.model.RuleConfig;

public interface RuleExecutorBuilder {

	public RuleExecutor build(Rule rule);

	public RuleExecutor build(List<Rule> rules, boolean stopWhenFail);

	public RuleExecutor build(RuleConfig ruleConfig);

}