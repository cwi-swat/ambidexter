package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.AmbiguityDetector.DetectionMethod;

public class AmbiDexterConfig {
	
	// constants
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
	public static final boolean nonTerminalLookahead = false;

	
	// grammar filtering settings
	public DetectionMethod filterMethod = DetectionMethod.NONE;
	public boolean findHarmlessProductions;

	// derivation generation settings
	public DetectionMethod derivGenMethod = DetectionMethod.NONE;
	public int derivGenMinDepth = 10;
	public int derivGenMaxDepth = 10000; // only used within Rascal IDE
	public int derivGenDealLength = 1;
	public int threads = 1;
	public boolean incrementalDerivGen = false;
	
	// general settings
	public int precision = SLR1;
	
	// grammar settings
	public String filename;
	public String alternativeStartSymbol;
	
	// output settings
	public static boolean verbose = false;
	public static boolean quick = true;
	public static boolean outputGraphs = false;
	public static boolean outputDistMap = false;
	public static boolean writeDFA = false;

	public boolean outputStatistics = false;
	public boolean outputReconstructedGrammar = false;
	public boolean outputReconstructedGrammarYacc = false;
	public boolean outputReconstructedGrammarCFGA = false;
	public boolean outputReconstructedGrammarAccent = false;
	public boolean outputReconstructedGrammarDKBrics = false;
	public boolean convert = false;
	public boolean readDFA = false;
	public String dfaName;
	public String filterFile;
	
		
	public boolean filterUnmatchedDerivesReduces = false;

	public boolean alternating = false; // EXPERIMENTAL


	public boolean unfoldStronglyConnectedComponents = false;
	public boolean unfoldNonRecursiveTails = false;
	public boolean unfoldJoiningTails = false;
	public boolean unfoldOnlyLexicalTails = false;
	public boolean unfoldEmpties = false;
	public boolean unfoldLexical = false;
	public boolean unfoldLayout = false;
	public boolean tokenizeLexicalSyntax = false; // EXPERIMENTAL

	public boolean unfoldStackDepth = false;
	public boolean unfoldStackContents = false;
	public int stackUnfoldingDepth = 0;
	
	
	public boolean doRejects = true;
	public boolean doFollowRestrictions = true;
	public boolean doPriorities = true;
	public boolean doPreferAvoid = true;
	
	public String parseTableSGLR; // for testing
	public boolean outputAmbiguities; // for testing

}
