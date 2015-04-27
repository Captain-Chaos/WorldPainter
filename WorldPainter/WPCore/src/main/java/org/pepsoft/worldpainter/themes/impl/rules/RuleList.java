/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.themes.impl.rules;

/**
 * A rule which contains an ordered list of rules which will be tried one at a
 * time until the first one which matches.
 * 
 * @author SchmitzP
 */
public final class RuleList extends Rule {
    public RuleList(Rule[] rules, Action[] actions) {
        super(actions);
        this.rules = rules;
    }

    @Override
    public boolean apply(Context context) {
        for (Rule rule: rules) {
            if (rule.apply(context)) {
                applyActions(context);
                return true;
            }
        }
        return false;
    }

    public Rule[] getRules() {
        return rules;
    }
    
    private final Rule[] rules;
}