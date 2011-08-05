package nl.cwi.sen1.AmbiDexter.nu2;

import java.util.Map;

import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Reduce;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.nu2.PairGraph.PairGraphExtension;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;

public class AlternatingEmptyPGE implements PairGraphExtension {
	
	// see grammar 267-269
	
	private static final int lastEmptyReduceBits = 8;
	private static final int lastSideOffset = ItemPair.newFlags(1);
	private static final int lastEmptyReduceOffset = ItemPair.newFlags(lastEmptyReduceBits);
	private static final long lastSideMask = ItemPair.makeMask(1, lastSideOffset);
	private static final long lastEmptyReduceMask = ItemPair.makeMask(lastEmptyReduceBits, lastEmptyReduceOffset);
	
	public Map<Symbol, Long> emptyReductions;
	public Map<Long, Symbol> emptyReductionsInv;
	
	public AlternatingEmptyPGE() {
		super();
	}
	
	public void init(PairGraph pg) {
		emptyReductions = new ShareableHashMap<Symbol, Long>();
		emptyReductionsInv = new ShareableHashMap<Long, Symbol>();
		long nr = 2;
		// 0 means no last empty reduction
		// 1 means no empty reductions allowed on other side (emptyReduceForbiddenMarker)
		emptyReductions.put(emptyReduceForbiddenMarker, 1L);
		emptyReductionsInv.put(1L, emptyReduceForbiddenMarker);
		
		for (Production p : pg.nfa.grammar.productions) {
			if (p.isEmpty()) {
				emptyReductions.put(p.reduction, nr);
				emptyReductionsInv.put(nr, p.reduction);
				++nr;
			}
		}
	}
	
	public boolean getPairAfterDerive(ItemPair from, Transition t1,	Transition t2, ItemPair to) {
		// just pass bits from 'from' to 'to'
		setBits(to, from.flags & lastEmptyReduceMask, from.flags & lastSideMask);
		return true;
	}

	public boolean getPairAfterShift(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		return true; // do nothing, leave flag and symbol bits zero
	}

	public boolean getPairAfterEmptyShift(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		Symbol ler = getLastEmptyReduce(from);
		if (ler == null) {
			// fresh start, leave bits at 0
			return true;
		}
		if (ler == emptyReduceForbiddenMarker) {
			return false;
		}
		return ((Reduce) ler).production.lhs != t1.label;
	}

	public boolean getPairAfterSingleReduce(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		long symbolBits = from.flags & lastEmptyReduceMask;
		long sideBit = from.flags & lastSideMask; // 1 means left
		
		if (t1 != null) {
			if (((Reduce) t1.label).production.isEmpty()) {
				if (sideBit == lastSideMask) { // left
					if (symbolBits == 0L) {
						symbolBits = ((long)emptyReductions.get(t1.label)) << lastEmptyReduceOffset;
					} else {
						symbolBits = 1L << lastEmptyReduceOffset; // mark forbidden
					}
				} else { // right
					long t1nr = emptyReductions.get(t1.label);
					if (symbolBits != 0L) {
						long s = symbolBits >>> lastEmptyReduceOffset;
						if (s == 1L || s == t1nr) {
							//System.out.println("" + from + " " + t1);
							return false; // empty reduce forbidden
						}
					}
					symbolBits = t1nr << lastEmptyReduceOffset;
					sideBit = lastSideMask; // left
				}
			}
		} else {
			if (((Reduce) t2.label).production.isEmpty()) {
				if (sideBit == lastSideMask) { // left
					long t2nr = emptyReductions.get(t2.label);
					if (symbolBits != 0L) {
						long s = symbolBits >>> lastEmptyReduceOffset;
						if (s == 1L || s == t2nr) {
							//System.out.println("" + from + " " + t2);
							return false; // empty reduce forbidden
						}
					}
					symbolBits = t2nr << lastEmptyReduceOffset;
					sideBit = 0L; // right
				} else { // right
					if (symbolBits == 0L) {
						symbolBits = ((long)emptyReductions.get(t2.label)) << lastEmptyReduceOffset;
					} else {
						symbolBits = 1L << lastEmptyReduceOffset; // mark forbidden
					}
				}
			}
		}

		setBits(to, symbolBits, sideBit);
		return true;
	}

	public boolean getPairAfterPairwiseReduce(ItemPair from, Transition t1,	Transition t2, ItemPair to) {
		// just pass bits from 'from' to 'to'
		setBits(to, from.flags & lastEmptyReduceMask, from.flags & lastSideMask);
		return true;
	}

	private void setBits(ItemPair to, long symbolBits, long sideBit) {
		if (symbolBits != 0L) {
			if (to.symmetrical) {
				to.symmetrical = false;
				if (sideBit != 0L) {
					to.swapped = true;
				}
			}
			if (to.swapped) {
				sideBit ^= lastSideMask;
			}
			to.flags |= symbolBits;
			to.flags |= sideBit;
		}
	}

	public String printSize() {
		return "";
	}

	public String toString(ItemPair p) {
		return (getLastSide(p) ? " 1:" : " 0:") + getLastEmptyReduce(p);
	}

	public boolean getLastSide(ItemPair p) {
		return (p.flags & lastSideMask) == lastSideMask;
	}
	
	public Symbol getLastEmptyReduce(ItemPair p) {
		return emptyReductionsInv.get((p.flags & lastEmptyReduceMask) >>> lastEmptyReduceOffset);
	}
	
	private final static Symbol emptyReduceForbiddenMarker = new Symbol() {
		public String toString() {
			return "X";
		}
	};
	
}
