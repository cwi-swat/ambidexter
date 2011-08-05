package nl.cwi.sen1.AmbiDexter.automata;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.automata.NFA.EndItem;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.StartItem;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Util;

// Pushdown automaton

public abstract class PDA <R> {

	public NFA nfa;
	public ShareableHashMap<Set<Item>, PDAState> stateMap = new ShareableHashMap<Set<Item>, PDAState>();
	public ShareableHashSet<PDAState> states = new ShareableHashSet<PDAState>();
	public PDAState startState;
	public Item endItem;
	public static int stateIDs;
	
	protected Map<Integer, Item> itemMap;
	protected Map<String, Production> prodMap;

	
	public PDA() {}
	
	// make a DFA from an NFA
	public void build(NFA nfa) {
		this.nfa = nfa;
		build(nfa.startItem, nfa.endItem);
	}

	public void build(NFA nfa, Item startItem, Item endItem) {
		this.nfa = nfa;
		
		// clone start and endItem
		Item newStart = nfa.getDummyState(-1);
		Item newEnd = nfa.getDummyState(-2);
		
		newStart.derives = startItem.derives;
		newStart.shift = new Transition(newStart, startItem.getNextSymbol(), newEnd);
		// reduces not needed luckily
		
		build(newStart, newEnd);		
	}
	
	protected abstract void build(Item startItem, Item endItem);

	protected PDAState getState(Set<Item> items) {
		PDAState s = stateMap.get(items);
		if (s == null) {
			Set<Item> closure = new ShareableHashSet<Item>();
			closure.addAll(items);
			boolean containsEndState = false;
			
			for (Item i : items) {
				closure.addAll(nfa.getClosure(i));
				if (i == endItem) {
					containsEndState = true;
				}
			}
			
			// split states used for reject
			PDAState r = null;
			if (Main.doRejects) {
				Set<Item> rejectItems = new ShareableHashSet<Item>();
				for (Item i : closure) {
					if (i.production != null && i.production.usedForReject) {
						rejectItems.add(i);
					}
				}
				
				if (rejectItems.size() > 0) {
					closure.removeAll(rejectItems);
					r = stateMap.get(rejectItems);
					if (r == null) {
						r = new PDAState(rejectItems);
						r.rejects = true;
						r.finish();
						stateMap.put(rejectItems, r);
						states.add(r);
					}
				}
			}
			
			if (closure.size() > 0) {
				s = new PDAState(closure);
				s.isEndState = containsEndState;
				s.rejectState = r;
				s.finish();
				stateMap.put(items, s);
				states.add(s);
			} else {
				s = r;
			}
		}
		return s;
	}

	public void printSize(String prefix) {
		int shifts = 0, gotos = 0;
		for (PDAState s : states) {
			shifts += s.shifts.size();
			gotos += s.gotos.size();
		}
		System.out.println(prefix + " size: " + states.size() + " nodes, " + shifts + " shifts, " + gotos + " gotos");
	}
	
	protected abstract String serializeR(R r);
	protected abstract R deserializeR(String s);
	
