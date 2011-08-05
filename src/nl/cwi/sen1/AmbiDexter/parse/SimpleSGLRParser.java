package nl.cwi.sen1.AmbiDexter.parse;

import java.util.Collection;
import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.StartItem;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.grammar.Derive;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree.AmbNode;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree.LeafNode;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree.ParseTreeNode;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree.ProdNode;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.FragmentStack;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Util;

// TODO parse tree construction is buggy, check out grammar 267

public class SimpleSGLRParser implements IParser {

	public NFA nfa;
	private ShareableHashMap<GSSNode, Set<GSSNode>> shiftsTC;
	private Set<Pair<GSSNode, GSSNode>> ambNodesCreated;
	private int ambiguityNodes;

	public SimpleSGLRParser(NFA nfa) {
		this.nfa = nfa;
	}
	
	public ParseTree parse(SymbolString s) {
		return parse(s, nfa.grammar.startSymbol);
	}
	
	public ParseTree parse(SymbolString s, NonTerminal nt) {
		// set up initial gss
		ShareableHashMap<Item, GSSNode> gss = new ShareableHashMap<Item, GSSNode>();
		Pair<Item, Item> startAndEnd = nfa.getStartAndEndItem(nt);
		GSSNode startNode = new GSSNode(startAndEnd.a, 0);
		gss.put(startAndEnd.a, startNode);
		
		gss = parse(s, gss);

		
		/*if (true) {
		  	// benchmarking
			System.out.println(GSSNode.ids);
			return null;
		}*/
		
		// see if endItem is reached
		GSSNode n = gss.get(startAndEnd.b);
		if (n != null) {
			// TODO do a check to see it is reduced from startNode at level 0?
			ambNodesCreated = new ShareableHashSet<Pair<GSSNode,GSSNode>>();
			shiftsTC = Util.transitiveClosure2(getShifts(gss.values()));
			//gssToDot(gss.values());
			return buildParseTree(n, startNode);
		} else {
			//gssToDot(gss.values());
			//System.out.println("Parse error at position " + s.size() + ": eof");				
			return null;
		}
	}
	
