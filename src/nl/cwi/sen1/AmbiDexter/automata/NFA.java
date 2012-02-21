package nl.cwi.sen1.AmbiDexter.automata;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.IAmbiDexterMonitor;
import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.Derive;
import nl.cwi.sen1.AmbiDexter.grammar.FollowRestrictions;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Reduce;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolSet;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Stack;
import nl.cwi.sen1.AmbiDexter.util.Triple;
import nl.cwi.sen1.AmbiDexter.util.Util;

// Nondeterministic Finite Automaton
public abstract class NFA {

	public Set<Item> items; // does not include start and end items
	public ShareableHashSet<Transition> transitions = new ShareableHashSet<Transition>();
	public StartItem startItem;
	public EndItem endItem;
	
	public Grammar grammar;
	public Map<Production, List<Item>> prodItems = new ShareableHashMap<Production, List<Item>>();
	
	private Set<Set<Item>> clusters;
	private Relation<Item, Item> closures = new Relation<Item, Item>();
	private ShareableHashMap<Transition, Symbol[]> minimalStrings; // shifts
	public int precision;
	protected boolean includeRejects;
	public boolean followRestrictionsPropagated;
	public boolean shiftsInSets = false;
	public boolean reversed = false;

	public static int itemID;
	public static Queue<Transition> transQueue;
	public static Queue<Item> itemQueue;
	public static int IDedItems;
	
	public int maxDistanceToEnd = 0;
	
	// TODO unfold all other nullable productions?
	// or just make them empty skips
	// (well, these are less precise (especially with SLR1)) why??
	
	// TODO implement reject as difference on NFA?
	// only for bounded languages: for all strings:
	// parse and translate tree into bracketed string
	// subtract bracketed string from automaton
	
	public NFA(Grammar g) {
		grammar = g;
		startItem = new StartItem(0, g.startSymbol);
		endItem = new EndItem(1);
	}

	protected Set<Item> createItems(Grammar g, IItemFactory f) {
		Set<Item> items = new ShareableHashSet<Item>();
		
		for (Production p : g.productions) {
			if (p.reachable || (includeRejects && p.usedForReject)) {
				List<Item> list = new ArrayList<Item>(p.getLength() + 1);
				list.add(f.createItem(p, 0, itemID++));
				
				for (int i = 0; i < p.getLength(); ++i) {
					list.add(f.createItem(p, i + 1, itemID++));
				}
	
				prodItems.put(p, list);
				items.addAll(list);
			}
		}
		
		return items;
	}
	
	protected Transition addTransition(Item from, Symbol label, Item to) {
		Transition t = new Transition(from, label, to);
		Transition c = transitions.getContained(t);
		if (c == null) {
			transitions.add(t);
			c = t;
		}
		return c;
	}
	
	protected Transition addEmptyTransition(Item from, Symbol label, Item to) {
		Transition t = new Transition(from, label, to);
		t.empty = true;
		Transition c = transitions.getContained(t);
		if (c == null) {
			transitions.add(t);
			c = t;
		}
		return c;
	}
	
	protected abstract void buildNFA();

	public void build(boolean includeRejects, AmbiDexterConfig config) {
		itemID = 2; // 0 : StartItem, 1 : EndItem
		this.includeRejects = includeRejects;
		buildNFA();

		if (config != null) {
			if (config.unfoldNonRecursiveTails) {
				unfoldNonRecursiveTails();
			} else if (config.unfoldStronglyConnectedComponents) {
				unfoldStronglyConnectedComponents();
			} else {
				if (config.unfoldLayout || config.unfoldLexical || config.unfoldEmpties || config.unfoldNonRecursiveTails || config.unfoldStackDepth || config.unfoldStackContents) {
					doUnfoldings(config.unfoldLayout, config.unfoldLayout, config.unfoldLexical, config.unfoldEmpties, config.unfoldNonRecursiveTails, config.unfoldStackDepth || config.unfoldStackContents, config);
				}
				if (config.unfoldJoiningTails) {
					unfoldJoiningTails(config.unfoldOnlyLexicalTails);
				}
			}
		}
				
		addReduceTransitions();
	}
	
	// call after optimize()
	public void finish() {
		
		// reset
		closures = new Relation<Item, Item>();
		
		//computeClusters();
		
		// build dff sets
		for (Item i : allItems()) {
			i.computeDFF();
		}
		
		computeDistances();
		
		if (AmbiDexterConfig.writeDFA) {
			//calcMinimalStrings();
			minimalStrings = new NFAMinimalStringGen().getMinimalStrings(this);

			if (!AmbiDexterConfig.quick) {
				for (Transition t : startItem.shifts) {
					System.out.println("Minimal string for start symbol: " + Arrays.toString(minimalStrings.get(t)));
				}
			}
		}
	}
	
	public Item getItem(Production p, int index) {
		return prodItems.get(p).get(index);
	}
	
	public void addReduceTransitions() {
		for (Item i : allItems()) {
			for (Transition d : i.derives) {
				Item e = d.target.followShifts();
				Item to = (i == startItem ? endItem : i.shift.target);
				Transition r = addTransition(e, e.production.reduction, to); 
				e.reduces.add(r);

				r.reverse.add(d);
				d.reverse.add(r);
			}
		}
	}
	
	public void nrTransitionsAndItems() {
		transQueue = new Queue<Transition>(transitions.size());
		transQueue.add(null); // at position 0
		int id = 1;
		for (Transition t : transitions) {
			t.ID = id++;
			transQueue.add(t);
			if (transQueue.get(t.ID) != t) {
				throw new RuntimeException("fixthisplease");
			}
		}
		
		id = 0;
		Set<Item> allItems = allItems();
		itemQueue = new Queue<Item>(allItems.size());
		for (Item i : allItems) {
			i.ID = id++;
			itemQueue.add(i);
			if (itemQueue.get(i.ID) != i) {
				throw new RuntimeException("fixthisplease");
			}
		}
		IDedItems = id;
	}
	
	// we don't use clusters anymore b/c they can become too big to store
	// instead, we call NFASCC.getItemsFromIncompleteProductions in optimize()
	/*protected void computeClusters() {
		
		// A cluster is a set of items related to one shift sequence of a production.
		// If at least one item in a cluster is not used, the whole cluster can be removed.
		// However, some items might be used in more than one cluster, so they can only be
		// removed if all their clusters can be removed.
		
		System.out.println("Computing clusters");
		
		clusters = new ShareableHashSet<Set<Item>>();
		
		if (unfoldedForFollowRestrictions) {
			class A {
				Stack<Item> stack = new Queue<Item>(64);
				void traverse(Item i) {
					stack.add(i);
					if (i.shifts == null) {
						Set<Item> cluster = new ShareableHashSet<Item>();
						cluster.addAll(stack);
						clusters.add(cluster);
					} else {
						for (Transition t : i.shifts) {							
							traverse(t.target);							
						}
					}
					stack.pop();
				}				
			};
			A a = new A();
			
			for (Item i : items) {
				if (i.atBegin() && i.production != null) {
					System.out.println("Computing clusters for " + i);
					int s = clusters.size();
					a.traverse(i);
					System.out.println("" + (clusters.size() - s) + " : " + clusters.size());
				}
			}
		} else {
			for (Item i : items) {
				if (i.atBegin() && i.production != null) {
					Set<Item> c = new ShareableHashSet<Item>();
					i.followShifts(c);
					clusters.add(c);
				}
			}
		}
		
		if (config.verbose) {
			System.out.println("Number of clusters: " + clusters.size());
		}
	}*/
	
	protected Set<Item> allItems() {
		Set<Item> ai = new ShareableHashSet<Item>();
		ai.addAll(items);
		ai.add(startItem);
		ai.add(endItem);
		return ai;
	}
	
	public void optimize(boolean removeDeadEnds) {

		// remove unreachable items
		ESet<Item> todo = new ShareableHashSet<Item>();
		Set<Item> reachableItems = new ShareableHashSet<Item>();
		
		todo.add(startItem);
		while (todo.size() > 0) {
			Item i = todo.removeOne();
			reachableItems.add(i);
			
			for (Transition t : i.derives) {
				if (!reachableItems.contains(t.target)) {
					todo.add(t.target);
				}
			}
			if (i.shift != null) {
				if (!reachableItems.contains(i.shift.target)) {
					todo.add(i.shift.target);
				}
			}
			if (i.shifts != null) {
				for (Transition t : i.shifts) {
					if (!reachableItems.contains(t.target)) {
						todo.add(t.target);
					}
				}
			}
		}
		
		Set<Item> unreachableItems = new ShareableHashSet<Item>();
		for (Item i : items) {
			if (!reachableItems.contains(i)) {
				unreachableItems.add(i);
			}
		}
		removeItems(unreachableItems);
		
		if (removeDeadEnds) {
			// remove items with no outgoing transitions		
			int oldsize;
			int newsize = transitions.size();
			do {
				Set<Item> deadEnds = new ShareableHashSet<Item>();
				for (Item i : items) {
					if (i.derives.size() + i.reduces.size() + (i.shifts == null ? 0 : i.shifts.size()) == 0 && i.shift == null) {
						deadEnds.add(i);
					}
					if (i.canShift() && i.shift == null && (i.shifts == null || i.shifts.size() == 0)) {
						deadEnds.add(i);
					}
				}
				
				// remove them
				removeItems(deadEnds);
				
				// clean up dead cycles
				removeItems(new NFASCC().getDeadItems(startItem, endItem));
				
				// remove incomplete productions
				removeItems(new NFASCC().getItemsFromIncompleteProductions(items));
				
				// remove transitions with no reverse transitions left
				Set<Transition> toRemove = new ShareableHashSet<Transition>();
				for (Transition t : transitions) {
					if (t.reverse.size() > 0) {
						boolean reverseAlive = false;
						for (Transition r : t.reverse) {
							if (transitions.contains(r)) {
								reverseAlive = true;
								break;
							}
						}
						if (!reverseAlive) {
							toRemove.add(t);
						}
					}
				}
				removeTransitions(toRemove);
				
				oldsize = newsize;
				newsize = transitions.size();
			} while (oldsize != newsize);
		}
	}
	
