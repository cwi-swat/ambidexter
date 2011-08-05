package nl.cwi.sen1.AmbiDexter.nu2;

import java.util.Set;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Reduce;
import nl.cwi.sen1.AmbiDexter.nu2.PairGraph.PairGraphExtension;
import nl.cwi.sen1.AmbiDexter.util.IdShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class ScannerlessPGE implements PairGraphExtension {
	
	private static final int reduceBits = 9;
	private static final int reduced1Offset = ItemPair.newFlags(reduceBits);
	private static final int reduced2Offset = ItemPair.newFlags(reduceBits);
	private static final long reduced1Mask = ItemPair.makeMask(reduceBits, reduced1Offset);	
	private static final long reduced2Mask = ItemPair.makeMask(reduceBits, reduced2Offset);
	private static final int maxReduceSets = 1 << reduceBits;

	static ShareableHashSet<IdShareableHashSet<NonTerminal>> reducedCache;
	private static Queue<IdShareableHashSet<NonTerminal>> reducedIndex;
	private static int reducedId;
	
	public ScannerlessPGE() {
		super();
	}

	public void init(PairGraph pg) {
		clearCaches();
	}
	
	private static void clearCaches() {
		reducedCache = new ShareableHashSet<IdShareableHashSet<NonTerminal>>();
		reducedIndex = new Queue<IdShareableHashSet<NonTerminal>>(maxReduceSets);
		reducedIndex.add(null); // at 0
		reducedId = 1;
	}		

	public boolean getPairAfterShift(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		// reset reduce sets, leave them at 0
		return true;
	}
	
	public boolean getPairAfterEmptyShift(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		// reset reduce sets, leave them at 0
		return true;
	}
	
	public boolean getPairAfterDerive(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		setReducedFlags(to, from.flags & reduced1Mask, from.flags & reduced2Mask);		
		return true;
	}
	
	public boolean getPairAfterSingleReduce(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		if (t1 != null) {
			Reduce r = (Reduce) t1.label;
			NonTerminal n = r.production.lhs;
			if (!canReduce1(from, n)) {
				return false;
			}	
			setReducedFlags(to, getReduced1AfterReduce(from, n) << reduced1Offset, from.flags & reduced2Mask);
		} else {
			Reduce r = (Reduce) t2.label;
			NonTerminal n = r.production.lhs;
			if (!canReduce2(from, n)) {
				return false;
			}
			setReducedFlags(to, from.flags & reduced1Mask, getReduced2AfterReduce(from, n) << reduced2Offset);
		}
		return true;
	}
	
	public boolean getPairAfterPairwiseReduce(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		Reduce r = (Reduce) t1.label;
		NonTerminal n = r.production.lhs;
		if (!canReduce1(from, n) || !canReduce2(from, n)) {
			return false;
		}
		setReducedFlags(to, getReduced1AfterReduce(from, n) << reduced1Offset, getReduced2AfterReduce(from, n) << reduced2Offset);
		
		return true;
	}
	
	public String printSize() {
		return ", " + reducedCache.size() + " reduced sets";
	}
	
	public String toString(ItemPair p) {
		Set<NonTerminal> reduced1 = getReduced1(p);
		Set<NonTerminal> reduced2 = getReduced2(p);
	
		String s = " [";
		if (reduced1 != null) {
			s += reduced1;
		}
		s += ",";
		if (reduced2 != null) {
			s += reduced2; 
		}
		return s + "]";
	}

	private static void setReducedFlags(ItemPair p, long red1, long red2) {
		if (p.symmetrical) {
			if (red1 > red2) {
				p.symmetrical = false;
				p.swapped = true;
			} else if (red1 < red2) {
				p.symmetrical = false;
			}
		}
		if (p.swapped) {
			p.flags |= (red1 >>> reduced1Offset) << reduced2Offset; 
			p.flags |= (red2 >>> reduced2Offset) << reduced1Offset; 
		} else {
			p.flags |= red1; 
			p.flags |= red2;
		}
	}
		
	private IdShareableHashSet<NonTerminal> getReduced1(ItemPair p) {
		return reducedIndex.get((int) ((p.flags & reduced1Mask) >>> reduced1Offset));
	}

	private IdShareableHashSet<NonTerminal> getReduced2(ItemPair p) {
		return reducedIndex.get((int) ((p.flags & reduced2Mask) >>> reduced2Offset));
	}
	
	private boolean canReduce1(ItemPair p, NonTerminal n) {
		if (n.usedInRejectFilter || n.rejectedLiterals != null) {
			Set<NonTerminal> reduced2 = getReduced2(p);
			if (n.usedInRejectFilter) {
				// check with previously reduced nonterminals
				if (reduced2 != null) {
					for (NonTerminal n2 : reduced2) {
						if (n2.rejectedLiterals != null && n2.rejectedLiterals.contains(n)) {
							return false;
						}
					}
				}
			} else {
				// check with previously reduced nonterminals
				if (reduced2 != null) {
					for (NonTerminal n2 : reduced2) {
						if (n.rejectedLiterals.contains(n2)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private boolean canReduce2(ItemPair p, NonTerminal n) {
		if (n.usedInRejectFilter || n.rejectedLiterals != null) {
			Set<NonTerminal> reduced1 = getReduced1(p);
			if (n.usedInRejectFilter) {
				// check with previously reduced nonterminals
				if (reduced1 != null) {
					for (NonTerminal n2 : reduced1) {
						if (n2.rejectedLiterals != null && n2.rejectedLiterals.contains(n)) {
							return false;
						}
					}
				}
			} else {
				// check with previously reduced nonterminals
				if (reduced1 != null) {
					for (NonTerminal n2 : reduced1) {
						if (n.rejectedLiterals.contains(n2)) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private int getReduced1AfterReduce(ItemPair p, NonTerminal n) {
		if (!Main.doRejects) {
			return 0;
		}
	
		IdShareableHashSet<NonTerminal> reduced1 = getReduced1(p);
		if (n.usedInRejectFilter || n.rejectedLiterals != null) {
			IdShareableHashSet<NonTerminal> newReduced = new IdShareableHashSet<NonTerminal>();
			if (reduced1 != null) {
				newReduced.addAll(reduced1);
			}
			newReduced.add(n);
			reduced1 = newReduced;
		}
		
		if (reduced1 == null) {
			return 0;
		}
		if (reduced1.id == 0) {
			IdShareableHashSet<NonTerminal> c = reducedCache.getContained(reduced1);
			if (c != null) {
				return c.id;
			} else {
				if (reducedId == maxReduceSets) {
					throw new RuntimeException("Max reduced sets reached");
				}
				reducedCache.add(reduced1);
				reducedIndex.add(reduced1);
				reduced1.id = reducedId++;
			}
		}
		return reduced1.id;
	}
	
	private int getReduced2AfterReduce(ItemPair p, NonTerminal n) {
		if (!Main.doRejects) {
			return 0;
		}

		IdShareableHashSet<NonTerminal> reduced2 = getReduced2(p);
		if (n.usedInRejectFilter || n.rejectedLiterals != null) {
			IdShareableHashSet<NonTerminal> newReduced = new IdShareableHashSet<NonTerminal>();
			if (reduced2 != null) {
				newReduced.addAll(reduced2);
			}
			newReduced.add(n);
			reduced2 = newReduced;
		}
		
		if (reduced2 == null) {
			return 0;
		}
		if (reduced2.id == 0) {
			IdShareableHashSet<NonTerminal> c = reducedCache.getContained(reduced2);
			if (c != null) {
				return c.id;
			} else {
				if (reducedId == maxReduceSets) {
					throw new RuntimeException("Max reduced sets reached");
				}
				reducedCache.add(reduced2);
				reducedIndex.add(reduced2);
				reduced2.id = reducedId++;
			}
		}
		return reduced2.id;
	}
	
}
