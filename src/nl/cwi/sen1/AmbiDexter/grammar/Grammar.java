package nl.cwi.sen1.AmbiDexter.grammar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.IAmbiDexterMonitor;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.LinkedList;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Util;

public class Grammar {
	
	public static Terminal empty; 
	public static Terminal endmarker;
	public static int EOF = Integer.MAX_VALUE;
	private static Grammar instance;

	public String name;
	public boolean scannerless = false;
	public NonTerminal startSymbol = null;
	public Map<String, Terminal> terminals;
	public Map<String, NonTerminal> nonTerminals;
	public Set<Production> productions;

	private boolean doReject = true;
	private boolean doFollow = true;
	private boolean doPreferAvoid = true;
	private int productionId = 0;
	
	public Map<NonTerminal, SymbolSet> first;
	public Map<NonTerminal, SymbolSet> follow;
	public SymbolSet[] emptyFreeFirst; // only terminals
	public Relation<Pair<Production, Integer>, SymbolString> itemFirst;
	public Relation<Pair<Production, Integer>, SymbolString> itemFollow;
		
	public Map<NonTerminal, List<Symbol>> minimalStrings;

	public int nrReachableProductions, nrPrioritiesRead;
	
	{
		terminals = new ShareableHashMap<String, Terminal>();
		nonTerminals = new ShareableHashMap<String, NonTerminal>();
		productions = new ShareableHashSet<Production>();
		instance = this;
	}
	
	public static Grammar getInstance() {
		return instance;
	}

	public Grammar(String name, boolean usesCharacterClasses, boolean doRejects, boolean doFollowRestrictions) {
		this.name = name;
		this.scannerless = usesCharacterClasses;
		this.doReject = doRejects;
		this.doFollow = doFollowRestrictions;

		Symbol.resetSymbolCache();
		empty = new Terminal("empty");
		endmarker = new Terminal("$");
	}
	
	public Grammar(Grammar original, Set<Production> potentiallyHarmful, boolean terminalizeUnproductive) {  
	
		/*
		 * Reconstruct a grammar from the given set of productions.
		 * Retains symbol objects, production objects are cloned.
		 * Only for non-scannerless grammars.
		 * 
		 * algorithm:
		 * 1. collect defined nonterminals
		 * 2. collect used terminals and nonterminals
		 * 3. collect undefined nonterminals
		 * 4. clone productions (the potentially harmful ones)
		 * 5. terminalize undefined nonterminals
		 * 6. fix unproductive nonterminals
		 * 7. 
		 * 8. 
		 */
		
		if (original.scannerless) {
			throw new RuntimeException("Grammar reconstruction only on non-scannerless grammars. Use NFA reconstruction instead.");
		}
				
		name = original.name;
		productionId = original.productionId;
		scannerless = original.scannerless;
		doReject = original.doReject;
		doFollow = original.doFollow;
		doPreferAvoid = original.doPreferAvoid;
		
		// 1. collect defined nonterminals in this.nonTerminals
		// 2a. collect used terminals in this.terminals
		for (Production p : potentiallyHarmful) {
			NonTerminal n = p.lhs;
			nonTerminals.put(n.s, n);
			
			for (int i = 0; i < p.getLength(); i++) {
				Symbol s = p.getSymbolAt(i);
				if (s instanceof Terminal) {
					Terminal t = (Terminal) s;
					terminals.put(t.s, t);
				}
			}
		}
		
		// 2b. collect used nonterminals
		Set<NonTerminal> usedNTs = new ShareableHashSet<NonTerminal>();
		for (Production p : potentiallyHarmful) {
			for (int i = 0; i < p.getLength(); i++) {
				Symbol s = p.getSymbolAt(i);
				if (s instanceof NonTerminal) {
					usedNTs.add((NonTerminal) s);
				}
			}
		}
		
		startSymbol = original.startSymbol;
		
		// 3. collect undefined nonterminals: used but not defined
		Set<NonTerminal> undefinedNTs = new ShareableHashSet<NonTerminal>();
		undefinedNTs.addAll(usedNTs);
		undefinedNTs.removeAll(nonTerminals.values());

		// 4a. remove productions from nonterminals
		for (NonTerminal n : original.nonTerminals.values()) {
			n.productions.clear();
		}
		
		// 4b. clone productions
		ShareableHashMap<Production, Production> prodMap = new ShareableHashMap<Production, Production>();
		for (Production p : potentiallyHarmful) {
			Production newP = new Production(p, p.nr);
			addProduction(newP);
			prodMap.put(p, newP);
		}
		
		// 5. terminalize undefined nonterminals
		for (NonTerminal n : undefinedNTs) {
			nonTerminals.put(n.s, n);
			terminalize(n, original);
		}
		
		// 6. fix unproductive nonterminals
		if (terminalizeUnproductive) {
			Set<NonTerminal> productiveNTs = getProductiveNonterminals(productions);
			Set<NonTerminal> improductiveNTs = new ShareableHashSet<NonTerminal>();
			for (NonTerminal n : nonTerminals.values()) {
				if (n.reachable || n.usedForReject) {
					improductiveNTs.add(n);
				}
			}
			improductiveNTs.removeAll(productiveNTs);
			
			if (improductiveNTs.size() > 0) {
				if (AmbiDexterConfig.verbose) {
					System.out.println("Improductive: " + improductiveNTs);
				}
				
				ShareableHashMap<NonTerminal, Set<NonTerminal>> dep = buildDependencyGraph(potentiallyHarmful, improductiveNTs);
				ShareableHashMap<NonTerminal, Set<NonTerminal>> depScc = Util.stronglyConnectedComponents2(dep);
				ShareableHashMap<Set<NonTerminal>, Set<Set<NonTerminal>>> sccdep = Util.lift(dep, depScc);
				Set<Set<NonTerminal>> leafs = new ShareableHashSet<Set<NonTerminal>>();
				for (Entry<Set<NonTerminal>, Set<Set<NonTerminal>>> e : sccdep) {
					e.getValue().remove(e.getKey());
					leafs.addAll(e.getValue());
				}
				for (Entry<Set<NonTerminal>, Set<Set<NonTerminal>>> e : sccdep) {
					if (e.getValue().size() > 0) {
						leafs.remove(e.getKey());
					}
				}
				
				for (Set<NonTerminal> s : leafs) {
					NonTerminal n = s.iterator().next();
					terminalize(n, original);
				}
			}
		}
	}

