package nl.cwi.sen1.AmbiDexter.nu2;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.imageio.ImageIO;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.IAmbiDexterMonitor;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.StartItem;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.Derive;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Reduce;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public abstract class PairGraph implements IPairGraph {

	protected NFA nfa;
	IItemPairSet done; // all pairs
	Set<ItemPair> startPairs;
	Set<ItemPair> endPairs;
	protected static int progressInterval = 100000;
	protected long nrTransitions = 0;
	//protected long nrPotentialAmbiguities = 0;
	private Queue<PairGraphExtension> extensions = new Queue<PairGraphExtension>();
	protected long endPairItems;
	protected int iteration;
	protected IAmbiDexterMonitor monitor;

	public void init(NFA nfa, IAmbiDexterMonitor monitor) {
		this.nfa = nfa;
		this.monitor = monitor;
		
		nfa.nrTransitionsAndItems();
		ItemPair.initItemMasks(NFA.IDedItems);
		monitor.println("Item bits: " + ItemPair.ITEM_BITS);
		
		endPairItems = nfa.endItem.ID | (nfa.endItem.ID << ItemPair.ITEM_BITS);
		
		for (int i = extensions.size() - 1; i >= 0; --i) {
			extensions.get(i).init(this);
		}
	}
	
	public void addExtension(PairGraphExtension e) {
		extensions.add(e);
	}

	public boolean detectAmbiguities() {
		iteration = 0;
		int maxpairs = 0;
		int prevSize;
		int size = nfa.items.size() + 2 + nfa.transitions.size();
		
		do {
			++iteration;
			
			monitor.println("\nBuilding pair graph:");
			if (!traverse()) {
				// aborted by user
				return false;
			}			
			monitor.println(done.usageStatistics());

			if (maxpairs == 0 ) {
				maxpairs = done.size();
			}
	
			if (AmbiDexterConfig.outputGraphs) {
				toDot(nfa.grammar.name + ".pp" + iteration + ".dot");
			}
			if (AmbiDexterConfig.outputDistMap) {
				computeDistanceMap(nfa.grammar.name + ".distmap" + iteration + ".png");
				//computeUsageMap(nfa.grammar.name + ".pairmap" + iterations + ".png");
			}
			
			filter();
			nfa.printSize("\nNFA " + iteration, monitor);
			monitor.println("Used productions: " + nfa.getUsedProductions().size());
			
			if (AmbiDexterConfig.outputGraphs) {
				nfa.toDot(nfa.grammar.name + ".nfa" + iteration + ".dot");
			}
			
			prevSize = size;
			size = nfa.items.size() + 2 + nfa.transitions.size();
		} while (size > 3 && size != prevSize); // 3 are always used: startstate, endstate, and shift between the two
		
		monitor.println("\nIterations: " + iteration + ", max pairs: " + maxpairs);
		return true;
	}
	
	protected abstract boolean traverse();
	protected abstract Set<Item> getUsedItems();
	protected abstract void filter();
	public abstract Set<Production> getUsedProductions();

	protected void buildStartAndEndPairs() {
		startPairs = new ShareableHashSet<ItemPair>();
		endPairs = new ShareableHashSet<ItemPair>();
		
		ItemPair startPair = getPair(nfa.startItem, nfa.startItem, false, false);
		startPairs.add(startPair);
	
		ItemPair endPair = getPair(nfa.endItem, nfa.endItem, true, true);
		// TODO do we need to include endpair with flags 10, now that we allow unconflicted reduces with follow restrictions??
		endPairs.add(endPair);
	}

	protected ItemPair lookupPair(ItemPair pair) {
		return lookupPair(pair.items, pair.flags);
	}
	
	final protected ItemPair lookupPair(long items, long flags) {
		if (items == endPairItems) {
			// clear all flags
			flags &= (ItemPair.ALLOW_PAIRWISE_REDUCE_1 | ItemPair.ALLOW_PAIRWISE_REDUCE_2);
		}
		ItemPair p = done.getContained(items, flags); // TODO optimize into 1 call
		if (p == null) {
			p = done.unsafeAdd(items, flags);
		}
		return p;
	}

	protected ItemPair getPair(Item i1, Item i2, boolean allowPWR1, boolean allowPWR2) {
		return lookupPair(new ItemPair(i1, i2, allowPWR1, allowPWR2));
	}
	
	
	// XXX inlining these add...Transition functions has a negative effect on performance
	
	private void addDeriveTransition(ItemPair from, Transition t1, Transition t2) {
		ItemPair to;
		if (t1 != null) {
			ItemPair p = new ItemPair(t1.target, from.getB(), false, from.getAllowPairwiseReduce2());
			to = p;
		} else {
			ItemPair p = new ItemPair(from.getA(), t2.target, from.getAllowPairwiseReduce1(), false);
			to = p;
		}
		for (int i = extensions.size() - 1; i >= 0; --i) {
			if (!extensions.get(i).getPairAfterDerive(from, t1, t2, to)) {
				return;
			}
		}
		addTransition(from, t1, t2, to);
	}

	private ItemPair addShiftTransition(final ItemPair from, final Transition t1, final Transition t2) {
		ItemPair to = new ItemPair(t1.target, t2.target, from.getAllowPairwiseReduce1(), from.getAllowPairwiseReduce2());
		if (to.items != endPairItems) {
			for (int i = extensions.size() - 1; i >= 0; --i) {
				if (!extensions.get(i).getPairAfterShift(from, t1, t2, to)) {
					return null;
				}
			}
		}
		addTransition(from, t1, t2, to);
		return to;
	}
	
	private void addEmptyShiftTransition(ItemPair from, Transition t1, Transition t2, ItemPair notTo) {
		ItemPair to = new ItemPair(t1.target, t2.target, from.getAllowPairwiseReduce1(), from.getAllowPairwiseReduce2());
		if (to.items != endPairItems) {
			for (int i = extensions.size() - 1; i >= 0; --i) {
				if (!extensions.get(i).getPairAfterEmptyShift(from, t1, t2, to)) {
					return;
				}
			}
		}
		
		if (to != notTo) {
			addTransition(from, t1, t2, to);
		}		
	}

	private void addSingleReduceTransition(ItemPair from, Transition t1, Transition t2) {
		ItemPair to;
		if (t1 != null) {
			to = new ItemPair(t1.target, from.getB(), true, from.getAllowPairwiseReduce2());
		} else {
			to = new ItemPair(from.getA(), t2.target, from.getAllowPairwiseReduce1(), true);
		}
		
		for (int i = extensions.size() - 1; i >= 0; --i) {
			if (!extensions.get(i).getPairAfterSingleReduce(from, t1, t2, to)) {
				return;
			}
		}
		
		addTransition(from, t1, t2, to);
	}

	private void addPairwiseReduceTransition(ItemPair from, Transition t1, Transition t2) {
		ItemPair to = new ItemPair(t1.target, t2.target, true, true);
		for (int i = extensions.size() - 1; i >= 0; --i) {
			if (!extensions.get(i).getPairAfterPairwiseReduce(from, t1, t2, to)) {
				return;
			}
		}
		
		addTransition(from, t1, t2, to);
	}

	protected abstract void addTransition(ItemPair from, Transition t1, Transition t2, ItemPair to);

	protected void buildTransitions(ItemPair p) { // there seems to be very little performance difference between arrays or iterators 
		final Item a = p.getA();
		final Item b = p.getB();
		
		// TODO if we know a certain nonterminal is unambiguous, and we're at the beginning of it in identical items
		// then don't build any transitions. could save a lot of space, especially with layout unfolding...
		// something like this: (has very little effect though!! why?
//		if (a.production != null && a.production.lhs.usedInLayout && b.production != null && b.production.lhs.usedInLayout) {
//			return;
//		}
		// XXX Remember: this IS useful when the layout is found unambiguous with a higher precision!
				
		// shift
		if (a.shift != null && b.shift != null && a.shift.label.canShiftWith(b.shift.label)) {
			ItemPair p2 = addShiftTransition(p, a.shift, b.shift);
			Symbol s = a.shift.label;
			if (s instanceof NonTerminal && ((NonTerminal) s).directlyNullable) {
				addEmptyShiftTransition(p, a.shift, b.shift, p2);
			}
		}
		
		if (a.shifts != null && b.shifts != null) { // TODO optimization possible here
			for (Transition t1 : a.shifts) {
				for (Transition t2 : b.shifts) {
					if (t1.label.canShiftWith(t2.label) && t1.empty == t2.empty) {
						if (t1.empty) {
							addEmptyShiftTransition(p, t1, t2, null);
						} else {
							addShiftTransition(p, t1, t2);
						}
					}					
				}
			}
		}
		
		// derive of a
		for (Transition t : a.derives) {
			addDeriveTransition(p, t, null);
		}

		// derive of b
		for (Transition t : b.derives) {
			addDeriveTransition(p, null, t);
		}

		// don't reduce if the other path hasn't started yet! 
		if (a instanceof StartItem || b instanceof StartItem) {
			return;
		}
		
		// conflicting reduce of a
		if (a.canReduceWith(b)) {
			for (Transition t : a.reduces) {
				if (b.conflict(t.label)) {
					addSingleReduceTransition(p, t, null);
				}
			}
		}
		
		// conflicting reduce of b
		if (b.canReduceWith(a)) {
			for (Transition t : b.reduces) {
				if (a.conflict(t.label)) {
					addSingleReduceTransition(p, null, t);
				}
			}
		}
		
		// pairwise reduce if allowed
		if (p.inConflict() && a.canReduceWith(b) && b.canReduceWith(a)) {
			for (Transition t1 : a.reduces) {
				for (Transition t2 : b.reduces) {
					if (t1.label == t2.label) {
						addPairwiseReduceTransition(p, t1, t2);
					}
				}
			}
		}
	}

	public boolean potentiallyAmbiguous() {
		return nfa.items.size() > 0;
	}
	
	/*************************************************************************/
	
	public void printSize(String prefix) {
		prefix += " size: " + done.size() + " pairs, " + nrTransitions + " transitions";
		for (int i = extensions.size() - 1; i >= 0; --i) {
			prefix += extensions.get(i).printSize();
		}
		monitor.println(prefix);
	}

	public abstract void toDot(String filename);
	
	public String toString(ItemPair p) {
		String s = p.toString();
		for (int i = extensions.size() - 1; i >= 0; --i) {
			s += extensions.get(i).toString(p);
		}
		return s;
	}

	public void computeDistanceMap(String filename) {
		int map[][] = new int[nfa.maxDistanceToEnd + 1][nfa.maxDistanceToEnd + 1];
		int max = 0;
		
		for (ItemPair p : done) {
			Item a = p.getA();
			Item b = p.getB();
			int i = ++map[a.distanceToEnd][b.distanceToEnd]; 
			if (i > max) {
				max = i;
			}		
		}
		
		// 0 = black
		// 127 = gray
		// -128 = gray + 1
		// -1 = white
		
		/*BufferedImage bi = new BufferedImage(nfa.maxDistanceToEnd + 1, nfa.maxDistanceToEnd + 1, BufferedImage.TYPE_BYTE_GRAY);
		for (int i = nfa.maxDistanceToEnd; i >= 0; --i) {
			for (int j = nfa.maxDistanceToEnd; j >= 0; --j) {
//				int grey = (map[i][j] * 255) / max;
//				if (grey > 127) {
//					grey -= 256;
//				}
				int grey = 0;
				if (map[i][j] > 0) {
					grey = ((map[i][j] * 127) / max) - 128;
				}
					
				byte b[] = {(byte) grey};
				bi.getRaster().setDataElements(i, j, b);
			}
		}*/
		
		BufferedImage bi = new BufferedImage(nfa.maxDistanceToEnd + 1, nfa.maxDistanceToEnd + 1, BufferedImage.TYPE_INT_RGB);
		for (int i = nfa.maxDistanceToEnd; i >= 0; --i) {
			for (int j = nfa.maxDistanceToEnd; j >= 0; --j) {
				int a[] = {map[i][j]};
				bi.getRaster().setDataElements(i, j, a);
			}
		}
		
		monitor.println("Max distance to end: " + nfa.maxDistanceToEnd + ", max occuring: " + max);
		
		/*bi.setRGB(0, 0, 0);
		bi.setRGB(1, 0, 127);
		bi.setRGB(2, 0, -128);
		bi.setRGB(3, 0, 255);
		bi.setRGB(4, 0, 0xFFFFFFFF);
		byte[] b = {0};
		bi.getRaster().setDataElements(0, 1, b);
		b[0] = 127; bi.getRaster().setDataElements(1, 1, b);
		b[0] = 120; bi.getRaster().setDataElements(2, 1, b);
		b[0] = 110; bi.getRaster().setDataElements(3, 1, b);
		b[0] = 100; bi.getRaster().setDataElements(4, 1, b);
		b[0] = 90; bi.getRaster().setDataElements(5, 1, b);
		b[0] = 80; bi.getRaster().setDataElements(6, 1, b);
		b[0] = 70; bi.getRaster().setDataElements(7, 1, b);
		b[0] = -1; bi.getRaster().setDataElements(8, 1, b);*/
		
		try {
			ImageIO.write(bi, "png", new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void computeUsageMap(String filename) {
		int items = NFA.IDedItems;
		BufferedImage bi = new BufferedImage(items, items, BufferedImage.TYPE_INT_RGB);
		int a[] = {0};
		WritableRaster r = bi.getRaster();
		for (ItemPair p : done) {
			int i = p.getA().ID;
			int j = p.getB().ID;
			
			r.getDataElements(i, j, a);
			
			if (a[0] == 0) {
				a[0] = 64;
			}
			a[0] += 4;
			bi.getRaster().setDataElements(i, j, a);
		}
		try {
			ImageIO.write(bi, "png", new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class PairTransition {
		ItemPair source;
		ItemPair target;
		Transition t1;
		Transition t2;
		
		public PairTransition(ItemPair from, Transition t1, Transition t2, ItemPair to) {
			source = from;
			target = to;
			this.t1 = t1;
			this.t2 = t2;
		}
		
		public String toString() {			
			String s = "< ";
			s += (t1 == null ? "_" : t1.toString());
			s += " , ";
			s += (t2 == null ? "_" : t2.toString());
			return s + " >";
		}
		
		public String toStringExt() {
			String s = source.toString();
			s += " < ";
			s += (t1 == null ? "_" : t1.toString());
			s += " , ";
			s += (t2 == null ? "_" : t2.toString());
			s += " > ";
			s += target.toString();
			return s;
		}
		
		public Class<?> getType() {
			if (t1 != null) {
				return t1.getType();
			} else {
				return t2.getType();
			}
		}
		
		public boolean isShift() {
			return (!(getSingleLabel() instanceof Derive) && !(getSingleLabel() instanceof Reduce));
		}
		
		public Symbol getSingleLabel() {
			if (t1 != null) {
				return t1.label;
			} else {
				return t2.label;
			}
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result	+ ((target == null) ? 0 : target.hashCode());
			result = prime * result + ((t1 == null) ? 0 : t1.hashCode());
			result = prime * result + ((t2 == null) ? 0 : t2.hashCode());
			return result;
		}
	
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof PairTransition))
				return false;
			PairTransition other = (PairTransition) obj;
			/*if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;*/
			return source == other.source && target == other.target
				&& t1 == other.t1 && t2 == other.t2;
		}
	}

	public interface PairGraphExtension {
		void init(PairGraph pg);
		
		boolean getPairAfterDerive(ItemPair from, Transition t1, Transition t2, ItemPair to);
		boolean getPairAfterShift(ItemPair from, Transition t1, Transition t2, ItemPair to);
		boolean getPairAfterEmptyShift(ItemPair from, Transition t1, Transition t2, ItemPair to);
		boolean getPairAfterSingleReduce(ItemPair from, Transition t1,	Transition t2, ItemPair to);
		boolean getPairAfterPairwiseReduce(ItemPair from, Transition t1, Transition t2, ItemPair to);

		String toString(ItemPair p);
		String printSize();
	}
	
}
