package nl.cwi.sen1.AmbiDexter.automata;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolSet;

public class SLR1NFA extends LR0NFA {

	public SLR1NFA(Grammar g, AmbiDexterConfig config) {
		super(g);
	}

	public void createItems() {
		items = createItems(grammar,
				new IItemFactory() {
					public Item createItem(Production p, int index, int id) {
						return new SLR1Item(p, index, id);
					}
				});
	}

	@Override
	public void buildNFA() {
		
		// create LR0 automaton
		super.buildNFA();
		
		// add lookahead
		for (Item s : items) {
			if (s.canReduce()) {
				SLR1Item i = (SLR1Item) s;
				i.lookahead = grammar.follow.get(i.production.lhs);
			}
		}
	}

	@Override
	public void verify() {
		super.verify();
		/*
		for (Item i : items) {
			if (i instanceof SLR1Item && i.canReduce()) {
				if (((SLR1Item) i).lookahead.size() == 0) {
					throw new RuntimeException("Empty lookahead for reduceable item: " + i);
				}
			}
		}*/
	}
	
	public class SLR1Item extends Item {

		public SymbolSet lookahead = null; 
		
		public SLR1Item(Production p, int index, int id) {
			super(p, index, id);			
		}

		@Override
		public boolean canReduceWith(Item i) {
			if (!super.canReduceWith(i)) {
				return false;
			}
					 	
			if (i.canReduce()) {
				return lookahead.intersects(((SLR1Item) i).lookahead);
			} else {
				Symbol s = i.getNextSymbol(); // endItem.getNextSymbol() returns $
				if (s instanceof NonTerminal) {
					if (AmbiDexterConfig.nonTerminalLookahead) {
						return lookahead.contains(s);
					} else {
						//return lookahead.intersects(Grammar.getInstance().emptyFreeFirst.get(s));
						return lookahead.intersects(Grammar.getInstance().emptyFreeFirst[s.id]);
					}
				} else if (s instanceof CharacterClass) {
					return lookahead.intersects((CharacterClass) s);
				} else {
					// terminal
					return lookahead.contains(s);
				}
			}
		}

		@Override
		public String toString() {
			return "[" + super.toString() + ", " + lookahead + "]";
		}
		
		@Override
		public Object clone() throws CloneNotSupportedException {
			SLR1Item i = (SLR1Item) super.clone();
			i.lookahead = lookahead;
			return i;
		}

		/*@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
					+ ((lookahead == null) ? 0 : lookahead.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof SLR1Item))
				return false;
			SLR1Item other = (SLR1Item) obj;
			if (lookahead == null) {
				if (other.lookahead != null)
					return false;
			} else if (!lookahead.equals(other.lookahead))
				return false;
			return true;
		}*/
		
		/*@Override
		public int compareTo(Item other) {
			int c = super.compareTo(other);
			
			if (c != 0) {
				return c;
			}

			SLR1Item i = (SLR1Item) other;
			
			if (lookahead == null) {
				if (i.lookahead == null) {
					return 0;
				} else {
					return -1;
				}
			}
			if (i.lookahead == null) {
				return 1;
			}
			if (lookahead.equals(i.lookahead)) {
				return 0;
			}
			if (lookahead.size() != i.lookahead.size()) {
				return lookahead.size() < i.lookahead.size() ? -1 : 1;
			}

			// compare contents of lookahead sets, by sorting them and concatenating them into a string
			return lookaheadToString().compareTo(i.lookaheadToString());
		}*/
	}
}