	public void finish() {
		
		for (NonTerminal n : nonTerminals.values()) {
			n.finish();
		}
		
		// calculate highest character used
		if (scannerless) {
			int max = 0;
			for (Production p : productions) {
				for (Symbol s : p.rhs) {
					if (s instanceof CharacterClass) {
						int m = ((CharacterClass) s).getMaxCharacter();
						if (m > max) {
							max = m;
						}
					}
				}
			}
			Character.maxCharacterUsed = Character.maxCharacterInOriginalGrammar = max; // probably always 256
		}
		
		calcReachableAndRejects();
		splitReachableAndReject();
	
		verifyBasic();
		
		buildFirst();
		buildFollow();
		
		buidItemFirst(1);
		verifyTemp();
		buildItemFollow(1);
		
		buildEmptyFreeFirst();
		
		if (!scannerless) {
			calcMinimalStrings();
		}
	}

	// Create new production with empty rhs.
	// After rhs is finished call addProduction();
	public Production newProduction(NonTerminal n) {
		return new Production(n, productionId++);
	}
	
	// Create new production with empty rhs.
	// After rhs is finished call addProduction();
	public Production cloneProduction(Production p) {
		return new Production(p, productionId++);
	}

	public void addProduction(Production p) {
		p.done();
		productions.add(p);
	}
	
	/* ====================================================================== */
	
	private Production terminalize(NonTerminal n, Grammar original) {
		// take a minimal length representative
		Production newP = newProduction(n);
		newP.reconstructed = true;
		for (Symbol s : original.minimalStrings.get(n)) {
			newP.addSymbol(s);
		}
		addProduction(newP);
		return newP;
	}
	
	private Symbol getDummyTerminal(String s) {
		if (scannerless) {
			int c = ++Character.maxCharacterUsed;
			int r[] = {c, c};
			return new CharacterClass(r);
		} else {
			return getTerminal(s);
		}
	}
	
	Map<NonTerminal, Symbol> terminalizeds = new ShareableHashMap<NonTerminal, Symbol>();

	private Symbol getDummyTerminal(NonTerminal n) {
		Symbol s = terminalizeds.get(n);
		if (s == null) {
			s = getDummyTerminal("Term_" + n.s);
			terminalizeds.put(n, s);
		}
		return s;
	}
	
	private Set<NonTerminal> getUnproductiveNonterminals() {
		Set<NonTerminal> s = new ShareableHashSet<NonTerminal>();
		s.addAll(nonTerminals.values());
		s.removeAll(getProductiveNonterminals(productions));
		return s;
	}

	private static Set<NonTerminal> getProductiveNonterminals(Set<Production> productions) {
		Set<NonTerminal> productive = new ShareableHashSet<NonTerminal>();
		
		int oldSize = 0;
		int newSize = 0;
		
		do {
			for (Production p : productions) {
				if (p.reject) {
					continue;
				}
				boolean all = true;
				for (int i = 0; i < p.getLength(); i++) {
					Symbol s = p.getSymbolAt(i);
					if (s instanceof NonTerminal) {
						NonTerminal n = (NonTerminal) s;
						if (!productive.contains(n)) {
							all = false;
							break;
						}
					}
				}
				if (all) {
					productive.add(p.lhs);
				}
			}
			
			oldSize = newSize;
			newSize = productive.size();
		} while (oldSize != newSize);		

		return productive;
	}
	
	// respects priorities and does not follow rejects
	// (reject productions are included though)
	private static Relation<Production, Production> getProductionDependencies(Set<Production> prods) {
		Relation<Production, Production> dep = new Relation<Production, Production>();
		
		for (Production p : prods) {
			for (int i = 0; i < p.getLength(); i++) {
				Symbol s = p.getSymbolAt(i);
				if (s instanceof NonTerminal) {
					for (Production p2 : ((NonTerminal) s).productions) {
						if (prods.contains(p2) && !p2.reject && p.isDerivationAllowed(p2, i)) {
							dep.add(p, p2);
						}
					}
				}
			}
		}
		
		return dep;
	}
	
	/* ====================================================================== */
	
	public Relation<Pair<Production, Integer>, Production> getItemDerives() {
		// includes only reachable productions, ignores rejected productions
		Relation<Pair<Production, Integer>, Production> der = new Relation<Pair<Production, Integer>, Production>();

		for (Production p : productions) {
			if (p.reachable) {
				for (int i = 0; i < p.getLength(); ++i) {
					Symbol s = p.getSymbolAt(i);
					if (s instanceof NonTerminal) {
						for (Production p2 : ((NonTerminal) s).productions) {
							if (p2.reachable && p.isDerivationAllowed(p2, i)) {
								der.add(new Pair<Production, Integer>(p, i), p2);
							}
						}
					}
				}
			}
		}
		
		return der;
	}

	public Relation<Production, Pair<Production, Integer>> getItemReduces() {
		// ignores rejected productions
		Relation<Production, Pair<Production, Integer>> red = new Relation<Production, Pair<Production, Integer>>();

		for (Production p : productions) {
			if (p.reachable) {
				for (int i = 0; i < p.getLength(); ++i) {
					Symbol s = p.getSymbolAt(i);
					if (s instanceof NonTerminal) {
						for (Production p2 : ((NonTerminal) s).productions) {
							if (p2.reachable && p.isDerivationAllowed(p2, i)) {
								red.add(p2, new Pair<Production, Integer>(p, i + 1));
							}
						}
					}
				}
			}
		}
		
		return red;
	}
	
