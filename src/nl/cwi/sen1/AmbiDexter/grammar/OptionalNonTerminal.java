package nl.cwi.sen1.AmbiDexter.grammar;

public class OptionalNonTerminal extends NonTerminal {
	public Symbol elem = null;
	public Reduce emptyReduce = null;

	public OptionalNonTerminal(String s) {
		super(s);
	}
}
