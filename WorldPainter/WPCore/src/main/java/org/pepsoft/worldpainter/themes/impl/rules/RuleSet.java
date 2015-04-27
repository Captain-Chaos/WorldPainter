/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.themes.impl.rules;

/**
 * A rule which contains an ordered list of rules which will all be applied in
 * order.
 * 
 * @author SchmitzP
 */
public class RuleSet extends Rule {
    public RuleSet(Rule[] rules, Action[] actions) {
        super(actions);
        this.rules = rules;
    }

    @Override
    public boolean apply(Context context) {
        boolean applied = false;
        for (Rule rule: rules) {
            applied |= rule.apply(context);
        }
        if (applied) {
            applyActions(context);
        }
        return applied;
    }

    public Rule[] getRules() {
        return rules;
    }
    
    private final Rule[] rules;
}