	public void serialize(String filename) {
		System.out.println("Writing " + filename);
		BufferedWriter w = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			w = new BufferedWriter(fstream);

			Set<Item> allItems = nfa.allItems();
			
			// reconstructed productions
			Set<Production> reconstructed = new ShareableHashSet<Production>();
			for (Production p : nfa.grammar.productions) {
				if (p.reconstructed) {
					reconstructed.add(p);
				}
			}
			
			w.write("" + reconstructed.size() + "\n");
			for (Production p : reconstructed) {
				p.serialize(w);
			}
			
			// NFA items
			w.write("" + allItems.size() + "\n");
			for (Item i : allItems) {
				i.serialize(w);
			}

			// DFA states
			w.write("" + states.size() + "\n");
			for (PDAState s : states) {
				s.serialize(w);
			}
			
			// DFA edges
			for (PDAState s : states) {
				w.write(s.id + "\n");
				s.serializeTransitions(w);
			}
			
			// DFA startState and endItem
			w.write(startState.id + "\n");
			w.write(endItem.id + "\n");
			
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) { }
			}
		}
	}
	
	public void deserialize(String filename, Grammar g) {
		Map<Integer, PDAState> states = new ShareableHashMap<Integer, PDAState>();
		itemMap = new ShareableHashMap<Integer, Item>();
		prodMap = new ShareableHashMap<String, Production>();
		// XXX this might not always return the right production
		// for instance, it does not look at reachable and usedForReject flags
		// also, it might not find productions that were reconstructed between grammar loading and the call to this function
		// however, in the current setting, it always returns a production that is right enough
		for (Production p : g.productions) {
			prodMap.put(p.toString(), p);
		}
		
		String[] s = Util.readTextFileIntoArray(filename);
		int pos = 0;
		
		// reconstructed productions
		int nrProds = Integer.parseInt(s[pos++]);
		for (int i = 0; i < nrProds; i++) {
			Symbol lhs = g.getSymbol(s[pos++]);
			Production p = g.newProduction((NonTerminal) lhs);
			pos = p.deserialize(s, pos, g);
			g.addProduction(p);
			prodMap.put(p.toString(), p);
		}
		
		// NFA items
		int nrItems = Integer.parseInt(s[pos++]);
		for (int i = 0; i < nrItems; i++) {
			Item item;
			if (s[pos].equals("start")) {
				item = new StartItem(0, g.startSymbol);
				++pos;
			} else if (s[pos].equals("end")){
				item = new EndItem(0);
				++pos;
			} else {
				item = new Item(0);
			}
			pos = item.deserialize(s, pos, prodMap);
			itemMap.put(item.id, item);
		}

		// DFA items
		int nrStates = Integer.parseInt(s[pos++]);
		for (int i = 0; i < nrStates; i++) {
			PDAState state = new PDAState(null);
			pos = state.deserialize(s, pos, g, itemMap);
			states.put(state.id, state);
			this.states.add(state);
		}
		
		// DFA edges
		for (int i = 0; i < nrStates; i++) {
			int id = Integer.parseInt(s[pos++]);
			pos = states.get(id).deserializeTransitions(s, pos, itemMap, states);
		}
		
		// DFA startState and endItem
		startState = states.get(Integer.parseInt(s[pos++]));
		endItem = itemMap.get(Integer.parseInt(s[pos++]));
	}

	public void toDot(String filename) {
		System.out.println("Writing " + filename);
		BufferedWriter w = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			w = new BufferedWriter(fstream);
		
			w.write("digraph G {\n");
			
			// nodes
			for (PDAState s : states) {
				String id = Util.dotId(s);
				if (id.length() > 200) {
					id = Util.dotId(s.id);
				}
				w.write("" + s.id + " [label=" + id + (s.rejects ? ", color=red" : "") + ", shape=box];\n");
	
				// shift edges
				for (Entry<Symbol, PDAState> e2 : s.shifts.entrySet()) {
					w.write("" + s.id + " -> " + e2.getValue().id + " [label=" + Util.dotId(e2.getKey()) + "];\n");
				}
				
				// goto edges
				for (Entry<R, PDAState> e2 : s.gotos.entrySet()) {
					w.write("" + s.id + " -> " + e2.getValue().id + " [label=" + Util.dotId(e2.getKey()) + "];\n");
				}
				
				if (s.rejectState != null) {
					w.write("" + s.id + " -> " + s.rejectState.id + " [color=red];\n");
				}
			}
						
			w.write("}\n");
			
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) { }
			}
		}
	}

	/* inner classes */
	
	public class PDAState {
		Set<Item> items;
		Queue<NonTerminal> derivedFrom = new Queue<NonTerminal>(8);
		public Symbol incoming = null; // all incoming edges always have the same symbol
		public ShareableHashMap<Symbol, PDAState> shifts = new ShareableHashMap<Symbol, PDAState>();
		//public Queue<Pair<Integer, NonTerminal>> reduces = new Queue<Pair<Integer,NonTerminal>>(8);
		int id = stateIDs++;
		public Set<Symbol> shiftables = new ShareableHashSet<Symbol>(); // only terminals
		public boolean isEndState = false;
		
		public ShareableHashMap<R, PDAState> gotos = new ShareableHashMap<R, PDAState>();
		public Queue<R> reductions = new Queue<R>(8);
		
		public PDAState rejectState;
		public boolean rejects = false; // true if contains items that are used in reject

		public PDAState(Set<Item> items) {
			this.items = items;
		}

		public void finish() {
			for (Item i : items) {
				if (i.production != null) {
					NonTerminal n = i.production.lhs;
					if (!derivedFrom.contains(n)) {
						derivedFrom.add(n);
					}
				}
			}
		}

		public boolean canReduce() {
			return gotos.size() > 0;
		}
		
		@Override
		public int hashCode() {
			return id;
		}
	
		@Override
		public String toString() {
			return items.toString();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof PDA.PDAState))
				return false;
			PDAState other = (PDAState) obj;
			if (items == null) {
				if (other.items != null)
					return false;
			} else if (!items.equals(other.items))
				return false;
			return true;
		}
		
		public void serialize(Writer w) throws IOException {
			w.write(id + "\n");
			w.write(isEndState + "\n");
			w.write(rejects + "\n");
			
			w.write(shiftables.size() + "\n");
			for (Symbol s : shiftables) {
				w.write(((CharacterClass) s).serialize() + "\n");
			}
			
			w.write(items.size() + "\n");
			for (Item i : items) {
				w.write(i.id + "\n");
			}
		}
		
		public int deserialize(String[] s, int pos, Grammar g, Map<Integer, Item> itemMap) {
			id = Integer.parseInt(s[pos++]);
			isEndState = Boolean.parseBoolean(s[pos++]);
			rejects = Boolean.parseBoolean(s[pos++]);
			
			shiftables = new ShareableHashSet<Symbol>();
			int size = Integer.parseInt(s[pos++]);
			for (int i = size - 1; i >= 0; --i) {
				shiftables.add(new CharacterClass(s[pos++]));
			}
			
			items = new ShareableHashSet<Item>();
			size = Integer.parseInt(s[pos++]);
			for (int i = size - 1; i >= 0; --i) {
				items.add(itemMap.get(Integer.parseInt(s[pos++])));
			}
			
			return pos;
		}

		public void serializeTransitions(Writer w) throws IOException {
			w.write(shifts.size() + "\n");
			for (Entry<Symbol, PDAState> e : shifts) {
				w.write(((CharacterClass) e.getKey()).serialize() + "\n");
				w.write(e.getValue().id + "\n");
			}
			
			w.write(gotos.size() + "\n");
			for (Entry<R, PDAState> e : gotos) {
				w.write(serializeR(e.getKey()) + "\n");
				w.write(e.getValue().id + "\n");
			}
			
			w.write(reductions.size() + "\n");
			for (R r : reductions) {
				w.write(serializeR(r) + "\n");
			}
			
			if (rejectState == null) {
				w.write("\n");
			} else {
				w.write(rejectState.id + "\n");	
			}
		}

		public int deserializeTransitions(String[] s, int pos, Map<Integer, Item> items, Map<Integer, PDAState> states) {
			int nr = Integer.parseInt(s[pos++]);
			for (int i = 0; i < nr; ++i) {
				CharacterClass c = new CharacterClass(s[pos++]);
				PDAState state = states.get(Integer.parseInt(s[pos++]));
				if (shifts.put(c, state) != null) {
					System.out.println("state " + id);
					System.out.println("Duplicate shift " + c + " " + state.id);
				}
			}
			
			nr = Integer.parseInt(s[pos++]);
			for (int i = 0; i < nr; ++i) {
				R r = deserializeR(s[pos++]);
				PDAState state = states.get(Integer.parseInt(s[pos++]));
				if (gotos.put(r, state) != null) {
					System.out.println("Duplicate goto " + r + " " + state.id);
				}
			}
			
			nr = Integer.parseInt(s[pos++]);
			for (int i = 0; i < nr; ++i) {
				R r = deserializeR(s[pos++]);
				reductions.add(r);
			}
			
			String r = s[pos++];
			if (!r.equals("")) {
				rejectState = states.get(Integer.parseInt(r));
			}
			
			return pos;			
		}
	}

}
