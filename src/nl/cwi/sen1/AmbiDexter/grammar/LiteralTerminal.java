package nl.cwi.sen1.AmbiDexter.grammar;

public class LiteralTerminal extends Terminal {
	public LiteralTerminal(char c) {
		super("'" + c + "'");
	}
	
	public String toString(char quote) {
		return s.replace('\'', quote);
	}
	
	public String getCC() {	
		return "CC" + (int) s.charAt(1);
	}
	
	public char getChar() {
		return s.charAt(1);
	}
}