	public void verifyBasic() {
		
		/*int eq = 0;
		for (Entry<String, Terminal> e1 : terminals.entrySet()) {
			for (Entry<String, Terminal> e2 : terminals.entrySet()) {
				if (e1.getValue().equals(e2.getValue()))
					eq++;
			}			
		}
		
		if (eq != terminals.size()) {
			throw new RuntimeException("Error in Terminal.equal()");
		}

		
		eq = 0;
		for (Entry<String, NonTerminal> e1 : nonTerminals.entrySet()) {
			for (Entry<String, NonTerminal> e2 : nonTerminals.entrySet()) {
				if (e1.getValue().equals(e2.getValue()))
					eq++;
			}			
		}
		
		if (eq != nonTerminals.size()) {
			throw new RuntimeException("Error in NonTerminal.equal()");
		}
		
		for (Production p1 : productions) {
			for (Production p2 : productions) {
				if (p1 != p2 && p1.equals(p2)) {
					throw new RuntimeException("Error in Production.equals(): " + p1 + ", " + p2);
				}
			}
		}
		
		for (NonTerminal n : nonTerminals.values()) {
			for (Production p : n.productions) {
				if (p.getLength() == 1 && p.getSymbolAt(0) == n) {
					throw new RuntimeException("Grammar contains identity injection: " + p);
				}
			}
		}*/
		
		for (Production p : productions) {
			if (!p.reachable) {
				continue;
			}
			
			if (!nonTerminals.containsValue(p.lhs)) {
				throw new RuntimeException("Unknown lhs nonterminal: " + p.lhs);
			}
			for (int i = 0; i < p.getLength(); i ++) {
				Symbol s = p.getSymbolAt(i);
				if (s instanceof NonTerminal) {
					if (!nonTerminals.containsValue(s)) {
						throw new RuntimeException("Unknown nonterminal: " + s);
					}
				} else if (s instanceof Terminal) {
					if (!terminals.containsValue(s)) {
						throw new RuntimeException("Unknown terminal: " + s);
					}
				}
			}
		}
		
		// check for injection cycles
		for (Entry<NonTerminal, Set<NonTerminal>> e : getInjectionTC()) {
			if (e.getValue().contains(e.getKey())) {
				throw new RuntimeException("Grammar contains injection cycle for: " + e.getKey());
			}
		}
		
		// check for unreachable nonterminals
		/*buildDependencyGraph();
		Set<NonTerminal> reachableN = Util.transitiveClosure(dependencies).get(startSymbol);
		Set<NonTerminal> unreachableN = new ShareableHashSet<NonTerminal>();
		for (NonTerminal n : nonTerminals.values()) {
			if (n != startSymbol && !reachableN.contains(n)) {
				//throw new RuntimeException("Unreachable nonterminal: " + n);
				if (!AmbiDexterConfig.quick) {
					System.out.println("Unreachable nonterminal: " + n);
				}
				n.reachable = false;
				for (Production p : n.productions) {
					p.reachable = false;
				}
				unreachableN.add(n);
			}
		}
		for (NonTerminal n : unreachableN) {
			nonTerminals.remove(n.toString());
			for (Production p : n.productions) {
				productions.remove(p);
			}
		}

		// check for unreachable productions b/c of priorities
		Set<Production> unreachableP = new ShareableHashSet<Production>();
		unreachableP.addAll(productions);
		unreachableP.removeAll(getReachableProductions());
		for (Production p : unreachableP) {
			if (p.lhs.reachable) {
				//throw new RuntimeException("Unreachable production: " + p);
				if (!AmbiDexterConfig.quick) {
					System.out.println("Unreachable production: " + p);
				}
			}
			p.reachable = false;
			p.lhs.productions.remove(p);
		}
		productions.removeAll(unreachableP);*/
	}
	
	public void verify() {
		verifyBasic();

		// compare item first sets with (less refined) nonterminal first sets
		Map<NonTerminal, SymbolSet> ntfirst = new ShareableHashMap<NonTerminal, SymbolSet>();
		for (Pair<Pair<Production, Integer>, SymbolString> p : itemFirst) {
			if (p.a.b == 0) {
				if (p.b.size() == 1) {
					SymbolSet s = ntfirst.get(p.a.a.lhs);
					if (s == null) {
						s = newSymbolSet();
						ntfirst.put(p.a.a.lhs, s);
					}
					s.add(p.b.get(0));
				}
			}			
		}
		
		for (NonTerminal n : nonTerminals.values()) {
			SymbolSet o = first.get(n); 
			final SymbolSet i = ntfirst.get(n);
			if (!o.equals(i)) {
				if (n.productive && n.reachable) {
					if (AmbiDexterConfig.verbose) {
						System.out.println("different first: " + n);
						System.out.println("o    " + o);
						System.out.println("n    " + i);
					}
					if (o instanceof CharacterClass && i != null) {
						if (((CharacterClass) i).differenceNonEmpty((CharacterClass) o)) {
							System.out.println("different first: " + n);
							System.out.println("o    " + o);
							System.out.println("n    " + i);							
							System.out.println("NON-EMPTY DIFF!!!");
						}
					}
					
					// replace default first sets with more precise one
					first.put(n, i);
				}
			}
		}
		
		// compare item follow sets with (less refined) nonterminal follow sets
		Map<NonTerminal, SymbolSet> ntfollow = new ShareableHashMap<NonTerminal, SymbolSet>();
		for (Pair<Pair<Production, Integer>, SymbolString> p : itemFollow) {
			if (p.a.b == p.a.a.getLength() && p.b.size() == 1) {
				SymbolSet s = ntfollow.get(p.a.a.lhs);
				if (s == null) {
					s = newSymbolSet();
					ntfollow.put(p.a.a.lhs, s);
				}
				s.add(p.b.get(0));
			}			
		}
		
		for (NonTerminal n : nonTerminals.values()) {
			SymbolSet o = follow.get(n);
			final SymbolSet i = ntfollow.get(n);
			if (!o.equals(i)) {
				if (AmbiDexterConfig.verbose) {
					System.out.println("different follow: " + n);
					System.out.println("o  " + o);
					System.out.println("n  " + i);
				}
				if (o instanceof CharacterClass && i != null) {
					if (((CharacterClass) i).differenceNonEmpty((CharacterClass) o)) {
						System.out.println("different follow: " + n);
						System.out.println("o  " + o);
						System.out.println("n  " + i);
						System.out.println("NON-EMPTY DIFF!!!");
					}
				}
				
				// replace default follow sets with more precise one
				follow.put(n, i);
			}
		}
	}

	private void verifyTemp() {
		for (Production p : productions) {
			if (p.reachable) {
				if (itemFirst.get(new Pair<Production, Integer>(p, 0)).size() == 0) {
					System.out.println("No item first for " + p);
				}
			}
		}
	}
	
	public String toString() {
		String s = "Grammar: " + name + "\n";
		
		s += String.valueOf(terminals.size()) + " terminals: " + terminals.values().toString() + "\n";
		s += String.valueOf(nonTerminals.size()) + " nonterminals: " + nonTerminals.values().toString() + "\n";
		s += String.valueOf(productions.size()) + " productions:\n" + productions.toString() + "\n";
		
		return s;
	}
	
