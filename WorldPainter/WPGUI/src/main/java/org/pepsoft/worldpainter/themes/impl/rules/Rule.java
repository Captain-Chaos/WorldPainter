/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.pepsoft.worldpainter.themes.impl.rules;

/**
 * A combination of one or more rules and zero or more actions to
 * perform when the rule applies. Rules must be immutable.
 * 
 * @author SchmitzP
 */
public abstract class Rule {
    public Rule(Action[] actions) {
        this.actions = actions;
    }
    
    public abstract boolean apply(Context context);
    
    protected void applyActions(Context context) {
        for (Action action: actions) {
            action.apply(context);
        }
    }
    
    private final Action[] actions;
}