	//-------------------------------------------------------------------------

	public void doUnfoldings(boolean layout, boolean literals, boolean cftolex, boolean empties, boolean tails, boolean stackdepth, AmbiDexterConfig config) {
		
		// XXX Make sure we don't unfold two transitions of which one leads to the other in the same loop!!  
		
		Set<Transition> toUnfold = new ShareableHashSet<Transition>();
		for (Transition t : transitions) {
			if (t.getType() == Derive.class) { // only unfold derivations
				if (empties && t.source.production != null && t.source.production.isEmpty()) {
					continue; // do later
				}
				if (layout && t.source.production != null && !t.source.production.lhs.layout
						   && t.target.production != null &&  t.target.production.lhs.layout) {
					toUnfold.add(t);
				}
				if (literals && t.target.production != null && t.target.production.lhs.literal) {
					toUnfold.add(t);
				}
			}
		}		
		unfoldTransitions(toUnfold);
		
		if (cftolex) {
			toUnfold = new ShareableHashSet<Transition>();			
			for (Transition t : transitions) {
				if (t.getType() == Derive.class) { // only unfold derivations
					if (cftolex && t.target.production != null && t.target.production.isCfToLex()) {
						toUnfold.add(t);
					}
				}
			}			
			unfoldTransitions(toUnfold);
			
			// unfold lexical lists
			/*toUnfold = new ShareableHashSet<Transition>();
			for (Transition t : transitions) {
				if (t.getType() == Derive.class) { // only unfold derivations
					if (literals && t.target.production != null
							&& t.target.production.lhs instanceof ListNonTerminal
							&& t.target.production.lhs.lexical) {
						toUnfold.add(t);
					}
				}
			}
			unfoldTransitions(toUnfold);*/
		}		
		
		if (empties) {
			toUnfold = new ShareableHashSet<Transition>();			
			for (Transition t : transitions) {
				if (t.target.production != null && t.target.production.isEmpty()) {
					toUnfold.add(t);
				}
			}			
			unfoldTransitions(toUnfold);
		}
		
		if (tails) {
			unfoldNonRecursiveTails();
		}
		
		if (stackdepth) {
			unfoldStackDepths(config.unfoldStackDepth, config.stackUnfoldingDepth);
		}
	}

	// pre: toUnfold may not include two transitions of which one can lead to the other
	private void unfoldTransitions(Set<Transition> toUnfold) {
		for (Transition t : toUnfold) {
			unfoldTransition(t);
		}		
		optimize(false);
	}

	private void unfoldTransition(Transition t) {
		if (t.target instanceof EndItem) {
			return;
		}
		
		//System.out.println("    Unfolding " + t.toStringExt());
		if (t.getType() == Derive.class) {
			Item i = t.source;
			i.derives.remove(t);
			transitions.remove(t);
			
			Item u = unfold(t.target, i);
			i.derives.add(addTransition(i, t.label, u));			
		} else {
			System.out.println(t.toStringExt());
			throw new RuntimeException("implement this!");
		}		
	}
	
	private Map<Pair<Item, Item>, Item> unfolds = new ShareableHashMap<Pair<Item, Item>, Item>();
	
	private Item unfold(Item i, Item from) {
		if (i instanceof EndItem) {
			return i;
		}
		
		Pair<Item, Item> p = new Pair<Item, Item>(i, from);
		Item clone = unfolds.get(p); 

		if (clone == null) {
			clone = i.unfold(from);
			unfolds.put(p, clone);
			items.add(clone);
			
			clone.derives = new ShareableHashSet<Transition>();
			clone.reduces = new ShareableHashSet<Transition>();
			
			for (Transition t : i.derives) {
				clone.derives.add(addTransition(clone, t.label, unfold(t.target, from)));
			}
			if (i.shift != null) {
				clone.shift = addTransition(clone, i.shift.label, unfold(i.shift.target, from));
			}
			// no reduces b/c we do this before calling addReduceTransitions()
		}
		return clone;
	}
	
	// pre: addReduceTransitions() not called yet
	private void unfoldNonRecursiveTails() {
		// a la mohri nederhof determinization:
		// every time a nonrecursive tail is used, make a copy for the specific used position.
		// this can be done before addReduceTransitions
		
		Relation<Item, Item> edges = new Relation<Item, Item>();
		for (Item i : allItems()) {
			for (Transition t : i.derives) {
				edges.add(i, t.target);
			}
			if (i.shift != null) {
				edges.add(i, i.shift.target);
			}
		}
		
		Relation<Item, Item> tc = new Relation<Item, Item>(Util.transitiveClosure2(edges.m));
		Set<Transition> old = new ShareableHashSet<Transition>();
		Set<Transition> neww = new ShareableHashSet<Transition>();
		for (Item i : allItems()) {
			if (tc.get(i).contains(i)) { // i is recursive
				for (Transition t : i.derives) {
					Item j = t.target;
					if (!tc.get(j).contains(j)) { // j is not recusive
						// unfold
						Item u = unfold(j, i);
						neww.add(addTransition(i, t.label, u));
						old.add(t);
					}
				}
			}
		}
		for (Transition t : old) {
			t.source.derives.remove(t);
		}
		transitions.removeAll(old);
		
		for (Transition t : neww) {
			t.source.derives.add(t);
		}
		transitions.addAll(neww);		
	}
	
	private void unfoldJoiningTails(final boolean unfoldOnlyLexicalTails) {
		// depth first traversal with in place unfolding if we reach an already 
		// visited node that is not part of the same strongly connected component
		
		final Set<Item> allItems = allItems();
		final Relation<Item, Item> scc = new Relation<Item, Item>(new NFASCC().getStronglyConnectedComponents(startItem));
		final Set<Item> visited = new ShareableHashSet<Item>();
		
		class A {
			public void visit(Transition t) {
				if (t.target == t.source) {
					return;
				}
				
				//System.out.println("Visiting " + t.toStringExt());
				Item origTarget = originalItem(t.target);
				
				if (visited.contains(origTarget)) {
					if (t.getType() == Derive.class) {
						Set<Item> targetScc = scc.get(origTarget);
						boolean unfold = true;
						if (targetScc.size() > 1) {
							// target is part of a cycle
							Item origSource = originalItem(t.source);
							if (targetScc == scc.get(origSource)) {
								// source and target are part of same scc's
								unfold = false;
							}
						}
						if (unfold && (!unfoldOnlyLexicalTails || 
								(t.target.production != null && t.target.production.lhs.lexical))) {
							unfoldTransition(t);
						}
					}
				} else {
					visited.add(origTarget);
					for (Transition t2 : t.target.derives) {
						visit(t2);
					}
					if (t.target.shift != null) {
						visit(t.target.shift);
					}
				}
			}
			
			private Item originalItem(Item i) {
				Item origItem = i;
				while (origItem != null) {
					if (allItems.contains(origItem)) {
						break;
					} else {
						origItem = origItem.unfoldedFrom;
					}
				}
				return origItem;
			}
		};		
		A a = new A();
		
		for (Transition t : startItem.derives) {
			a.visit(t);
		}
		if (startItem.shift != null) {
			a.visit(startItem.shift);
		}
	}
	
	public void unfoldStronglyConnectedComponents() {
		Relation<Item, Item> edges = new Relation<Item, Item>();
		for (Item i : allItems()) {
			for (Transition t : i.derives) {
				edges.add(i, t.target);
			}
			if (i.shift != null) {
				edges.add(i, i.shift.target);
			}
		}

		Relation<Item, Item> scc = new Relation<Item, Item>(new NFASCC().getStronglyConnectedComponents(startItem));
		
		unfoldSCC(startItem, null, scc.m, scc.get(startItem));		
	}
	
