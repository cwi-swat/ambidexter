package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.AmbiguityDetector.DetectionMethod;
import nl.cwi.sen1.AmbiDexter.automata.ItemPDA;
import nl.cwi.sen1.AmbiDexter.automata.LALR1NFA;
import nl.cwi.sen1.AmbiDexter.automata.LR0NFA;
import nl.cwi.sen1.AmbiDexter.automata.LR1NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.PDA;
import nl.cwi.sen1.AmbiDexter.automata.ProductionPDA;
import nl.cwi.sen1.AmbiDexter.automata.SLR1NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.derivgen.DerivationGenerator;
import nl.cwi.sen1.AmbiDexter.derivgen.ParallelDerivationGenerator;
import nl.cwi.sen1.AmbiDexter.derivgen.ScannerlessDerivGen1;
import nl.cwi.sen1.AmbiDexter.derivgen.ScannerlessDerivGen2;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.GrammarMetrics;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;
import nl.cwi.sen1.AmbiDexter.grammar.importexport.GrammarExporter;
import nl.cwi.sen1.AmbiDexter.grammar.importexport.GrammarImporter;
import nl.cwi.sen1.AmbiDexter.nu2.NoncanonicalUnambiguityTest;
import nl.cwi.sen1.AmbiDexter.parse.SGLRStub;
import nl.cwi.sen1.AmbiDexter.parse.SimpleSGLRParser;
import nl.cwi.sen1.AmbiDexter.test.ScannerlessDerivGenTest;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.Util;

public class Main {
	
	public final static int LR0 = 0;
	public final static int SLR1 = 1;
	public final static int LALR1 = 2;
	public final static int LR1 = 3;
	final static String precisionName[] = {
		"lr0",
		"slr1",
		"lalr1",
		"lr1",
		"Mohri-Nederhof"
	};
	
	DetectionMethod method = DetectionMethod.NU;
	int derivGenDepth = 10; // depth of derivation generation
	int derivGenDealLength = 0;
	int threads = 1; // parallel derivation generation
	boolean incrementalDerivGen = false;
	String alternativeStartSymbol;
	public static boolean findCommonTops = false;
	public static boolean findHarmlessProductions = false;
	public static boolean outputGraphs = false;
	public static boolean outputDistMap = false;
	boolean outputStatistics = false;
	boolean outputReconstructedGrammar = false;
	boolean outputReconstructedGrammarYacc = false;
	boolean outputReconstructedGrammarCFGA = false;
	boolean outputReconstructedGrammarAccent = false;
	boolean outputReconstructedGrammarDKBrics = false;
	boolean readDFA = false;
	public static boolean writeDFA = false;
	String dfaName;
	
	boolean convert = false;
	public static boolean alternating = false;
	public static boolean filterUnmatchedDerivesReduces = false;

	public static boolean unfoldStronglyConnectedComponents = false;
	public static boolean unfoldNonRecursiveTails = false;
	public static boolean unfoldJoiningTails = false;
	public static boolean unfoldOnlyLexicalTails = false;
	public static boolean unfoldEmpties = false;
	public static boolean unfoldLexical = false;
	public static boolean unfoldLayout = false;
	public static boolean tokenizeLexicalSyntax = false;

	public static boolean unfoldStackDepth = false;
	public static boolean unfoldStackContents = false;
	public static int stackUnfoldingDepth = 0;
	
	public static boolean verbose = false;
	public static boolean quick = false;
	public static final boolean nonTerminalLookahead = false;
	public static boolean ignoreLayout = false;
	public static boolean doRejects = true;
	public static boolean doFollowRestrictions = true;
	public static boolean doPriorities = true;
	public static boolean doPreferAvoid = true;
	
	public static String parseTable;
	public static boolean outputAmbiguities;
	
	int precision = LR0;
    String filename = "";
    public static String filterFile;
    Grammar grammar;
    GrammarMetrics metrics;
	
    {
    	parseTable = null;
    	doPriorities = doFollowRestrictions = doRejects = true;
    }
    
    public Main() {
    }
    
	public static void main(String[] args) {

		//try { Thread.sleep(8000); } catch (Exception e) {}

		Main m = new Main();
		m.executeArguments(args);
		if (m.metrics != null) {
			m.metrics.write();
		}
	}
	
