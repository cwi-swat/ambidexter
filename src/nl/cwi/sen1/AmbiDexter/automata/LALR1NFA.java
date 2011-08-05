package nl.cwi.sen1.AmbiDexter.automata;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolSet;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

// XXX grammar 218 can be used to verify with dragon book 

public class LALR1NFA extends SLR1NFA {

	// map from set of LR0Items (kernel) to ItemSet containing LALR1Items
	Map<Set<Item>, ItemSet> kernels = new ShareableHashMap<Set<Item>, ItemSet>();

	public LALR1NFA(Grammar g) {
		super(g);
	}

	@Override
	public void buildNFA() {
		// efficient LALR1 parse automaton construction, according to the dragon
		// book by Aho, Sethi, Ullman

		// first create LR0 items
		((LR0NFA) this).createItems();

		// step 1: construct kernels

		kernels = new ShareableHashMap<Set<Item>, ItemSet>();

		ItemSet startSet = new ItemSet();
		startSet.items.add(startItem);
		for (Production p : grammar.startSymbol.productions) {
			Item i = new LALR1Item(p, 0, itemID++);
			startItem.derives.add(new Transition(startItem, p.derivation, i));
			startSet.kernel.add(i);
		}
		startSet.closure();
		startSet.kernel.clear();
		startSet.kernel.add(startItem);
		kernels.put(startSet.kernel, startSet);

		ESet<ItemSet> todo = new ShareableHashSet<ItemSet>(); // TODO make Queue
		todo.add(startSet);

		// gather LALR1 item sets
		while (todo.size() > 0) {
			ItemSet s = todo.removeOne();

			Relation<Symbol, Item> shifts = s.shifts();
			if (shifts.size() > 0) {
				for (Entry<Symbol, Set<Item>> e : shifts.m) {

					Set<Item> toKernel = e.getValue();
					ItemSet to = kernels.get(toKernel);
					if (to == null) {
						to = new ItemSet(toKernel);
						to.closure();
						todo.add(to);
						kernels.put(toKernel, to);
					}

					// add shift transitions
					for (Item i : s.items) {
						if (i.canShift() && i.production != null
								&& i.getNextSymbol() == e.getKey()) {
							for (Item i2 : to.kernel) {
								if (i2.production == i.production
										&& i2.index == i.index + 1) {
									i.shift = new Transition(i, e.getKey(), i2);
									break;
								}
							}
						}
					}
				}
			}
		}

		// fill items array
		items = new ShareableHashSet<Item>();
		for (ItemSet s : kernels.values()) {
			items.addAll(s.items);
		}

		// step 2: determine spontaneously generated lookaheads and propagation

		class DummySymbol extends Symbol {
			public DummySymbol() {
				super("__dummy__");
			}

			public boolean equals(Object o) {
				return this == o;
			}
		}
		;

		// TODO this does not work with CharacterClasses
		DummySymbol dummy = new DummySymbol();

		ShareableHashMap<Item, SymbolSet> lookaheads = new ShareableHashMap<Item, SymbolSet>();

		for (ItemSet s : kernels.values()) {
			for (Item k : s.kernel) {
				ShareableHashMap<Item, SymbolSet> c = closure(k, dummy);

				for (Entry<Item, SymbolSet> e : c) {
					if (e.getKey() instanceof LALR1Item) {
						Item i = e.getKey();
						SymbolSet l = e.getValue();

						if (i.production != null && i.canShift()
								&& i.shift.target instanceof LALR1Item) {
							LALR1Item lt = (LALR1Item) i.shift.target;

							if (l.contains(dummy)) {
								lt.propagatedFrom.add(k);
								l.remove(dummy);
							}

							if (l.size() > 0) {
								lt.spontaneously = true;

								SymbolSet lah = lookaheads.get(lt);
								if (lah == null) {
									lah = Grammar.newSymbolSet();
									lookaheads.put(lt, lah);
								}
								lah.addAll(l);
							}
						}
					}
				}
			}
		}

		// step 2.5: also enable normal propagation
		// not mentioned in dragon book !!!!!!

		for (ItemSet s : kernels.values()) {
			for (Item i : s.items) {
				if (i instanceof LALR1Item) { // also do this for nonreducable
												// items!!
					LALR1Item li = (LALR1Item) i;
					if (li.propagatedFrom.size() == 0 && !li.spontaneously) {
						for (Item j : s.items) {
							for (Transition d : j.derives) {
								if (d.target == li) {

									Item src = d.source;
									SymbolSet f;
									if (src instanceof LALR1Item) {
										f = grammar.first(src.production.rhs,
												src.index + 1);
										if (f.contains(Grammar.empty)) {
											f.remove(Grammar.empty);
											li.propagatedFrom.add(src);
										}
										if (f.size() > 0) {
											li.spontaneously = true;

											SymbolSet lah = lookaheads.get(li);
											if (lah == null) {
												lah = Grammar.newSymbolSet();
												lookaheads.put(li, lah);
											}
											lah.addAll(f);
										}
									} else {
										li.propagatedFrom.add(src);
									}
								}
							}
						}
					}
				}
			}
		}

		// step 3: set spontaneously generated lookaheads

		for (Item i : items) {
			if (i instanceof LALR1Item) {
				((LALR1Item) i).lookahead = Grammar.newSymbolSet();
			}
		}

		for (Entry<Item, SymbolSet> e : lookaheads) {
			if (e.getKey() instanceof LALR1Item) {
				((LALR1Item) e.getKey()).lookahead.addAll(e.getValue());
			}
		}

		// step 4: propagate lookaheads

		int newSize = 0;
		int oldSize = 0;
		do {
			oldSize = newSize;
			newSize = 0;

			for (Item i : items) {
				if (i instanceof LALR1Item) {
					LALR1Item li = (LALR1Item) i;
					for (Item p : li.propagatedFrom) {
						if (p instanceof LALR1Item) {
							LALR1Item lp = (LALR1Item) p;
							li.lookahead.addAll(lp.lookahead);
						} else {
							if (p == startItem) {
								li.lookahead.add(Grammar.endmarker);
							}
						}
					}
					newSize += li.lookahead.size();
				}
			}
		} while (newSize != oldSize);

		// round up

		for (Item i : items) {
			for (Transition t : i.derives) {
				transitions.add(t);
			}
			if (i.shift != null) {
				transitions.add(i.shift);
			}
		}
		startItem.shift = addTransition(startItem, grammar.startSymbol, endItem);

		items.remove(startItem);
		items.remove(endItem);

		// done
		// print statistics
		System.out.println("Item sets: " + kernels.values().size());
		if (Main.verbose) {
			printItemSets();

			int ls = 0;
			for (Item i : items) {
				if (i instanceof LALR1Item) {
					ls += ((LALR1Item) i).lookahead.size();
				}
			}
			System.out.println("Total lookahead: " + ls + " ("
					+ lookaheads.size() + ")");
		}
	}