	private Item unfoldSCC(Item i, Item from, Map<Item, Set<Item>> scc, Set<Item> current) {		
		if (i instanceof EndItem) {
			return i;
		}
		
		Pair<Item, Item> p = new Pair<Item, Item>(i, from);
		Item clone = unfolds.get(p);
		if (clone != null) {
			return clone;
		}

		if (from == null) {
			clone = i;
		} else {
			clone = i.unfold(from);
			items.add(clone);
		}
		unfolds.put(p, clone);
		
		ShareableHashSet<Transition> newDerives = new ShareableHashSet<Transition>();
		transitions.removeAll(i.derives);
		for (Transition t : i.derives) {
			Set<Item> targetScc = scc.get(t.target);
			Item newTo;
			if (targetScc != current) {
				newTo = unfoldSCC(t.target, clone, scc, targetScc);
			} else {
				newTo = unfoldSCC(t.target, from, scc, current);
			}
			newDerives.add(addTransition(clone, t.label, newTo));
		}
		
		Transition newShift = null;
		if (i.shift != null) {
			Transition t = i.shift;
			transitions.remove(t);
			Set<Item> targetScc = scc.get(t.target);
			Item newTo;
			if (targetScc != current) {
				newTo = unfoldSCC(t.target, clone, scc, targetScc);
			} else {
				newTo = unfoldSCC(t.target, from, scc, current);
			}
			newShift = addTransition(clone, t.label, newTo);
		}
		
		clone.derives = newDerives;
		clone.shift = newShift;
		
		return clone;
	}

	//-------------------------------------------------------------------------

	private int maxStackDepth = 0;
	private boolean onlyDepth;
	private Map<Pair<Item, Object>, Item> stackDepthItems = new ShareableHashMap<Pair<Item, Object>, Item>();
	private Queue<Pair<Item, Object>> stackDepthItemsTodo = new Queue<Pair<Item,Object>>();
	private void unfoldStackDepths(boolean onlyDepth, int maxDepth) {
		maxStackDepth = maxDepth;
		this.onlyDepth = onlyDepth;
		if (onlyDepth) {
			stackDepthItemsTodo.add(new Pair<Item, Object>(startItem, 0));
		} else {
			stackDepthItemsTodo.add(new Pair<Item, Object>(startItem, new StackFragment(maxDepth)));
		}
		items = new ShareableHashSet<Item>();
		transitions = new ShareableHashSet<Transition>();

		startItem = new StartItem(itemID++, startItem.startSymbol);
		endItem = new EndItem(itemID++);
		
		while (stackDepthItemsTodo.size() > 0) {
			Pair<Item, Object> p = stackDepthItemsTodo.pop();
			unfoldStackDepth(p.a, p.b);
		}
	}

	private void unfoldStackDepth(Item item, Object depth) {
		Item clone = null;
		
		if (item instanceof StartItem) {
			clone = startItem;
		} else {
			clone = getItemAtStackDepth(item, depth);
		}
		
		Object nextDepth = null;
		if (onlyDepth) {
			nextDepth = (((Integer)depth + 1) % maxStackDepth);
		} else {
			nextDepth = new StackFragment((StackFragment) depth, item);
		}
		
		for (Transition t : item.derives) {
			Transition nt = addTransition(clone, t.label, getItemAtStackDepth(t.target, nextDepth)); 
			clone.derives.add(nt);
		}
		
		if (item.shift != null) {
			Transition nt = addTransition(clone, item.shift.label, getItemAtStackDepth(item.shift.target, depth));
			clone.shift = nt;
		}
	}
	
	private Item getItemAtStackDepth(Item i, Object depth) {		
		if (i instanceof EndItem) {
			return endItem;
		}
		
		Pair<Item, Object> p = new Pair<Item, Object>(i, depth);
		Item j = stackDepthItems.get(p);
		if (j == null) {
			try{
				j = (Item) i.clone();
			} catch (CloneNotSupportedException e) {}
			j.stackInfo = depth;
			
			stackDepthItems.put(p, j);
			stackDepthItemsTodo.add(p);
			items.add(j);
		}
		return j;
	}
	
	//-------------------------------------------------------------------------
	
	// pre: can only be called after addReduceTransitions and all unfolding functions
	public void propagateFollowRestrictions() {
	
		if (!shiftsInSets) {
			moveShiftsToSets();
		}
		
		final Map<Pair<Item, FollowRestrictions>, Item> followProp = new ShareableHashMap<Pair<Item, FollowRestrictions>, Item>();
		final Queue<Item> followPropTodo = new Queue<Item>();
		
		// save up new items so that we do not mess up existing nfa (needed for FirstKGenerator) 
		final Set<Item> newItems = new ShareableHashSet<Item>();
		final StartItem newStartItem = new StartItem(itemID++, startItem.startSymbol);
		final EndItem newEndItem = new EndItem(itemID++);
		newStartItem.unfoldedFrom = startItem;
		newStartItem.withFollow = true;
		newEndItem.unfoldedFrom = endItem;
		newEndItem.withFollow = true;
		final Relation<Item, Transition> ntShifts = new Relation<Item, Transition>();
		
		// some inner functions for convenience
		class A {
			Item getItemWithFollowRestrictions(Item i, FollowRestrictions f) {		
				if (i instanceof EndItem) {
					return newEndItem;
				}
				
				if (i instanceof StartItem) {
					return newStartItem;
				}
				
				if ((reversed ? i.atEnd() : i.atBegin()) && i.production.reject) {
					f = null; // do not propagate into reject parts
				}
				
				Pair<Item, FollowRestrictions> p = new Pair<Item, FollowRestrictions>(i, f);
				Item j = followProp.get(p);
				if (j == null) {
					try{
						j = (Item) i.clone();
					} catch (CloneNotSupportedException e) {}
					
					j.unfoldedFrom = i;
					j.followRestrictions = f;
					j.withFollow = true;
					
					followProp.put(p, j);
					followPropTodo.add(j);
					newItems.add(j);
				}
				return j;
			}
			
			private void linkDeriveShiftReduce(Transition derive, Transition shift, Transition reduce) {
				//System.out.println("3: " + reduce.toStringExt());
				reduce.reverse.add(derive);
				derive.reverse.add(reduce);
				
				//if (!i.shifts.contains(shift)) System.out.println("4: " + shift.toStringExt());
				
				if (shift.derivesReduces == null) {
					shift.derivesReduces = new ShareableHashSet<Pair<Transition,Transition>>();
				}
				shift.derivesReduces.add(new Pair<Transition, Transition>(derive, reduce));
				
				if (reduce.shifts == null) {
					reduce.shifts = new ShareableHashSet<Transition>();
				}
				reduce.shifts.add(shift);
			}
		}
		A a = new A();
		
		if (reversed) {
			followPropTodo.add(newEndItem);
			for (Transition t : endItem.shifts) {
				ntShifts.add(newEndItem, t);
			}
		} else {
			followPropTodo.add(newStartItem);
			for (Transition t : startItem.shifts) {
				ntShifts.add(newStartItem, t);
			}			
		}
		
		transitions = new ShareableHashSet<Transition>();
		int oldSize = -1;
		int newSize = transitions.size();
		
		while (oldSize != newSize) {
			
			// first derive and shift all new items
			while (followPropTodo.size() > 0) {
				Item i = followPropTodo.pop();
				Item old = i.unfoldedFrom;
				FollowRestrictions f = i.followRestrictions;
				
				// propagate over derives
				for (Transition t : old.derives) {
					Transition nt = addTransition(i, t.label, a.getItemWithFollowRestrictions(t.target, f)); 
					//if (!i.derives.contains(nt)) System.out.println("1: " + nt.toStringExt());
					i.derives.add(nt);
				}
				
				// propagate over terminal or charclass shifts
				if (old.shifts != null) {
					i.shifts = new ShareableHashSet<Transition>();
					for (Transition shift : old.shifts) {
						Symbol s = shift.label;
						
						if (s instanceof NonTerminal) {
							ntShifts.add(i, shift);
						} else {// character class shifts
							if (f == null) {
								Transition nt = addTransition(i, s,	a.getItemWithFollowRestrictions(shift.target, null));
								i.shifts.add(nt);
							} else if (f.canShift(s)) {
								Pair<CharacterClass, ShareableHashMap<CharacterClass, FollowRestrictions>> p = f.getShiftPossibilities(s);
								if (p.a.size() > 0) {
									Transition nt = addTransition(i, p.a, a.getItemWithFollowRestrictions(shift.target, null));
									i.shifts.add(nt);
								}
								for (Entry<CharacterClass, FollowRestrictions> e : p.b) {
									Transition nt = addTransition(i, e.getKey(), a.getItemWithFollowRestrictions(shift.target, e.getValue()));
									i.shifts.add(nt);
								}
							}
						}
					}
				}
			}
			
			// then reconnect reductions
			for (Pair<Item, Transition> p : ntShifts) {
				Item i = p.a;
				Transition shift = p.b;

				if (followPropTodo.contains(i)) {
					continue;
				}
				
				NonTerminal n = (NonTerminal) shift.label;
				
				for (Transition derive : i.derives) {
					Set<Item> ends = derive.target.followShiftSets(reversed);
					for (Item e : ends) {
						if (e.production.reject) {
							// do not let follow restrictions propagate over reduces of reject productions
							// instead, add reduce transition of reject productions to already existing NT shift targets 
							for (Transition s : i.shifts) {
								Transition reduce = addTransition(e, e.production.reduction, s.target);
								e.reduces.add(reduce);
								a.linkDeriveShiftReduce(derive, s, reduce);
							}
							continue;
						}
						
						FollowRestrictions f = e.followRestrictions;
						if (f == null) {
							f = n.followRestrictions;
						} else {
							f = f.getNextAfterReduce(n);
						}
						
						if (f != null && f.mustFollow() && (shift.target instanceof StartItem || shift.target instanceof EndItem)) {
							continue;
						}
						
						Item next = a.getItemWithFollowRestrictions(shift.target, f);
						
						Transition reduce = addTransition(e, e.production.reduction, next);
						e.reduces.add(reduce);
						
						Transition s;
						if (e.production.isEmpty()) {
							s = addEmptyTransition(i, n, next);
						} else {
							s = addTransition(i, n, next);
						}
						i.shifts.add(s); // may contain same object already
						
						a.linkDeriveShiftReduce(derive, s, reduce);
					}
				}
			}
			
			//System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++");
			
			oldSize = newSize;
			newSize = transitions.size();
		}
		
		items = newItems;
		startItem = newStartItem;
		endItem = newEndItem;
		followRestrictionsPropagated = true;
	}

