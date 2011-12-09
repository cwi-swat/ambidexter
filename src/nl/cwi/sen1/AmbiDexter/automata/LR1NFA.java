package nl.cwi.sen1.AmbiDexter.automata;

import java.util.Map;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

// TODO integrate empty skips

public class LR1NFA extends LR0NFA {

	// map from <LR0Item, lookahead> to LR1Item
	Map<Pair<Item, Symbol>, Item> lr1items = new ShareableHashMap<Pair<Item, Symbol>, Item>();

	// all lr1sets
	Set<Set<LR1Item>> lr1sets = new ShareableHashSet<Set<LR1Item>>();
	
	// transitions between sets
	Map<Pair<Set<LR1Item>, Symbol>, Set<LR1Item>> setTrans = new ShareableHashMap<Pair<Set<LR1Item>, Symbol>, Set<LR1Item>>();

	Set<LR1Item> startSet;
	
	public LR1NFA(Grammar g, AmbiDexterConfig config) {
		super(g);
	}

	/*public void createItems() {
		items = createItems(grammar,
				new ItemFactory() {
					Item createItem(Production p, int index, int id) {
						return new LR1Item(p, index, id);
					}
				});
	}*/
	
	LR1Item getItem(/*LR0*/Item i, Symbol lookahead) {
		
		if (lookahead == null) {
			throw new RuntimeException("null lookahead");
		}
		
		Pair<Item, Symbol> p = new Pair<Item, Symbol>(i, lookahead);
		LR1Item lri = (LR1Item) lr1items.get(p);
		
		if (lri == null) {
			lri = new LR1Item(i, lookahead, itemID++);
			lr1items.put(p, lri);
		}
		
		return lri;
	}

	@Override
	public void buildNFA() {		
		createLR1Automaton();
		items = (Set<Item>) lr1items.values();
		
		// remove LR0 items
		prodItems = null;
	}
	
	// TODO with characterclasses see if we can use ranges as lookahead
	
	public void createLR1Automaton() {
		// create LR0 items
		createItems();
		
		itemID = 2; // reset
		
		Set<Item> todo = new ShareableHashSet<Item>();
		Set<Item> done = new ShareableHashSet<Item>();

		startItem.shift = addTransition(startItem, grammar.startSymbol, endItem);		
		
		for (Production p : grammar.startSymbol.productions) {
			// StartItem -> ...
			LR1Item i = getItem(getItem(p, 0), Grammar.endmarker);
			startItem.derives.add(addTransition(startItem, p.derivation, i));
			todo.add(i);
		}

		done.add(startItem);
		done.add(endItem);

		// first add shifts and derives to create all lr1 items
		while (todo.size() > 0) {
			LR1Item i = (LR1Item) todo.iterator().next();
			todo.remove(i);
			done.add(i);
			
			if (i.canShift()) {
				Symbol s = i.getNextSymbol();
				{
					// shift
					LR1Item n = getItem(getItem(i.production, i.index + 1), i.lookahead);
					i.shift = addTransition(i, s, n);
					if (!done.contains(n)) {
						todo.add(n);
					}
				}
				
				if (s instanceof NonTerminal) {
					// derives
					Set<Symbol> f = grammar.first(i.production.rhs, i.index + 1);
					
					for (Production p : ((NonTerminal)s).productions) {
						if (includeRejects || !p.reject) {
							if (i.canDeriveTo(getItem(p, 0))) { // TODO extend this condition! (see LR0NFA.buildNFA())
								for (Symbol t : f) {
									LR1Item n;
									if (t == Grammar.empty) {
										n = getItem(getItem(p, 0), i.lookahead);
									} else {
										n = getItem(getItem(p, 0), t);
									}
									i.derives.add(addTransition(i, p.derivation, n));
									if (!done.contains(n)) {
										todo.add(n);
									}
								}
							}
						}
					}
				}
			}
		}
	}	
	
	public class LR1Item extends Item {

		public Symbol lookahead;
		
		public LR1Item(Item i, Symbol lookahead, int id) {
			super(i.production, i.index, id);
			this.lookahead = lookahead;
		}
		
		@Override
		public String toString() {
			return "[" + super.toString() + ", " + lookahead + "]";
		}
		
		@Override
		public boolean canReduceWith(Item i) {
			if (!super.canReduceWith(i)) {
				return false;
			}
					 	
			if (i.canReduce()) {
			 	return lookahead == ((LR1Item) i).lookahead;
			} else {
				Symbol s = i.getNextSymbol(); // endItem.getNextSymbol() returns $
				if (s instanceof NonTerminal) {
					if (AmbiDexterConfig.nonTerminalLookahead) {
						return lookahead == s;
					} else {
						return Grammar.getInstance().emptyFreeFirst[s.id].contains(lookahead);						
					}
				} else { // Terminal
					return lookahead == s;
				}
			}
		}

		/*@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((lookahead == null) ? 0 : lookahead.hashCode());
			return result;
		}*/

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof LR1Item))
				return false;
			LR1Item other = (LR1Item) obj;
			if (lookahead == null) {
				if (other.lookahead != null)
					return false;
			} else if (!lookahead.equals(other.lookahead))
				return false;
			return true;
		}
		
		@Override
		public int compareTo(Item other) {
			int c = super.compareTo(other);
			if (c == 0) {
				LR1Item i = (LR1Item) other;
				
				return lookahead.s.compareTo(i.lookahead.s);
			} else {
				return c;
			}
		}
	}
}
