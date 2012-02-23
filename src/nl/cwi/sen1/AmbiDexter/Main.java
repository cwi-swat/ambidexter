package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.AmbiguityDetector.DetectionMethod;
import nl.cwi.sen1.AmbiDexter.automata.ItemPDA;
import nl.cwi.sen1.AmbiDexter.automata.LALR1NFA;
import nl.cwi.sen1.AmbiDexter.automata.LR0NFA;
import nl.cwi.sen1.AmbiDexter.automata.LR1NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.PDA;
import nl.cwi.sen1.AmbiDexter.automata.SLR1NFA;
import nl.cwi.sen1.AmbiDexter.derivgen.DerivationGenerator;
import nl.cwi.sen1.AmbiDexter.derivgen.ParallelDerivationGenerator;
import nl.cwi.sen1.AmbiDexter.derivgen.ScannerlessDerivGen2;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.GrammarMetrics;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.importexport.GrammarExporter;
import nl.cwi.sen1.AmbiDexter.grammar.importexport.GrammarImporter;
import nl.cwi.sen1.AmbiDexter.nu2.NoncanonicalUnambiguityTest;
import nl.cwi.sen1.AmbiDexter.parse.SGLRStub;
import nl.cwi.sen1.AmbiDexter.parse.SimpleSGLRParser;
import nl.cwi.sen1.AmbiDexter.util.Util;

public class Main {
	
	private AmbiDexterConfig config = new AmbiDexterConfig();
	private Grammar grammar;
    private GrammarMetrics metrics;
    private IAmbiDexterMonitor monitor;
	
    public Main(IAmbiDexterMonitor m) {
    	monitor = m;
    }
    
	public static void main(String[] args) {

		//try { Thread.sleep(8000); } catch (Exception e) {}
		IAmbiDexterMonitor monitor = new ConsoleMonitor(); 
		
		Main m = new Main(monitor);
		m.executeArguments(args);
		if (m.metrics != null) {
			m.metrics.write();
		}
	}
	
	public void setGrammar(Grammar g) {
		grammar = g;
	}
	
	public void setConfig(AmbiDexterConfig cfg) {
		config = cfg;
	}
	
	public AmbiDexterConfig getConfig() {
		return config;
	}
	
	static String usage() {
		return
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
			"    -pg          do parallel depth first sentence generation\n" +
			"    -k <nr>      specify maximum length of sentences to generate\n" +
			"    -ik <nr>     incrementally search from <nr> upwards\n" +
			"    -p <nr>      specify number of parallel threads (default 1)\n" +
			"    -dl <nr>     specify length of strings dealt to threads\n" +
			"    -rdfa <name> generate sentences from dfa <name> (otherwise a new dfa is made)\n"
		;
	}