	public void connectShiftAndReduces() {
		for (Item i : allItems()) {			
			if (!((reversed ? i.getPrevSymbol() : i.getNextSymbol()) instanceof NonTerminal)) {
				continue;
			}
			
			Relation<Item, Transition> shifts = new Relation<Item, Transition>();
			for (Transition s : i.shifts) {
				shifts.add(s.target, s);
			}
			
			for (Transition d : i.derives) {
				for (Transition r : d.reverse) {
					boolean set = false;
					for (Transition s : shifts.get(r.target)) {
						if (d.target.production.isEmpty() == s.empty) {
							if (s.derivesReduces == null) {
								s.derivesReduces = new ShareableHashSet<Pair<Transition,Transition>>();
							}
							s.derivesReduces.add(new Pair<Transition, Transition>(d, r));
							
							if (r.shifts == null) {
								r.shifts = new ShareableHashSet<Transition>();
							}
							r.shifts.add(s);
							
							if (set) {
								throw new RuntimeException("Why is this?");
							} else {
								set = true;
							}
						}
					}
				}
			}
		}
	}
		
	public NFA simplify() {
		final NFA n = new LR0NFA(grammar);
		n.items = new ShareableHashSet<Item>();
		
		class A {
			Map<Pair<Production, Integer>, Item> canonicalItems = new ShareableHashMap<Pair<Production,Integer>, Item>();
			
			Item getCanonicalItem(Item i) {
				if (i instanceof StartItem) {
					return n.startItem;
				} else if (i instanceof EndItem) {
					return n.endItem;
				} else {
					Pair<Production, Integer> p = i.getCanonicalItem();
					Item c = canonicalItems.get(p);
					if (c == null) {
						c = new Item(i.production, i.index, itemID++);
						canonicalItems.put(p, c);
						n.items.add(c);
					}
					return c;
				}
			}
			
			void simplifyItem(Item i) {
				Item c = getCanonicalItem(i);
				for (Transition t : i.derives) {
					c.derives.add(n.addTransition(c, t.label, getCanonicalItem(t.target)));
				}
				if (i.shifts != null) {
					//c.shifts = new ShareableHashSet<Transition>();
					for (Transition t : i.shifts) {
						Transition nt = n.addTransition(c, t.label, getCanonicalItem(t.target));
						if (c.shift != null && c.shift != nt) { // due to follow restriction propagation shifted character classes can be reduced
							nt = n.addTransition(c, i.getNextSymbol(), getCanonicalItem(t.target));
						}
						c.shift = nt;
					}
				}
				for (Transition t : i.reduces) {
					c.reduces.add(n.addTransition(c, t.label, getCanonicalItem(t.target)));
				}
			}
		};
		A a = new A();
		
		for (Item i : items) {
			a.simplifyItem(i);		
		}
		a.simplifyItem(startItem);
		a.simplifyItem(endItem);
		
		return n;
	}
	
	public NFA simplifyWithFollowRestrictions() {
		final NFA n = new LR0NFA(grammar);
		n.items = new ShareableHashSet<Item>();
		
		class A {
			Map<Triple<Production, Integer, FollowRestrictions>, Item> canonicalItems = new ShareableHashMap<Triple<Production,Integer,FollowRestrictions>, Item>();
			Map<Transition, Transition> newTrans = new ShareableHashMap<Transition, Transition>();
			
			Item getCanonicalItem(Item i) {
				if (i instanceof StartItem) {
					return n.startItem;
				} else if (i instanceof EndItem) {
					return n.endItem;
				} else {
					Triple<Production, Integer, FollowRestrictions> p = new Triple<Production, Integer, FollowRestrictions>(i.production, i.index, i.followRestrictions);
					Item c = canonicalItems.get(p);
					if (c == null) {
						c = new Item(i.production, i.index, itemID++);
						c.followRestrictions = i.followRestrictions;
						canonicalItems.put(p, c);
						n.items.add(c);
					}
					return c;
				}
			}
			
			Transition simplify(Transition t) {
				Transition nt = newTrans.get(t);
				if (nt == null) {
					if (t.empty) {
						nt = n.addEmptyTransition(getCanonicalItem(t.source), t.label, getCanonicalItem(t.target));
					} else {
						nt = n.addTransition(getCanonicalItem(t.source), t.label, getCanonicalItem(t.target));
					}
					newTrans.put(t, nt);
					
					for (Transition r : t.reverse) {
						nt.reverse.add(simplify(r));
					}
					
					if (t.derivesReduces != null) {
						nt.derivesReduces = new ShareableHashSet<Pair<Transition,Transition>>();
						for (Pair<Transition, Transition> p : t.derivesReduces) {
							nt.derivesReduces.add(new Pair<Transition, Transition>(simplify(p.a), simplify(p.b)));
						}
					}
					
					if (t.shifts != null) {
						nt.shifts = new ShareableHashSet<Transition>();
						for (Transition s : t.shifts) {
							nt.shifts.add(simplify(s));
						}
					}
				}
				return nt;
			}
			
			void simplifyItem(Item i) {
				Item c = getCanonicalItem(i);
				for (Transition t : i.derives) {
					c.derives.add(simplify(t));
				}
				if (i.shifts != null) {
					c.shifts = new ShareableHashSet<Transition>();
					for (Transition t : i.shifts) {
						c.shifts.add(simplify(t));
					}
				}
				for (Transition t : i.reduces) {
					c.reduces.add(simplify(t));
				}
			}
		};
		A a = new A();
		
		for (Item i : items) {
			a.simplifyItem(i);		
		}
		a.simplifyItem(startItem);
		a.simplifyItem(endItem);
		
		return n;
	}
	
	//-------------------------------------------------------------------------

	private void removeItems(Set<Item> s) {
		for (Item i : s) {
			//System.out.println("Removing " + i);
			
			items.remove(i);
			
			Set<Transition> remove = new ShareableHashSet<Transition>();
			remove.addAll(i.derives);
			remove.addAll(i.reduces);
			if (i.shift != null) {
				remove.add(i.shift);
			}
			if (i.shifts != null) {
				remove.addAll(i.shifts);
			}
			removeTransitions(remove);
		}

		for (Item i : allItems()) {
			Set<Transition> trash = new ShareableHashSet<Transition>();			
			for (Transition t : i.derives) {
				if (s.contains(t.target)) {
					trash.add(t);
				}
			}
			removeTransitions(trash);
	
			trash = new ShareableHashSet<Transition>();			
			for (Transition t : i.reduces) {
				if (s.contains(t.target)) {
					trash.add(t);
				}
			}
			removeTransitions(trash);
	
			if (i.shifts != null) {
				trash = new ShareableHashSet<Transition>();			
				for (Transition t : i.shifts) {
					if (s.contains(t.target)) {
						trash.add(t);
					}
				}
				removeTransitions(trash);
			}
			
			if (i.shift != null && s.contains(i.shift.target)) {
				trash = new ShareableHashSet<Transition>();
				trash.add(i.shift);
				removeTransitions(trash);
			}
		}
	}
	
	private void removeTransitions(Set<Transition> trans) {		
		for (Transition t : trans) {
			if (t.getType() == Derive.class) {
				t.source.derives.remove(t);
			} else if (t.getType() == Reduce.class) {
				t.source.reduces.remove(t);
				
				if (t.shifts != null) {
					for (Transition shift : t.shifts) {
						Set<Pair<Transition, Transition>> remove = new ShareableHashSet<Pair<Transition,Transition>>();
						for (Pair<Transition, Transition> p : shift.derivesReduces) {
							if (p.b == t) {
								remove.add(p);
							}
						}
						shift.derivesReduces.removeAll(remove);
					}
				}
			} else {
				if (t.source.shift == t) {
					t.source.shift = null;
				}
				if (t.source.shifts != null) {
					t.source.shifts.remove(t);
				}
				
				if (t.label instanceof NonTerminal && t.derivesReduces != null) {
					for (Pair<Transition, Transition> p : t.derivesReduces) {
						p.b.shifts.remove(t);
					}
				}
			}
			
			transitions.remove(t);
		}
	}
	
