package nl.cwi.sen1.AmbiDexter.nu2;

import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.Derive;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Reduce;
import nl.cwi.sen1.AmbiDexter.util.FragmentStack;
import nl.cwi.sen1.AmbiDexter.util.LongArrayStack;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Stack;

public class DepthFirstPairGraph extends PairGraph {

	protected Set<Item> usedItems;
	protected LongArrayStack[] transArray;
	protected int minD;
	protected int maxD;
	protected int nrTrans;
	
	public DepthFirstPairGraph() { }
	
	@Override
	public void init(NFA nfa) {
		super.init(nfa);		
		transArray = new LongArrayStack[nfa.maxDistanceToEnd * 2 + 1];
		for (int i = transArray.length - 1; i >= 0; --i) {
			transArray[i] = new LongArrayStack(256);
		}
	}

	@Override
	protected void addTransition(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		if (to != null) {
			//final int d = to.getDistanceToEnd();

			// order: reduce (3), shift (2), derive (1)
			int d = 2; // shift
			if (t1 != null) {
				if (t1.getType() == Reduce.class) {
					d = 3;
				} else if (t1.getType() == Derive.class) {
					d = 1;
				}
			} else {
				if (t2.getType() == Reduce.class) {
					d = 3;
				} else if (t2.getType() == Derive.class) {
					d = 1;
				}
			}
			
			transArray[d].add(to.items);
			transArray[d].add(to.flags);
			
			if (d > maxD) {
				maxD = d;
			}
			if (d < minD) {
				minD = d;
			}
			++nrTrans;
			++nrTransitions;
		}
	}

	protected long[] sortCurrentTrans() {
		final long[] s = new long[nrTrans * 2 + 1];
		int k = 1;
		s[0] = nrTrans * 2;
		for (int d = maxD; d >= minD; --d) // maximizing distance first
		//for (int d = minD; d <= maxD; ++d) // minimizing distance (tends towards longer stacks)
		{
			LongArrayStack b = transArray[d];
			k = b.copyIntoArray(s, k);
			b.quickClear();
		}
	
		minD = transArray.length;
		nrTrans = maxD = 0;
		return s;
	}

	@Override
	protected void traverse() {
		done = new ItemPairArrayHashSet(NFA.IDedItems);
		usedItems = null;
		nrTransitions = 0;
		
		final int initStackSize = 256*256*2;
		final LongArrayStack callStack = new LongArrayStack(initStackSize);
		final LongArrayStack itemStack = new LongArrayStack(initStackSize);
		final Stack<long[]> transStack = new FragmentStack<long[]>(initStackSize);
		
		long[] trans;		
		
		int nr = 1; // i in paper, start at 1, 0 means unnumbered
		int maxCallStackSize = 0, maxItemStackSize = 0;
		
		buildStartAndEndPairs();

		for (ItemPair p : endPairs) {
			p.setAlive();
		}
		
		for (ItemPair start : startPairs) {
			int csp = 0, isp = -1, tsp = 0;
			callStack.set(csp, start.items);
			callStack.set(csp + 1, start.flags);

			ItemPair p = start;

			while(csp >= 0) {
				if (p != null) {
					// p is new
					// begin of TRAVERSE-PAIR
					p.init(nr);
					++nr;
					
					isp += 2;
					itemStack.set(isp, p.items);		
					itemStack.set(isp + 1, p.flags);
					if (isp > maxItemStackSize) {
						maxItemStackSize = isp;
					}
					
					/*if (endPairs.contains(p)) { // endpair, print path
						System.out.println("--------------------");
						for (int i = 0; i <= csp; i += 2) {
							System.out.println(toString(lookupPair(callStack.get(i), callStack.get(i + 1))));
						}
						System.out.println("--------------------");
					}*/
					
					int bucketSize = p.bucket.properties.length;
					buildTransitions(p); // super
					trans = sortCurrentTrans();
					transStack.set(tsp, trans);
					
					if (bucketSize != p.bucket.properties.length) {
						// bucket has been rehashed, re-lookup p
						p.bucket.relookup(p);
					}
					
					// print progress periodically
					if ((nr - 1) % progressInterval == 0) {
						printSize("" + iteration);
						
						if ((nr - 1) % (progressInterval * 20) == 0) {
							System.out.println("    " + done.usageStatistics());
							//System.out.println("    Can be compressed to: " + done.getCompressedSize());
							int inTrans = 0;
							int transSpace = 0;
							for (int i = tsp; i >= 0; --i) {
								inTrans += transStack.get(i)[0];
								transSpace += transStack.get(i).length;
							}
							
							System.out.println("    Call stack depth: " + csp + ", Item stack depth: " + isp + ", in trans: " + inTrans + ", allocated trans: " + transSpace);
						}
					}
				} else {
					p = lookupPair(callStack.get(csp), callStack.get(csp + 1));
					trans = transStack.get(tsp);
				}
				
				if (trans[0] == 0) { // no transitions for p left
					csp -= 2; // pop
					--tsp;
					
					// code after call to TRAVERSE-EDGES in TRAVERSE-PAIR
					if (p.getLowlink() == p.getNumber()) {
						// p is root of SCC
						ItemPair p2 = lookupPair(itemStack.get(isp), itemStack.get(isp + 1));
						isp -= 2;
						p2.unsetOnStack();
						if (p.isAlive()) {
							while (p2.items != p.items || p2.flags != p.flags) {
								p2.setAlive();
								p2 = lookupPair(itemStack.get(isp), itemStack.get(isp + 1));
								isp -= 2;
								p2.unsetOnStack();
							}							
						} else {
							while (p2.items != p.items || p2.flags != p.flags) {
								p2 = lookupPair(itemStack.get(isp), itemStack.get(isp + 1));
								isp -= 2;
								p2.unsetOnStack();
							}
						}
					}
					
					if (csp >= 0) {
						// code after call to TRAVERSE-PAIR in TRAVERSE-EDGE (line 4)
						final ItemPair p2 = p; // to keep consistency with paper
						final ItemPair p1 = lookupPair(callStack.get(csp), callStack.get(csp + 1));
						int p2ll = p2.getLowlink();
						if (p2ll < p1.getLowlink()) {
							p1.setLowlink(p2ll);
						}
						if (p2.isAlive()) { // line 6 in TRAVERSE-EDGE
							p1.setAlive();
						}
					}
					
					p = null;
				} else {
					// continue with transitions, leave p on stack
					final int i = (int) (trans[0] - 1);
					final ItemPair p2 = lookupPair(trans[i], trans[i + 1]);
					trans[0] -= 2;
					
					final int p2nr = p2.getNumber();
					if (p2nr == 0) {
						// p2 is unvisited, push on call stack
						// call of TRAVERSE-PAIR in TRAVERSE-EDGE
						csp += 2;
						++tsp;
						callStack.set(csp, p2.items);
						callStack.set(csp + 1, p2.flags);
						if (csp > maxCallStackSize) {
							maxCallStackSize = csp;
						}
						p = p2;
					} else {
						// alternative in TRAVERSE-EDGE (line 4)
						if (p2.isOnStack()) {
							if (p2nr < p.getLowlink()) {
								p.setLowlink(p2nr);
							}
						}
						if (p2.isAlive()) { // line 6 in TRAVERSE-EDGE
							p.setAlive();
						}
						p = null;
					}
				}
			}
		}
				
		printSize("Done: " + (nr - 1) + " -");
		System.out.println("Max call stack size: " + maxCallStackSize + ", max item stack size: " + maxItemStackSize);		
	}