	public void executeArguments(String[] args) {
		if (args.length == 0) {
			monitor.println(usage());
			return;
		}

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.charAt(0) == '-') {
				if (arg.equals("-//"))    { return; } // end of arguments

				if (arg.equals("-nu"))    { config.filterMethod = DetectionMethod.NU; continue; }
				if (arg.equals("-anu"))   { config.filterMethod = DetectionMethod.NU; config.alternating = true; continue; }
				if (arg.equals("-ru"))    { config.filterMethod = DetectionMethod.RU; continue; }
				if (arg.equals("-pg"))    { config.derivGenMethod = DetectionMethod.PG; continue; }
				
				// NFA precision
				if (arg.equals("-lr0"))   { config.precision = AmbiDexterConfig.LR0; continue; }
				if (arg.equals("-slr1"))  { config.precision = AmbiDexterConfig.SLR1; continue; }
				if (arg.equals("-lalr1")) { config.precision = AmbiDexterConfig.LALR1; continue; }
				if (arg.equals("-lr1"))   { config.precision = AmbiDexterConfig.LR1; continue; }

				// general output and grammar options
				if (arg.equals("-s"))     { config.outputStatistics = true; AmbiDexterConfig.verbose = true; continue; }
				if (arg.equals("-v"))	  { AmbiDexterConfig.verbose = true; continue; }
				if (arg.equals("-q"))     { AmbiDexterConfig.quick = true; continue; }
				if (arg.equals("-g"))     { AmbiDexterConfig.outputGraphs = true; continue; }
				if (arg.equals("-dm"))    { AmbiDexterConfig.outputDistMap = true; continue; }
				if (arg.equals("-m"))     { metrics = new GrammarMetrics(args[++i]); continue; }
				if (arg.equals("-as"))	  { config.alternativeStartSymbol = args[++i]; continue; }

				// NU test
				if (arg.equals("-h"))     { config.filterMethod = DetectionMethod.NU; config.findHarmlessProductions = true; continue; }
				if (arg.equals("-udr"))   { config.filterUnmatchedDerivesReduces = true; continue; }

				// grammar conversion/filtering
				if (arg.equals("-c"))     { config.convert = true; continue; }
				if (arg.equals("-oy"))	  { config.outputReconstructedGrammarYacc = true; continue; }
				if (arg.equals("-oc"))	  { config.outputReconstructedGrammarCFGA = true; continue; }
				if (arg.equals("-oa"))	  { config.outputReconstructedGrammarAccent = true; continue; }
				if (arg.equals("-ob"))	  { config.outputReconstructedGrammarDKBrics = true; continue; }
				if (arg.equals("-wdfa"))  { AmbiDexterConfig.writeDFA = true; config.dfaName = args[++i]; continue; }
				if (arg.equals("-rdfa"))  { config.readDFA = true; config.filterFile = config.dfaName = args[++i]; continue; }
				
				// derivation generation
				if (arg.equals("-pt"))    { config.parseTableSGLR = args[++i]; continue; }
				if (arg.equals("-ogs"))   { config.outputAmbiguities = true; continue; }
				if (arg.equals("-k"))     { config.derivGenMinDepth = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-ik"))    { config.derivGenMinDepth = Integer.valueOf(args[++i]); config.incrementalDerivGen = true; continue; }
				if (arg.equals("-dl"))    { config.derivGenDealLength = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-p"))     { config.threads = Integer.valueOf(args[++i]); continue; }
				
				// NFA unfolding
				if (arg.equals("-ue"))    { config.unfoldEmpties = true; continue; }
				if (arg.equals("-ulex"))  { config.unfoldLexical = true; continue; }
				if (arg.equals("-ulo"))   { config.unfoldLayout = true; continue; }
				if (arg.equals("-usd"))   { config.unfoldStackDepth = true; config.stackUnfoldingDepth = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-us"))    { config.unfoldStackContents = true; config.stackUnfoldingDepth = Integer.valueOf(args[++i]); continue; }
				if (arg.equals("-unrt"))  { config.unfoldNonRecursiveTails = true; continue; }
				if (arg.equals("-ut"))    { config.unfoldJoiningTails = true; continue; }
				if (arg.equals("-ult"))   { config.unfoldJoiningTails = true; config.unfoldOnlyLexicalTails = true; continue; }
				if (arg.equals("-uscc"))  { config.unfoldStronglyConnectedComponents = true; continue; }
				
				// various (test) settings
				if (arg.equals("-ndf"))   { config.doPriorities = config.doFollowRestrictions = config.doRejects = config.doPreferAvoid = false; continue; } // no disambiguation filters
				if (arg.equals("-tok"))   { config.tokenizeLexicalSyntax = true; continue; }
				
				monitor.println("Unknown option: " + arg); return;
			} else {
//				try {
					config.filename = arg;
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
		config.outputReconstructedGrammar = config.outputReconstructedGrammarAccent || config.outputReconstructedGrammarCFGA || config.outputReconstructedGrammarYacc || config.outputReconstructedGrammarDKBrics;
		if (config.outputReconstructedGrammar && config.filename.endsWith(".pt")) {
			monitor.println("Output of SDF grammars not supported, use -of instead.");
			return false;
		}
		monitor.println("Loading grammar " + config.filename + " ...");
		grammar = GrammarImporter.importGrammar(config.filename, config.alternativeStartSymbol, config);
		
		if (grammar.productions.size() == 0) {
			monitor.println("Invalid grammar: " + config.filename);
			return false;
		}		
		
		grammar.printSize(monitor);

		if (AmbiDexterConfig.outputGraphs) {
			grammar.dependencyGraphToDot(config.filename + ".deps.dot");
		}
		
		if (config.convert) {
			if (!config.outputReconstructedGrammar) {
				monitor.println("Error, unspecified output format.");
			} else {
				if (config.filename.endsWith(".y") && config.outputReconstructedGrammarYacc) {
					monitor.println("Conversion from Yacc to Yacc not allowed.");
				} else {
					dumpGrammar(grammar, "");
				}
			}
		} else {
			if (metrics != null) {
				metrics.read(grammar);
				return true;
			}
			
			if (AmbiDexterConfig.verbose) {
				printGrammar(grammar);
			}
			if (config.outputStatistics) {
				return true;
			}
			
			checkGrammar(grammar);
			monitor.println("");
		}
		return true;
	}

	public void checkGrammar(Grammar grammar) {
		if (config.filterMethod == DetectionMethod.NONE && config.derivGenMethod == DetectionMethod.NONE) {
			monitor.println("Please select a filtering and/or derivation generation setting.");
			return;
		}
		
		monitor.println("----------------------------------------------------------------");
		monitor.println("Checking with " + AmbiDexterConfig.precisionName[config.precision] +" ...");
		
		switch (config.filterMethod) {
			case RU: doNUtest(); break; 
			case NU: doNUtest(); break;
		}
		
		switch (config.derivGenMethod) {
			case PG: doDerivationGeneration(config.derivGenMinDepth, null); break;
		}
	}

	public void printGrammar(Grammar grammar) {
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
		
		monitor.println("Empty production rules: " + emptyRules);
		monitor.println("Injections: " + injections + "\n");			
		
		monitor.println("Nonterminal usage (in reachable productions):");
		grammar.printNonTerminalUsage(monitor);
		
		monitor.println("\nProductions:");
		for (NonTerminal n : grammar.nonTerminals.values()) {
			for (Production p : n.productions) {
				monitor.println("" + p.nr + ": " + (p.reachable ? "r" : " ") + (p.usedForReject ? "u" : " ") + " " + p);
			}
		}
		
		grammar.printFirstAndFollow(monitor);
		grammar.printPrioritiesAndFollowRestrictions(monitor);
	}		
	
	// check nfa for potential ambiguities with pg
	NFA buildNFA(boolean includeRejects, boolean unfold, boolean propagateFollowRestrictions) {
		
		NFA nfa = null;		
		switch (config.precision) {
			case AmbiDexterConfig.LR0   : nfa = new LR0NFA(grammar); break;
			case AmbiDexterConfig.SLR1  : nfa = new SLR1NFA(grammar, config); break;
			case AmbiDexterConfig.LALR1 : nfa = new LALR1NFA(grammar, config); break;
			case AmbiDexterConfig.LR1   : nfa = new LR1NFA(grammar, config); break;
			default    : throw new RuntimeException("Unknown precision " + config.precision);
		}
		nfa.precision = config.precision;
		
		monitor.println("");

		nfa.build(includeRejects, unfold ? config : null);
		nfa.printSize("NFA", monitor);
		
		if (propagateFollowRestrictions && grammar.scannerless) {
			nfa.propagateFollowRestrictions();
			nfa.printSize("Propagated follow restrictions", monitor);
			
			nfa.reverse();
			//nfa.printSize("Reversed", monitor);
			nfa.propagateFollowRestrictions();
			nfa.printSize("Propagated precede restrictions", monitor);			
			
			nfa.reverse();
			//nfa.printSize("Reversed", monitor);
		} else {
			if (AmbiDexterConfig.writeDFA && !nfa.shiftsInSets) {
				nfa.moveShiftsToSets(); // otherwise minimalstring calc won't work
			}
		}
		
		nfa.optimize(true);
		if (AmbiDexterConfig.writeDFA && includeRejects) {
			nfa.disconnectRejectPart();
		}
		nfa.printSize("Optimized", monitor);

		nfa.finish();
		if (!AmbiDexterConfig.quick) {
			nfa.verify();
		}
		
		if (AmbiDexterConfig.verbose) {
			monitor.println("Items + DFF");
			for (Item i : nfa.items) {
				String o = "" + i.id + ": " + i;
				while (o.length() < 80) {
					o += " ";
				}
				monitor.println(o + i.dffb);
			}
		}
		
		return nfa;
	}

	public void doNUtest() {		
		NFA nfa = buildNFA(AmbiDexterConfig.writeDFA, true, config.doFollowRestrictions);
		if (AmbiDexterConfig.outputGraphs) {
			nfa.toDot(config.filename + "." + AmbiDexterConfig.precisionName[config.precision] + ".nfa.dot");
		}

		NoncanonicalUnambiguityTest nu = new NoncanonicalUnambiguityTest();
		nu.setConfig(config);
		nu.setMonitor(monitor);
		nu.build(nfa);		
		nu.detectAmbiguities(config.filterMethod);
		
		if (config.findHarmlessProductions) {
			monitor.println("\nHarmless productions: " + nu.harmlessProductions.size() + " / " + grammar.nrReachableProductions);
			for (Production p : nu.harmlessProductions) {
				String s = "";
				if (!p.reachable) {
					s = "*** ";
				}
				monitor.println(s + p);
			}
			
			if (AmbiDexterConfig.verbose) {
				monitor.println("\nPotentially harmful: ");
				for (Production p : nu.harmfulProductions) {
					if (p.reachable) {
						monitor.println(p.toString());
					}
				}
			}
			
			if (nu.harmfulProductions.size() > 0 && config.outputReconstructedGrammar && !grammar.scannerless) {
				Grammar g2 = new Grammar(grammar, nu.harmfulProductions, true);
				dumpGrammar(g2, ".-" + AmbiDexterConfig.precisionName[config.precision] + ".pa");
			}
		}
		
		if (AmbiDexterConfig.writeDFA) {			
			monitor.println();
			nfa.reconnectRejectPart();
			if (!nfa.shiftsInSets) {
				nfa.moveShiftsToSets();
			}
			nfa.reconstruct();
			nfa.printSize("Reconstructed NFA", monitor);

			NFA snfa = nfa.simplifyWithFollowRestrictions();
			snfa.printSize("Simplified NFA", monitor);
			
			ItemPDA dfa = new ItemPDA();
			dfa.build(snfa);
			dfa.printSize("IDFA", monitor);			
			dfa.serialize(config.filename + "." + config.dfaName + ".idfa");			
			
			if (AmbiDexterConfig.outputGraphs) {
				nfa.toDot(grammar.name + ".nfa.reconstructed.dot");
				snfa.toDot(grammar.name + ".nfa.simplified.dot");
				dfa.toDot(grammar.name + ".dfa.reconstructed.dot");
			}
		}
	}

	public void doDerivationGeneration(int depth, String parseTableFile) {
		DerivationGenerator dg = null;
		switch (config.derivGenMethod) {
		case PG:
			dg = new ScannerlessDerivGen2(config.threads);
			((ParallelDerivationGenerator) dg).setDealerLength(config.derivGenDealLength);
			break;
		}
		dg.setScannerless(grammar.scannerless);
		dg.setConfig(config);
		dg.setMonitor(monitor);
		if (config.outputAmbiguities) {
			dg.setOutputFilePrefix(grammar.name + "." + config.filterFile);
		}

		if (config.readDFA) {
			@SuppressWarnings("rawtypes")
			PDA dfa = new ItemPDA();
			dfa.deserialize(config.filename + "." + config.dfaName + ".idfa", grammar);
			dg.setDFA(dfa);
		} else {
			NFA nfa;
			nfa = buildNFA(config.doRejects, false, config.doFollowRestrictions);
			if (!nfa.shiftsInSets) {
				nfa.moveShiftsToSets();
			}
			
			if (AmbiDexterConfig.outputGraphs) {
				nfa.toDot(config.filename + "." + AmbiDexterConfig.precisionName[config.precision] + ".gnfa.dot");
			}
			dg.build(nfa);
		}
		dg.optimize();
		dg.verify();
		
		if (AmbiDexterConfig.outputGraphs) {
			dg.getDFA().toDot("dfa.dot");
		}
		
		dg.setLength(depth);
		dg.setIncremental(config.incrementalDerivGen);
		if (parseTableFile == null) {
			dg.setParser(new SimpleSGLRParser(buildNFA(config.doRejects, false, false)));
		} else {
			dg.setParser(new SGLRStub(parseTableFile));
		}
		dg.detectAmbiguities(config.derivGenMethod);
	}

	private void dumpGrammar(Grammar g, String postfix) {
		String basename = config.filename.substring(0, config.filename.length() - 2) + postfix;
		if (config.outputReconstructedGrammarYacc) {
			dumpGrammar(basename + ".y", GrammarExporter.exportYacc(g));
		}
		if (config.outputReconstructedGrammarCFGA) {
			dumpGrammar(basename + ".cfga", GrammarExporter.exportCFGA(g));
		}
		if (config.outputReconstructedGrammarAccent) {
			dumpGrammar(basename + ".acc", GrammarExporter.exportAccent(g));
		}
		if (config.outputReconstructedGrammarDKBrics) {
			dumpGrammar(basename + ".cfg", GrammarExporter.exportDKBrics(g));
		}
	}
	
	private void dumpGrammar(String filename, String contents) {
		monitor.println("Exporting grammar to " + filename);
		Util.writeTextFile(filename, contents);
	}
}