	static void usage() {
		System.out.println(
			"AmbiDexter 0.4, April 15th, 2011\n" +
			"Bas Basten, CWI, Amsterdam, The Netherlands (basten@cwi.nl)\n" +
			"\n" +
			"AmbiDexter is an ambiguity detection tool for context-free grammars in YACC or SDF2 format.\n" +
			"* It can filter production rules from a grammar that do not contribute to its ambiguity, using an extension to the Noncanonical Unambiguity Test by Sylvain Schmitz. In the case of scannerless grammars we filter the generated NFA instead of the grammar.\n" +
			"* It contains a parallel sentence generator that explores all strings of a grammar up to a certain length and checks them for ambiguity.\n" +
			"\n" +
			"Usage: java -jar AmbiDexter_0.4.jar <options> <grammar files>\n" +
			"Input grammars have to be in YACC file format (.y) or normalized imploded parse trees of SDF modules (.pt).\n" +
			"\n" +
			"General options:\n" +
			"    -q           quick (less checks and output) \n" +
			"    -v           verbose \n" +
			"    -s           output some grammar statistics and quit\n" +
			"    -c           convert (.y) grammar and quit, specify output format with -oX\n" +
			"    -g           output all generated graphs (NFA, pair graph, etc.) to dot\n" +
			"    -ndf         ignore disambiguation filters in SDF grammars\n" +
			"\n" +
			"Options for filtering grammars and NFAs:\n" +
			"    -nu          do Noncanonical Unambiguity Test (default)\n" +
			"    -h           filter harmless production rules and NFA edges\n" +
			"    -lr0         use LR(0) precision\n" +
			"    -slr1        use SLR(1) precision (default)\n" +
			"    -lalr1       use LALR(1) precision\n" +
			"    -lr1         use LR(1) precision\n" +
			"    -udr         filter unmatched derive and reduce edges from NFA\n" +
			"    -oy          output filtered (.y) grammar in YACC file format\n" +
			"    -oc          output (.y) grammar in CFGAnalyzer file format\n" +
			"    -oa          output (.y) grammar in ACCENT (AMBER) file format\n" +
			"    -ob          output (.y) grammar in dk.brics.grammar file format\n" +
			"    -wdfa <name> convert filtered NFA to DFA for sentence generation,\n" +
			"                 and save to <grammar>.<name>.idfa (SDF only)\n" +
			"\n" +
			"NFA unfolding options:\n" +
			"    -ulo         unfold layout and literal non-terminals (SDF only)\n" +
			"    -ue          unfold empty rules\n" +
			"    -ulex        unfold cf-to-lex injections (SDF only)\n" +
			"    -us <depth>  unfold stack context of <depth> levels\n" +
			"    -unrt        unfold non-recursive leaf branches\n" +
			"    -uscc        unfold strongly connected components\n" +
			"\n" +
			"Options for sentence generation:\n" +
			"    -pgp         do parallel depth first sentence generation\n" +
			"    -k <nr>      specify maximum length of sentences to generate\n" +
			"    -ik <nr>     incrementally search from <nr> upwards\n" +
			"    -p <nr>      specify number of parallel threads (default 1)\n" +
			"    -dl <nr>     specify length of strings dealt to threads\n" +
			"    -rdfa <name> generate sentences from dfa <name> (otherwise a new dfa is made)\n"
			);
	}

