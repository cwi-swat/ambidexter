package nl.cwi.sen1.AmbiDexter.grammar.importexport;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.FollowRestrictions;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.ListNonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.OptionalNonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.LinkedList;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermInt;
import aterm.ATermList;
import aterm.pure.SingletonFactory;

public class SDFImporter extends GrammarImporter {
	
	Map<ATerm, CharacterClass> characterClasses = new ShareableHashMap<ATerm, CharacterClass>();
	Map<ATerm, NonTerminal> nonterminals = new ShareableHashMap<ATerm, NonTerminal>();
	AmbiDexterConfig config = null;
	
	public SDFImporter() {
	}

	@Override
	public Grammar importGrammar(String filename, AmbiDexterConfig config) {
		this.config = config;
		
		ATerm term = null;
		try {
			term = SingletonFactory.getInstance().readFromFile(filename);
		} catch (IOException e) {
			throw new RuntimeException("Cannot read file " + filename);
		}
		
		g = new Grammar(filename, true, config.doRejects, config.doFollowRestrictions);
		
		readConcGrammars((ATermAppl)term);
		g.layoutOpt = g.getNonTerminal("cf(LAYOUT?)");
		g.layout = g.getNonTerminal("cf(LAYOUT)");
		setRejects();
		
		simplifyIterProductions(); // TODO check to see if we can do this?? (b/c of manually added iter productions)
		
		if (config.doPriorities) {
			g.nrPrioritiesRead = priorities.size();
			prioritiesTransitiveClosure();
			addPrioritiesToProductions();
			if (AmbiDexterConfig.verbose) {
				System.out.println("Priorities read: " + g.nrPrioritiesRead);
				System.out.println("Priorities after closure: " + priorities.size());
			}
		}		
		
		g.startSymbol = g.getNonTerminal("FILE-START");
		return g;
	}

	private void readConcGrammars(ATermAppl conc) {
		for (int i = 0; i < conc.getArity(); i++) {
			ATerm a = conc.getArgument(i); 
			if (checkAppl(a, "syntax")) {
				readSyntax(castAppl(a, "syntax"));
			} else if (checkAppl(a, "priorities")) {
				readPriorities(castAppl(a, "priorities"));
			} else if (checkAppl(a, "restrictions")) {
				if (config.doFollowRestrictions) {
					readRestrictions(castAppl(a, "restrictions"));
				}
			} else if (checkAppl(a, "conc-grammars")) {
				readConcGrammars(castAppl(a, "conc-grammars"));
			} else if (a instanceof ATermList) {
				ATermList l = (ATermList) a;
				for (int j = 0; j < l.getLength(); j++) {
					ATerm b = l.elementAt(j);
					if (checkAppl(b, "chain")) {
						ATermList l2 = (ATermList) castAppl(b, "chain").getArgument(0);
						for (int k = 0; k < l2.getLength(); k++) {
							readProductionGroup(l2.elementAt(k));
						}
					}
				}
			} else if (checkAppl(a, "appl")) {
				System.out.println(a);
			} else {
				throw new RuntimeException("Unknown term " + a);
			}
		}		
	}
	
	private void readSyntax(ATermAppl syntax) {
		ATermList prods = (ATermList) syntax.getArgument(0);
		for (int i = 0; i < prods.getLength(); i++) {
			ATermAppl prod = castAppl(prods.elementAt(i), "prod");
			getProduction(prod); // creates the Production if it didn't exists already
		}
	}

	private void readProductionGroup(ATerm group) {
		if (checkAppl(group, "simple-group")) {
			ATermAppl prod = castAppl(castAppl(group, "simple-group").getArgument(0), "prod");
			getProduction(prod);
		} else if (checkAppl(group, "prods-group")) {
			readSyntax(castAppl(group, "prods-group"));			
		}
	}

	/***************************************************************************************/
	
