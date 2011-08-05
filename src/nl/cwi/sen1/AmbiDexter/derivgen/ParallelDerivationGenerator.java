package nl.cwi.sen1.AmbiDexter.derivgen;

import java.util.Set;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.automata.PDA;
import nl.cwi.sen1.AmbiDexter.automata.PDA.PDAState;
import nl.cwi.sen1.AmbiDexter.grammar.FollowRestrictions;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;
import nl.cwi.sen1.AmbiDexter.parse.IParser;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree;
import nl.cwi.sen1.AmbiDexter.parse.SGLRStub;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree.ParseTreeNode;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.LinkedList;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Util;

public abstract class ParallelDerivationGenerator implements DerivationGenerator {

	@SuppressWarnings("unchecked")
	protected PDA dfa;
	int length;
	protected Queue<Job> jobs = new Queue<Job>();
	Queue<Relation<Symbol, SymbolString>> possibleAmbiguities = new Queue<Relation<Symbol, SymbolString>>(256);
	public static Relation<Symbol, SymbolString> ambiguities;
	protected int workers;
	protected int dealLength = 4;
	long startTime;
	IParser parser;
	protected boolean scannerless = false;
	protected boolean incremental = false;

	public abstract void build(NFA nfa);
	@SuppressWarnings("unchecked")
	public abstract void setDFA(PDA dfa);
	@SuppressWarnings("unchecked")
	protected abstract IStackFrame newStackFrame(PDAState t);
	protected abstract AbstractWorker newWorker(String id);

	public ParallelDerivationGenerator(int threads) {
		workers = threads;
	}
	
	public void setLength(int l) {
		length = l;
	}

	public void setDealerLength(int l) {
		dealLength = l;
	}

	@SuppressWarnings("unchecked")
	public PDA getDFA() {
		return dfa;
	}

	public void setParser(IParser p) {
		parser = p;
	}

	public void setScannerless(boolean s) {
		scannerless = s;
	}
	
	@Override
	public void setIncremental(boolean i) {
		incremental = i;
	}

	public void detectAmbiguities(DetectionMethod method) {
		ambiguities = new Relation<Symbol, SymbolString>();
		for (int i = 0; i <= length; i++) {
			possibleAmbiguities.add(new Relation<Symbol, SymbolString>());
		}
		
		if (incremental) {
			while (true) {
				detect();
				++length;
				possibleAmbiguities.set(length, new Relation<Symbol, SymbolString>());
			}
		} else {
			detect();
			if (Main.outputAmbiguities) {
				outputAmbiguousCores();
			}
		}	
	}

	private void outputAmbiguousCores() {
		Grammar g = dfa.nfa.grammar;
		String outputFile = g.name + "." + Main.filterFile + "." + length + ".amb";
		System.out.println("Writing " + outputFile);
		
		StringBuilder b = new StringBuilder();
		for (Pair<Symbol, SymbolString> p : ambiguities) {
			b.append(p.a);
			b.append(" : ");
			b.append(p.b);
			b.append("\n");
		}
	
		Util.writeTextFile(outputFile, b.toString());
	}

	protected void detect() {
		System.out.println("\nDerivation length: " + length);
		
		AbstractWorker workerPool[] = new AbstractWorker[workers];
	
		startTime = System.currentTimeMillis();
		Job j = new Job(length, newStackFrame(dfa.startState));
		// have one worker hand out a predefined number of jobs
		AbstractWorker dealer = newWorker("dealer");
		dealer.setDealer(true);
		dealer.go(j, true);
	
		System.out.println("Jobs: " + jobs.size());
	
		
		long sentences = 0;
		if (jobs.size() > 0) {
			for (int i = 0; i < workers; i++) {
				workerPool[i] = newWorker(("" + i));
				workerPool[i].start();
			}
			for (int i = 0; i < workers; i++) {
				while (workerPool[i].isAlive()) {
					try {
						workerPool[i].join();
					} catch (InterruptedException e) {
						return;
					}
				}
				sentences += workerPool[i].sentences;
			}
		}
		
		System.out.println("Done");
		System.out.println("Sentences: " + sentences);
		int parsed = 0;
		for (Relation<Symbol, SymbolString> r : possibleAmbiguities) {
			parsed += r.size();
		}
		System.out.println("Parsed: " + parsed);
		System.out.println("Ambiguities found: " + ambiguities.size());
		System.out.println("Ambiguous nonterminals: " + ambiguities.m.size());
		System.out.println("Time: " + (System.currentTimeMillis() - startTime));
	}