	private ShareableHashMap<Item, GSSNode> parse(SymbolString sentence, ShareableHashMap<Item, GSSNode> gss) {
		
		int level = 0;
		FragmentStack<GSSNode> todo = new FragmentStack<GSSNode>(1024);
		for (Entry<Item, GSSNode> e : gss) {
			todo.add(e.getValue());
		}
		
		while (true) {
			
			while (todo.size() > 0) {
				GSSNode n = todo.pop();
				if (n.rejected) {
					// do nothing with this one
					// remove later on (not just yet b/c there might come more edges to it)
					continue;
				}
				
				// derives
				for (Transition d : n.item.derives) {
					GSSNode to = gss.get(d.target);
					boolean toIsNew = to == null;
					if (toIsNew) {
						to = new GSSNode(d.target, level);
						gss.put(d.target, to);
						todo.add(to);
					}
					if (to.edges.add(new GSSEdge(n, d.label))) {
						//System.out.println("deriving from " + n + " to " + to);
						if (!toIsNew) {
							// we added a new edge to an already existing node
							// try to re-reduce all nodes again
							for (Entry<Item, GSSNode> e : gss) {
								todo.add(e.getValue());
							}
						}
					}
				}
				
				// reduces
				// TODO look at lookahead
				if (n.item.canReduce() && canReduce(n.item.production.lhs, sentence, level)) {
					// look back length of production rules
					Set<GSSNode> from1 = new ShareableHashSet<GSSNode>();
					from1.add(n);
					Production p = n.item.production; 
					for (int i = p.getLength(); i > 0; i--) {
						Set<GSSNode> from2 = new ShareableHashSet<GSSNode>();
						for (GSSNode f : from1) {
							for (GSSEdge e : f.edges) {
								if (!(e.symbol instanceof Derive)) {
									from2.add(e.from);
								}
							}
						}
						from1 = from2;
					}
					
					for (GSSNode f : from1) {
						for (GSSEdge e : f.edges) {
							if (e.symbol == p.derivation) {
								Item i = null;
								i = e.from.item.getNext();
								GSSNode to = gss.get(i);
								boolean toIsNew = to == null;
								if (toIsNew) {
									to = new GSSNode(i, level);
									gss.put(i, to);
									todo.add(to);
								}
								
								GSSEdge newEdge = new GSSEdge(e.from, p.lhs, n);
								if (to.edges.add(newEdge)) {
									newEdge.nodeAfterMatchingDerive = f;
									if (p.reject) {
										to.rejected = true;
										//System.out.println("Rejected " + to);
									}
									
									//System.out.println("reducing " + p.lhs + " from " + e.from + " to " + to + " via " + n);
									if (!toIsNew) {
										// we added a new edge to an already existing node
										// try to re-reduce all nodes again
										for (Entry<Item, GSSNode> entry : gss) {
											todo.add(entry.getValue());
										} // TODO enable for test 4
									}
								}
							}
						}
					}
				}
			}
			
			// cleanup of rejected stacks (might be done during reducing)
			int oldSize = 0;
			Queue<GSSNode> removeN = new Queue<GSSNode>(gss.size());
			Queue<GSSEdge> removeE = new Queue<GSSEdge>(64);
			while (oldSize != gss.size()) {
				oldSize = gss.size();
				removeN.quickClear();
				for (Entry<Item, GSSNode> entry : gss) {
					GSSNode n = entry.getValue();
					if (n.rejected) {
						removeN.add(n);
					} else {
						removeE.quickClear();
						for (GSSEdge e : n.edges) {
							if (e.from.rejected || (e.reducedFrom != null && e.reducedFrom.rejected)) {
								removeE.add(e);
								//System.out.println("Removing " + e + " from " + n);
							}
						}
						n.edges.removeAll(removeE);
						if (n.edges.size() == 0 && !(n.item instanceof StartItem)) {
							n.rejected = true;
							removeN.add(n);
						}						
					}
				}
				for (GSSNode n : removeN) {
					//System.out.println("Removing " + n);
					gss.remove(n.item);
				}
			}
			
			// shifts
			if (level < sentence.size()) {
				Symbol s = sentence.get(level);
				ShareableHashMap<Item, GSSNode> nextGss = new ShareableHashMap<Item, GSSNode>();
				for (Entry<Item, GSSNode> entry : gss) {
					GSSNode n = entry.getValue();
					if (n.canShift(s)) {
						Item i = null;
						i = n.item.getNext();
						GSSNode to = nextGss.get(i);
						if (to == null) {
							to = new GSSNode(i, level + 1);
							nextGss.put(i, to);
							todo.add(to);
						}
						to.edges.add(new GSSEdge(n, s));
					}
				}
				
				if (nextGss.size() == 0) {
					//System.out.println("Parse error at position " + level + ": " + s);				
					return nextGss;
				}
				
				gss = nextGss;
				level++;
			} else {
				// at end of sentence, done
				return gss;
			}
		}		
	}
	
	private ShareableHashMap<GSSNode, Set<GSSNode>> getShifts(Collection<GSSNode> gss) {
		// collect all shift transitions in the gss
		ShareableHashMap<GSSNode, Set<GSSNode>> shifts = new ShareableHashMap<GSSNode, Set<GSSNode>>();
		ShareableHashSet<GSSNode> todo = new ShareableHashSet<GSSNode>();
		Set<GSSNode> done = new ShareableHashSet<GSSNode>();
		todo.addAll(gss);
		while (todo.size() > 0) {
			GSSNode n = todo.removeOne();
			done.add(n);
			
			Set<GSSNode> from = shifts.get(n);
			if (from == null) {
				from = new ShareableHashSet<GSSNode>();
				from.add(n);
				shifts.put(n, from);
			}
			
			for (GSSEdge e : n.edges) {
				if (!(e.symbol instanceof Derive)) {
					from.add(e.from);
				}
				if (!done.contains(e.from)) {
					todo.add(e.from);
				}
				if (e.reducedFrom != null && !done.contains(e.reducedFrom)) {
					todo.add(e.reducedFrom);
				}
			}
		}
		
		return shifts;
	}
	
