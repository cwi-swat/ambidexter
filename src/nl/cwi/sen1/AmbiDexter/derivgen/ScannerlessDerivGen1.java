package nl.cwi.sen1.AmbiDexter.derivgen;

import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.PDA;
import nl.cwi.sen1.AmbiDexter.automata.ProductionPDA;
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
 * Default derivation generator, works with ProductionDFAs
 */

public class ScannerlessDerivGen1 extends ParallelDerivationGenerator {
		
	public ScannerlessDerivGen1(int threads) {
		super(threads);
	}

	@Override
	public void build(NFA nfa) {
		dfa = new ProductionPDA();
		dfa.build(nfa);
		dfa.printSize("PDFA");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setDFA(PDA dfa) {
		this.dfa = dfa;
		dfa.printSize("PDFA");
	}
	
	@Override
	protected AbstractWorker newWorker(String id) {
		return new Worker(id);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected IStackFrame newStackFrame(PDAState t) {
		return new StackFrame(t);
	}
	
	protected class Worker extends AbstractWorker {
		
		public Worker(String id) {
			super(id);
		}
		
		@SuppressWarnings("unchecked")
		protected void go(Job job, boolean fresh) {
			// don't try to refactor: recursive functions are almost twice as slow as this (i've tried it)

			LinkedList<ESet<Symbol>> shiftablesStack = job.shiftablesStack;		
			Object gss[] = job.gss;
			int shifted = job.shifted;
			int maxdepth = job.maxdepth;
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
								if (Main.doPreferAvoid) {
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
						shiftablesStack.elem = getShiftables(top);
						
						if (shiftablesStack.elem.size() == 0) {
							backTrack = true;
							++sentences;
						}
						
						/*for (int i = top.size() - 1; i >= 0; i--) {
							StackFrame f = top.get(i);
							if (f.state.isEndState) {
								SymbolString ss = new SymbolString(shifted);
								for (int j = 0; j < shifted; ++j) {
									ss.add(sentence[j]);
								}
								generated.add(new Pair<SymbolString, FollowRestrictions>(ss, f.followRestrictions));
							}
						}*/
						
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
							shiftablesStack.elem = getShiftables(top);
							
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
						//System.out.println("Shifting at " + shifted + ": " + s);
						
						Queue<StackFrame> newTop = (Queue<StackFrame>) gss[shifted + 1];
						Queue<StackFrame> newRejectTop = (Queue<StackFrame>) rejectGss[shifted + 1];
						newTop.quickClear();
						newRejectTop.quickClear();
						
						for (int i = top.size() - 1; i >= 0; i--) {
							StackFrame l = top.get(i);
							if (l.canShift(s)) {
								for (Entry<Symbol, ProductionPDA.PDAState> e : l.state.shifts) {
									if (e.getKey().canShiftWith(s)) {
										final ProductionPDA.PDAState next = e.getValue();
										final StackFrame f = l.pushShift(next, s);
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
						}
						
						for (int i = rejectTop.size() - 1; i >= 0; i--) {
							StackFrame l = rejectTop.get(i);
							for (Entry<Symbol, ProductionPDA.PDAState> e : l.state.shifts) {
								if (e.getKey().canShiftWith(s)) {
									final ProductionPDA.PDAState next = e.getValue();
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
						
						//printSentence(sentence, "shifted: ");
						//printStacks(top);
						//System.out.println("===============");
					} else {
						// shifts exhausted, backtrack
						backTrack = true;
					}
				}
				
				if (backTrack) {
					if (shiftablesStack.next == null) {
						return; // done
					}
					
//					String s = "";
//					for (int i = 0; i < shifted; i++) {
//						s += " " + sentence[i].prettyPrint();
//					}
//					System.out.println(s);
						
					shiftablesStack = shiftablesStack.next;
					shifted--;
					sentence[shifted] = null; // unnecessary, but for debugging

					//System.out.println("Backtracking to " + shifted);	
				}
			}
		}

		private void reduce(StackFrame l, Queue<StackFrame> top, Relation<Integer,NonTerminal> rejected) {
			for (int m = l.state.reductions.size() - 1; m >= 0 ; m--) {
				Production p = l.state.reductions.get(m);
				//System.out.println("Reducing " + p);

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
						//System.out.println("Rejected");
						continue; // nonterminal is rejected
					}
				}
				
				// lookup goto state
				ProductionPDA.PDAState to = from.state.gotos.get(p);
				if (to == null) {
					continue;
				}
				
				// check here if we already have this stackframe in top or in prevAdd -> ambiguity
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

		private ESet<Symbol> getShiftables(Queue<StackFrame> top) {
			ESet<Symbol> shiftables;
			if (scannerless) {
				Set<CharacterClass> ccs = new ShareableHashSet<CharacterClass>();
				for (int i = top.size() - 1; i >= 0; i--) {
					StackFrame f = top.get(i);
					for (Symbol s : f.state.shiftables) {
						if (f.canShift(s)) {
							ccs.add(f.getAfterShift((CharacterClass) s));
						}
					}
				}
				shiftables = CharacterClass.getCommonShiftables(ccs);
			} else {
				shiftables = Grammar.newESetSymbol();
				for (int i = top.size() - 1; i >= 0; i--) {
					shiftables.addAll(top.get(i).state.shiftables);
				}
			}
			
			return shiftables;
		}
	}

	// we will do no sharing of the gss nodes (stackframes), since this is bad for performance (for some reason)
	// it might be faster for long strings, but for deriv gen they are usually relatively short :)

	protected static class StackFrame implements IStackFrame {
		ProductionPDA.PDAState state = null;
		StackFrame prev = null;
		FollowRestrictions followRestrictions;
		int level = 0;
		int prefers = 0, avoids = 0, normals = 0;
		NonTerminal n;
				
		// initial first element
		public StackFrame(ProductionPDA.PDAState t) {
			state = t;
		}
		
		// push t onto s
		protected StackFrame(ProductionPDA.PDAState t, StackFrame s) {
			state = t;
			prev = s;
		}
		
		// reduce
		public StackFrame pushReduce(ProductionPDA.PDAState t, NonTerminal reduced, StackFrame from) {
			StackFrame f = new StackFrame(t, this);
			f.followRestrictions = from.getNextRestrictions(reduced);
			f.level = from.level;
			f.n = reduced;
			return f;
		}
		
		// shift
		public StackFrame pushShift(ProductionPDA.PDAState t, Symbol shifted) {
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
			if (!Main.doFollowRestrictions) {
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
