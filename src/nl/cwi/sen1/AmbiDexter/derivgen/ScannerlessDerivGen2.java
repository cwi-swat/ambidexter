package nl.cwi.sen1.AmbiDexter.derivgen;

import java.util.Map.Entry;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.automata.ItemPDA;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.PDA;
import nl.cwi.sen1.AmbiDexter.automata.PDA.PDAState;
import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.FollowRestrictions;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.LinkedList;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

/*
 * Derivation generator for ItemDFAs, which contain propagated follow restrictions
 */

public class ScannerlessDerivGen2 extends ParallelDerivationGenerator {

	static boolean doFollow = true;
	
	public ScannerlessDerivGen2(int threads) {
		super(threads);
	}

	@Override
	public void build(NFA nfa) {
		dfa = new ItemPDA();
		dfa.build(nfa);
		dfa.printSize("IDFA", monitor);
	}
	
	@Override
	public void setDFA(PDA<?> dfa) {
		this.dfa = dfa;
		dfa.printSize("IDFA", monitor);
	}
	
	@Override
	protected void detect() {
		doFollow = config.doFollowRestrictions && !dfa.nfa.followRestrictionsPropagated;
		super.detect();
	}
	
	@Override
	protected AbstractWorker newWorker(String id) {
		return new Worker2(id);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected IStackFrame newStackFrame(@SuppressWarnings("rawtypes") PDAState t) {
		return new StackFrame(t);
	}
	
	/* inner classes */
	
	protected class Worker2 extends AbstractWorker  {
		
		private int maxdepth;
		
		public Worker2(String id) {
			super(id);
		}

		@SuppressWarnings("unchecked")
		protected boolean go(Job job, boolean fresh) {
			// don't try to refactor: recursive functions are almost twice as slow as this (i've tried it)

			LinkedList<ESet<Symbol>> shiftablesStack = job.shiftablesStack;		
			Object gss[] = job.gss;
			int shifted = job.shifted;
			maxdepth = job.maxdepth;
			Symbol sentence[] = job.sentence;
			
			// split reject and normal stackframes
			Object rejectGss[] = new Object[gss.length];
			for (int i = gss.length - 1; i >= 0; --i) {
				Queue<IStackFrame> rq = new Queue<IStackFrame>();
				rejectGss[i] = rq;
				Queue<IStackFrame> q = (Queue<IStackFrame>) gss[i];
				if (q != null) {
					for (int j = q.size() - 1; j >= 0; --j) {
						StackFrame sf = (StackFrame) q.get(j);
						if (sf.state.rejects) {
							rq.add(sf);
							q.remove(j); // safe during iteration
						}
					}
				}
			}
			
			Symbol[] debugString = null; //{ Symbol.getSymbol(-97), Symbol.getSymbol(-94), Symbol.getSymbol(-97), Symbol.getSymbol(-94), Symbol.getSymbol(-97) };
			
			boolean backTrack;
			while (true) {
				backTrack = false;
				Queue<StackFrame> top = (Queue<StackFrame>) gss[shifted];
				Queue<StackFrame> rejectTop = (Queue<StackFrame>) rejectGss[shifted];
				if (fresh) {
					fresh = false;
					// new stackframe
					// do all possible reduces
					{	
						Relation<Integer, NonTerminal> rejected = new Relation<Integer, NonTerminal>();
						
						// first reduce all reject frames
						for (int i = 0; i < rejectTop.size(); i++) { // rejectTop grows in this loop
							final StackFrame l = rejectTop.get(i);
							reduce(l, rejectTop, rejected);
							//++rejectReduced;
						}
						
						final int topSizeBeforeReduce = top.size();
						
						// then reduce all other states
						for (int i = 0; i < top.size(); i++) { // top grows in this loop
							final StackFrame l = top.get(i);
							reduce(l, top, rejected);
							//++reduced;
						}
						
						for (int i = top.size() - 1; i >= topSizeBeforeReduce; --i) {
							final StackFrame l = top.get(i);
							if (l.n != null) {
								// TODO there's still a problem with prefer/avoid, see Stratego len 19
								// after fixing, remove workaround in ParallelDerivationGenerator.ambiguity()
								if (config.doPreferAvoid) {
									if (!(l.prefers == 1 || (l.prefers == 0 && l.normals == 1) || (l.prefers == 0 && l.normals == 0 && l.avoids == 1))) {
										ambiguity(sentence, l.prev.level, l.level, l.n, id);
									}
								} else {
									if (l.prefers + l.normals + l.avoids > 1) {
										ambiguity(sentence, l.prev.level, l.level, l.n, id);
									}
								}
							}
							
							if (i >= topSizeBeforeReduce) {
								// add reject states of newly added normal state
								@SuppressWarnings("rawtypes")
								final PDAState rs = l.state.rejectState;
								if (rs != null) {
									boolean containsRS = false;
									for (int j = rejectTop.size() - 1; j >= 0; --j) {
										if (rejectTop.get(j).state == rs) {
											containsRS = true;
											break;
										}
									}
									if (!containsRS) {
										final StackFrame rsf = new StackFrame(rs);
										rsf.level = l.level;
										rejectTop.add(rsf);
									}
								}
							}
						}
					}
					
					// collect all possible shifts or backtrack
					if (dealer) {
						getShiftables(shiftablesStack, top);
						
						if (shiftablesStack.elem.size() == 0) {
							backTrack = true;
							++sentences;
						}
						
						if (shifted == dealLength) {
							if (dealLength < maxdepth) {
								// first merge reject and normal gss
								for (int i = gss.length - 1; i>= 0; --i) {
									Queue<IStackFrame> rq = (Queue<IStackFrame>) rejectGss[i];
									Queue<IStackFrame> q = (Queue<IStackFrame>) gss[i];
									if (rq != null && q != null) {
										for (int j = rq.size() - 1; j >= 0; --j) {
											q.add(rq.get(j));
										}
									}	
								}
								
								synchronized (jobs) {
									while (shiftablesStack.elem.size() > 0) {
									  if (monitor.canceling()) {
									    return false;
									  }
										Symbol s = shiftablesStack.elem.removeOne();
										Job j2 = new Job(s, gss, sentence, shifted);
										//print("Handing over " + s + " at " + shifted);
										//print("Handing over " + j2.id); 
										jobs.add(j2);
									}
								}
							}
							
							backTrack = true;
						}
					} else {
						if (shifted == maxdepth) {
							// length reached, backtrack
							backTrack = true;
							++sentences;
						} else {
							getShiftables(shiftablesStack, top);
							
							if (shiftablesStack.elem.size() == 0) {
								backTrack = true;
								++sentences;
							}
						}
					}
				} // end fresh

				if (!backTrack) {
					if (shifted < maxdepth && shiftablesStack.elem.size() > 0) {						
						// pick a symbol and shift
						
						Symbol s = shiftablesStack.elem.removeOne();
//						System.out.println("Shifting at " + shifted + ": " + s);
						
						if (debugString != null) {
							s = debugString[shifted];
						}						
						
						Queue<StackFrame> newTop = (Queue<StackFrame>) gss[shifted + 1];
						Queue<StackFrame> newRejectTop = (Queue<StackFrame>) rejectGss[shifted + 1];
						newTop.quickClear();
						newRejectTop.quickClear();
						
						for (int i = top.size() - 1; i >= 0; i--) {
							StackFrame l = top.get(i);
							for (Entry<Symbol, ItemPDA.PDAState> e : l.state.shifts) {
								if (e.getKey().canShiftWith(s)) {
									final ItemPDA.PDAState next = e.getValue();
									final StackFrame f = new StackFrame(next, l);
									f.level = shifted + 1;
									newTop.add(f);
									
									if (next.rejectState != null) {
										boolean containsRS = false;
										for (int j = rejectTop.size() - 1; j >= 0; --j) {
											if (rejectTop.get(j).state == next.rejectState) {
												containsRS = true;
												break;
											}
										}
										if (!containsRS) {									
											final StackFrame rf = new StackFrame(next.rejectState);
											rf.level = shifted + 1;
											newRejectTop.add(rf);
										}
									}
								}
							}
						}
						
						for (int i = rejectTop.size() - 1; i >= 0; i--) {
							StackFrame l = rejectTop.get(i);
							for (Entry<Symbol, ItemPDA.PDAState> e : l.state.shifts) {
								if (e.getKey().canShiftWith(s)) {
									@SuppressWarnings("rawtypes")
									final PDAState next = e.getValue();
									final StackFrame f = new StackFrame(next, l);
									f.level = shifted + 1;
									newRejectTop.add(f);
								}
							}
						}
						
						// push
						shiftablesStack = new LinkedList<ESet<Symbol>>(null, shiftablesStack);
						sentence[shifted] = s;
						shifted++;
						fresh = true;
					} else {
						// shifts exhausted, backtrack
						backTrack = true;
					}
				}
				
				if (backTrack) {
					if (shiftablesStack.next == null) {
						return true; // done
					}
						
//					String s = "";
//					for (int i = 0; i < shifted; i++) {
//						s += " " + sentence[i].prettyPrint();
//					}
//					monitor.println(s);
					
					shiftablesStack = shiftablesStack.next;
					shifted--;
					sentence[shifted] = null; // unnecessary, but for debugging

//					System.out.println("Backtracking to " + shifted);	
				}
				
				if (monitor.canceling()) {
					return false;
				}
			}
		}

		private void reduce(StackFrame l, Queue<StackFrame> top, Relation<Integer,NonTerminal> rejected) {
			for (int m = l.state.reductions.size() - 1; m >= 0 ; m--) {
				Item i = l.state.reductions.get(m);
				Production p = i.production;
				
				//System.out.println("Reducing " + p + (l.state.rejects ? " (r)" : ""));

				// check if 'follow' condition can be met
				if (p.lhs.followRestrictions != null && p.lhs.followRestrictions.mustFollowLength + l.level > maxdepth) {
					continue;
				}

				// look back (length of production rule) symbols back
				StackFrame from = l;
				for (int k = p.getLength() - 1; k >= 0; --k) {
					from = from.prev;
				}

				// check for rejects
				if (l.state.rejects) {
					if (p.reject) {
						rejected.add(from.level, p.lhs);
						//System.out.println("Adding reject " + from.level + " " + p.lhs);
					}
				} else { // we're in the normal gss
					if (rejected.contains(from.level, p.lhs)) {
						//System.out.println("Rejected " + p.lhs);
						continue; // nonterminal is rejected
					}
				}
				
				// lookup goto state
				ItemPDA.PDAState to = from.state.gotos.get(i);
				if (to == null) {
					continue;
				}

				// check here if we already have a stackframe with the goto state
				StackFrame existing = null;
				for (int n = top.size() - 1; n >= 0; n--) {
					StackFrame f = top.get(n);
					if (f.state == to && f.prev == from) {
						existing = f;
						break;
					}
				}
				
				if (existing == null) {
					// prevent empty right recursion loops
					if (to != l.state || to != from.state) {
						existing = from.pushReduce(to, p.lhs, l);
						top.add(existing); // unsafe
					}
				}

				if (existing != null) {
					if (p.prefer) {
						++existing.prefers;
					} else if (p.avoid) {
						++existing.avoids;
					} else if (p.isInjection() && l.prefers + l.avoids + l.normals == 1) { // pass over injections, only if previous node was unambiguous
						existing.prefers += l.prefers;
						existing.avoids += l.avoids;
						existing.normals += l.normals;
					} else {
						++existing.normals;
					}
				}
			}
		}

		private void getShiftables(LinkedList<ESet<Symbol>> shiftablesStack, Queue<StackFrame> top) {
			if (scannerless) {
				final Set<CharacterClass> ccs = new ShareableHashSet<CharacterClass>();
				for (int i = top.size() - 1; i >= 0; i--) {
					StackFrame f = top.get(i);
					for (Symbol s : f.state.shiftables) {
						//ccs.add((CharacterClass) s);
						if (f.canShift(s)) {
							ccs.add(f.getAfterShift((CharacterClass) s));
						}
					}
				}
				shiftablesStack.elem = CharacterClass.getCommonShiftables(ccs);
			} else {
				shiftablesStack.elem = Grammar.newESetSymbol();
				for (int i = top.size() - 1; i >= 0; i--) {
					shiftablesStack.elem.addAll(top.get(i).state.shiftables);
				}
			}
		}
	}
	
	protected static class StackFrame implements IStackFrame {
		ItemPDA.PDAState state = null;
		StackFrame prev = null;
		int prefers = 0, avoids = 0, normals = 0;
		int level = 0;
		NonTerminal n;
		FollowRestrictions followRestrictions = null;
				
		// initial first element
		public StackFrame(ItemPDA.PDAState t) {
			state = t;
		}
		
		// push t onto s (shift)
		private StackFrame(ItemPDA.PDAState t, StackFrame s) {
			state = t;
			prev = s;
		}
		
		// reduce
		public StackFrame pushReduce(ItemPDA.PDAState t, NonTerminal reduced, StackFrame from) {
			StackFrame f = new StackFrame(t, this);
			f.followRestrictions = from.getNextRestrictions(reduced);
			f.level = from.level;
			f.n = reduced;
			return f;
		}
		
		// shift
		public StackFrame pushShift(ItemPDA.PDAState t, Symbol shifted) {
			StackFrame f = new StackFrame(t, this);
			f.followRestrictions = getNextRestrictions(shifted);
			return f;
		}
		
		public boolean canShift(Symbol s) {
			if (followRestrictions != null) {
				return followRestrictions.canShift(s);
			}
			return true;
		}
		
		public CharacterClass getAfterShift(CharacterClass cc) {
			if (followRestrictions != null) {
				return followRestrictions.getNextCharClassAfterShift(cc);
			}
			return cc;
		}
		
		public FollowRestrictions getNextRestrictions(Symbol s) {
			if (!doFollow) {
				return null;
			}
			if (s instanceof NonTerminal) { // restrictions after reduce of s
				NonTerminal n = (NonTerminal) s;
				if (followRestrictions != null) {
					return followRestrictions.getNextAfterReduce(n);
				} else {
					return n.followRestrictions; 
				}
			} else { // restrictions after shift of s
				if (followRestrictions != null) {
					return followRestrictions.getNextAfterShift(s);
				} else {
					return null;
				}
			}			
		}

		@Override
		public String toString() {
			return /*"@" + level + "@" +*/ state.toString() + (followRestrictions == null ? "" : " -/- " + followRestrictions);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((state == null) ? 0 : state.hashCode());
			result = prime * result	+ ((followRestrictions == null) ? 0 : followRestrictions.hashCode());
			return result;
		}

		// Watch out: doesn't look at entire list,
		// only at this element and prev pointer.
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof StackFrame))
				return false;
			StackFrame other = (StackFrame) obj;
			if (state == null) {
				if (other.state != null)
					return false;
			} else if (!state.equals(other.state))
				return false;
			if (this.prev != other.prev) {
				return false;
			}
			if (followRestrictions == null) {
				if (other.followRestrictions != null)
					return false;
			} else if (!followRestrictions.equals(other.followRestrictions))
				return false;
			return true;
		}	
	}
}