	/*
	 * Create two special start and end items for a specific nonterminal,
	 * as were this the start symbol of this nfa's grammar.
	 * Doesn't change existing structure, so reduces can still point to
	 * items of productions unreachable from nt.
	 */
	public Pair<Item, Item> getStartAndEndItem(NonTerminal nt) {
		if (nt == grammar.startSymbol) {
			return new Pair<Item, Item>(startItem, endItem);
		}
		
		Item start = new StartItem(0, nt);
		Item end = new EndItem(1);
		
		start.shift = new Transition(start, nt, end);
		for (Item i : items) {
			if (i.atBegin() && i.production != null && !i.production.usedForReject) {
				Production p = i.production;
				if (p != null && p.lhs == nt) {
					start.derives.add(new Transition(start, p.derivation, i));
				}
			}
		}
		
		return new Pair<Item, Item>(start, end);
	}
	
	// non-reflexive transitive closure of derive
	public Set<Item> getClosure(Item item) {
		Set<Item> c;
		if (closures.inDomain(item)) {
			c = closures.get(item);
		} else {
			c = closure(item);
			closures.put(item, c);
		}
		return c;
	}
	
	// non-reflexive transitive closure of derive
	private Set<Item> closure(Item item) {
		Set<Item> c = new ShareableHashSet<Item>();
		Set<Item> prevAdd = new ShareableHashSet<Item>();
		prevAdd.add(item);
		while (prevAdd.size() > 0) {
			ShareableHashSet<Item> add = new ShareableHashSet<Item>();
			for (Item i : prevAdd) {
				for (Transition t : i.derives) {
					Item to = t.target;
					if (to == item) {
						c.add(item);
					} else if (!add.contains(to) && !c.contains(to)) {
						add.unsafeAdd(to);
					}
				}
			}
			c.addAll(add);
			prevAdd = add;
		}
		return c;
	}
	
	public void filter(Set<Item> usedItems, boolean filterTransitions) {
		
		if (filterTransitions) {
			Set<Transition> removeTrans = new ShareableHashSet<Transition>();
			Set<Transition> usedTrans = new ShareableHashSet<Transition>();
			for (Transition t : transitions) {
				if (t.reverse.size() > 0) { // only derives and reduces
					if (!t.used) {
						removeTrans.add(t);
					} else {
						boolean noReverseUsed = true;
						for (Transition r : t.reverse) {
							if (r.used) {
								noReverseUsed = false;
								break;
							}
						}
						if (noReverseUsed) {
							removeTrans.add(t);
							//System.out.println(t.toStringExt());
						} else {
							usedTrans.add(t);
						}
					}
				}
			}
			removeTrans.removeAll(usedTrans);
			removeTransitions(removeTrans);
		}
				
		if (clusters == null) {
			Set<Item> remove = allItems();
			remove.removeAll(usedItems);
			removeItems(remove);
			optimize(true); // now removes items of incomplete clusters
		} else {
			/*
			// XXX some items can appear in multiple clusters (for instance with LALR)
			// therefore we need to remove only the items not used in any cluster

			Set<Item> remove = new ShareableHashSet<Item>();
			Set<Item> used = new ShareableHashSet<Item>();
			for (Set<Item> c : clusters) {
				if (usedItems.containsAll(c)) {
					used.addAll(c);
				} else {
					remove.addAll(c);
					//System.out.println("rc: " + c);				
				}
			}
			
			// after a call to optimize, certain clusters might be removed partially
			// again, remove incompletely used clusters
			// taking into account items used in multiple clusters
			
			int oldSize = 0;
			while (items.size() != oldSize) {
				oldSize = items.size();
				remove.removeAll(used);
				removeItems(remove);
				
				optimize(true);
				
				remove = new ShareableHashSet<Item>();
				used = new ShareableHashSet<Item>();
				for (Set<Item> c : clusters) {
					int contained = 0;
					for (Item i : c) {
						if (items.contains(i)) {
							++contained;
						}
					}
					if (contained > 0) { // avoid removing the same clusters over and over again
						if (contained == c.size()) {
							used.addAll(c);
						} else {
							remove.addAll(c);
							//System.out.println("rc: " + c);
						}
					}
				}
			}		
			
			computeClusters();
			verify(); */
		}
	}
	
	private void computeDistances() {
		endItem.distanceToEnd = 0;
		boolean updated = true;
		Set<Item> allItems = allItems();
		
		while (updated) {
			updated = false;
			for (Item i : allItems) {
				int min = i.distanceToEnd - 1;
				for (Transition t : i.derives) {
					if (t.target.distanceToEnd < min) {
						min = t.target.distanceToEnd;
					}
				}
				for (Transition t : i.reduces) {
					if (t.target.distanceToEnd < min) {
						min = t.target.distanceToEnd;
					}
				}
				if (i.shift != null) {
					if (i.shift.target.distanceToEnd < min) {
						min = i.shift.target.distanceToEnd;
					}
				}
				if (i.shifts != null) {
					for (Transition t : i.shifts) {
						if (t.target.distanceToEnd < min) {
							min = t.target.distanceToEnd;
						}
					}
				}
				++min;
				if (min < i.distanceToEnd) {
					i.distanceToEnd = min;
					updated = true;
				}
			}
		}

		for (Item i : allItems) {
			if (i.distanceToEnd > maxDistanceToEnd) {
				maxDistanceToEnd = i.distanceToEnd;
			}
		}		
	}
	
	public void moveShiftsToSets() {
		for (Item i : allItems()) {
			if (i.shift != null) {
				i.shifts = new ShareableHashSet<Transition>();
				i.shifts.add(i.shift);
				i.shift.derivesReduces = new ShareableHashSet<Pair<Transition,Transition>>();
				for (Transition d : i.derives) {
					for (Transition r : d.reverse) {
						r.shifts = new ShareableHashSet<Transition>();
						r.shifts.add(i.shift);
						i.shift.derivesReduces.add(new Pair<Transition, Transition>(d, r));
					}
				}
				i.shift = null;
			}			
		}
		shiftsInSets = true;
	}