	public void printItemSets() {
		System.out.println("Item sets: " + kernels.size());
		for (ItemSet s : kernels.values()) {
			System.out.println(s);
		}
	}

	private ShareableHashMap<Item, SymbolSet> closure(Item item, Symbol l) {
		ShareableHashMap<Item, SymbolSet> result = new ShareableHashMap<Item, SymbolSet>();

		Queue<Item> todo = new Queue<Item>();
		todo.add(item);
		SymbolSet initL = Grammar.newSymbolSet();
		initL.add(l);
		result.put(item, initL);

		while (todo.size() > 0) {
			Item i = todo.pop();

			if (i.derives.size() == 0) {
				continue;
			}

			SymbolSet f;
			if (i.canShift() && i.production != null
					&& i.getNextSymbol() instanceof NonTerminal) {
				f = grammar.first(i.production.rhs, i.index + 1);
				if (f.contains(Grammar.empty)) {
					f.remove(Grammar.empty);
					f.addAll(result.get(i));
				}
			} else {
				f = result.get(i);
			}

			for (Transition d : i.derives) {
				Item j = d.target;

				SymbolSet s = result.get(j);
				if (s == null) {
					s = Grammar.newSymbolSet();
					result.put(j, s);
				}

				int oldSize = s.size();
				s.addAll(f);
				if (s.size() > oldSize) {
					todo.add(j);
				}
			}
		}

		return result;
	}

	/* inner classes */

	private class ItemSet {
		Set<Item> items = new ShareableHashSet<Item>(); // LALR1Items
		Set<Item> kernel = new ShareableHashSet<Item>(); // LALR1Items

		public ItemSet(Set<Item> k) {
			for (Item i : k) {
				kernel.add(new LALR1Item(i.production, i.index, itemID++));
			}
		}

		public ItemSet() {
			// kernel is filled later
		}

		public void closure() {
			Queue<Item> todo = new Queue<Item>();
			Map<Pair<Production, Integer>, Item> canonicalItems = new ShareableHashMap<Pair<Production, Integer>, Item>();
			for (Item i : kernel) {
				todo.add(i);
				items.add(i);
				canonicalItems.put(i.getCanonicalItem(), i);
			}

			// calculate closure and create derive transitions
			while (todo.size() > 0) {
				Item i = todo.pop();

				if (i.canShift() && i.production != null
						&& i.getNextSymbol() instanceof NonTerminal) {
					NonTerminal n = (NonTerminal) i.getNextSymbol();

					for (Production p : n.productions) {
						if (includeRejects || !p.reject) {
							Pair<Production, Integer> pair = new Pair<Production, Integer>(
									p, 0);
							Item c = canonicalItems.get(pair);
							if (c == null) {
								c = new LALR1Item(p, 0, itemID++);
								items.add(c);
								todo.add(c);
								canonicalItems.put(pair, c);
							}

							if (i.canDeriveTo(c)) {
								i.derives
										.add(new Transition(i, p.derivation, c));
							}
						}
					}
				}
			}
		}

		public Relation<Symbol, Item> shifts() {
			Relation<Symbol, Item> result = new Relation<Symbol, Item>();

			for (Item i : items) {
				if (i.canShift() && i.production != null) {
					Symbol s = i.getNextSymbol();
					result.add(s, getItem(i.production, i.index + 1));
				}
			}

			return result;
		}

		@Override
		public String toString() {
			String s = "{\n";
			for (Item i : items) {
				s += (kernel.contains(i) ? "K " : "  ");
				s += i + "\n";
			}
			s += "}";
			return s;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((kernel == null) ? 0 : kernel.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ItemSet))
				return false;
			ItemSet other = (ItemSet) obj;
			if (kernel == null) {
				if (other.kernel != null)
					return false;
			} else if (!kernel.equals(other.kernel))
				return false;
			return true;
		}
	}

	public class LALR1Item extends SLR1Item {
		boolean spontaneously = false;
		Set<Item> propagatedFrom = new ShareableHashSet<Item>();

		public LALR1Item(Production production, int index, int itemId) {
			super(production, index, itemId);
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			LALR1Item clone = (LALR1Item) super.clone();
			return clone;
		}
	}
}
