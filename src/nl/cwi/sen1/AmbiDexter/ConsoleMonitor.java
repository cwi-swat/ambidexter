package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;

public class ConsoleMonitor implements IAmbiDexterMonitor {
	
	@Override
	public void println() {
		System.out.println();
	}
	
	@Override
	public void println(Object o) {
		System.out.println(o);
	}
	
	@Override
	public void errPrintln() {
		System.err.println();
	}
	
	@Override
	public void errPrintln(Object o) {
		System.err.println(o);
	}

	@Override
	public void ambiguousString(AmbiDexterConfig cfg, SymbolString s, NonTerminal n, String messagePrefix) {
		println(messagePrefix + "Ambiguous string found for " + n.prettyPrint() + ": " + s.prettyPrint());
	}
	
}
