package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.automata.NFA;


public interface AmbiguityDetector {

	public enum DetectionMethod {
		NONE,
		RU, // regular unambiguity
		NU, // noncanonical unambiguity
		//HV, // horizontal vertical (ACLA, grambiguity)
		//BG, // breadth first derivation generator
		PG, // parallel derivation generator
	}

	public void setConfig(AmbiDexterConfig config);
	public AmbiDexterConfig getConfig();
	public void setMonitor(IAmbiDexterMonitor monitor);
	public void build(NFA nfa);
	public void optimize();
	public void verify();
	public boolean detectAmbiguities(DetectionMethod method);
}