	public void executeArguments(String[] args) {
		if (args.length == 0) {
			usage();
			return;
		}

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.charAt(0) == '-') {
				if (arg.equals("-//"))    { return; } // end of arguments

				if (arg.equals("-nu"))    { method = DetectionMethod.NU; continue; }
				if (arg.equals("-anu"))   { method = DetectionMethod.NU; alternating = true; continue; }
				if (arg.equals("-ru"))    { method = DetectionMethod.RU; continue; }
				if (arg.equals("-pg"))    { method = DetectionMethod.PG; continue; }
				if (arg.equals("-pgp"))   { method = DetectionMethod.PGP; continue; }
				if (arg.equals("-tsdg"))  { method = DetectionMethod.TSDG; continue; }
				
				// NFA precision
				if (arg.equals("-lr0"))   { precision = LR0; continue; }
				if (arg.equals("-slr1"))  { precision = SLR1; continue; }
				if (arg.equals("-lalr1")) { precision = LALR1; continue; }
				if (arg.equals("-lr1"))   { precision = LR1; continue; }

				// general output and grammar options
				if (arg.equals("-s"))     { outputStatistics = true; verbose = true; continue; }
				if (arg.equals("-v"))	  { verbose = true; continue; }
				if (arg.equals("-q"))     { quick = true; continue; }
				if (arg.equals("-g"))     { outputGraphs = true; continue; }
				if (arg.equals("-dm"))    { outputDistMap = true; continue; }
				if (arg.equals("-m"))     { metrics = new GrammarMetrics(args[++i]); continue; }
				if (arg.equals("-as"))	  { alternativeStartSymbol = args[++i]; continue; }

				// NU test
				if (arg.equals("-t"))     { findCommonTops = true; continue; }
				if (arg.equals("-h"))     { findHarmlessProductions = true; continue; }
				if (arg.equals("-udr"))   { filterUnmatchedDerivesReduces = true; continue; }

				// grammar conversion/filtering
				if (arg.equals("-c"))     { convert = true; continue; }
				if (arg.equals("-oy"))	  { outputReconstructedGrammarYacc = true; continue; }
				if (arg.equals("-oc"))	  { outputReconstructedGrammarCFGA = true; continue; }
				if (arg.equals("-oa"))	  { outputReconstructedGrammarAccent = true; continue; }
				if (arg.equals("-ob"))	  { outputReconstructedGrammarDKBrics = true; continue; }
				if (arg.equals("-wdfa"))  { writeDFA = true; dfaName = args[++i]; continue; }
				if (arg.equals("-rdfa"))  { readDFA = true; filterFile = dfaName = args[++i]; continue; }
				
				// derivation generation
				if (arg.equals("-pt"))    { parseTable = args[++i]; continue; }
				if (arg.equals("-ogs"))   { outputAmbiguities = true; continue; }
				if (arg.equals("-k"))     { derivGenDepth = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-ik"))    { derivGenDepth = Integer.valueOf(args[++i]); incrementalDerivGen = true; continue; }
				if (arg.equals("-dl"))    { derivGenDealLength = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-p"))     { threads = Integer.valueOf(args[++i]); continue; }
				
				// NFA unfolding
				if (arg.equals("-ue"))    { unfoldEmpties = true; continue; }
				if (arg.equals("-ulex"))  { unfoldLexical = true; continue; }
				if (arg.equals("-ulo"))   { unfoldLayout = true; continue; }
				if (arg.equals("-usd"))   { unfoldStackDepth = true; stackUnfoldingDepth = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-us"))    { unfoldStackContents = true; stackUnfoldingDepth = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-unrt"))  { unfoldNonRecursiveTails = true; continue; }
				if (arg.equals("-ut"))    { unfoldJoiningTails = true; continue; }
				if (arg.equals("-ult"))   { unfoldJoiningTails = true; unfoldOnlyLexicalTails = true; continue; }
				if (arg.equals("-uscc"))  { unfoldStronglyConnectedComponents = true; continue; }
				
				// various (test) settings
				if (arg.equals("-iglo"))  { ignoreLayout = true; continue; }
				if (arg.equals("-ndf"))   { doPriorities = doFollowRestrictions = doRejects = doPreferAvoid = false; continue; } // no disambiguation filters
				if (arg.equals("-tok"))   { tokenizeLexicalSyntax = true; continue; }
				
				System.out.println("Unknown option: " + arg); return;
			} else {
//				try {
					filename = arg;
					if (!handleGrammar()) {
						return;
					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			}
		}
	}

	private boolean handleGrammar() {
		outputReconstructedGrammar = outputReconstructedGrammarAccent || outputReconstructedGrammarCFGA || outputReconstructedGrammarYacc || outputReconstructedGrammarDKBrics;
		if (outputReconstructedGrammar && filename.endsWith(".pt")) {
			System.out.println("Output of SDF grammars not supported, use -of instead.");
			return false;
		}
		System.out.println("Loading grammar " + filename + " ...");
		grammar = GrammarImporter.importGrammar(filename, alternativeStartSymbol);
		
		if (grammar.productions.size() == 0) {
			System.out.println("Invalid grammar: " + filename);
			return false;
		}
		
		if (outputGraphs) {
			grammar.dependencyGraphToDot(filename + ".deps.dot");
		}
		
		if (convert) {
			if (!outputReconstructedGrammar) {
				System.out.println("Error, unspecified output format.");
			} else {
				if (filename.endsWith(".y") && outputReconstructedGrammarYacc) {
					System.out.println("Conversion from Yacc to Yacc not allowed.");
				} else {
					dumpGrammar(grammar, "");
				}
			}
		} else {
			if (metrics != null) {
				metrics.read(grammar);
				return true;
			}
			
			checkGrammar(grammar);
			System.out.println("");
		}
		return true;
	}