	Map<Pair<ATerm, ATerm>, Production> applProds = new ShareableHashMap<Pair<ATerm, ATerm>, Production>();
	Map<ATerm, Symbol> applSymbols = new ShareableHashMap<ATerm, Symbol>();
	Relation<Production, Integer> ignoredLayout = new Relation<Production, Integer>();
	
	private Production getProduction(ATermAppl a) {
		Pair<ATerm, ATerm> pair = new Pair<ATerm, ATerm>(a.getArgument(0), a.getArgument(1));
		Production p = applProds.get(pair);
		if (p != null) return p;

		NonTerminal n = (NonTerminal) getSymbol(a.getArgument(1));
		p = g.newProduction(n);
		
		ATermList rhs = (ATermList) a.getArgument(0);
		Set<Integer> skipped = new ShareableHashSet<Integer>();
		for (int i = 0; i < rhs.getLength(); i++) {
			Symbol s = getSymbol(rhs.elementAt(i));
			p.addSymbol(s);
		}
//		
//		// check for cf(list) -> lex(list)
//		if (n.list && !n.lexical && rhs.getLength() == 1) {
//			Symbol s = getSymbol(rhs.elementAt(0));
//			if (s instanceof NonTerminal) {
//				NonTerminal e = (NonTerminal) s;
//				if (e.list && e.lexical) {
//					n.listIsLexical = true;
//				}
//			}
//		}
		
		g.addProduction(p);
		applProds.put(pair, p);
		ignoredLayout.put(p, skipped);

		readAttributes(a.getArgument(2), p);
		
		return p;
	}

	private void readAttributes(ATerm a, Production p) {
		if (checkAppl(a, "attrs")) {
			ATermAppl attrs = castAppl(a, "attrs");
			ATermList list = (ATermList)attrs.getArgument(0);
			for (int i = 0; i < list.getLength(); i++) {
				ATerm attr = list.elementAt(i);
				if (checkAppl(attr, "reject")) {
					p.reject = true;
				} else if (checkAppl(attr, "prefer")) {
					p.prefer = true;
				} else if (checkAppl(attr, "avoid")) {
					p.avoid = true;
				}
			}
		}		
	}

	protected Symbol getSymbol(ATerm a) {
		Symbol s = applSymbols.get(a);
		if (s == null) {
			if (checkAppl(a, "char-class")) {
				s = getCharacterClass(a);
			} else if (checkAppl(a, "label")) {
				ATermAppl label = castAppl(a, "label");
				return getSymbol(label.getArgument(1));
			} else {
				s = getNonTerminal(a);
			}
			applSymbols.put(a, s);
		}
		return s;
	}

	private NonTerminal getNonTerminal(ATerm a) {
		NonTerminal n = nonterminals.get(a);
		if (n == null) {
			switch (isListSort(a)) {
			case 0:
				n = new NonTerminal("");
				break;
			case 1:
				n = new ListNonTerminal("");
				break;
			case 2:
				n = new OptionalNonTerminal("");
			}
			String s = readSort(a, n);
			n.s = s;
			if (n.asfixName == null) {
				n.asfixName = getAsfixName(a);
			}
			
			g.nonTerminals.put(n.s, n);
			nonterminals.put(a, n);
		}
		return n;
	}
	
	private String getAsfixName(ATerm a) {
		
		if (a.getType() == ATerm.APPL && a.getChildCount() > 0) {
			ATermAppl aa = (ATermAppl) a;
			String s = aa.getName() + "(";
			if (s.equals("seq(")) {
				s += "[";
				ATermList l = aa.getArguments();
				s += getAsfixName(l.elementAt(0));
				l = (ATermList) l.elementAt(1);
				for (int i = 0; i < l.getLength(); ++i) {
					s += ",";
					s += getAsfixName(l.elementAt(i));
				}				
				return s + "])";
			} else {
				ATermList l = aa.getArguments();
				for (int i = 0; i < l.getLength(); ++i) {
					if (i > 0) {
						s += ",";
					}
					s += getAsfixName(l.elementAt(i));
				}
				return s + ")";			
			}
		} else {
			return a.toString();
		}
	}

