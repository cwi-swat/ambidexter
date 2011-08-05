package nl.cwi.sen1.AmbiDexter;

import nl.cwi.sen1.AmbiDexter.automata.NFA;


public interface AmbiguityDetector {

	public enum DetectionMethod {
		RU, // regular unambiguity
		NU, // noncanonical unambiguity
		//HV, // horizontal vertical (ACLA, grambiguity)
		//BG, // breadth first derivation generator
		PG, // parallel derivation generator
		PGP, // parallel derivation generator with follow restrictions propagated through NFA 
		TSDG // test scannerless derivation generator
	}

	public void build(NFA nfa);
	public void optimize();
	public void verify();
	public void detectAmbiguities(DetectionMethod method);
}
