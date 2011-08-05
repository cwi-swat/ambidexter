package nl.cwi.sen1.AmbiDexter.grammar.importexport;

import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;


public abstract class GrammarImporter {

	protected Grammar g;
	protected abstract Grammar importGrammar(String filename);
	
	public final static Grammar importGrammar(String filename, String alternativeStartSymbol) {
		GrammarImporter importer = null;
		if (filename.endsWith(".y")) {
			importer = new YaccImporter();
		} else if (filename.endsWith(".pt")) {
			importer = new SDFImporter();
		} else {
			throw new RuntimeException("Unknown extension: " + filename);
		}
		
		Grammar g = importer.importGrammar(filename);
		
		if (alternativeStartSymbol != null) { // TODO validate: goes wrong with Pico STATEMENT
			if (!g.setNewStartSymbol(alternativeStartSymbol)) {
				throw new RuntimeException("Alternative start symbol not found: " + alternativeStartSymbol);
			}
			System.out.println("Using alternative start symbol: " + alternativeStartSymbol);
		}
		
		if (Main.tokenizeLexicalSyntax) {
			g.removeLexicalPart();
		}

		g.finish();
		g.verify();
		g.printSize();
		
		return g;
	}
}