	private String readSort(ATerm a, NonTerminal n) {
		if (checkAppl(a, "cf")) {
			if (n != null) n.lexical = false;
			return "cf(" + readSort(castAppl(a, "cf").getArgument(0), n) + ")";
			
		} else if (checkAppl(a, "lex")) {
			if (n != null) n.lexical = true;
			return "lex(" + readSort(castAppl(a, "lex").getArgument(0), n) + ")";
			
		} else if (checkAppl(a, "layout")) {
			if (n != null) {
				n.layout = true;
				n.asfixName = "cf(sort(\"LayoutDummy\"))";
			}
			return "LAYOUT";
			
		} else if (checkAppl(a, "start")) {
			if (n != null) n.lexical = false;
			return "START";
			
		} else if (checkAppl(a, "file-start")) {
			if (n != null) n.lexical = false;
			return "FILE-START";
			
		} else if (checkAppl(a, "opt")) {
			return readSort(castAppl(a, "opt").getArgument(0), null) + "?";
			
		} else if (checkAppl(a, "iter")) {
			ATerm elem = castAppl(a, "iter").getArgument(0);
			if (n != null) {
				//((ListNonTerminal) n).listElem = SDFImporter.this.getSymbol(elem);
			}
			return readSort(elem, null) + "+";
			
		} else if (checkAppl(a, "iter-star")) {
			ATerm elem = castAppl(a, "iter-star").getArgument(0);
			if (n != null) {
				((ListNonTerminal) n).star = true;
				//((ListNonTerminal) n).listElem = SDFImporter.this.getSymbol(elem);
			}
			return readSort(elem, null) + "*";

		} else if (checkAppl(a, "iter-sep")) {
			ATerm elem = castAppl(a, "iter-sep").getArgument(0);
			ATerm sepa = castAppl(a, "iter-sep").getArgument(1);
			if (n != null) {
				((ListNonTerminal) n).sep = true;
				//((ListNonTerminal) n).listElem = SDFImporter.this.getSymbol(elem);
				//((ListNonTerminal) n).separator = SDFImporter.this.getSymbol(sepa);					
			}
			return "{" + readSort(elem, null) + " " + readSort(sepa, null) + "}+";
		
		} else if (checkAppl(a, "iter-star-sep")) {
			ATerm elem = castAppl(a, "iter-star-sep").getArgument(0);
			ATerm sepa = castAppl(a, "iter-star-sep").getArgument(1);
			if (n != null) {
				((ListNonTerminal) n).star = true;
				((ListNonTerminal) n).sep = true;
				//((ListNonTerminal) n).listElem = SDFImporter.this.getSymbol(elem);
				//((ListNonTerminal) n).separator = SDFImporter.this.getSymbol(sepa);					
			}
			return "{" + readSort(elem, null) + " " + readSort(sepa, null) + "}*";
		
		} else if (checkAppl(a, "alt")) {
			return readSort(castAppl(a, "alt").getArgument(0), null) + " | " + readSort(castAppl(a, "alt").getArgument(1), null);
		
		} else if (checkAppl(a, "char-class")) {
			return getCharacterClass(a).toString();
		
		} else if (checkAppl(a, "sort")) {
			String s = readString(castAppl(a, "sort").getArgument(0));
			return s;
		
		} else if (checkAppl(a, "lit")) {
			if (n != null) {
				n.literal = true;
			}
			String s;
			try {
				s = readString(castAppl(a, "lit").getArgument(0));
			} catch (Exception e) {
				s = castAppl(a, "lit").getArgument(0).toString();
			}
			return "\"" + s + "\"";
		
		} else if (checkAppl(a, "ci-lit")) {
			if (n != null) {
				n.literal = true;
			}
			return "\'" + readString(castAppl(a, "ci-lit").getArgument(0)) + "\'";
		
		} else if (checkAppl(a, "seq")) {
			String s = "(" + readSort(castAppl(a, "seq").getArgument(0), null);
			ATermList list = (ATermList) castAppl(a, "seq").getArgument(1);
			for (int i = 0; i < list.getLength(); i++) {
				s += " " + readSort(list.elementAt(i), null);
			}
			return s + ")";
		
		} else if (checkAppl(a, "parameterized-sort")) {
			ATermAppl param = castAppl(a, "parameterized-sort");
			ATermList args = (ATermList) param.getArgument(1);
			String s = readString(param.getArgument(0)) + "[";
			for (int i = 0; i < args.getLength(); i++) {
				s += (i == 0 ? "" : ",") + readSort(args.elementAt(i), null);
			}
			return s + "]";
			
		} else if (checkAppl(a, "empty")) {
			return "empty"; // TODO what to do here?
		} else {
			throw new RuntimeException("unknown term " + a);
		}
	}
	