	void checkGrammar(Grammar grammar) {
		if (verbose) {
			int emptyRules = 0, injections = 0;
			for (Production p : grammar.productions) {
				if (p.reachable) {
					if (p.isEmpty()) {
						++emptyRules;
					} else if (p.isInjection()) {
						++injections;
					}
				}
			}
			
			System.out.println("Empty production rules: " + emptyRules);
			System.out.println("Injections: " + injections + "\n");			
			
			System.out.println("Nonterminal usage (in reachable productions):");
			grammar.printNonTerminalUsage();
			
			System.out.println("\nProductions:");
			for (NonTerminal n : grammar.nonTerminals.values()) {
				for (Production p : n.productions) {
					System.out.println("" + p.nr + ": " + (p.reachable ? "r" : " ") + (p.usedForReject ? "u" : " ") + " " + p);
				}
			}
			
			grammar.printFirstAndFollow();
			grammar.printPrioritiesAndFollowRestrictions();
		}
		
		if (outputStatistics) {
			return;
		}
		
		System.out.println("----------------------------------------------------------------");
		System.out.println("Checking with " + precisionName[precision] +" ...");
		
		switch (method) {
			case RU: doNUtest(); break; 
			case NU: doNUtest(); break;
			case PG: case PGP:  doDerivationGeneration(derivGenDepth); break;
			case TSDG: testScannerlessDerivationGeneration(); break;
			default: throw new RuntimeException("Method not implemented yet");
		}
	}		
	
	// check nfa for potential ambiguities with pg
	NFA buildNFA(boolean includeRejects, boolean unfold, boolean propagateFollowRestrictions) {
		
		NFA nfa = null;		
		switch (precision) {
			case LR0   : nfa = new LR0NFA(grammar); break;
			case SLR1  : nfa = new SLR1NFA(grammar); break;
			case LALR1 : nfa = new LALR1NFA(grammar); break;
			case LR1   : nfa = new LR1NFA(grammar); break;
			default    : throw new RuntimeException("Unknown precision");
		}
		nfa.precision = precision;
		
		System.out.println("");

		nfa.build(includeRejects, unfold);
		nfa.printSize("NFA");
		
		if (propagateFollowRestrictions && grammar.scannerless) {
			nfa.propagateFollowRestrictions();
			nfa.printSize("Propagated follow restrictions");
		} else {
			if (writeDFA) {
				nfa.moveShiftsToSets(); // otherwise minimalstring calc won't work
			}
		}
		
		nfa.optimize(true);
		if (writeDFA && includeRejects) {
			nfa.disconnectRejectPart();
		}
		nfa.printSize("Optimized");

		nfa.finish();
		if (!quick) {
			nfa.verify();
		}
		
		if (verbose) {
			System.out.println("Items + DFF");
			for (Item i : nfa.items) {
				String o = "" + i.id + ": " + i;
				while (o.length() < 80) {
					o += " ";
				}
				System.out.println(o + i.dffb);
			}
		}
		
		return nfa;
	}