	@Override
	protected void filter() {
		nfa.filter(getUsedItems(), false);
	}

	@Override
	protected Set<Item> getUsedItems() {
		if (usedItems == null) {
			usedItems = new ShareableHashSet<Item>();
			int alivePairs = 0;
			for (ItemPair p : done) {
				if (p.isAlive()) {
					alivePairs++;
					usedItems.add(p.getA());
					usedItems.add(p.getB());
				}
			} // alive items contains <end>
			System.out.println("Alive pairs: " + alivePairs + ", States used: " + usedItems.size());
		}
		return usedItems;
	}

	@Override
	public Set<Production> getUsedProductions() {
		return nfa.getUsedProductions();
	}

	@Override
	public void toDot(String filename) {
	}

	/******************************************************************/
	public Relation<Pair<Production, Integer>, Production> getHarmlessPatterns(Set<Production> usedProductions) {
		// first collect all possible derivation patterns
		Relation<Pair<Production, Integer>, Production> pat = nfa.grammar.getItemDerives();
		int nrOrigPatterns = pat.size();
	
		// then remove all used patterns
		for (Transition t : nfa.transitions) {
			if (t.getType() == Derive.class && t.source.production != null && t.target.production != null) {
				pat.remove(t.source.getCanonicalItem(), t.target.production);
			}
		}
		
		System.out.println("Harmless derivation patterns: " + pat.size() + " / " + nrOrigPatterns);
		Relation<Pair<Production, Integer>, Production> inUsed = new Relation<Pair<Production,Integer>, Production>();
		ShareableHashMap<Production, Integer> prodUseCount = new ShareableHashMap<Production, Integer>();
		for (Entry<Pair<Production, Integer>, Set<Production>> e : pat.m) {
			if (!Main.quick && e.getValue().size() > 0) {
				System.out.println( "    " + e.getKey().a.toString(e.getKey().b)); // print item						
			}
	
			for (Production p : e.getValue()) {
				if (usedProductions.contains(e.getKey().a) && usedProductions.contains(p)) {
					inUsed.add(e.getKey(), p);
					if (!Main.quick) {
						System.out.println("        u " + p);
					}
				} else if (!Main.quick) {
					System.out.println("        " + p);						
				}
				
				Integer i = prodUseCount.get(p);
				if (i == null) {
					prodUseCount.put(p, 1);
				} else {
					prodUseCount.put(p, i + 1);
				}
			}
		}
		System.out.println("Harmless derivation patterns in used productions: " + inUsed.size());
		
		if (Main.verbose) {
			System.out.println("Production usage in right side of harmless patterns:");
			for (Entry<Production, Integer> e : prodUseCount) {
				System.out.println("    " + e.getValue() + " " + e.getKey());
			}
			System.out.println("Number of productions in right sides: " + prodUseCount.size());
		}
		
		return inUsed;
	}
}
