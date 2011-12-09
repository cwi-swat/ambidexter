package nl.cwi.sen1.AmbiDexter.grammar.importexport;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.Terminal;
import nl.cwi.sen1.AmbiDexter.util.StringReplacer;
import nl.cwi.sen1.AmbiDexter.util.Util;

public class YaccImporter extends GrammarImporter {

	public YaccImporter() {
	}

	@Override
	public Grammar importGrammar(String filename, AmbiDexterConfig config) {
		String s = Util.readTextFile(filename);
		return importYacc(s, filename);
	}

	// read grammar from yacc file
	public Grammar importYacc(String s, String name) {
		g = new Grammar(name, false, false, false);
		String startSymbol = null;
		
		final String openBraceChar = "!!OPENBRACECHAR!!";
		final String closeBraceChar = "!!CLOSEBRACECHAR!!";
		final String lessThanChar = "!!LESSTHANCHAR!!";
		final String greaterThanChar = "!!GREATERTHANCHAR!!";
		final String colonChar = "!!COLONCHAR!!";
		final String semicolonChar = "!!SEMICOLONCHAR!!";
		final String pipeChar = "!!PIPECHAR!!";
		final String atChar = "!!ATCHAR!!";
		final String spaceChar = "!!SPACECHAR!!";
		
		// escape and remove whitespace inside characters
		{
			int open = s.indexOf('\'');
			int prev = 0;
			String t = "";
			while (open != -1) {
				int close = s.indexOf('\'', open + 1);
				String c = null;
				if (close != -1) {
					c = s.substring(open + 1, close);
					c = StringReplacer.strip(c);
					if (c.equals("\\")) {
						// escaped single quote
						close = s.indexOf('\'', close + 1);
						if (close != -1) {
							c = s.substring(open, close);
							c = StringReplacer.strip(c);
						} else {
							c = null;
						}
					}
				}
				
				if (c == null) {
					throw new RuntimeException("Unmatched '");
				}
				
				t += s.substring(prev, open); // include '
				
				if (c.equals("")) t += spaceChar;
				else if (c.equals("{")) t += openBraceChar;
				else if (c.equals("}")) t += closeBraceChar;
				else if (c.equals("<")) t += lessThanChar;
				else if (c.equals(">")) t += greaterThanChar;
				else if (c.equals(":")) t += colonChar;
				else if (c.equals(";")) t += semicolonChar;
				else if (c.equals("|")) t += pipeChar; 
				else if (c.equals("@")) t += atChar; 
				else { 
					t += "'" + c + "'";
				}
					
				t += " ";
				prev = close + 1;
				open = s.indexOf('\'', close + 1);
			}
			t += s.substring(prev);
			s = t;
		}
		
		// remove C code
		s = StringReplacer.replaceRegions(s, "%{", "%}", " ");
		s = StringReplacer.replaceRegionsNested(s, "{", "}", " ");
		// remove comments
		s = StringReplacer.replaceRegions(s, "/*", "*/", " ");
		s = StringReplacer.replaceRegions(s + "\n", "//", "\n", "\n");
		// remove types
		s = StringReplacer.replaceRegions(s, "<", ">", " ");
		
		// split in tokens
		String d = "@";
		s = s.replaceAll(":", d+":"+d);
		s = s.replaceAll(";", d+";"+d);
		s = s.replaceAll("\\|", d+"|"+d);
		
		s = s.replaceAll("\n", d);
		s = s.replaceAll(" ", d);
		s = s.replaceAll("\t", d);		
	
		String t[] = s.split(d);
		
		// first collect tokens
		int i = 0;
		final int TOKEN = 1;
		final int LEFT = 2;
		final int RIGHT = 3;
		final int NONASSOC = 4;
		final int START = 5;
		final int TYPE = 6;
		final int UNION = 7;
		final int PREC = 8;

		int type = 0;
		while (i < t.length) {
			String tok = t[i];
			i++;
			if (tok.equals("")) continue;
			if (tok.equals("%%")) break;
			if (tok.charAt(0) == '<') continue; // ignore types
			else if (tok.equals("%token")) type = TOKEN;
			else if (tok.equals("%left")) type = LEFT;
			else if (tok.equals("%right")) type = RIGHT;
			else if (tok.equals("%nonassoc")) type = NONASSOC;
			else if (tok.equals("%start")) type = START;
			else if (tok.equals("%type")) type = TYPE;
			else if (tok.equals("%union")) type = UNION;
			else if (tok.equals("%prec")) type = PREC;
			else {
				switch (type) {
				case TOKEN: g.terminals.put(tok, new Terminal(tok)); break;
				case LEFT: break; // TODO
				case RIGHT: break; // TODO
				case NONASSOC: break; // TODO
				case START: startSymbol = tok; break;
				case TYPE: break; // ignore
				case UNION: break; // ignore
				case PREC: break; // ignore
				default: throw new RuntimeException("Unknown identifier " + tok);
				}				
			}
		}
		
		// collect productions
		Production prod = null;
		NonTerminal lhs = null;
		while (i < t.length) {
			String tok = t[i];
			if (!tok.equals("")) {
				if (tok.equals("%%")) {
					break;
				}
				switch (tok.charAt(0)) {
				case ':':
					if (g.startSymbol == null && (startSymbol == null || startSymbol.equals(lhs.s))) {
						g.startSymbol = lhs;
					}
					break;
				
				case '|':
					g.addProduction(prod);
					prod = g.newProduction(lhs);
					break;
					
				case ';':
					g.addProduction(prod);
					prod = null;
					break;
	
				default:
					//System.err.println(tok);
					char c = tok.charAt(0);
					if (c >= 'A' && c <= 'Z') { // token or nonterminal
						if (prod == null) {
							lhs = g.getNonTerminal(tok);
							prod = g.newProduction(lhs);
						} else {
							Symbol symb = g.terminals.get(tok);
							if (symb == null) {
								// it's a nonterminal
								symb = g.getNonTerminal(tok);								
							}
							prod.addSymbol(symb);
						}
						
					} else if (c >= 'a' && c <= 'z') { // nonterminal
						if (prod == null) {
							lhs = g.getNonTerminal(tok);
							prod = g.newProduction(lhs);
						} else {
							prod.addSymbol(g.getNonTerminal(tok));
						}
					
					} else if (c == '\'' && tok.length() > 2 && tok.endsWith("'")) { // literal char token
						prod.addSymbol(g.getLiteralTerminal(tok));
					}
					// escaped characters
					else if (tok.equals(spaceChar)) prod.addSymbol(g.getLiteralTerminal(' '));
					else if (tok.equals(openBraceChar)) prod.addSymbol(g.getLiteralTerminal('{'));
					else if (tok.equals(closeBraceChar)) prod.addSymbol(g.getLiteralTerminal('}'));
					else if (tok.equals(lessThanChar)) prod.addSymbol(g.getLiteralTerminal('<'));
					else if (tok.equals(greaterThanChar)) prod.addSymbol(g.getLiteralTerminal('>'));
					else if (tok.equals(colonChar)) prod.addSymbol(g.getLiteralTerminal(':'));
					else if (tok.equals(semicolonChar)) prod.addSymbol(g.getLiteralTerminal(';'));
					else if (tok.equals(pipeChar)) prod.addSymbol(g.getLiteralTerminal('|'));
					else if (tok.equals(atChar)) prod.addSymbol(g.getLiteralTerminal('@'));
					else {
						throw new RuntimeException("Unknown token: " + tok);
					}				
					
					break;
				}
			}			
			i++;
		}
		
		return g;
	}

}