	// 0 = normal
	// 1 = list
	// 2 = optional
	private int isListSort(ATerm a) {
		if (checkAppl(a, "cf")) {
			return isListSort(castAppl(a, "cf").getArgument(0));
		} else if (checkAppl(a, "lex")) {
			return isListSort(castAppl(a, "lex").getArgument(0));	
		} else if (checkAppl(a, "opt")) {
			return 2; 
		} else if (checkAppl(a, "iter") || checkAppl(a, "iter-star") ||
				   checkAppl(a, "iter-sep") || checkAppl(a, "iter-star-sep")) {
			return 1;		
		} else {
			return 0;
		}
	}

	private void simplifyIterProductions() {
		Set<Production> removeProds = new ShareableHashSet<Production>();
		ShareableHashMap<ListNonTerminal, ListNonTerminal> starPlus = new ShareableHashMap<ListNonTerminal, ListNonTerminal>();
		for (NonTerminal n : nonterminals.values()) {
			if (n instanceof OptionalNonTerminal) {
				OptionalNonTerminal on = (OptionalNonTerminal) n;
				for (Production p : on.productions) {
					if (p.getLength() == 1) {
						on.elem = p.getSymbolAt(0);
					} else {
						on.emptyReduce = p.reduction;
					}
				}
			} else if (n instanceof ListNonTerminal) {
				ListNonTerminal ln = (ListNonTerminal) n;
				if (ln.star) {
					// remove * -> * *
					// identical for cf,lex,sep,no-sep
					for (Production p : ln.productions) {
						if (p.getLength() > 1) {
							removeProds.add(p);
						}
						if (p.getLength() == 1) {
							starPlus.put(ln, (ListNonTerminal) p.getSymbolAt(0));
						}
					}
				} else { // + lists
					// remove all but + -> e
					for (Production p : ln.productions) {
						if (p.getLength() == 1) {
							Symbol s = p.getSymbolAt(0);
							if (!(!ln.lexical && (s instanceof ListNonTerminal && ((ListNonTerminal) s).lexical))) {
								ln.listElem = s;
							} // else it's cf() -> lex()
						} else {
							if (ln.sep && ln.separator == null) {
								if (n.lexical) {
									ln.separator = p.getSymbolAt(1);
								} else {
									ln.separator = p.getSymbolAt(2);
								}
							}
							removeProds.add(p);
						}
					}
					
					// add + -> + e
					if (ln.listElem != null) {
						Production p = g.newProduction(ln);
						p.addSymbol(ln);
						if (!ln.lexical) {
							p.addSymbol(g.layoutOpt);
						}
						if (ln.sep) {
							p.addSymbol(ln.separator);
							if (!ln.lexical) {
								p.addSymbol(g.layoutOpt);
							}
						}
						p.addSymbol(ln.listElem);
						g.addProduction(p);
					}
				}
			}
		}
		
		// transfer listElem and separator from plus to star
		// b/c star is encoded as plus or empty during normalizing
		for (Entry<ListNonTerminal, ListNonTerminal> e : starPlus) {
			e.getKey().listElem = e.getValue().listElem;
			e.getKey().separator = e.getValue().separator;
		}
		
		/*// check if all list nonterminals have elements and/or separators.
		// doesn't matter b/c these are inproductive anyway 
		for (NonTerminal n : nonterminals.values()) {
			if (n instanceof ListNonTerminal) {
				ListNonTerminal ln = (ListNonTerminal) n;
				if (ln.listElem == null || (ln.sep && ln.separator == null)) {
					ln = ln;
				}
			}
		}*/
		
		for (Production p : removeProds) {
			p.lhs.productions.remove(p);
		}
		g.productions.removeAll(removeProds);
		
		// remove all priorities on removed iter productions
		Set<Priority> removePrios = new ShareableHashSet<Priority>();
		for (Priority p : priorities) {
			if (removeProds.contains(p.p1) || removeProds.contains(p.p2)) {
				int l = 0;
				if (p.p1.lhs instanceof ListNonTerminal) {
					l++;
				}
				if (p.p2.lhs instanceof ListNonTerminal) {
					l++;
				}
				if (l == 1) {
					throw new RuntimeException("Manually added priorities over normalized list production not supported:\n" + p);
				}
				removePrios.add(p);
			}			
		}
		priorities.removeAll(removePrios);
	}
	
