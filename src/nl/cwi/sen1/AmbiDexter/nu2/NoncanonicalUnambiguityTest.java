package nl.cwi.sen1.AmbiDexter.nu2;

import java.util.Set;

import nl.cwi.sen1.AmbiDexter.AmbiguityDetector;
import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class NoncanonicalUnambiguityTest implements AmbiguityDetector {

	public Set<Production> harmlessProductions = null;
	public Set<Production> harmfulProductions = null;
	public Relation<Pair<Production, Integer>, Production> harmlessPatterns = null;
	
	NFA nfa;
	
	public void build(NFA nfa) {
		this.nfa = nfa;
	}

	public void detectAmbiguities(DetectionMethod method) {		
		IPairGraph pg = createPairGraph(method);			
		pg.init(nfa);
		pg.detectAmbiguities();

		if (!pg.potentiallyAmbiguous()) {
			if (Main.findHarmlessProductions) {
				harmlessProductions = nfa.grammar.productions;
				harmfulProductions = new ShareableHashSet<Production>();
				harmlessPatterns = new Relation<Pair<Production,Integer>, Production>();
			}
		} else {
			if (Main.findHarmlessProductions) {
				Set<Production> usedProductions = pg.getUsedProductions();
				System.out.println("Used productions: " + usedProductions.size() + " / " + nfa.grammar.nrReachableProductions);
				
				harmlessProductions = new ShareableHashSet<Production>();
				for (Production p : nfa.grammar.productions) {
					if (p.reachable && !p.reconstructed && !usedProductions.contains(p)) {
						if (!p.lhs.s.startsWith("[")) { // don't include character classes TODO hack
							harmlessProductions.add(p);
						}
					}
				}
				harmfulProductions = new ShareableHashSet<Production>();
				harmfulProductions.addAll(nfa.grammar.productions);
				harmfulProductions.removeAll(harmlessProductions);
				
				// includes only reachable productions (b/c of Grammar.getItemDerives())
				harmlessPatterns = pg.getHarmlessPatterns(usedProductions);
			}
		}		
	}
	
	private IPairGraph createPairGraph(DetectionMethod method) {
		PairGraph pg;
		if (Main.outputGraphs) {
			pg = new DotPairGraph();
		} else if (Main.filterUnmatchedDerivesReduces) {
			pg = new DepthFirstTransitionPairGraph();
		} else {
			pg = new DepthFirstPairGraph();
		}
		
		if (nfa.grammar.scannerless && Main.doRejects) {
			pg.addExtension(new ScannerlessPGE());
		}
		
		if (Main.alternating) {
			pg.addExtension(new AlternatingEmptyPGE());
		}
		
		return pg;
	}

	public void optimize() {
	}

	public void verify() {
	}
}
