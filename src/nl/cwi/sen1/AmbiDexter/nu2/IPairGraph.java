package nl.cwi.sen1.AmbiDexter.nu2;

import java.util.Set;

import nl.cwi.sen1.AmbiDexter.IAmbiDexterMonitor;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Relation;

public interface IPairGraph {

	boolean potentiallyAmbiguous();

	void init(NFA nfa, IAmbiDexterMonitor monitor);

	void detectAmbiguities();

	Set<Production> getUsedProductions();

	Relation<Pair<Production, Integer>, Production> getHarmlessPatterns(Set<Production> usedProductions);
}
