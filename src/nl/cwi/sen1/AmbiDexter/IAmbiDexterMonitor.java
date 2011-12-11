package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;

public interface IAmbiDexterMonitor {

	public void println();

	public void println(Object o);

	public void errPrintln();

	public void errPrintln(Object o);
	
	public void ambiguousString(AmbiDexterConfig cfg, SymbolString s, NonTerminal n, String messagePrefix);

}