	/***************************************************************************************/
		
	private static int characterClassSize(ATerm a) {
		if (checkAppl(a, "char-class")) return characterClassSize(((ATermAppl) a).getArgument(0));
		if (checkAppl(a, "simple-charclass")) return characterClassSize(((ATermAppl) a).getArgument(0));
		if (checkAppl(a, "present")) return characterClassSize(((ATermAppl) a).getArgument(0));
		if (checkAppl(a, "numeric")) return 1;
		if (checkAppl(a, "range")) return 1;
		if (checkAppl(a, "conc")) {
			ATermAppl conc = castAppl(a, "conc");
			int s = 0;
			for (int i = 0; i < conc.getArity(); i++) {
				s += characterClassSize(conc.getArgument(i));
			}
			return s;
		}
		
		throw new RuntimeException("Unknown term " + a);
	}
	
	private static void readCharClass(ATerm a, CharacterClass cc) {		
		if (checkAppl(a, "char-class")) readCharClass(((ATermAppl) a).getArgument(0), cc);
		else if (checkAppl(a, "simple-charclass")) readCharClass(((ATermAppl) a).getArgument(0), cc);
		else if (checkAppl(a, "present")) readCharClass(((ATermAppl) a).getArgument(0), cc);
		else if (checkAppl(a, "numeric")) {
			String s = readString(((ATermAppl) a).getArgument(0)); 
			cc.append(Integer.parseInt(s.replace("\\", "")));
		}
		else if (checkAppl(a, "conc")) {
			ATermAppl conc = castAppl(a, "conc");
			for (int i = 0; i < conc.getArity(); i++) {
				readCharClass(conc.getArgument(i), cc);
			}
		} else if (checkAppl(a, "range")) {
			ATermAppl range = castAppl(a, "range");
			String s1 = readString(castAppl(range.getArgument(0), "numeric").getArgument(0)); 
			String s2 = readString(castAppl(range.getArgument(1), "numeric").getArgument(0));
			int c1 = Integer.parseInt(s1.replace("\\", ""));
			int c2 = Integer.parseInt(s2.replace("\\", ""));
			cc.append(c1, c2);
		} else throw new RuntimeException("Unknown term " + a);
	}