	private void doNUtest() {		
		NFA nfa = buildNFA(writeDFA, true, doFollowRestrictions);
		if (outputGraphs) {
			nfa.toDot(filename + "." + precisionName[precision] + ".nfa.dot");
		}

		NoncanonicalUnambiguityTest nu = new NoncanonicalUnambiguityTest();
		nu.build(nfa);		
		nu.detectAmbiguities(method);
		
		if (findHarmlessProductions) {
			if (!quick) {
				System.out.println("\nPotentially Harmful: ");
				for (Production p : nu.harmfulProductions) {
					if (p.reachable) {
						System.out.println(p.toString());
					}
				}
				System.out.println("\nHarmless:");
				for (Production p : nu.harmlessProductions) {
					String s = "";
					if (!p.reachable) {
						s = "*** ";
					}
					System.out.println(s + p);
				}
			}
			
			System.out.println("Harmless productions: " + nu.harmlessProductions.size() + " / " + grammar.nrReachableProductions);
			
			if (nu.harmfulProductions.size() > 0 && outputReconstructedGrammar && !grammar.scannerless) {
				Grammar g2 = new Grammar(grammar, nu.harmfulProductions, true);
				dumpGrammar(g2, ".-" + precisionName[precision] + ".pa");
			}
		}
		
		if (writeDFA) {			
			System.out.println();
			nfa.reconnectRejectPart();
			if (!doFollowRestrictions) {
				nfa.moveShiftsToSets();
			}
			nfa.reconstruct();
			nfa.printSize("Reconstructed NFA");

			NFA snfa = nfa.simplifyWithFollowRestrictions();
			snfa.printSize("Simplified NFA");
			
			ItemPDA dfa = new ItemPDA();
			dfa.build(snfa);
			dfa.printSize("IDFA");			
			dfa.serialize(filename + "." + dfaName + ".idfa");			
			
			if (outputGraphs) {
				nfa.toDot(grammar.name + ".nfa.reconstructed.dot");
				snfa.toDot(grammar.name + ".nfa.simplified.dot");
				dfa.toDot(grammar.name + ".dfa.reconstructed.dot");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void doDerivationGeneration(int depth) {
		DerivationGenerator dg = null;
		switch (method) {
		case PG:
			dg = new ScannerlessDerivGen1(threads);
			((ParallelDerivationGenerator) dg).setDealerLength(derivGenDealLength);
			break;
		case PGP:
			dg = new ScannerlessDerivGen2(threads);
			((ParallelDerivationGenerator) dg).setDealerLength(derivGenDealLength);
			break;
		}
		dg.setScannerless(grammar.scannerless);

		if (readDFA) {
			PDA dfa;
			if (method == DetectionMethod.PGP) {
				dfa = new ItemPDA();
			} else {
				dfa = new ProductionPDA();
			}
			dfa.deserialize(filename + "." + dfaName + ".idfa", grammar);
			dg.setDFA(dfa);
		} else {
			NFA nfa;
			if (method == DetectionMethod.PGP) {
				nfa = buildNFA(Main.doRejects, false, Main.doFollowRestrictions);
				if (!grammar.scannerless || !Main.doFollowRestrictions) {
					nfa.moveShiftsToSets();
				}
			} else {
				nfa = buildNFA(Main.doRejects, false, false);
			}
			
			if (outputGraphs) {
				nfa.toDot(filename + "." + precisionName[precision] + ".gnfa.dot");
			}
			dg.build(nfa);
		}
		dg.optimize();
		dg.verify();
		
		if (outputGraphs) {
			dg.getDFA().toDot("dfa.dot");
		}
		
		dg.setLength(depth);
		dg.setIncremental(incrementalDerivGen);
		if (parseTable == null) {
			dg.setParser(new SimpleSGLRParser(buildNFA(Main.doRejects, false, false)));
		} else {
			dg.setParser(new SGLRStub());
		}
		dg.detectAmbiguities(method);
	}

	private void testScannerlessDerivationGeneration() {
		method = DetectionMethod.PGP;
		
		if (parseTable == null) {
			System.out.println("Parse table file(s) not set");
			return;
		}
		
		String pt = parseTable;

		for (int depth = 1; depth <= derivGenDepth; ++depth) {
			// generate without disambiguation filters and parse with SGLR
			Main.parseTable = pt;
			Main.doFollowRestrictions = Main.doPriorities = Main.doRejects = Main.doPreferAvoid = false;
			doDerivationGeneration(depth);
			Relation<Symbol, SymbolString> sglr = ParallelDerivationGenerator.ambiguities;
			
			// generate with disambiguation filters
			Main.parseTable = null;
			Main.doFollowRestrictions = Main.doPriorities = Main.doRejects = Main.doPreferAvoid = true;
			doDerivationGeneration(depth);
			Relation<Symbol, SymbolString> scan = ParallelDerivationGenerator.ambiguities;
	
			if (!sglr.equals(scan)) {
				ScannerlessDerivGenTest.compareAmbiguities(sglr, scan, depth);
			}
		}
	}

	private void dumpGrammar(Grammar g, String postfix) {
		String basename = filename.substring(0, filename.length() - 2) + postfix;
		if (outputReconstructedGrammarYacc) {
			dumpGrammar(basename + ".y", GrammarExporter.exportYacc(g));
		}
		if (outputReconstructedGrammarCFGA) {
			dumpGrammar(basename + ".cfga", GrammarExporter.exportCFGA(g));
		}
		if (outputReconstructedGrammarAccent) {
			dumpGrammar(basename + ".acc", GrammarExporter.exportAccent(g));
		}
		if (outputReconstructedGrammarDKBrics) {
			dumpGrammar(basename + ".cfg", GrammarExporter.exportDKBrics(g));
		}
	}
	
	private void dumpGrammar(String filename, String contents) {
		System.out.println("Exporting grammar to " + filename);
		Util.writeTextFile(filename, contents);
	}
}