	public void printSize(IAmbiDexterMonitor monitor) {
		int pos = productions.size();
		int prios = 0;
		int restr = 0;
		int reject = 0;
		int reachable = 0;
		int usedForReject = 0;
		int both = 0;
		int unused = 0;
		Set<Integer> followLengths = new ShareableHashSet<Integer>();
		
		for (Production p : productions) {
			pos += p.getLength();
			for (Entry<Integer, Set<Production>> e : p.deriveRestrictions.m) {
				prios += e.getValue().size();
			}
			if (p.reachable) {
				if (p.usedForReject) {
					both++;
				} else {
					reachable++;
				}
			} else {
				if (p.usedForReject) {
					usedForReject++;
				} else {
					unused++;
				}
			}
			if (p.reject) {
				reject++;
			}
		}
		
		int usedInReject = 0;
		for (NonTerminal n : nonTerminals.values()) {
			if (n.followRestrictions != null) {
				restr += n.followRestrictions.size();
				if (n.usedInRejectFilter || n.rejectedLiterals != null) {
					usedInReject++;
				}
				followLengths.add(n.followRestrictions.length());
			}
		}
		
		monitor.println("Grammar size: " + 
				productions.size() + " productions, " + 
				nonTerminals.size() + " nonterminals, " + 
				terminals.size() + " terminals, " + 
				prios + " priorities, " + 
				restr + " follow restrictions, " +
				reject + " rejects, " +
				pos + " positions.");
		monitor.println("Productions: " + reachable + " reachable, " + usedForReject + " usedForReject, " + both + " both, " + unused + " unused.");
		monitor.println("Nonterminals used for reject: " + usedInReject);
		monitor.println("Follow restriction lengths: " + followLengths);
	}

	public void printPrioritiesAndFollowRestrictions(IAmbiDexterMonitor monitor) {
		monitor.println("\nPriorities:");
		for (NonTerminal n : nonTerminals.values()) {
			for (Production p : n.productions) {
				for (Entry<Integer, Set<Production>> e : p.deriveRestrictions.m) {
					for (Production p2 : e.getValue()) {
						monitor.println("" + p);
						monitor.println("    <" + e.getKey() +"> > " + p2);
					}
				}
			}
		}		
		
		monitor.println("\nFollow restrictions:");
		for (NonTerminal n : nonTerminals.values()) {
			if (n.followRestrictions != null) {
				monitor.println("" + n + " -/- " + n.followRestrictions.toString());
			}
		}

		monitor.println("\nRejects:");
		for (Production p : productions) {
			if (p.reject) {
				monitor.println(p);
			}
		}
		
		monitor.println("\nPrefers:");
		for (Production p : productions) {
			if (p.prefer) {
				monitor.println(p);
			}
		}
		
		monitor.println("\nAvoids:");
		for (Production p : productions) {
			if (p.avoid) {
				monitor.println(p);
			}
		}
		
		monitor.println();
	}
	
	/* ====================================================================== */

	protected static ShareableHashMap<NonTerminal, Set<NonTerminal>> buildDependencyGraph(Set<Production> prods, Collection<NonTerminal> lhss) {
		ShareableHashMap<NonTerminal, Set<NonTerminal>> d = new ShareableHashMap<NonTerminal, Set<NonTerminal>>();

		for (NonTerminal n : lhss) {
			d.put(n, new ShareableHashSet<NonTerminal>());			
		}
		
		for (Production p : prods) {
			Set<NonTerminal> dep = d.get(p.lhs);
			if (dep != null) {
				for (int i = 0; i < p.getLength(); i++) {
					Symbol s = p.getSymbolAt(i);
					if (s != null && lhss.contains(s)) {
						dep.add((NonTerminal) s);
					}
				}
			}
		}
		
		return d;
	}
	