	private CharacterClass getCharacterClass(ATerm a) {
		CharacterClass cc = characterClasses.get(a);
		if (cc == null) {
			cc = new CharacterClass(characterClassSize(a) * 2, a.getUniqueIdentifier());
			readCharClass(a, cc);
			characterClasses.put(a, cc);
		}
		return cc;
	}

	/***************************************************************************************/
	
	Set<Priority> priorities = new ShareableHashSet<Priority>();
	
	private void readPriorities(ATermAppl prios) {
		ATermList chains = (ATermList) prios.getArgument(0);
		for (int i = 0; i < chains.getLength(); i++) {
			ATermList chain = (ATermList) castAppl(chains.elementAt(i), "chain").getArgument(0);
			Priority p1 = new Priority();
			getChainPart(chain.elementAt(0), p1);
			for (int j = 1; j < chain.getLength(); j++) {
				Priority p2 = new Priority(); 
				getChainPart(chain.elementAt(j), p2);
				p1.p2 = p2.p1;
				if (p1.p2 != null && p1.p2 != null) { // productions can be null if layout is ignored
					priorities.add(p1);
				}
				
				/*if (p1.p1.isInjection())
					throw new RuntimeException("aargh");
				if (p1.p2.isInjection())
					throw new RuntimeException("oorgh");*/

				p1 = p2;
			}
		}
	}
	
	private void getChainPart(ATerm a, Priority p) {
		if (checkAppl(a, "non-transitive")) {
			p.transitive = false;
			getChainPart(castAppl(a, "non-transitive").getArgument(0), p);
		} else
		if (checkAppl(a, "with-arguments")) {
			p.pos = readInt(((ATermList)castAppl(castAppl(a, "with-arguments").getArgument(1), "default").getArgument(0)).elementAt(0));
			getChainPart(castAppl(a, "with-arguments").getArgument(0), p);			
		} else
		if (checkAppl(a, "simple-group")) {
			p.p1 = getProduction(castAppl(castAppl(a, "simple-group").getArgument(0), "prod"));
		} else		
			throw new RuntimeException("Unknown term: " + a);
	}
	
	private void prioritiesTransitiveClosure() {		
		int oldSize = 0;
		while (oldSize != priorities.size()) {
			oldSize = priorities.size();
			
			Set<Priority> add = new ShareableHashSet<Priority>();
			for (Priority p : priorities) {
				if (p.transitive) {
					for (Priority q : priorities) {
						if (q.transitive && p.p2 == q.p1) {
							Priority n = new Priority();
							n.p1 = p.p1;
							n.p2 = q.p2;
							n.pos = p.pos;
							n.transitive = true;
							add.add(n);
						}
					}
				}
			}
			
			priorities.addAll(add);
		}
	}
	
	private void addPrioritiesToProductions() {
		for (Priority prio : priorities) {
			if (prio.pos == -1) {
				for (int i = 0; i < prio.p1.getLength(); i++) {
					Symbol s = prio.p1.getSymbolAt(i);
					if (s == prio.p2.lhs) {
						prio.p1.addDeriveRestriction(i, prio.p2);
					}
				}
			} else {
				prio.p1.addDeriveRestriction(prio.pos, prio.p2);
			}
		}		
	}


	/***************************************************************************************/
	
	private void readRestrictions(ATermAppl restr) {
		ATermList l = (ATermList) restr.getArgument(0);
		for (int i = 0; i < l.getLength(); i++) {
			ATerm a = l.elementAt(i);
			if (checkAppl(a, "follow")) {
				ATermAppl follow = castAppl(a, "follow");
				ATermList nonterms = (ATermList) follow.getArgument(0);
				ATermAppl list = castAppl(follow.getArgument(1), "list");
				ATermList seqs = (ATermList) list.getArgument(0);

				FollowRestrictions restrictions = new FollowRestrictions();
				for (int j = 0; j < seqs.getLength(); j++) {
					restrictions.add(getRestrictionSequence(seqs.elementAt(j)));
				}
				
				for (int j = 0; j < nonterms.getLength(); j++) {
					NonTerminal n = (NonTerminal) getSymbol(nonterms.elementAt(j));
					if (n.followRestrictions == null) {
						n.followRestrictions = new FollowRestrictions();
					}
					n.followRestrictions.add(restrictions);
				}
			} else {
				throw new RuntimeException("Unknown term " + a);
			}
		}		
	}
	