	// check follow restrictions
	private boolean canReduce(NonTerminal n, SymbolString s, int pos) {
		if (n.followRestrictions != null && pos < s.size()) {
			return n.followRestrictions.canShift(s.get(pos));
		}		
		return true;
	}
	
	protected void gssToDot(Collection<GSSNode> gss) {
		String dot = "digraph G {\nrankdir=\"RL\"\n";
		
		ShareableHashSet<GSSNode> todo = new ShareableHashSet<GSSNode>();
		Set<GSSNode> done = new ShareableHashSet<GSSNode>();
		todo.addAll(gss);
		while (todo.size() > 0) {
			GSSNode n = todo.removeOne();
			done.add(n);

			dot += "" + n.id + " [label=" + Util.dotId(n) + "]\n";
			
			for (GSSEdge e : n.edges) {
				dot += "" + n.id + " -> " + e.from.id + " [label=" + Util.dotId(e.symbol) + "]\n";
				if (e.reducedFrom != null) {
					dot += "" + e.reducedFrom.id + " -> " + n.id + " [style=dotted, constraint=false]\n";
					if (!done.contains(e.reducedFrom)) {
						todo.add(e.reducedFrom);
					}
				}

				if (!done.contains(e.from)) {
					todo.add(e.from);
				}
			}
		}
		dot += "}";
		
		Util.writeTextFile("gss.dot", dot);
	}
	
	// =============================== Parse tree reconstruction ==============================================
	
	private ParseTree buildParseTree(GSSNode end, GSSNode begin) {
		ParseTreeNode top = new AmbNode();
		ambiguityNodes = 0;
		
		getParseTreeNode(end, top, begin);
		
		return new ParseTree(top.getChild(0), ambiguityNodes);
	}
	
	private void getParseTreeNode(GSSNode n, ParseTreeNode parent, GSSNode first) {
				
		Set<GSSEdge> edges = new ShareableHashSet<GSSEdge>();
		ShareableHashSet<GSSNode> syblings = new ShareableHashSet<GSSNode>();
		for (GSSEdge e : n.edges) {
			if (!(e.symbol instanceof Derive)) {
				Set<GSSNode> reachable = shiftsTC.get(e.from);
				if ((reachable != null && reachable.contains(first)) || e.from == first) {
					edges.add(e);
					syblings.add(e.from);
				}
			}
		}
		
		if (syblings.size() == 0) {
			return; // no backwards shifts from here
		}
		
		ParseTreeNode prevParent = null;
		boolean ambigV = edges.size() > 1;
		boolean ambigH = syblings.size() > 1;
		boolean ambNodeExists = false;
		if (ambigV || ambigH) {
			if (ambigV) {
				// in the case of a vertical ambiguity we might need to reconnect to an earlier created amb node (parent)
				Pair<GSSNode, GSSNode> pair = new Pair<GSSNode, GSSNode>(n, first);
				if (ambNodesCreated.contains(pair)) {
					ambNodeExists = true;
				} else {
					ambNodesCreated.add(pair);
				}
			}

			if (!ambNodeExists) {
				prevParent = parent;
				parent = new AmbNode();
				++ambiguityNodes;
				((AmbNode)parent).horizontal = ambigH;
				prevParent.addChild(parent);
			}
		}
		
		for (GSSEdge e : edges) {
			ParseTreeNode prevParent2 = null;
			if (ambigH) {
				prevParent2 = parent;
				parent = new ProdNode(n.item.production);
				prevParent2.addChild(parent);
			}
			
			if (e.reducedFrom == null) {
				parent.addChild(new LeafNode(e.symbol));
			} else {
				ProdNode p = new ProdNode(e.reducedFrom.item.production);
				getParseTreeNode(e.reducedFrom, p, e.nodeAfterMatchingDerive);
				
				if (p.getNumChildren() == 1 && p.getChild(0) instanceof AmbNode && ((AmbNode)p.getChild(0)).horizontal) {
					parent.addChild(p.getChild(0));
					// TODO
					// --ambiguityNodes;
				} else {
					parent.addChild(p);
				}
			}
			
			if (ambNodeExists) {
				break;
			}
			
			if (prevParent2 != null) {
				getParseTreeNode(e.from, parent, first);
				parent = prevParent2;
			}
		}
		
		if (prevParent != null) {
			parent = prevParent;
		}
		
		if (!ambigH) {
			getParseTreeNode(syblings.getOne(), parent, first);
		}
		
		// filter prefers and avoids
		for (int i = 0; i < parent.children.size(); ++i) {
			ParseTreeNode child = parent.children.get(i);
			if (child instanceof AmbNode && !((AmbNode) child).horizontal) {
				ESet<ParseTreeNode> prefers = new ShareableHashSet<ParseTreeNode>();
				ESet<ParseTreeNode> avoids = new ShareableHashSet<ParseTreeNode>();
				for (ParseTreeNode cc : child.children) {
					if (cc instanceof ProdNode) {
						ProdNode pn = (ProdNode) cc;
						if (pn.prod.prefer) {
							prefers.add(cc);
						} else if (pn.prod.avoid) {
							avoids.add(cc);
						} else {
							while (pn.prod.isInjection()) {
								ParseTreeNode p2 = pn.getChild(0);
								if (p2 instanceof ProdNode) {
									pn = (ProdNode) p2;
									if (pn.prod.prefer) {
										prefers.add(cc);
										break;
									} else if (pn.prod.avoid) {
										avoids.add(cc);
										break;
									}
								} else {
									break;
								}
							}
						}
					}
				}
			
				if (prefers.size() == 1) {
					parent.children.set(i, prefers.removeOne());
					--ambiguityNodes;
				} else if (prefers.size() == 0 && child.children.size() - avoids.size() == 1) {
					ESet<ParseTreeNode> one = new ShareableHashSet<ParseTreeNode>();
					one.addAll(child.children);
					one.removeAll(avoids);
					parent.children.set(i, one.removeOne());
					--ambiguityNodes;
				}
			}
		}
	}	
	
