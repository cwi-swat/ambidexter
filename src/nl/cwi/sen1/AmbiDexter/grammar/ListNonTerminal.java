package nl.cwi.sen1.AmbiDexter.grammar;

public class ListNonTerminal extends NonTerminal {
	public boolean star = false;
	public boolean sep = false;
	//public boolean opt = false;
	public Symbol listElem = null;
	public Symbol separator = null;

	public ListNonTerminal(String s) {
		super(s);
	}
}
