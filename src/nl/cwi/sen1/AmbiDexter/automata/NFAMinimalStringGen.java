package nl.cwi.sen1.AmbiDexter.automata;

import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class NFAMinimalStringGen {

	ShareableHashMap<Transition, Symbol[]> minimalStrings = new ShareableHashMap<Transition, Symbol[]>();
	Queue<Transition> q = new Queue<Transition>(256);
	
	public ShareableHashMap<Transition, Symbol[]> getMinimalStrings(NFA nfa) {
		
		// find all begins of productions and fill initial minimal strings
		Set<Item> begins = new ShareableHashSet<Item>();
		for (Item i : nfa.items) {
			if (i.atBegin()) {
				if (i.production.containsNonTerminals()) {
					begins.add(i);
				} else {
					traverseTerminalStrings(i);
				}
			}
		}
		
		for (Transition t : nfa.transitions) {
			if (!(t.label instanceof NonTerminal)) {
				minimalStrings.put(t, new Symbol[] {t.label});
			}
		}
		
		boolean changed;
		do {
			changed = false;
			
			for (Item i : begins) {
				ShareableHashMap<Item, StackNode> level = new ShareableHashMap<Item, StackNode>();
				level.put(i, new StackNode(null, null));
				
				// breadth first traverse all shift transitions
				// stacks join and continue with stack with minimal string length
				for (int j = i.production.getLength() - 1; j >= 0 && level.size() > 0; --j) {
					ShareableHashMap<Item, StackNode> nextLevel = new ShareableHashMap<Item, StackNode>();
					for (Entry<Item, StackNode> e : level) {
						for (Transition t : e.getKey().shifts) {
							Symbol[] tstr = minimalStrings.get(t);
							if (tstr != null) {
								StackNode n = e.getValue();
								StackNode nn = nextLevel.get(t.target);
								if (nn == null) {
									nextLevel.put(t.target, new StackNode(tstr, n));
								} else {
									if (nn.len > n.len + tstr.length) {
										// replace nn
										nextLevel.put(t.target, new StackNode(tstr, n));
									}
								}
							}
						}
					}
					level = nextLevel;
				}
				
				if (level.size() == 0) {
					continue;
				}
				
				// add minimalStrings for stacks that have reached the end of i.production
				for (Entry<Item, StackNode> e : level) {
					StackNode n = e.getValue();
					Symbol[] nstr = null;
					for (Transition r : e.getKey().reduces) {
						for (Transition shift : r.shifts) {
							Symbol[] sstr = minimalStrings.get(shift);
							if (sstr == null || sstr.length > n.len) {
								if (nstr == null) {
									nstr = new Symbol[n.len];
									int j = n.len;
									StackNode tail = n;
									while (tail.prev != null) {
										for (int k = tail.tstr.length - 1; k >= 0; --k) {
											nstr[--j] = tail.tstr[k];
										}
										tail = tail.prev;
									}
								}
								minimalStrings.put(shift, nstr);
								changed = true; // TODO can we make a todo set?
							}
						}
					}
				}				
			}
		} while (changed);
		
		return minimalStrings;
	}

	private void traverseTerminalStrings(Item i) {
		if (i.shifts != null) {
			for (Transition t : i.shifts) {				
				q.add(t);
				traverseTerminalStrings(t.target);
				q.pop();
			}
		} else {
			Symbol[] str = new Symbol[q.size()];
			for (int j = 0; j < q.size(); ++j) {
				str[j] = q.get(j).label;
			}
			
			// add it to minimalStrings
			for (Transition t : i.reduces) {
				for (Transition shift: t.shifts) {
					Symbol[] prev = minimalStrings.get(shift);
					if (prev == null || prev.length > str.length) {
						minimalStrings.put(shift, str);
					}
				}
			}
		}
	}
	
	private class StackNode {
		Symbol[] tstr; // of shift to i
		StackNode prev;
		int len = 0;
		
		public StackNode(Symbol[] tstr, StackNode prev) {
			this.prev = prev;			
			this.tstr = tstr;
			
			if (tstr != null) {
				len = tstr.length;
				if (prev != null) {
					len += prev.len;
				}
			}
		}
	}
}
