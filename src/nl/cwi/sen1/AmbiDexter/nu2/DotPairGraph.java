package nl.cwi.sen1.AmbiDexter.nu2;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.Reduce;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Util;

public class DotPairGraph extends DepthFirstPairGraph {
	protected Set<PairTransition> transitions;
	protected Set<PairTransition> potentialAmbiguities;
	
	public DotPairGraph() {		
	}

	@Override
	protected boolean traverse() {
		transitions = new ShareableHashSet<PairTransition>();
		potentialAmbiguities = new ShareableHashSet<PairTransition>();
		return super.traverse();		
	}
	
	@Override
	protected void addTransition(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		if (to != null) {
			final PairTransition pt = new PairTransition(from, t1, t2, to);
			transitions.add(pt);
			if (to.equalItems() && !from.equalItems() && 
					((t1 != null && t1.getType() == Reduce.class) || 
					 (t2 != null && t2.getType() == Reduce.class))) {
				potentialAmbiguities.add(pt);
			}
			super.addTransition(from, t1, t2, to);
		}
	}

	@Override
	public void toDot(String filename) {
		monitor.println("Writing " + filename);
		BufferedWriter w = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			w = new BufferedWriter(fstream);
		
			w.write("digraph G {\n");
			
			// nodes
			for (ItemPair p : done) {
				String id = Util.dotId(toString(p));
				String s = id + "[label=" + id;
				if (p.isStartOrEndPair()) {
					s += ", style=filled,fillcolor=orange";
				}
				s += "];\n";
				w.write(s);
			}
			
			// edges
			for (PairTransition t : transitions) {
				String dot = Util.dotId(toString(t.source)) + " -> " + Util.dotId(toString(t.target)) + " [";
				dot += "label=" + Util.dotId(t);
				if (potentialAmbiguities.contains(t)) {
					dot += ",color=red";
				}
				dot += "];\n";
				w.write(dot);
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
}