	public void reconstruct() {
		
		// cache reconstructed items per production
		class Reconstructor {
			Map<Production, Pair<Item, Item>> reconstructed = new ShareableHashMap<Production, Pair<Item,Item>>();
			
			public Transition reconstruct(Set<Transition> shifts) {
				// pick the transition with the shortest minimalstring and terminalize it
				Transition shift = null;
				Symbol[] str = null;
				for (Transition t : shifts) {
					Symbol[] tstr = minimalStrings.get(t);
					if (tstr == null) {
						System.out.println("No minimal string for " + t.toStringExt());
					}
					if (shift == null || tstr.length < str.length) {
						shift = t;
						str = tstr;
					}
				}
										
				//System.out.println(shift.toStringExt() + "  ==>  " + str);
				
				// create dummy production
				Production p = grammar.newProduction((NonTerminal) shift.label);
				for (Symbol s : str) {
					p.addSymbol(s);
				}
				
				Production existing = grammar.getProduction(p.toString(), true);
				if (existing != null) {
					p = existing;
				} else {
					p.reconstructed = true;
					p.reachable = true;
					grammar.addProduction(p);
				}
				
				reconstruct(p, shift);
				
				return shift;
			}
			
			private void reconstruct(Production p, Transition shift) {
				Pair<Item, Item> firstlast = reconstructed.get(p);
				if (firstlast == null) {
					final int len = p.getLength();
					
					//System.out.println(e);
					
					// create new items
					Item[] its = new Item[len + 1];
					for (int j = 0; j <= len; ++j) {
						its[j] = new Item(p, j, itemID++);
						items.add(its[j]);
					}
					
					// then link them together with shifts
					for (int j = 0; j < len; ++j) {
						its[j].shifts = new ShareableHashSet<Transition>();
						its[j].shifts.add(addTransition(its[j], p.getSymbolAt(j), its[j + 1]));
					}

					firstlast = new Pair<Item, Item>(its[0], its[len]);
					reconstructed.put(p, firstlast);
				}
				
				Transition derive = addTransition(shift.source, p.derivation, firstlast.a);
				Transition reduce = addTransition(firstlast.b, p.reduction, shift.target);
				shift.source.derives.add(derive);
				firstlast.b.reduces.add(reduce);
				
				reduce.shifts = new ShareableHashSet<Transition>();
				reduce.shifts.add(shift);

				//shift.derivesReduces = new ShareableHashSet<Pair<Transition,Transition>>(); // do not overwrite!
				shift.derivesReduces.add(new Pair<Transition, Transition>(derive, reduce));				
			}
		};
		Reconstructor reconstructor = new Reconstructor();
		
		// first reconstruct nonterminal shifts without derive/reduces
		
		Set<Transition> remove = new ShareableHashSet<Transition>();
		
		for (Item i : items) {
			if (i.getNextSymbol() instanceof NonTerminal) {
				
				Relation<Item, Transition> deadShifts = new Relation<Item, Transition>();
				Set<Item> reachable = new ShareableHashSet<Item>();
				
				for (Transition t : i.shifts) {
					boolean alive = false;
					for (Pair<Transition, Transition> dr : t.derivesReduces) {
						if (transitions.contains(dr.a) && transitions.contains(dr.b)) {
							alive = true;
							break;
						}
					}
					
					if (alive) {
						reachable.add(t.target);
					} else {
						deadShifts.add(t.target, t);
					}
				}

				for (Entry<Item, Set<Transition>> e : deadShifts.m) {
					Transition shift = null;
					if (!reachable.contains(e.getKey())) {
						shift = reconstructor.reconstruct(e.getValue());
					}
					
					for (Transition t : e.getValue()) {
						if (t != shift) {
							remove.add(t);
						}
					}
				}
			}
		}
		removeTransitions(remove);
		
		//printSize("Intermediate reconstructed NFA");
		
		// then reconstruct unproductive nonterminal shifts
		
		// first gather all initially productive shifts (terminal and character-class)
		final Set<Transition> productive = new ShareableHashSet<Transition>();
		ShareableHashSet<Transition> unproductive = new ShareableHashSet<Transition>();
		for (Item i : items) {
			if (i.production != null && !i.production.reject && i.shifts != null) {
				for (Transition s : i.shifts) {
					if (s.label instanceof NonTerminal) {
						unproductive.add(s);
					} else {
						productive.add(s);
					}
				}
			}
		}
		
		// then search for unproductive nonterminal shifts
		// if we find one, reconstruct it immediately, and search again
		
		class A {
			Stack<Item> stack = new Queue<Item>(64);
			Set<Item> reachProductive;
			
			Set<Item> productiveEnds(Item i) { // returns all items reachable from i by productive shifts
				reachProductive = new ShareableHashSet<Item>();
				traverse(i);
				return reachProductive;
			}
			
			void traverse(Item i) {
				stack.add(i);
				if (i.shifts == null) {
					reachProductive.add(i);
				} else {
					for (Transition t : i.shifts) {
						if (productive.contains(t)) {
							traverse(t.target);
						}
					}
				}
				stack.pop();
			}				
		};
		A a = new A();	
		
		while (unproductive.size() > 0) {
			// propagate productive
			int prevSize = 0;
			while (prevSize != productive.size()) {
				prevSize = productive.size();
				Set<Transition> foundProductive = new ShareableHashSet<Transition>();
				
				for (Transition s : unproductive) {
					Relation<Transition, Transition> dr = new Relation<Transition, Transition>(s.derivesReduces);
					search: for (Entry<Transition, Set<Transition>> e : dr.m) {
						Set<Item> pe = a.productiveEnds(e.getKey().target);
						if (pe.size() > 0) {
							for (Transition r : e.getValue()) {
								if (pe.contains(r.source)) {
									// s is productive
									foundProductive.add(s);
									productive.add(s);
									break search;
								}
							}
						}
					}
				}
				
				unproductive.removeAll(foundProductive);			
			}
			
			if (unproductive.size() > 0) {
				// pick the transition that is the deepest down in the dependency graph
				Transition shift = unproductive.getOne();
				for (Transition t : unproductive) {
					if (t.source.distanceToEnd > shift.source.distanceToEnd) {
						shift = t;
					}
				}

				// find other shifts that have the same source and target
				Set<Transition> same = new ShareableHashSet<Transition>();
				same.add(shift);
				for (Transition t : unproductive) {
					if (t.source == shift.source && t.target == shift.target) {
						same.add(t);
					}
				}
				
				shift = reconstructor.reconstruct(same);
				unproductive.removeAll(same);
				productive.add(shift);
			}
		}
	}

	public Set<Production> getUsedProductions() {
		Set<Production> used = new ShareableHashSet<Production>();
		for (Item i : items) {
			if (i.production != null) {
				used.add(i.production);
			}
		}
		return used;
	}
	
	public void verify() {
		for (Item i1 : items) {
			for (Item i2 : items) {
				if (i1.equals(i2)) {
					if (i1 != i2) {
						if (i1.hashCode() != i2.hashCode()) {
							throw new RuntimeException("Error in Item.hashCode()");
						}
						throw new RuntimeException("Error in Item.equal()");
					}
				}
			}
		}
		
		for (Transition t : transitions) {
			for (Transition r : t.reverse) {
				if (!r.reverse.contains(t)) {
					throw new RuntimeException("Error in Transition.reverse\n" + t.toStringExt() + "\n" + r.toStringExt());
				}
			}
		}
		
		int numTrans = 0;
		Set<Transition> ts = new ShareableHashSet<Transition>();
		for (Item i : allItems()) {
			numTrans += i.derives.size();
			numTrans += i.reduces.size();
			if (i.shift != null) numTrans++;
			if (i.shifts != null) numTrans += i.shifts.size();
			
			ts.addAll(i.derives);
			ts.addAll(i.reduces);
			if (i.shift != null) ts.add(i.shift);
			if (i.shifts != null) ts.addAll(i.shifts);
		}
		
		if (numTrans != transitions.size()) {
			System.out.println("numtrans " + numTrans + ", " + transitions.size() + ", " + ts.size());
			Set<Transition> diff = new ShareableHashSet<Transition>();
			diff.addAll(transitions);
			diff.removeAll(ts);
			for (Transition t : diff) {
				System.out.println("" + t.source + " " + t.label + " " + t.target);
			}
			
			throw new RuntimeException("Error in transitions");
		}
		
		/*int i = 0;
		for (Production p : grammar.productions) {
			i += p.items.size();
		}
	
		if (i != 0 && i != items.size()) {
			throw new RuntimeException("Error with Item and Production");
		}*/
		
		Set<Item> inClusters = new ShareableHashSet<Item>();
		if (clusters != null) {
			for (Set<Item> c : clusters) {
				inClusters.addAll(c);
			}
			
			if (inClusters.size() != items.size()) {
				if (inClusters.contains(startItem)) {
					System.out.println("contains startItem");
				}
				if (inClusters.contains(endItem)) {
					System.out.println("contains endItem");
				}
				
				Set<Item> diff = new ShareableHashSet<Item>();
				diff.addAll(items);
				for (Set<Item> c : clusters) {
					diff.removeAll(c);
				}
				
				System.out.println(diff);
				
				for (Set<Item> c : clusters) {
					boolean one = false;
					for (Item i : c) {
						Set<Item> f = new ShareableHashSet<Item>();
						i.followShifts(f);
						if (f.equals(c)) {
							one = true;
							break;
						}
					}
					if (!one) {
						System.out.println("Broken cluster: " + c);
					}
				}
							
				String s = "\n";
				for (Item i : diff) {
					if (!items.contains(i)) {
						s += "???";
					}
						
					s += i.toString() + "\n";
					
					boolean connected = false;
					for (Transition t : transitions) {
						if (t.target == i) {
							s += "con: " + t.source + "\n";
							connected = true;
							break;
						}
					}
					if (!connected) {
						s += "dangling!\n";
					}
					
					Item end = i.followShifts();
					if (end.reduces.size() == 0) {
						s += "dead end: " + end + "\n";
					} else {
						Item begin = end.reduces.iterator().next().reverse.iterator().next().target;
						Set<Item> c = new ShareableHashSet<Item>(); 
						begin.followShifts(c);
						
						s += "cluster: " + c.toString() + "\n";
					}
				}
				
				throw new RuntimeException("Error with clusters " + inClusters.size() + " != " + items.size() + " : " + s);
			}
		}
				
		if (followRestrictionsPropagated) {
			for (Item i : items) {
				for (Transition t : i.reduces) {
					for (Transition shift : t.shifts) {
						boolean found = false;
						for (Pair<Transition, Transition> p : shift.derivesReduces) {
							if (p.b == t) {
								found = true;
								break;
							}							
						}
						if (!found) {
							throw new RuntimeException("Unconnected shift (no reduces): " + t.toStringExt());
						}
					}
				}
				if (i.shifts != null) {
					for (Transition t : i.shifts) {
						if (t.label instanceof NonTerminal) {
							for (Pair<Transition, Transition> p : t.derivesReduces) {
								if (transitions.contains(p.a) && transitions.contains(p.b) && !p.b.shifts.contains(t)) {
									throw new RuntimeException("Reduce with incorrect shifts: \n" + p.b.toStringExt() + "\n (\n" + p.b.shifts + "\ndoes not contain\n" + t.toStringExt() + "\n)");
								}
							}
						}
					}
				}
			}
		}
	}

