package nl.cwi.sen1.AmbiDexter.nu2;

import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.util.FragmentStack;
import nl.cwi.sen1.AmbiDexter.util.LongArrayStack;
import nl.cwi.sen1.AmbiDexter.util.Stack;


public class DepthFirstTransitionPairGraph extends DepthFirstPairGraph {

	public DepthFirstTransitionPairGraph() {
		super();
	}

	@Override
	protected void addTransition(ItemPair from, Transition t1, Transition t2, ItemPair to) {
		if (to != null) {
			final int d = to.getDistanceToEnd();
			long ts = t1 != null ? t1.ID : 0;
			if (t2 != null) {
				ts |= (long)t2.ID << 32;
			}
			transArray[d].add(to.items);
			transArray[d].add(to.flags);
			transArray[d].add(ts);			
			
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

	@Override
	protected long[] sortCurrentTrans() {
		final long[] s = new long[nrTrans * 3 + 1];
		int k = 1;
		s[0] = nrTrans * 3;
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
		nfa.setTransitionsUnused();
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
			callStack.set(csp + 2, 0); // no transitions

			ItemPair p = start;
			long ts = 0;
			
			while(csp >= 0) {
				if (p != null) {
					// p is new
					// begin of TRAVERSE-PAIR
					p.init(nr);
					++nr;
					
					isp += 3;
					itemStack.set(isp, p.items);		
					itemStack.set(isp + 1, p.flags);
					itemStack.set(isp + 2, ts);
					if (isp > maxItemStackSize) {
						maxItemStackSize = isp;
					}
					
//					if (p.items == endPairItems) { // endpair, print path
//						System.out.println("--------------------");
//						for (int i = 0; i <= csp; i += 3) {							
//							long pt = callStack.get(i + 2);
//							if (pt != 0) {
//								final Transition t1 = NFA.transQueue.get((int)(pt & 0xFFFFFFFFL));
//								final Transition t2 = NFA.transQueue.get((int)(pt >>> 32));
//								System.out.println("    " + new PairTransition(null, t1, t2, null));
//							}
//							ItemPair p2 = lookupPair(callStack.get(i), callStack.get(i + 1));
//							System.out.println(toString(p2));
//						}
//						System.out.println("--------------------");
//					}
					
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
					ts = callStack.get(csp + 2);
					trans = transStack.get(tsp);					
				}
				
				if (trans[0] == 0) { // no transitions for p left
					csp -= 3; // pop
					--tsp;
					
					// code after call to TRAVERSE-EDGES in TRAVERSE-PAIR
					if (p.getLowlink() == p.getNumber()) {
						// p is root of SCC
						ItemPair p2 = lookupPair(itemStack.get(isp), itemStack.get(isp + 1));
						long ts2 = itemStack.get(isp + 2);
						isp -= 3;
						p2.unsetOnStack();
						
						// XXX here we pop until the current pair transition is found, not only p
						if (p.isAlive()) {
							while (p2.items != p.items || p2.flags != p.flags || ts2 != ts) {
								p2.setAlive();
								// set transitions alive
								final Transition t1 = NFA.transQueue.get((int)(ts2 & 0xFFFFFFFFL));
								final Transition t2 = NFA.transQueue.get((int)(ts2 >>> 32));
								if (t1 != null) {
									t1.used = true;
								}
								if (t2 != null) {
									t2.used = true;
								}
							
								p2 = lookupPair(itemStack.get(isp), itemStack.get(isp + 1));
								ts2 = itemStack.get(isp + 2);
								isp -= 3;
								p2.unsetOnStack();
							}
						} else {
							while (p2.items != p.items || p2.flags != p.flags || ts2 != ts) {
								p2 = lookupPair(itemStack.get(isp), itemStack.get(isp + 1));
								ts2 = itemStack.get(isp + 2);
								isp -= 3;
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
							Transition t1 = NFA.transQueue.get((int)(ts & 0xFFFFFFFFL));
							Transition t2 = NFA.transQueue.get((int)(ts >>> 32));
							if (t1 != null) {
								t1.used = true;
							}
							if (t2 != null) {
								t2.used = true;
							}
						}
					}
					p = null;
				} else {
					// continue with transitions, leave p on stack
					final int i = (int) (trans[0] - 2);
					final ItemPair p2 = lookupPair(trans[i], trans[i + 1]);
					final long ts2 = trans[i + 2]; // from p to p2
					trans[0] -= 3;
					
					final int p2nr = p2.getNumber();
					if (p2nr == 0) {
						// p2 is unvisited, push on call stack
						// call of TRAVERSE-PAIR in TRAVERSE-EDGE
						csp += 3;
						++tsp;
						callStack.set(csp, p2.items);
						callStack.set(csp + 1, p2.flags);
						callStack.set(csp + 2, ts2);
						if (csp > maxCallStackSize) {
							maxCallStackSize = csp;
						}
						p = p2;
						ts = ts2;
					} else {
						// alternative in TRAVERSE-EDGE (line 4)
						if (p2.isOnStack()) { 
							if (p2nr < p.getLowlink()) {
								p.setLowlink(p2nr);
							}
							// XXX: here we put the pair transition on the item stack, even though t.target already is on the stack
							// this is so that it becomes part of a SCC, and can thus be marked as alive
							isp += 3;
							itemStack.set(isp, p2.items);		
							itemStack.set(isp + 1, p2.flags);
							itemStack.set(isp + 2, ts2);
							if (isp > maxItemStackSize) {
								maxItemStackSize = isp;
							}
						}
						if (p2.isAlive()) { // line 6 in TRAVERSE-EDGE
							p.setAlive();
							final Transition t1 = NFA.transQueue.get((int)(ts2 & 0xFFFFFFFFL));
							final Transition t2 = NFA.transQueue.get((int)(ts2 >>> 32));
							if (t1 != null) {
								t1.used = true;
							}
							if (t2 != null) {
								t2.used = true;
							}
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
		nfa.filter(getUsedItems(), true);
	}
}