	protected void ambiguity(Symbol[] sentence, int from, int to, NonTerminal nt, String workerId) {
		int len = to - from;
		SymbolString s = new SymbolString(len);
		for (int z = 0; z < len; z++) {
			s.add(sentence[z + from]);
		}
		
		// only identify ambiguous nonterminals, do not track all their strings
		if (!Main.outputAmbiguities) {
			synchronized(ambiguities) {
				if (ambiguities.m.contains(nt)) {
					return;
				}
			}
		}
		
		boolean isnew;
		Relation<Symbol,SymbolString> r = possibleAmbiguities.get(len);
		synchronized(r) {
			isnew = !r.contains(nt, s);
			if (isnew) {
				r.add(nt, s);
			}
		}
	
		if (isnew) {
			Pair<Boolean, ParseTree> p = parse(s, nt); 
			if (p.a) {
				boolean reallyAmbiguous = true; // this is to compensate a bug in prefer/avoid filtering during sentence generation
				if (p.b != null) {
					ParseTreeNode n = p.b.getMinimalParseTree();
					if (n == null) {
						//System.out.println(workerId + ": Possible ambiguity found for " + nt + ": ");
						//System.out.println(p.b.top.prettyPrint());
						reallyAmbiguous = false;
					} else {
						System.out.println(workerId + ": Ambiguity found for " + n.getRootSymbol() + ": " + n.yield().prettyPrint());
					}
				} else {
					System.out.println(workerId + ": Ambiguity found for " + nt + ": " + s.prettyPrint());
				}
				if (reallyAmbiguous) {
					synchronized(ambiguities) {
						if (Main.outputAmbiguities) {
							ambiguities.add(p.b.getAmbiguousCore());
						} else {
							ambiguities.add(nt, s);
						}
					}
				}
			}
		}
	}

	protected Pair<Boolean, ParseTree> parse(SymbolString s, NonTerminal n) {
		if (s.containsReconstructedCharacters() || !n.reachable) { // not reachable happens when used for reject only
			return new Pair<Boolean, ParseTree>(Boolean.FALSE, null);
		}
		
		//System.out.println("Parsing " + n + ": " + s.prettyPrint());
		
		if (parser instanceof SGLRStub) {
			return new Pair<Boolean, ParseTree>(((SGLRStub) parser).parseSGLR(s, n), null);
		} else {
			ParseTree parse = parser.parse(s, n);
			boolean ambiguous = parse != null && parse.nrAmbiguities > 0;
			if (ambiguous && !Main.quick) {
				System.out.println(parse.top.prettyPrint());
			}
			return new Pair<Boolean, ParseTree>(ambiguous, parse);
		}		
	}

	public void optimize() {
		// do nothing
	}

	public void verify() {
		// do nothing
	}

	protected abstract class AbstractWorker extends Thread {
			
		protected String id;
		protected boolean dealer = false;
		protected long sentences = 0;
		protected Set<Pair<SymbolString, FollowRestrictions>> generated;		

		public AbstractWorker(String id) {
			super();
			this.id = id;
		}
		
		void setDealer(boolean dealer) {
			this.dealer = dealer;
			generated = new ShareableHashSet<Pair<SymbolString,FollowRestrictions>>();
		}

		public void run() {
			while (true) {
				Job j = null;
				synchronized (jobs) {
					if (jobs.size() > 0) {
						j = jobs.pop();
					} else{
						return;
					}
				}
				go(j, false);
			}
		}

		protected abstract void go(Job job, boolean fresh);
		
		@Override
		public String toString() {
			return id;
		}
	}

	protected static class Job {
		LinkedList<ESet<Symbol>> shiftablesStack;
		Object gss[];
		int maxdepth; // max length
		int shifted;
		Symbol sentence[];
		
		@SuppressWarnings("unchecked")
		public Job(Symbol next, Object gss[], Symbol sentence[], int shifted) {
			// a new job created by the dealer
			shiftablesStack = new LinkedList<ESet<Symbol>>();
			shiftablesStack.elem = Grammar.newESetSymbol();
			shiftablesStack.elem.add(next);
			maxdepth = sentence.length;
			this.gss = new Object[maxdepth + 1];
			for (int i = 0; i <= maxdepth; i++) {
				this.gss[i] = new Queue<IStackFrame>((Queue<IStackFrame>)gss[i]);
			}
			this.sentence = sentence.clone();
			this.shifted = shifted;
		}
	
		@SuppressWarnings("unchecked")
		public Job(int maxdepth, IStackFrame stack) {
			// initial job
			this.maxdepth = maxdepth;
			this.shiftablesStack = new LinkedList<ESet<Symbol>>();
			this.gss = new Object[maxdepth + 1];
			for (int i = 0; i <= maxdepth; i++) {
				gss[i] = new Queue<IStackFrame>(32); 
			}
			((Queue<IStackFrame>)gss[0]).add(stack);
			shifted = 0;
			sentence = new Symbol[maxdepth];
		}
	}
	
	protected interface IStackFrame {
	
	}
}