	private LinkedList<CharacterClass> getRestrictionSequence(ATerm a) {
		LinkedList<CharacterClass> l = new LinkedList<CharacterClass>();
		if (checkAppl(a, "seq")) {
			ATermAppl seq = castAppl(a, "seq");
			l.elem = getCharacterClass(seq.getArgument(0));
			ATermAppl list = castAppl(seq.getArgument(1), "list");
			l.next = getRestrictionSequence(((ATermList)list.getArgument(0)).elementAt(0));
		} else {
			l.elem = getCharacterClass(a);
		}

		return l;
	}
	
	private void setRejects() {
		for (NonTerminal n : nonterminals.values()) {
			Set<NonTerminal> rejects = new ShareableHashSet<NonTerminal>();
			for (Production p : n.productions) {
				if (p.reject) {
					getRejectedLiterals(p, rejects);
				}
			}
			if (rejects.size() > 0) {
				n.rejectedLiterals = rejects;
			}
		}
	}
	
	private void getRejectedLiterals(Production p, Set<NonTerminal> rejects) {
		if (p.getLength() == 1) {
			Symbol s = p.getSymbolAt(0);
			if (s instanceof NonTerminal) {
				NonTerminal n = (NonTerminal) s;
				if (n.literal) {
					rejects.add(n);
					n.usedInRejectFilter = true;
				} else {
					for (Production prod : n.productions) {
						getRejectedLiterals(prod, rejects);
					}
				}
				return;
			}
		}
		//throw new RuntimeException("Unsupported rejected production: " + p);
//		if (!Main.quick) {
//			System.out.println("Unsupported rejected production: " + p);
//		}
	}

	/***************************************************************************************/
	
	private int readInt(ATerm a) {
		if (a.getType() == ATerm.INT) {
			return ((ATermInt) a).getInt();
		}
		throw new RuntimeException("ATerm is not an int: " + a);
	}
	
	private static String readString(ATerm a) {
		if (a.getType() == ATerm.APPL) {
			ATermAppl apple = (ATermAppl) a;
			if (apple.getArity() == 0) {
				return apple.getName();
			}
		}
		throw new RuntimeException("ATerm is not a string: " + a);
	}
	
	private static ATermAppl castAppl(ATerm a, String name) {
		if (checkAppl(a, name)) {
			return (ATermAppl) a;
		}
		throw new RuntimeException("ATerm is not a " + name + " appl: " + a);
	}
	
	private static boolean checkAppl(ATerm a, String name) {
		return (a.getType() == ATerm.APPL && ((ATermAppl) a).getName().equals(name));
	}
	
	/***************************************************************************************/
	
	private static class Priority {
		Production p1 = null;
		Production p2 = null;
		int	pos = -1;
		boolean transitive = true;		
		
		public Priority() {}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
			result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
			result = prime * result + pos;
			result = prime * result + (transitive ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Priority))
				return false;
			Priority other = (Priority) obj;
			if (p1 == null) {
				if (other.p1 != null)
					return false;
			} else if (!p1.equals(other.p1))
				return false;
			if (p2 == null) {
				if (other.p2 != null)
					return false;
			} else if (!p2.equals(other.p2))
				return false;
			if (pos != other.pos)
				return false;
			if (transitive != other.transitive)
				return false;
			return true;
		}
		
		public String toString() {
			return p1.toString() + " (" + pos + (transitive ? ")" : ").") + " >\n" + p2; 
		}
	}
}
