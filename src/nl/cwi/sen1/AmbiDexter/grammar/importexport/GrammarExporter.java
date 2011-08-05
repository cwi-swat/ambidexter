package nl.cwi.sen1.AmbiDexter.grammar.importexport;

import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.LiteralTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.Terminal;


public class GrammarExporter {
	
	public static String exportYacc(Grammar g) {
		String s = "";
		
		// first terminals (tokens)
		for (Terminal t : g.terminals.values()) {
			if (!(t instanceof LiteralTerminal)) {
				s += "%token " + t.s + "\n";
			}
		}
		
		// separator
		s += "\n%%\n";
		
		// nonterminals and productions
		s += "\n" + productionsToYacc(g.startSymbol);
		
		for (NonTerminal n : g.nonTerminals.values()) {
			if (n != g.startSymbol) {
				s += "\n" + productionsToYacc(n);
			}
		}
		
		return s;
	}

	private static String productionsToYacc(NonTerminal n) {
		String s = n.s + " : ";
		
		boolean first = true;
		for (Production p : n.productions) {
			if (!first) {
				s += "| ";
			} else {
				first = false;
			}
			for (int i = 0; i < p.getLength(); i++) {
				Symbol e = p.getSymbolAt(i);
				s += " " + e.s;
			}
			s += "\n";
		}
		
		return s + ";\n";		
	}
	
	public static String exportCFGA(Grammar g) {
		String s = "";
		
		// nonterminals and productions
		s += productionsToCFGA(g.startSymbol);
		
		for (NonTerminal n : g.nonTerminals.values()) {
			if (n != g.startSymbol) {
				s += "\n" + productionsToCFGA(n);
			}
		}
		
		s += "\n/* terminals */\n";
		
		// terminals
		for (Terminal t : g.terminals.values()) {
			if (t instanceof LiteralTerminal) {
				LiteralTerminal l = (LiteralTerminal) t;
				s += l.getCC() + " : " + l.toString('\"') + " ;\n"; 
			} else {
				s += t.toString() + " : \"" + t.toString() + "\" ;\n";
			}
		}
		
		return s;
	}

	private static String productionsToCFGA(NonTerminal n) {
		String s = n.s;
		
		for (Production p : n.productions) {
			s += " : ";
			for (int i = 0; i < p.getLength(); i++) {
				Symbol e = p.getSymbolAt(i);
				if (e instanceof LiteralTerminal) {
					s += ((LiteralTerminal) e).getCC();
				} else {
					s += e;
				}
				s += " ";
			}
			s += ";\n";
		}
		
		return s;		
	}
	
	public static String exportAccent(Grammar g) {
		String s = "%token";
		
		// first terminals (tokens)
		boolean first = true;
		for (Terminal t : g.terminals.values()) {
			if (!(t instanceof LiteralTerminal)) {
				if (first) {
					first = false;
				} else {
					s += ",";
				}
				s += " " + t.s;
			}
		}
		
		s += ";\n\n%nodefault\n";
		
		// nonterminals and productions
		s += "\n" + productionsToAccent(g.startSymbol);
		
		for (NonTerminal n : g.nonTerminals.values()) {
			if (n != g.startSymbol) {
				s += "\n" + productionsToAccent(n);
			}
		}
		
		return s;
	}

	private static String productionsToAccent(NonTerminal n) {
		String s = n.s + " : ";
		
		boolean first = true;
		for (Production p : n.productions) {
			if (!first) {
				s += "| ";
			} else {
				first = false;
			}
			for (int i = 0; i < p.getLength(); i++) {
				Symbol e = p.getSymbolAt(i);
				s += " " + e.s;
			}
			s += "\n";
		}
		
		return s + ";\n";		
	}
	
	public static String exportDKBrics(Grammar g) {
		String s = "";
		
		// nonterminals and productions
		s += "\n" + productionsToDKBrics(g.startSymbol);
		
		for (NonTerminal n : g.nonTerminals.values()) {
			if (n != g.startSymbol) {
				s += "\n" + productionsToDKBrics(n);
			}
		}

		s += "\n/* terminals */\n";
		
		//char c = '0' - 1;
		char c = 32;
		
		// terminals
		for (Terminal t : g.terminals.values()) {			
			if (!(t instanceof LiteralTerminal)) {
				
				String subst;
				do {
					c++;
					subst = "'" + c + "'";
				} while (c == '\"' || c == '\\' || g.terminals.containsKey(subst));

				s += t.toString() + " : \"" + c + "\"\n";
			}
		}
		
		return s;
	}

	private static String productionsToDKBrics(NonTerminal n) {
		String s = n.s + " : ";
		
		boolean first = true;
		for (Production p : n.productions) {
			if (!first) {
				s += "| ";
			} else {
				first = false;
			}
			for (int i = 0; i < p.getLength(); i++) {
				Symbol e = p.getSymbolAt(i);
				if (e instanceof LiteralTerminal) {
					s += " " + ((LiteralTerminal) e).toString('\"');
				} else {
					s += " " + e.s;
				}
			}
			s += "\n";
		}
		
		return s + "\n";
	}
}