	public void printSize(String prefix, IAmbiDexterMonitor monitor) {
		monitor.println(prefix + " size: " + (items.size() + 2) + " states, " + transitions.size() + " transitions");
		if (AmbiDexterConfig.verbose) {
			int derives = 0;
			int shifts = 0;
			int reduces = 0;
			for (Item i : allItems()) {
				derives += i.derives.size();
				reduces += i.reduces.size();
				if (i.shift != null) {
					++shifts;
				}
				if (i.shifts != null) {
					shifts += i.shifts.size();
				}
			}
			monitor.println("Derives: " + derives + ", shifts: " + shifts + ", reduces: " + reduces);
		}
	}

	public void toDot(String filename) {
		System.out.println("Writing " + filename);
		BufferedWriter w = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			w = new BufferedWriter(fstream);
		
			w.write("digraph G {\n");
			
			// nodes
			for (Item i : allItems()) {
				w.write("" + i.id + " [label=" + Util.dotId(i));
				if (i instanceof StartItem) {
					w.write(",style=filled,fillcolor=green");
				}
				if (i instanceof EndItem) {
					w.write(",style=filled,fillcolor=orange");
				}
				if (i.production != null && i.production.usedForReject) {
					w.write(",color=red");
				}
				w.write("];\n");
			}
			
			// edges
			for (Transition t : transitions) {
				w.write("" + t.source.id + " -> " + t.target.id + " [label=" + Util.dotId(t.label) + 
						(t.target.production != null && t.target.production.usedForReject ? ", color=red" : "") +
						(t.empty ? ", color=blue" : "") + "];\n");
			}
			
			w.write("}\n");
			
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (w != null) {
				try {
					w.close();
				} catch (IOException e) { }
			}
		}
	}
	
	public void setTransitionsUnused() {
		for (Transition t : transitions) {
			t.used = false;
		}
	}

	public int getUsedTransitions() {
		int used = 0;
		for (Transition t : transitions) {
			if (t.used) {
				++used;
			}
		}
		return used;
	}

	public Item getDummyState(int id) {
		Item i = new Item(id) {
			@Override
			public String toString() {
				return "dummy " + this.id;
			}
			
			@Override
			public boolean canShift() {
				return shift != null;
			}
			
			@Override
			public Symbol getNextSymbol() {
				return shift.label;
			}
		};
		closures.remove(i); // reset
		return i;
	}
	
	// pre: shiftsInSets == true
	// does not swap startItem and endItem! 
	public void reverse() {
		Map<Transition, Transition> oldNew = new ShareableHashMap<Transition, Transition>(); 
		Set<Transition> oldTrans = transitions;
		transitions = new ShareableHashSet<Transition>();
		
		// first clear transitions of all items
		for (Item i : allItems()) {
			i.derives.clear();
			i.reduces.clear();
			if (i.shifts == null) {
				i.shifts = new ShareableHashSet<Transition>();
			} else {
				i.shifts.clear();
			}
			
			// swap follow and precede restrictions
			FollowRestrictions f = i.followRestrictions;
			i.followRestrictions = i.precedeRestrictions;
			i.precedeRestrictions = f;
		}
		
		// then add reversed transitions
		for (Transition old : oldTrans) {
			Symbol l = old.label;
			if (l instanceof Derive) {
				l = ((Derive) l).production.reduction;
			} else if (l instanceof Reduce) {
				l = ((Reduce) l).production.derivation;
			}			
			
			Transition t = addTransition(old.target, l, old.source);
			t.empty = old.empty;
			
			if (l instanceof Derive) {
				t.source.derives.add(t);
			} else if (l instanceof Reduce) {
				t.source.reduces.add(t);
			} else {
				t.source.shifts.add(t);
			}
			
			oldNew.put(old, t);
		}
		
		// fill 'reverse' fields of derives and reduces
		for (Transition old : oldTrans) {
			Transition n = oldNew.get(old);
			for (Transition r : old.reverse) {
				Transition nr = oldNew.get(r);
				n.reverse.add(nr);
			}
		}

		reversed = !reversed;
		
		for (Item i : allItems()) {
			if (reversed ? i.atBegin() : i.atEnd()) {
				i.shifts = null;
			}
		}
		
		connectShiftAndReduces();

	}
	
	//-------------------------------------------------------------------------
	
	Set<Item> rejectPartItems;
	Set<Transition> rejectPartTransitions;
	Set<Transition> rejectPartBridges;	
	
	private boolean reachableRejectBridge(Transition t) {
		return (t.source.production != null && t.source.production.reachable &&	t.target.production != null && t.target.production.reject) ||
			   (t.target.production != null && t.target.production.reachable &&	t.source.production != null && t.source.production.reject);
	}
	
	public void disconnectRejectPart() {
		rejectPartItems = new ShareableHashSet<Item>();
		for (Item i : items) {
			if (i.production.usedForReject) {
				rejectPartItems.add(i);
			}
		}
		items.removeAll(rejectPartItems);
		
		rejectPartTransitions = new ShareableHashSet<Transition>();
		rejectPartBridges = new ShareableHashSet<Transition>();
		for (Transition t : transitions) {
			if ((t.target.production != null && t.target.production.usedForReject) ||
				(t.source.production != null && t.source.production.usedForReject)) {
				if (reachableRejectBridge(t)) {
					rejectPartBridges.add(t);
				} else {
					rejectPartTransitions.add(t);
				}
			}
		}
		transitions.removeAll(rejectPartTransitions);
		removeTransitions(rejectPartBridges);
	}
	
	public void reconnectRejectPart() {
		for (Transition t : rejectPartBridges) {
			boolean add = false;
			if (t.getType() == Derive.class) {
				if (items.contains(t.source)) {
					add = true;
					t.source.derives.add(t);
				}
			} else { // t is a Reduce
				if (items.contains(t.target)) {
					add = true;
					t.source.reduces.add(t);
					for (Transition s : t.shifts) {
						if (!s.source.shifts.contains(s)) {
							s.source.shifts.add(s);
						}
					}
				}
			}
			if (add) {
				transitions.add(t);
			}
		}

		items.addAll(rejectPartItems);
		transitions.addAll(rejectPartTransitions);
		
		optimize(true); // some items might not be reachable anymore
	}

	public static class Item implements Cloneable {
		public Production production;
		public int index;
		public ShareableHashSet<Transition> derives; 
		public ShareableHashSet<Transition> reduces;
		public Transition shift = null;
		public ShareableHashSet<Transition> shifts;
		public SymbolSet dffb; // derive free bracketed first, called eff in Schmitz' thesis
		public Item unfoldedFrom = null;
		public Object stackInfo = null;
		public boolean withFollow = false;
		public FollowRestrictions followRestrictions = null;
		public FollowRestrictions precedeRestrictions = null;
		
		public int distanceToEnd = 999999999;
		public boolean canReduce;
		
		public int id; // for identity
		public int ID; // for caching

		{
			derives = new ShareableHashSet<Transition>();
			reduces = new ShareableHashSet<Transition>();
			dffb = Grammar.newSymbolSet();
		}
		
		public Item(int id) {
			production = null;
			index = 0;
			this.id = id;
		}
		
		public void serialize(Writer w) throws IOException {
			w.write(id + "\n");
			if (production != null) {
				w.write(production + "\n");
			} else {
				w.write("\n");
			}
			w.write(index + "\n");
		}
		
		public int deserialize(String[] s, int pos, Map<String, Production> prods) {
			id = Integer.parseInt(s[pos++]);
			String ps = s[pos++];
			if (!ps.equals("")) {
				production = prods.get(ps);
			}
			index = Integer.parseInt(s[pos++]);

			shifts = new ShareableHashSet<Transition>();
			if (production != null) {
				canReduce = canReduce();
			}

			return pos;
		}

		public Item(Production p, int index, int id) {
			this.production = p;
			this.index = index;
			this.id = id;
			canReduce = canReduce();
		}
		
		public Pair<Production, Integer> getCanonicalItem() {
			return new Pair<Production, Integer>(production, index);
		}
		
		public boolean atBegin() {
			return index == 0;
		}
		
		public boolean atEnd() {
			if (production != null) {
				return index == production.getLength();
			}
			return false;
		}
		
		public boolean isEmpty() {
			return production.isEmpty();
		}
		
		public boolean canDerive() {
			return index < production.getLength() && production.getSymbolAt(index) instanceof NonTerminal;
		}
		
		public boolean canDeriveTo(Item i) {
			return (i.atBegin() && i.production != null && 
				//i.production.lhs == getNextSymbol() &&
				getNextSymbol() instanceof NonTerminal &&
				((NonTerminal) getNextSymbol()).productions.contains(i.production) &&
				production.isDerivationAllowed(i.production, index));
		}

		public boolean canShift() {
			return index != production.getLength();
		}

		public boolean canReduce() {
			return index == production.getLength();
		}
		
		public boolean canReduceWith(Item i) {
			return canReduce;
		}

		public Symbol getPrevSymbol() {
			if (index > 0) {
				return production.getSymbolAt(index - 1);
			} else {
				return null;
			}
		}
		
		public Symbol getNextSymbol() {
			if (index < production.getLength()) {
				return production.getSymbolAt(index);
			} else {
				return null;
			}
		}
		
		public Symbol getNextSymbol(Item to) {
			if (index < production.getLength()) {
				for (Transition t : shifts) {
					if (t.target == to) {
						return t.label;
					}
				}
			}
			return null;
		}
		
		public Item getNext() {
			return shift.target;
		}
		
		public Item followShifts() {
			return followShifts(new ShareableHashSet<Item>());
		}
		
		private Item followShifts(Set<Item> visited) {
			visited.add(this);
			if (shift == null) {
				return this;
			}
			Item i1 = null;
			if (shift != null && !visited.contains(shift.target)) {
				i1 = shift.target.followShifts(visited);
			}
			if (i1 != null) {
				return i1;
			} else {
				return null; // can happen with lists on end of production
			}
		}
		
		public Set<Item> followShiftSets(boolean reversed) {
			Set<Item> result = new ShareableHashSet<Item>();
			Set<Item> s = new ShareableHashSet<Item>();
			Set<Item> s2;

			s.add(this);
			while (s.size() > 0) {
				s2 = new ShareableHashSet<Item>();
				for (Item i : s) {
					if (i.shifts == null) {
						if (reversed ? i.atBegin() : i.atEnd()) {
							result.add(i);
						}
					} else {
						for (Transition t : i.shifts) {
							s2.add(t.target);
						}
					}
				}
				s = s2;
			}
			return result;
		}
		
		// pre: r instanceof Reduce
		public boolean conflict(Symbol r) {
			if (dffb.contains(r)) {
				return dffb.size() > 1;
			} else {
				return dffb.size() > 0;
			}
		}
		
		public void computeDFF() {
			Set<Item> visited = new ShareableHashSet<Item>();
			visited.add(this);
			computeDFF(this, visited);
		}
		
		private void computeDFF(Item i, Set<Item> visited) {
			if (shift != null && !(shift.label instanceof NonTerminal)) {
				// XXX instanceof test previously not here!!
				// TODO verify first!!!
				i.dffb.add(shift.label);
			}
			
			if (shifts != null && !(getNextSymbol() instanceof NonTerminal)) {
				i.dffb.add(getNextSymbol());
			}
			
			for (Transition t : reduces) {
				i.dffb.add(t.label);
			}
			
			for (Transition t : derives) {
				Item to = t.target;
				if (!visited.contains(to)) {
					visited.add(to);
					to.computeDFF(i, visited);
				}
			}			
		}
		
		public Transition getDeriveOrReduceTo(Item i) {
			return getDeriveOrReduceTo(i, true);
		}
		
		public Transition getDeriveOrReduceTo(Item i, boolean shouldExist) {
			for (Transition t : derives) {
				if (t.target == i) {
					return t;
				}
			}
			for (Transition t : reduces) {
				if (t.target == i) {
					return t;
				}
			}
			if (shouldExist) {
				throw new RuntimeException("No transition found between " + this + " and " + i);
			} else {
				return null;
			}
		}
		
		public boolean directlyRecursive() {
			if (index == 0 && production != null && production.getLength() > 0) {
				for (Transition t : derives) {
					if (t.target == this) {
						return true;
					}
				}
			}
			return false;
		}
		
		@Override
		public Object clone() throws CloneNotSupportedException {
			Item i = (Item) super.clone();
			i.id = itemID++;
			
			i.derives = new ShareableHashSet<Transition>();
			i.reduces = new ShareableHashSet<Transition>();
			i.dffb = Grammar.newSymbolSet();
			i.shift = null;
			if (shifts != null) {
				i.shifts = new ShareableHashSet<Transition>();
			}
			
			return i;
		}
		
		public Item unfold(Item from) {
			Item clone = null;
			try {
				clone = (Item) clone();
			} catch (CloneNotSupportedException e) { }
			clone.unfoldedFrom = from;
			return clone;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			
			if (o.getClass() != getClass())
				return false;
			
			Item oi = (Item)o;
			
			return id == oi.id;
		}
		
		public String toString() {
			String s = production.toString(index);
			if (stackInfo != null) {
				if (stackInfo instanceof Item[]) {
					s += " [";
					Item[] array = (Item[]) stackInfo;
					for (int i = 0; i < array.length; i++) {
						s += array[i] + ",";
					}
					s += "]";
				} else {
					s += " [" + stackInfo + "]";
				}
			}
			if (withFollow) {
				s += " -/- " + followRestrictions;
			}
			if (unfoldedFrom != null) {
				s += " @ " + unfoldedFrom;
			}
			
			return s;
		}
		
		public int hashCode() {
			return id;
		}

		public int compareTo(Item other) {
			if (this instanceof StartItem) {
				return -1;
			}
			if (this instanceof EndItem) {
				if (other instanceof StartItem) {
					return 1;
				} else {
					return -1;
				}
			}
			if (other instanceof StartItem || other instanceof EndItem) {
				return 1;
			}
			return this.id - other.id;
		}		
	}
	
	public static class StartItem extends Item {

		Symbol startSymbol;
		
		public StartItem(int id, Symbol startSymbol) {
			super(id);
			this.startSymbol = startSymbol;
		}

		@Override
		public String toString() {
			return "<start>";
		}
		
		@Override
		public boolean canDerive() {
			return true;
		}
		
		@Override
		public boolean canDeriveTo(Item i) {
			return false;
		}

		@Override
		public boolean canShift() {
			return true;
		}
		
		@Override
		public boolean atBegin() {
			return true;
		}
		
		@Override
		public boolean atEnd() {
			return false;
		}
		
		@Override
		public Symbol getNextSymbol() {
			return startSymbol;
		}

		@Override
		public boolean canReduce() {
			return false;
		}
		
		@Override
		public void serialize(Writer w) throws IOException {
			w.write("start\n");
			super.serialize(w);
		}		
	}

	public static class EndItem extends Item {

		public EndItem(int id) {
			super(id);
		}
		
		@Override
		public String toString() {
			return "<end>";
		}

		@Override
		public boolean canDerive() {
			return false;
		}

		@Override
		public boolean canDeriveTo(Item i) {
			return false;
		}

		@Override
		public boolean canShift() {
			return true;
		}

		@Override
		public boolean atEnd() {
			return true;
		}
		
		@Override
		public boolean atBegin() {
			return false;
		}
		
		@Override
		public Symbol getNextSymbol() {
			return Grammar.endmarker;
		}
				
		@Override
		public boolean canReduce() {
			return false;
		}

		@Override
		public void computeDFF() {
			dffb.add(Grammar.endmarker);
		}
		
		@Override
		public void serialize(Writer w) throws IOException {
			w.write("end\n");
			super.serialize(w);
		}
	}
	
	public static class Transition {
		public Symbol label;
		public Item target;
		public Item source;
		public Set<Transition> reverse = new ShareableHashSet<Transition>(); // matching derives or reduces
		public int ID;
		public boolean used = false;
		public boolean empty = false;
		public Set<Pair<Transition, Transition>> derivesReduces; // for shifts
		public Set<Transition> shifts; // for reduce
		
		public Transition() {}

		public Transition(Item from, Symbol s, Item to) {
			source = from;
			target = to;
			label = s;
		}
		
		public Class<?> getType() {
			return label.getClass();
		}
		
		public boolean isShift() {
			return (!(label instanceof Derive) && !(label instanceof Reduce));
		}
		
		public Production getProduction() {
			if (label instanceof Derive) {
				return ((Derive) label).production;				
			} else if (label instanceof Reduce) {
				return ((Reduce) label).production;
			} else {
				throw new RuntimeException("Label is not a Derive or Reduce");
			}
		}

		@Override
		public String toString() {
			return label.toString();
		}

		public String toStringExt() {
			return source.toString() + " " + label.toString() + " " + target.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			result = prime * result
					+ ((target == null) ? 0 : target.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Transition other = (Transition) obj;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return empty == other.empty;
		}
	}
	
	public interface IItemFactory {
		Item createItem(Production p, int index, int id);
	}
	
	private static class StackFragment implements Comparable<StackFragment> {
		private Item[] array;
		
		public StackFragment(int depth) {
			array = new Item[depth];
		}
		
		public StackFragment(StackFragment sf, Item item) {
			array = new Item[sf.array.length];
			for (int i = 0; i < array.length - 1; i++) {
				array[i + 1] = sf.array[i];
			}
			array[0] = item;
		}
		
		@Override
		public String toString() {
			return Arrays.toString(array);
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(array);
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof StackFragment))
				return false;
			StackFragment other = (StackFragment) obj;
			for (int i = 0; i < array.length; i++) {
				if (array[i] != other.array[i]) {
					return false;
				}
			}
			return true;			
		}

		public int compareTo(StackFragment o) {
			if (array.length != o.array.length) {
				return array.length - o.array.length;
			}
			for (int i = 0; i < array.length; i++) {
				int c = array[i].compareTo(o.array[i]);
				if (c != 0) {
					return c;
				}
			}
			return 0;
		}
		
	}
}