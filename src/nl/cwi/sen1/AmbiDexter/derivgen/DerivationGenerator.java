package nl.cwi.sen1.AmbiDexter.derivgen;

import nl.cwi.sen1.AmbiDexter.AmbiguityDetector;
import nl.cwi.sen1.AmbiDexter.IAmbiDexterMonitor;
import nl.cwi.sen1.AmbiDexter.automata.PDA;
import nl.cwi.sen1.AmbiDexter.parse.IParser;

public interface DerivationGenerator extends AmbiguityDetector {

	public void setLength(int l);
	public PDA<?> getDFA();
	public void setDFA(PDA<?> dfa);
	public void setParser(IParser p);
	public void setScannerless(boolean s);
	public void setIncremental(boolean i);
	public void setOutputFilePrefix(String s);
	public void setMonitor(IAmbiDexterMonitor monitor);
}
