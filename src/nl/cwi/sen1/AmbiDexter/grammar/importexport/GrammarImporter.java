package nl.cwi.sen1.AmbiDexter.grammar.importexport;

import nl.cwi.sen1.AmbiDexter.AmbiDexterConfig;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;


public abstract class GrammarImporter {

	protected Grammar g;
	protected abstract Grammar importGrammar(String filename, AmbiDexterConfig config);
	
	public final static Grammar importGrammar(String filename, String alternativeStartSymbol, AmbiDexterConfig config) {
		GrammarImporter importer = null;
		if (filename.endsWith(".y")) {
			importer = new YaccImporter();
		}  else {
			throw new RuntimeException("Unknown extension: " + filename);
		}
		
		Grammar g = importer.importGrammar(filename, config);
		
		if (alternativeStartSymbol != null) { // TODO validate: goes wrong with Pico STATEMENT
			if (!g.setNewStartSymbol(alternativeStartSymbol)) {
				throw new RuntimeException("Alternative start symbol not found: " + alternativeStartSymbol);
			}
			System.out.println("Using alternative start symbol: " + alternativeStartSymbol);
		}
		
		if (config.tokenizeLexicalSyntax) {
			g.removeLexicalPart();
		}

		g.finish();
		g.verify();
		g.printSize();
		
		return g;
	}
}
