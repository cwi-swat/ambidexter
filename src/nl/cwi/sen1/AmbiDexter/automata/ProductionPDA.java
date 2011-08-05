package nl.cwi.sen1.AmbiDexter.automata;

import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class ProductionPDA extends PDA<Production> {
		
	public ProductionPDA() {
		super();
	}
	
	@Override
	protected void build(Item startItem, Item endItem) {
		Set<Item> startSet = new ShareableHashSet<Item>();
		startSet.add(startItem);
		startState = getState(startSet);
		
		this.endItem = endItem;
		
		ShareableHashSet<PDAState> todo = new ShareableHashSet<PDAState>();
		ShareableHashSet<PDAState> done = new ShareableHashSet<PDAState>();
		todo.add(startState);
		
		while (todo.size() > 0) {
			PDAState s = todo.removeOne();
			done.add(s);
			
			// calculate reduces
			for (Item i : s.items) {
				if (i.production != null && i.canReduce()) {
					s.reductions.add(i.production);	
				}
			}
			
			// collect shiftable symbols
			Relation<Symbol, Item> shifts = new Relation<Symbol, Item>();
			Relation<Production, Item> gotos = new Relation<Production, Item>();
			for (Item i : s.items) {
				if (i != endItem && i.canShift()) {
					if (i.getNextSymbol() instanceof NonTerminal) {
						for (Transition t : i.derives) {
							gotos.add(t.target.production, i.getNext());
						}
					} else {
						shifts.add(i.getNextSymbol(), i.getNext());
					}
				}
			}
			
			// fill actual shifts
			Iterator<Entry<Symbol, Set<Item>>> sit = shifts.entryIterator();
			while (sit.hasNext()) {
				Entry<Symbol, Set<Item>> e = sit.next();
				Symbol a = e.getKey();
				PDAState nextState = getState(e.getValue());
				nextState.incoming = a;

				s.shifts.put(a, nextState);
				
				boolean onlyForReject = true;
				for (Item i : nextState.items) {
					if (i.production == null || !i.production.usedForReject) { // reachable and usedForReject are separated
						onlyForReject = false;
						break;
					}
				}
				if (!onlyForReject) {
					s.shiftables.add(a);
				}
				
				if (!todo.contains(nextState) && !done.contains(nextState)) {
					todo.add(nextState);
					PDAState rejectState = nextState.rejectState;
					if (rejectState != null && !todo.contains(rejectState) && !done.contains(rejectState)) {
						todo.add(rejectState);
					}
				}
			}
			
			// fill actual gotos
			Iterator<Entry<Production, Set<Item>>> git = gotos.entryIterator();
			while (git.hasNext()) {
				Entry<Production, Set<Item>> e = git.next();
				Production p = e.getKey();
				PDAState nextState = getState(e.getValue());
				nextState.incoming = p.lhs;
				
				s.gotos.put(p, nextState);
				
				if (!todo.contains(nextState) && !done.contains(nextState)) {
					todo.add(nextState);
					PDAState rejectState = nextState.rejectState;
					if (rejectState != null && !todo.contains(rejectState) && !done.contains(rejectState)) {
						todo.add(rejectState);
					}
				}
			}
		}
	}

	@Override
	protected Production deserializeR(String s) {
		return prodMap.get(s);
	}

	@Override
	protected String serializeR(Production r) {
		return r.toString();
	}
}
