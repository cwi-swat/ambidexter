package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;

public class ConsoleMonitor implements IAmbiDexterMonitor {
	
	public void println() {
		System.out.println();
	}
	
	public void println(Object o) {
		System.out.println(o);
	}
	
	public void errPrintln() {
		System.err.println();
	}
	
	public void errPrintln(Object o) {
		System.err.println(o);
	}

	public void ambiguousString(AmbiDexterConfig cfg, SymbolString s, NonTerminal n, String messagePrefix) {
		println(messagePrefix + "Ambiguous string found for " + n.prettyPrint() + ": " + s.prettyPrint());
	}

	public void setTaskName(String name, int work) {
		// ignore
	}
	
	public void worked(int work) {
		// ignore
	}

	public boolean canceling() {
		return false;
	}
	
}
