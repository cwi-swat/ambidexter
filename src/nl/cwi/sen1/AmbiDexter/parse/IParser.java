package nl.cwi.sen1.AmbiDexter.parse;

import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;

public interface IParser {
	public ParseTree parse(SymbolString s);
	public ParseTree parse(SymbolString s, NonTerminal nt);
}