	static protected class GSSNode {
		Item item;
		int level;
		ShareableHashSet<GSSEdge> edges = new ShareableHashSet<GSSEdge>(64);
		boolean rejected = false;
				
		static int ids = 0;
		int id = ids++;
		
		public GSSNode(Item item, int level) {
			this.item = item;
			this.level = level;
		}
		
		public String toString() {
			return item.toString() + " @ " + level;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((item == null) ? 0 : item.hashCode());
			result = prime * result + level;
			return result;
		}
		
		public boolean canShift(Symbol s) {
			if (item.canShift() && item.getNextSymbol().canShiftWith(s)) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof GSSNode))
				return false;
			GSSNode other = (GSSNode) obj;
			return item == other.item && level == other.level;
			/*if (item == null) {
				if (other.item != null)
					return false;
			} else if (!item.equals(other.item))
				return false;
			if (level != other.level)
				return false;
			return true;*/
		}
	}
	
	static protected class GSSEdge {
		GSSNode from;
		Symbol symbol;
		GSSNode reducedFrom;
		GSSNode nodeAfterMatchingDerive;
		
		public GSSEdge(GSSNode prev, Symbol symbol) {
			this.from = prev;
			this.symbol = symbol;
		}

		public GSSEdge(GSSNode prev, Symbol symbol, GSSNode reducedFrom) {
			this.from = prev;
			this.reducedFrom = reducedFrom;
			this.symbol = symbol;
		}
		
		public String toString() {
			return from.toString() + " <-- " + symbol + " --" +
				(reducedFrom != null ? " ( " + reducedFrom +" )": "");
		}

		@Override
		public int hashCode() {
			return from.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof GSSEdge))
				return false;
			GSSEdge other = (GSSEdge) obj;
			return from == other.from && reducedFrom == other.reducedFrom; 
			/*if (prev == null) {
				if (other.prev != null)
					return false;
			} else if (!prev.equals(other.prev))
				return false;
			if (reducedFrom == null) {
				if (other.reducedFrom != null)
					return false;
			} else if (!reducedFrom.equals(other.reducedFrom))
				return false;
			return true;*/
		}		
	}
}
