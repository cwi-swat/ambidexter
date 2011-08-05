package nl.cwi.sen1.AmbiDexter.automata;

import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;

public class LR0NFA extends NFA {

	public LR0NFA(Grammar g){
		super(g);
	}
	
	public void createItems() {
		items = createItems(grammar,
				new IItemFactory() {
					public Item createItem(Production p, int index, int id) {
						return new Item(p, index, id);
					}
				});
	}

	@Override
	public void buildNFA() {
		createItems();
		
		startItem.shift = addTransition(startItem, grammar.startSymbol, endItem);
		
		for (Production p : grammar.startSymbol.productions) {
			// startItem -> ...
			startItem.derives.add(addTransition(startItem, p.derivation, getItem(p, 0)));
			
			// ... -> endItem
			//Item last = p.getLastItem();
			//last.reduces.add(addTransition(last, p.reduction, endItem));
		}
		
		for (Item i : items) {
			Symbol s = i.getNextSymbol();
			if (s != null) {
				// Terminal or Nonterminal: shift					
				i.shift = addTransition(i, s, getItem(i.production, i.index + 1));
			}
		}
		
		for (Item i : items) {
			if (i.shift != null) {
				Symbol s = i.shift.label;
				
				if (s instanceof NonTerminal) {
					// Nonterminal: derive
					for (Item next : items) {
						/*
						if (i.canDeriveTo(next)
								// keep separate sections for normal and rejected part
								// only add transitions between them for reject productions
								&& (!includeRejects || next.production == null || next.production.reject || i.production == null || i.production.usedForReject == next.production.usedForReject)) {
						/*/
						
						if (i.canDeriveTo(next)	&& (next.production == null ||
							(i.production == null && next.production.reachable) ||
							// keep separate sections for normal and rejected part
							// only add transitions between them for reject productions
							(i.production != null && i.production.usedForReject == next.production.usedForReject) ||
							(i.production != null && i.production.reachable == next.production.reachable) ||
							(includeRejects && next.production.reject)))
						{
						//*/
							i.derives.add(addTransition(i, next.production.derivation, next));
						}
					}
				}
			}
		}
	}
}
