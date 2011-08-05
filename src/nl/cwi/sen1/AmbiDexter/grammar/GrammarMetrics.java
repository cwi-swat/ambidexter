package nl.cwi.sen1.AmbiDexter.grammar;

import nl.cwi.sen1.AmbiDexter.util.Util;

public class GrammarMetrics {

	private String csv = "";
	private String filename;
	
	{
		// 14 fields
		csv += "\"grammar\",\"productions\",\"reachable\",\"nonterminals\"";
		csv += ",\"priorities\",\"followrestr\",\"rejects\",\"prefers\",\"avoids\"";
		csv += ",\"empty rules\",\"injections\"";
		csv += ",\"avg. rule length\",\"longest rule\",\"avg. nonterms per rule\"";
		csv += "\n";
	}
	
	public GrammarMetrics(String filename) {
		this.filename = filename;
	}
	
	public void read(Grammar g) {
		
		String s = "\"" + g.name + "\"";
		s += "," + g.productions.size();
		
		int reachable = 0, follow = 0, rejects = 0, prefers = 0, avoids = 0, emptyRules = 0, injections = 0;
		int totalRuleLen = 0, longestRuleLen = 0, nontermUsage = 0;
		
		for (Production p : g.productions) {
			if (p.reachable) {
				++reachable;
				
				if (p.prefer) {
					++prefers;
				}
				if (p.avoid) {
					++avoids;
				}
				
				if (p.isEmpty()) {
					++emptyRules;
				} else if (p.isInjection()) {
					++injections;
				}
				
				totalRuleLen += p.getLength();
				if (p.getLength() > longestRuleLen) {
					longestRuleLen = p.getLength();
				}
				
				for (int i = 0; i < p.getLength(); i++) {
					if (p.getSymbolAt(i) instanceof NonTerminal) {
						++nontermUsage;
					}
				}
			}
			
			if (p.reject) {
				++rejects;
			}
		}
		
		for (NonTerminal n : g.nonTerminals.values()) {
			if (n.followRestrictions != null) {
				follow += n.followRestrictions.size();
			}
		}
		
		s += "," + reachable;
		s += "," + g.nonTerminals.size();
		s += "," + g.nrPrioritiesRead;
		s += "," + follow;
		s += "," + rejects;
		s += "," + prefers;
		s += "," + avoids;
		s += "," + emptyRules;
		s += "," + injections;
		s += "," + ((float)totalRuleLen / reachable);
		s += "," + longestRuleLen;
		s += "," + ((float)nontermUsage / reachable);
		
		csv += s + "\n";
	}
	
	public void write() {
		Util.writeTextFile(filename, csv);
	}
}