	public void dependencyGraphToDot(String filename) {
		// TODO this should be productions i.s.o. nonterminals
		ShareableHashMap<NonTerminal, Set<NonTerminal>> dependencies = buildDependencyGraph(productions, nonTerminals.values());
		
		BufferedWriter w = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			w = new BufferedWriter(fstream);
		
			w.write("digraph G {\n");
			
			for (Entry<NonTerminal, Set<NonTerminal>> e : dependencies.entrySet()) {
				NonTerminal n1 = e.getKey();
				if (n1.literal || n1.layout) {
					continue;
				}
				for (NonTerminal n2 : e.getValue()) {
					if (n2.literal || n2.layout) {
						continue;
					}
					w.write("" + Util.dotId(e.getKey()) + " -> " + Util.dotId(n2) + " ;\n");
				}
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
	
	public static SymbolSet newSymbolSet() {
		if (instance.scannerless) {
			return new CharacterClass();
		} else {
			return new SymbolBitSet();
		}
	}
	
	public static ESet<Symbol> newESetSymbol() {
		if (instance.scannerless) {
			return new ShareableHashSet<Symbol>();
		} else {
			return new SymbolBitSet();
		}
	}
	
	public Terminal getTerminal(String s) {
		Terminal t = terminals.get(s);
		if (t == null) {
			t = new Terminal(s);
			terminals.put(s, t);
		}
		return t;
	}
	
	public Terminal getLiteralTerminal(String s) {
		Terminal t = terminals.get(s);
		if (t == null) {		
			t = new LiteralTerminal(s.charAt(1));
			terminals.put(s, t);
		}
		return t;
	}
	
	public Terminal getLiteralTerminal(char c) {
		String s = "'" + c + "'";
		Terminal t = terminals.get(s);
		if (t == null) {		
			t = new LiteralTerminal(c);
			terminals.put(s, t);
		}
		return t;
	}

	public NonTerminal getNonTerminal(String s) {
		NonTerminal n = nonTerminals.get(s);
		if (n == null) {
			n = new NonTerminal(s);
			nonTerminals.put(s, n);
			
			if (!nonTerminals.containsValue(n)) {
				throw new RuntimeException("What is this?");
			}
		}
		return n;
	}
	
	public void printFirstAndFollow(IAmbiDexterMonitor monitor) {
		monitor.println("");
		for (NonTerminal n : nonTerminals.values()) {
			monitor.println("FIRST( " + n + " ) = " + first.get(n));
		}
		monitor.println("");
		for (NonTerminal n : nonTerminals.values()) {
			monitor.println("EFF( " + n + " ) = " + emptyFreeFirst[n.id]);
		}
		monitor.println("");
		for (NonTerminal n : nonTerminals.values()) {
			monitor.println("FOLLOW( " + n + " ) = " + follow.get(n));
		}
		/*monitor.println("");
		for (NonTerminal n : nonTerminals.values()) {
			monitor.println("NULLABLE( " + n + " ) = " + n.isNullable);
		}*/
		if (minimalStrings != null) {
			monitor.println("");
			for (NonTerminal n : nonTerminals.values()) {
				monitor.println("MINSTRING( " + n + " ) = " + minimalStrings.get(n));
			}
		}
		/*monitor.println("");
		for (NonTerminal n : nonTerminals.values()) {
			monitor.println("MINREACH( " + n + " ) = " + minimalReachableLengths.get(n));
		}
		monitor.println("");
		for (NonTerminal n : nonTerminals.values()) {
			monitor.println("MINFOLLOW( " + n + " ) = " + minimalFollowLengths.get(n));
		}*/
	}
	
	// TODO optimize, this is quite slow on large grammars
	private void buidItemFirst(int len) {
		// takes into account priorities and follow restrictions, but not rejects
		
		itemFirst = new Relation<Pair<Production,Integer>, SymbolString>();
		
		Set<Pair<Production, Integer>> items = getCanonicalItems();
		Relation<Pair<Production, Integer>, Production> derives = getItemDerives();
		
		SymbolString emptyFirst = new SymbolString(1);
		emptyFirst.add(empty);
		
		// first create terminal only lookahead
		for (Pair<Production, Integer> p : items) {
			if (p.b == p.a.getLength()) { // item at end
				itemFirst.add(p, emptyFirst);
			} else {
				Symbol s = p.a.getSymbolAt(p.b);
				if (!(s instanceof NonTerminal)) {
					NonTerminal pn = null;
					if (p.b > 0 && p.a.getSymbolAt(p.b - 1) instanceof NonTerminal) {
						pn = (NonTerminal) p.a.getSymbolAt(p.b - 1);
					}
					SymbolString ss = new SymbolString(len);
					ss.add(s);
					if (!doFollow || pn == null || pn.followRestrictions == null || pn.followRestrictions.check(ss)) {
						itemFirst.add(p, ss);
					} else {
						continue;
					}
					
					int i = p.b;
					while (ss.size() != len) {
						if (++i == p.a.getLength()) {
							ss = new SymbolString(ss);
							ss.add(empty);
							itemFirst.add(p, ss); // check with follow restrictions not necessary
							break;
						}
						
						Symbol s2 = p.a.getSymbolAt(i);
						if (s2 instanceof NonTerminal) {
							break;
						}
						
						ss = new SymbolString(ss);
						ss.add(s2);
						if (!doFollow || pn == null || pn.followRestrictions == null || pn.followRestrictions.check(ss)) {
							itemFirst.add(p, ss);
						} else {
							break;
						}
					}
				}				
			}
		}
		
		// extend first sets in fixed point
		int oldSize = 0;
		int size = itemFirst.size();
		while (size != oldSize) {
			oldSize = size; 
			for (Pair<Production, Integer> p : items) {
				if (p.b != p.a.getLength()) {
					Symbol s = p.a.getSymbolAt(p.b);
					if (s instanceof NonTerminal) {
						for (Production to : derives.get(p)) {
							for (SymbolString ss : itemFirst.get(new Pair<Production, Integer>(to, 0))) {
								if (!ss.endsWith(empty)) {
									itemFirst.add(p, ss);
								} else {
									int i = p.b + 1;
									if (i == p.a.getLength()) {
										itemFirst.add(p, ss); // ends with empty
									} else {
										int left = len - ss.size() + 1;
										for (SymbolString ss2 : itemFirst.get(new Pair<Production, Integer>(p.a, i))) {
											int ss2len = ss2.size();
											if (ss2.endsWith(empty)) {
												--ss2len;
											}
											if (ss2len <= left) {
												SymbolString ss3 = new SymbolString(ss);
												ss3.copy(ss2, ss.size() - 1);
												if (!doFollow || p.a.lhs.followRestrictions == null || p.a.lhs.followRestrictions.check(ss3)) {
													itemFirst.add(p, ss3);
												}
											}
										}
									}
									
									// add ss without empty
									SymbolString ss2 = new SymbolString(ss);
									ss2.remove(ss2.size() - 1);
									if (ss2.size() > 0) {
										itemFirst.add(p, ss2);
									}
								}
							}
						}
					}
				}
			}			
	
			size = itemFirst.size();
		}
	}
	
	// TODO optimize, this is quite slow on large grammars
	private void buildItemFollow(int len) {
		// takes into account priorities and follow restrictions, but not rejects
		
		itemFollow = new Relation<Pair<Production,Integer>, SymbolString>();
		
		Relation<Pair<Production, Integer>, Production> derives = getItemDerives();
		Relation<Production, Pair<Production, Integer>> reduces = getItemReduces();
		
		Set<Pair<Production, Integer>> items = getCanonicalItems();
		
		// add lookahead for productions of start symbol
		for (int i = 1; i <= len; i++) {
			SymbolString s = new SymbolString(i);
			for (int j = 1; j <= i; j++) {
				s.add(endmarker);
			}
			for (Production p : startSymbol.productions) {
				itemFollow.add(new Pair<Production, Integer>(p, p.getLength()), s);
			}
		}
		
		// copy first into follow, removing empty symbols at end of strings
		for (Pair<Pair<Production, Integer>, SymbolString> p : itemFirst) {
			SymbolString ss = p.b;
			if (ss.endsWith(empty)) {
				ss = new SymbolString(ss);
				ss.remove(ss.size() - 1);
			}
			if (ss.size() > 0) {
				itemFollow.add(p.a, ss);
			}
		}
		
		// extend follow sets in fixed point
		int oldSize = 0;
		int size = itemFollow.size();
		while (size != oldSize) {
			oldSize = size; 
			for (Pair<Production, Integer> p : items) {
				if (p.b != p.a.getLength()) {
					Symbol s = p.a.getSymbolAt(p.b);
					if (s instanceof NonTerminal) {
						for (Production to : derives.get(p)) {
							for (SymbolString ss : itemFirst.get(new Pair<Production, Integer>(to, 0))) {
								if (!ss.endsWith(empty)) {
									itemFollow.add(p, ss);
								} else {
									int left = len - ss.size() + 1;
									int i = p.b + 1;
									if (i == p.a.getLength()) {
										Set<Pair<Production, Integer>> r = reduces.get(p.a);
										if (r.size() == 0) {
											for (SymbolString ss2 : itemFollow.get(new Pair<Production, Integer>(p.a, i))) {
												if (ss2.size() <= left) {
													if (doFollow) {
														if (p.a.lhs.followRestrictions != null && !p.a.lhs.followRestrictions.check(ss2)) {
															continue;
														}
														NonTerminal n = (NonTerminal) s;
														if (n.followRestrictions != null && !n.followRestrictions.check(ss2)) {
															continue;
														}														
													}
														
													SymbolString ss3 = new SymbolString(ss);
													ss3.copy(ss2, ss.size() - 1);
													itemFollow.add(p, ss3);
												}
											}
										} else {
											for (Pair<Production, Integer> p2 : r) {
												for (SymbolString ss2 : itemFollow.get(p2)) {
													if (ss2.size() <= left) {
														if (doFollow) {
															if (p.a.lhs.followRestrictions != null && !p.a.lhs.followRestrictions.check(ss2)) {
																continue;
															}
															NonTerminal n = (NonTerminal) s;
															if (n.followRestrictions != null && !n.followRestrictions.check(ss2)) {
																continue;
															}														
														}
															
														SymbolString ss3 = new SymbolString(ss);
														ss3.copy(ss2, ss.size() - 1);
														itemFollow.add(p, ss3);
													}
												}
											}
										}
									} else {										
										for (SymbolString ss2 : itemFollow.get(new Pair<Production, Integer>(p.a, i))) {
											if (ss2.size() <= left) {
												if (doFollow) {
													NonTerminal n = (NonTerminal) s;
													if (n.followRestrictions != null && !n.followRestrictions.check(ss2)) {
														continue;
													}														
												}
												
												SymbolString ss3 = new SymbolString(ss);
												ss3.copy(ss2, ss.size() - 1);
												itemFollow.add(p, ss3);
											}
										}
									}
									
									// remove empty and add ss to follow
									SymbolString ss2 = new SymbolString(ss);
									ss2.remove(ss2.size() - 1);
									if (ss2.size() > 0) {
										itemFollow.add(p, ss2);
									}
								}
							}
						}
					}
				} else {
					for (Pair<Production, Integer> p2 : reduces.get(p.a)) {
						for (SymbolString ss2 : itemFollow.get(p2)) {
							if (doFollow && p.a.lhs.followRestrictions != null && !p.a.lhs.followRestrictions.check(ss2)) {
								continue;
							}
							itemFollow.add(p, ss2);
						}
					}
				}
			}			
	
			size = itemFollow.size();
		}
	}

	private Set<Pair<Production, Integer>> getCanonicalItems() {
		Set<Pair<Production, Integer>> items = new ShareableHashSet<Pair<Production,Integer>>();
		for (Production p : productions) {
			if (p.reachable) {
				for (int i = p.getLength(); i >= 0; --i) {
					items.add(new Pair<Production, Integer>(p, i));
				}
			}
		}
		return items;
	}

	// TODO take priorities into account	
	private void buildFirst() {
		
		first = new ShareableHashMap<NonTerminal, SymbolSet>();
		
		for (NonTerminal n : nonTerminals.values()) {
			first.put(n, newSymbolSet());
		}
		
		int oldSize = 0;
		int newSize = 0;
		do {
			oldSize = newSize;
			newSize = 0;
			
			for (NonTerminal n : nonTerminals.values()) {
				SymbolSet f = first.get(n);
				
				for (Production p : n.productions) {
					f.addAll(first(p.rhs, 0));
				}
				
				newSize += f.size();
			}
			
		} while (newSize > oldSize);
	}
	
	public SymbolSet first(List<Symbol> l, int start) {
		SymbolSet result = newSymbolSet();
		
		Symbol s = null;
		SymbolSet fs = null;
		for (int i = start; i < l.size(); i++) {
			s = l.get(i);
			if (s instanceof NonTerminal) {
				if (AmbiDexterConfig.nonTerminalLookahead) {
					result.add(s); // add nonterminals to lookahead
				}
				fs = first.get(s);
				result.addAll(fs); // might add empty but we remove it below
				if (!fs.contains(empty)) {
					break;
				}
			} else { // Terminal or CharClass
				result.add(s);
				break;
			}
		}
		
		if (s == null || (s instanceof NonTerminal && fs.contains(empty))) {
			result.add(empty);
		} else {
			result.remove(empty);
		}
		
		return result;
	}
	
	private void buildFollow() {
		
		follow = new ShareableHashMap<NonTerminal, SymbolSet>();
		
		for (NonTerminal n : nonTerminals.values()) {
			follow.put(n, newSymbolSet());
		}
		
		follow.get(startSymbol).add(endmarker);
		
		int oldSize = 0;
		int newSize = 0;
		do {
			oldSize = newSize;
			newSize = 0;
			
			for (NonTerminal n : nonTerminals.values()) {
				SymbolSet f = follow.get(n);
				
				for (Production p : productions) {
					for (int i = 0; i < p.getLength(); i++) {
						if (p.getSymbolAt(i) == n) {
							SymbolSet fs = first(p.rhs, i+1);
							f.addAll(fs);
							
							if (fs.contains(empty) || i == p.getLength() - 1) {
								// TODO: this goes wrong if nested!! why???
								fs = follow.get(p.lhs);
								f.addAll(fs);
							}
						}
					}
				}
				
				f.remove(empty);
				
				newSize += f.size();
			}
			
		} while (newSize > oldSize);
	}

	private void buildEmptyFreeFirst() { // TODO, take derive restrictions (priorities) into account
		emptyFreeFirst = new SymbolSet[Symbol.getNumberOfSymbols()];
		for (NonTerminal n : nonTerminals.values()) {
			emptyFreeFirst[n.id] = newSymbolSet();
		}
		
		int oldSize = 0;
		int newSize = 0;
		do {
			oldSize = newSize;
			newSize = 0;
			
			for (NonTerminal n : nonTerminals.values()) {
				SymbolSet eff = emptyFreeFirst[n.id];
				
				for (Production p : n.productions) {
					if (p.getLength() > 0) {
						Symbol s = p.getSymbolAt(0);
						if (s instanceof NonTerminal) {
							eff.addAll(emptyFreeFirst[s.id]);
						} else {
							// terminal or charclass
							eff.add(s);
						}
					}
				}
				
				newSize += eff.size();
			}
			
		} while (newSize > oldSize);
	}

	/* ====================================================================== */
	
	private void calcMinimalStrings() {
		// calc minimal strings for productions
		// TODO does not take priorities into account
		minimalStrings = new ShareableHashMap<NonTerminal, List<Symbol>>();
		
		// first count all productions with only terminals (or empty ones)
		for (Production p : productions) {
			boolean nonterminal = false;
			for (int i = 0; i < p.getLength(); i++) {
				Symbol s = p.getSymbolAt(i);
				if (s instanceof NonTerminal) {
					nonterminal = true;
					break;
				}
			}
			if (!nonterminal) {
				List<Symbol> prev = minimalStrings.get(p.lhs);
				if (prev == null || prev.size() > p.getLength()) {
					minimalStrings.put(p.lhs, p.rhs);
				}
			}
		}
		
		// then calculate ones with nonterminals
		int oldSize = 0;
		while (minimalStrings.size() != oldSize) {
			oldSize = minimalStrings.size();
			for (Production p : productions) {
				int l = 0;
				boolean valid = true;
				for (int i = 0; i < p.getLength(); i++) {
					Symbol s = p.getSymbolAt(i);
					if (s instanceof NonTerminal) {
						List<Symbol> ntl = minimalStrings.get(s);
						if (ntl == null) {
							// unknown yet
							valid = false;
							break;
						} else {
							l += ntl.size();
						}
					} else {
						l++;
					}
				}
				if (valid) {
					List<Symbol> prev = minimalStrings.get(p.lhs);
					if (prev == null || prev.size() > l) {
						List<Symbol> min = new ArrayList<Symbol>();
						for (int i = 0; i < p.getLength(); i++) {
							Symbol s = p.getSymbolAt(i);
							if (s instanceof NonTerminal) {
								List<Symbol> ntl = minimalStrings.get(s);
								min.addAll(ntl);
							} else {
								min.add(s);
							}
						}
						minimalStrings.put(p.lhs, min);
					}
				}
			}
		}
	}

	private void calcReachableAndRejects() {
		
		// Steps:
		// check for undefined nonterminals + terminalize
		// calculate reachable flags
		// calculate usedForReject flags
		// check for unused productions and nonterminals
		// calculate reachable and usedForReject flags for nonterminals
		// remove unused nonterminals
		// remove unused productions of used nonterminals
		// check for unproductive nonterminals
		// propagate usedInLayout flags
		
		
		// check for undefined nonterminals + terminalize
		for (NonTerminal n : nonTerminals.values()) {
			if (n.productions.size() == 0) {
				if (AmbiDexterConfig.verbose) {
					System.out.println("Undefined nonterminal: " + n);
				}
				Production p = newProduction(n);
				p.reconstructed = true;
				p.addSymbol(getDummyTerminal(n));
				addProduction(p);
			}
		}
	
		// calculate reachable flags
		Relation<Production, Production> dep = getProductionDependencies(productions);
		Map<Production, Set<Production>> tc = Util.transitiveClosure2(dep.m);
		
		for (Production p : productions) {
			if (!p.lhs.s.startsWith("[")) {
				// reset
				p.reachable = false;
				p.usedForReject = false;
			}
		}
			
		for (Production p : startSymbol.productions) {
		  p.reachable = true;
		  Set<Production> s = tc.get(p);
		  if (s != null) {
		    for (Production p2 : s) {
		      p2.reachable = true;
		    }
		  }
		}
		
		// calculate usedForReject flags
		nrReachableProductions = 0;
		Set<Production> rejects = new ShareableHashSet<Production>();
		for (Production p : productions) {
			if (p.reject) {
				rejects.add(p);
				p.usedForReject = true;
				Set<Production> s = tc.get(p);
				if (s != null) {
					for (Production p2 : s) {
						p2.usedForReject = true;
					}
				}
			}
			if (p.reachable) {
				nrReachableProductions++;
			}
		}
		
		
		// calculate reachable and usedForReject flags for nonterminals
		for (NonTerminal n : nonTerminals.values()) {
			n.reachable = n.usedForReject = false; // reset
		}
		
		for (Production p : productions) {
			if (p.reachable || p.usedForReject) {
				p.lhs.reachable |= p.reachable;
				p.lhs.usedForReject |= p.usedForReject;
				for (int i = 0; i < p.getLength(); i++) {
					Symbol s = p.getSymbolAt(i);
					if (s instanceof NonTerminal) {
						NonTerminal n = (NonTerminal) s;
						n.reachable |= p.reachable;
						n.usedForReject |= p.usedForReject;
					}
				}
			}
		}
		
		// remove unused nonterminals
		Set<NonTerminal> unreachableN = new ShareableHashSet<NonTerminal>();
		for (NonTerminal n : nonTerminals.values()) {
			if (!n.reachable && !n.usedForReject) {
				if (AmbiDexterConfig.verbose) {
					System.out.println("Unused nonterminal: " + n);
				}
				unreachableN.add(n);
			}
		}
		for (NonTerminal n : unreachableN) {
			nonTerminals.remove(n.toString());
			for (Production p : n.productions) {
				productions.remove(p);
			}			
		}
		
		// remove unused productions of used nonterminals
		Set<Production> unreachableP = new ShareableHashSet<Production>();
		for (Production p : productions) {
			if (!(p.reachable || p.usedForReject)) {
				if (AmbiDexterConfig.verbose) {
					System.out.println("Unreachable production: " + p);
				}
				p.lhs.productions.remove(p);
				unreachableP.add(p);
			}
		}
		productions.removeAll(unreachableP);
		
		// check for unproductive nonterminals
		Set<NonTerminal> unproductive = getUnproductiveNonterminals();
		
		/*if (scannerless && unproductive.contains(startSymbol)) {
			// for SDF: if no start symbol is given, try if we can find one
			Set<Production> prds = new ShareableHashSet<Production>();
			Set<NonTerminal> nts = new ShareableHashSet<NonTerminal>();
			for (NonTerminal n : nonTerminals.values()) {
				if (!n.toString().equals("FILE-START") && !n.toString().equals("START")) {
					nts.add(n);
					prds.addAll(n.productions);
				}
			}
			ESet<NonTerminal> starts = findStartSymbols(prds, nts, null);
			if (starts.size() == 1) {
				startSymbol = starts.removeOne();
				System.out.println("Unproductive start symbol, inferred new one: " + startSymbol);
			} else {
				throw new RuntimeException("Unproductive start symbol, could not infer new one. Possible options: " + starts);
			}
			
			unproductive = getUnproductiveNonterminals();
		}*/
		
		
		if (unproductive.contains(startSymbol)) {
			throw new RuntimeException("Unproductive start symbol: " + startSymbol);
		}
		
		for (NonTerminal n : unproductive) {
			//throw new RuntimeException("Improductive nonterminal: " + n);
			if (AmbiDexterConfig.verbose) {
				System.out.println("Unproductive nonterminal: " + n);
			}
			n.productive = false;
		}
		
		// TODO remove productions that are only used for rejects of unused nonterminals
		
		// propagate usedInLayout flags
		for (NonTerminal n : nonTerminals.values()) {
			n.usedInLayout = false; // reset first
		}
			
		for (NonTerminal n : nonTerminals.values()) {
			if (n.reachable && n.layout) {
				n.usedInLayout = true;
				for (Production p : n.productions) {
					if (p.reachable) {
						Set<Production> tcp = tc.get(p);
						if (tcp != null) {
							for (Production p2 : tcp) {
								if (p2.reachable) {
									for (int i = p2.getLength() - 1; i >= 0; --i) {
										Symbol s = p2.getSymbolAt(i);
										if (s instanceof NonTerminal) {
											NonTerminal n2 = (NonTerminal) s;
											if (n2.reachable) {
												n2.usedInLayout = true;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	// call after calcReachableAndReject()
	public void splitReachableAndReject() {
		Set<Production> newRejects = new ShareableHashSet<Production>();
		
		// clone productions that are used in reachable part and reject part
		for (Production p : productions) {
			if (p.reachable && p.usedForReject) {
				Production p2 = cloneProduction(p);
				p2.reachable = false;
				p2.usedForReject = true;
				newRejects.add(p2);
				p.usedForReject = false;
			}
		}
		
		for (Production p : newRejects) {
			addProduction(p);
		}
	}
	
	public ShareableHashMap<NonTerminal, Set<NonTerminal>> getInjectionTC() {
		
		ShareableHashMap<NonTerminal, Set<NonTerminal>> m = new ShareableHashMap<NonTerminal, Set<NonTerminal>>();
		for (NonTerminal n : nonTerminals.values()) {
			m.put(n, new ShareableHashSet<NonTerminal>());
		}
		for (Production p : productions) {
			if (p.isInjection()) {
				m.get(p.lhs).add((NonTerminal)p.getSymbolAt(0));
			}
		}
		
		return Util.transitiveClosure2(m);
	}

	public Map<NonTerminal,Set<LinkedList<Production>>> getInjectionChains() {
		// inverted!!
		// for instance: <A, {B->A, C->A}> 
		Set<LinkedList<Production>> chains = new ShareableHashSet<LinkedList<Production>>();
		for (Production p : productions) {
			if (p.isInjection()) {
				LinkedList<Production> l = new LinkedList<Production>(p);
				chains.add(l);
			}
		}

		Set<LinkedList<Production>> todo = chains;
		do {
			Set<LinkedList<Production>> newChains = new ShareableHashSet<LinkedList<Production>>();
			for (LinkedList<Production> l : todo) {
				continueInjectionChain(l, newChains);
			}
			chains.addAll(newChains);
			todo = newChains;			
		} while (todo.size() > 0);

		// inverted
		// for instance: <A, [B->C, A->B]>
		Map<NonTerminal, Set<LinkedList<Production>>> m = new ShareableHashMap<NonTerminal, Set<LinkedList<Production>>>();
		for (LinkedList<Production> l : chains) {
			while (l.next != null) l=l.next;
			NonTerminal n = l.elem.lhs;
			Set<LinkedList<Production>> s = m.get(n);
			if (s == null) {
				s = new ShareableHashSet<LinkedList<Production>>();
				m.put(n, s);
			}
			s.add(l);
		}

		return m;
	}
	
	private void continueInjectionChain(LinkedList<Production> l, Set<LinkedList<Production>> s) {
		NonTerminal end = (NonTerminal) l.elem.getSymbolAt(0);
		for (Production p : end.productions) {
			if (p.isInjection()) {
				LinkedList<Production> c = l;
				boolean allowed = true;
				do {
					if (!c.elem.isDerivationAllowed(p, 0)) {
						allowed = false;
						break;
					}
					c = c.next;
				} while (c != null);
				
				if (allowed) {
					s.add(new LinkedList<Production>(p, l));
				}
			}
		}
	}

	public boolean setNewStartSymbol(String s) {
		NonTerminal n = nonTerminals.get(s);
		if (n != null) {
			startSymbol = n;
			return true;
		} else {
			return false;
		}
	}

	public void removeLexicalPart() {
		Set<NonTerminal> tokenize = new ShareableHashSet<NonTerminal>();
		for (NonTerminal n : nonTerminals.values()) {
			if (!n.lexical && n.productions.size() == 1) {
				Production p = n.productions.iterator().next();
				if (p.getLength() == 1) {
					Symbol s = p.rhs.get(0);
					if (s instanceof NonTerminal && ((NonTerminal) s).lexical) {
						tokenize.add(n);
					}
				}
			}
			if (n.literal) {
				tokenize.add(n);
			}
		}
		
		//System.out.println(tokenize);
		
		Set<Production> newProd = new ShareableHashSet<Production>();
		for (Production p : productions) {
			if (!tokenize.contains(p.lhs) && !p.lhs.lexical && !p.lhs.literal) {
				if (p.isCfToLex()) {
					p.lhs.productions.remove(p);
				} else {
					for (int i = 0; i < p.getLength(); i++) {
						Symbol s = p.getSymbolAt(i);
						if (tokenize.contains(s)) {
							p.setSymbol(i, getDummyTerminal((NonTerminal)s));
						}
					}
					newProd.add(p);
				}
			}
		}
		productions = newProd;
		
		Set<String> remove = new ShareableHashSet<String>();
		for (NonTerminal n : nonTerminals.values()) {
			if (n.lexical || n.literal || tokenize.contains(n)) {
				remove.add(n.s);
			}
		}
		for (String s : remove) {
			nonTerminals.remove(s);
		}
	}

	public void printNonTerminalUsage(IAmbiDexterMonitor monitor) {
		Map<Symbol, Integer> usage = new ShareableHashMap<Symbol, Integer>();
		for (Production p : productions) {
			if (p.reachable) {
				for (int i = 0; i < p.getLength(); i++) {
					Symbol s = p.getSymbolAt(i);
					if (s instanceof NonTerminal) {
						Integer u = usage.get(s);
						if (u == null) {
							usage.put(s, 1);
						} else {
							usage.put(s, u + 1);
						}
					}
				}
			}
		}

		Relation<Integer, Symbol> inv = new Relation<Integer, Symbol>();
		for (Entry<Symbol, Integer> e : usage.entrySet()) {
			inv.add(e.getValue(), e.getKey());
		}
		
		Object nrs[] = inv.domain().toArray();
		Arrays.sort(nrs);
		for (int i = nrs.length - 1; i >= 0; i--) {
			int nr = (Integer) nrs[i];
			for (Symbol s : inv.get(nr)) {
				monitor.println("" + s + ": " + nr);
			}
		}
	}

	public Production getProduction(String ps, boolean reachable) {
		for (Production p : productions) {
			if (p.toString().equals(ps) && p.reachable == reachable) {
				return p;
			}
		}
		return null;
	}
	
	public Symbol getSymbol(String ss) {
		if (ss.startsWith("[]")) {
			return new CharacterClass(ss);
		}
		
		Symbol s = nonTerminals.get(ss);
		if (s != null) {
			return s;
		}
		
		s = terminals.get(ss);
		if (s != null) {
			return s;
		}
		
		return null;
	}
}
