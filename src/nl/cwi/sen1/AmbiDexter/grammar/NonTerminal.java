/**
 * 
 */
package nl.cwi.sen1.AmbiDexter.grammar;

import java.util.Set;

import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;


public class NonTerminal extends Symbol {
	public Set<Production> productions;
	public FollowRestrictions followRestrictions;
	public FollowRestrictions precedeRestrictions;
	public boolean usedForReject = false;
	public boolean productive = true;
	public boolean reachable = false;
	public Set<NonTerminal> rejectedLiterals; // only literals
	public boolean usedInRejectFilter = false;
	public boolean lexical = true; // cf(..) or lex(..)
	public boolean layout = false;
	public boolean usedInLayout = false;
	public boolean literal = false;
	public String asfixName;
	public boolean directlyNullable = false;
	
	public NonTerminal(String s) {
		super(s);
		productions = new ShareableHashSet<Production>();
	}
	
	public void finish() {
		for (Production p : productions) {
			if (p.isEmpty()) {
				directlyNullable = true;
				return;
			}
		}
	}
	
	public void addFollowRestrictions(FollowRestrictions fr) {
		if (followRestrictions == null) {
			followRestrictions = fr;
		} else {
			followRestrictions.add(fr);
		}
	}

	public void addPrecedeRestrictions(FollowRestrictions pr) {
		if (precedeRestrictions == null) {
			precedeRestrictions = pr;
		} else {
			precedeRestrictions.add(pr);
		}
	}
}