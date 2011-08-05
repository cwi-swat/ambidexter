package nl.cwi.sen1.AmbiDexter.test;

import nl.cwi.sen1.AmbiDexter.grammar.importexport.YaccImporter;
import junit.framework.TestCase;

public class GrammarImportTest extends TestCase {

	public void test() {
		if (new YaccImporter().importYacc(" %% S : '{' '<' ' ' '\123' '>' '}' ; ", "test").terminals.size() != 6) {
			throw new RuntimeException("GrammarImporter test failed");
		}
	}

}
