package nl.cwi.sen1.AmbiDexter.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import nl.cwi.sen1.AmbiDexter.ConsoleMonitor;
import nl.cwi.sen1.AmbiDexter.Main;
import nl.cwi.sen1.AmbiDexter.derivgen.ParallelDerivationGenerator;

public class DerivGenTest extends TestCase {

	public static Test suite(){
		return new TestSuite(DerivGenTest.class);
	}
	
	public void testDerivGenSmall() {
		testYaccGrammar("grammars/grammar001.norm.y", 4, true);
		testYaccGrammar("grammars/grammar002.norm.y", 5, true);
		testYaccGrammar("grammars/grammar003.norm.y", 3, true);
		testYaccGrammar("grammars/grammar004.norm.y", 4, true);
		testYaccGrammar("grammars/grammar005.norm.y", 2, true);
		testYaccGrammar("grammars/grammar006.norm.y", 9, true);
		testYaccGrammar("grammars/grammar007.norm.y", 50, false);
		testYaccGrammar("grammars/grammar008.norm.y", 20, false);
		testYaccGrammar("grammars/grammar009.norm.y", 50, false);
		
		testYaccGrammar("grammars/grammar010.norm.y", 4, true);
		testYaccGrammar("grammars/grammar011.norm.y", 8, true);
		testYaccGrammar("grammars/grammar012.norm.y", 6, true);
		testYaccGrammar("grammars/grammar013.norm.y", 5, true);
		testYaccGrammar("grammars/grammar014.norm.y", 3, true);
		testYaccGrammar("grammars/grammar015.norm.y", 22, false);
		testYaccGrammar("grammars/grammar016.norm.y", 50, false);
		testYaccGrammar("grammars/grammar017.norm.y", 40, false);
		testYaccGrammar("grammars/grammar018.norm.y", 5, true);
		testYaccGrammar("grammars/grammar019.norm.y", 25, false);
		
		testYaccGrammar("grammars/grammar020.norm.y", 50, false);
		testYaccGrammar("grammars/grammar021.norm.y", 3, true);
		testYaccGrammar("grammars/grammar022.norm.y", 3, true);
		testYaccGrammar("grammars/grammar023.norm.y", 4, true); // XXX amber detects ambiguity at len 3 b/c last A of S does not matter for horizontal ambiguity
		testYaccGrammar("grammars/grammar024.norm.y", 14, false);
		testYaccGrammar("grammars/grammar025.norm.y", 4, true);
		testYaccGrammar("grammars/grammar026.norm.y", 50, false);
		testYaccGrammar("grammars/grammar027.norm.y", 3, true);
		testYaccGrammar("grammars/grammar028.norm.y", 30, false);
		testYaccGrammar("grammars/grammar029.norm.y", 4, true);
		
		testYaccGrammar("grammars/grammar030.norm.y", 4, true);
		testYaccGrammar("grammars/grammar031.norm.y", 8, true);
		testYaccGrammar("grammars/grammar032.norm.y", 6, true);
		testYaccGrammar("grammars/grammar033.norm.y", 50, false);
		testYaccGrammar("grammars/grammar034.norm.y", 3, true);
		testYaccGrammar("grammars/grammar035.norm.y", 50, false);
		testYaccGrammar("grammars/grammar036.norm.y", 50, false);
		testYaccGrammar("grammars/grammar037.norm.y", 50, false);
		testYaccGrammar("grammars/grammar038.norm.y", 2, true);
		testYaccGrammar("grammars/grammar039.norm.y", 3, true);
		
		testYaccGrammar("grammars/grammar040.norm.y", 50, false);
		testYaccGrammar("grammars/grammar041.norm.y", 21, true);
		testYaccGrammar("grammars/grammar042.norm.y", 3, true);
		testYaccGrammar("grammars/grammar043.norm.y", 8, true);
		testYaccGrammar("grammars/grammar044.norm.y", 4, true);
		testYaccGrammar("grammars/grammar045.norm.y", 25, false);
		testYaccGrammar("grammars/grammar046.norm.y", 21, true);
		testYaccGrammar("grammars/grammar047.norm.y", 30, false);
		testYaccGrammar("grammars/grammar048.norm.y", 4, true);
		testYaccGrammar("grammars/grammar049.norm.y", 3, true);
		
		testYaccGrammar("grammars/grammar050.norm.y", 2, true);
		testYaccGrammar("grammars/grammar051.norm.y", 5, true);
		testYaccGrammar("grammars/grammar052.norm.y", 1, true);
		testYaccGrammar("grammars/grammar053.norm.y", 2, true);
		testYaccGrammar("grammars/grammar054.norm.y", 50, false);
		testYaccGrammar("grammars/grammar055.norm.y", 21, true); // XXX amber detects ambiguity at len 20 b/c of horizontal ambiguity
		testYaccGrammar("grammars/grammar056.norm.y", 1, true);
		testYaccGrammar("grammars/grammar057.norm.y", 50, false);
		testYaccGrammar("grammars/grammar058.norm.y", 50, false);
		testYaccGrammar("grammars/grammar059.norm.y", 1, true);
		
		testYaccGrammar("grammars/grammar060.norm.y", 11, true);
		testYaccGrammar("grammars/grammar061.norm.y", 1, true);
		testYaccGrammar("grammars/grammar062.norm.y", 3, true);
		testYaccGrammar("grammars/grammar063.norm.y", 3, true);
		testYaccGrammar("grammars/grammar064.norm.y", 6, true);
		testYaccGrammar("grammars/grammar065.norm.y", 50, false);
		testYaccGrammar("grammars/grammar066.norm.y", 3, true);
		testYaccGrammar("grammars/grammar067.norm.y", 1, true);
		testYaccGrammar("grammars/grammar068.norm.y", 1, true);
		testYaccGrammar("grammars/grammar069.norm.y", 17, false);
		
		testYaccGrammar("grammars/grammar070.norm.y", 15, false);
		testYaccGrammar("grammars/grammar071.norm.y", 15, false);
		testYaccGrammar("grammars/grammar072.norm.y", 18, false);
		testYaccGrammar("grammars/grammar073.norm.y", 22, false);
		testYaccGrammar("grammars/grammar074.norm.y", 22, false);
		testYaccGrammar("grammars/grammar075.norm.y", 50, false);
		testYaccGrammar("grammars/grammar076.norm.y", 11, false);
		testYaccGrammar("grammars/grammar077.norm.y", 18, false);
		testYaccGrammar("grammars/grammar078.norm.y", 20, false);
		testYaccGrammar("grammars/grammar079.norm.y", 50, false);
		
		testYaccGrammar("grammars/grammar080.norm.y", 50, false);				
		testYaccGrammar("grammars/grammar081.norm.y", 20, false);
		testYaccGrammar("grammars/grammar082.norm.y", 50, false);
		testYaccGrammar("grammars/grammar083.norm.y", 50, false);
		testYaccGrammar("grammars/grammar084.norm.y", 3, true);
		testYaccGrammar("grammars/grammar085.norm.y", 23, false); // actually ambiguous
		testYaccGrammar("grammars/grammar086.norm.y", 22, false); // actually ambiguous
		testYaccGrammar("grammars/grammar087.norm.y", 28, true);
	}
	
	public void testDerivGenBig() {
		testYaccGrammar("grammars/grammar090.norm.y", 15, false);
		testYaccGrammar("grammars/grammar100.norm.y", 14, false);
		testYaccGrammar("grammars/grammar101.norm.y", 15, true);
		testYaccGrammar("grammars/grammar106.norm.y", 7, true);
		testYaccGrammar("grammars/grammar107.norm.y", 6, true);
		testYaccGrammar("grammars/grammar108.norm.y", 9, true);
		testYaccGrammar("grammars/grammar109.norm.y", 11, true);
		testYaccGrammar("grammars/grammar110.norm.y", 12, false); // 1 min, 13: about 1 hr
		testYaccGrammar("grammars/grammar111.norm.y", 9, true);
		testYaccGrammar("grammars/grammar116.norm.y", 7, true);
		testYaccGrammar("grammars/grammar117.norm.y", 11, true);
		testYaccGrammar("grammars/grammar118.norm.y", 8, true);
		testYaccGrammar("grammars/grammar119.norm.y", 8, true);
		testYaccGrammar("grammars/grammar120.norm.y", 6, false); // 45 sec, 7: about 15 min 
		testYaccGrammar("grammars/grammar121.norm.y", 5, true);
		testYaccGrammar("grammars/grammar126.norm.y", 6, false); // XXX is actually ambiguous, but very expensive to find! > 11
		testYaccGrammar("grammars/grammar127.norm.y", 3, true);
		testYaccGrammar("grammars/grammar128.norm.y", 5, true);
		testYaccGrammar("grammars/grammar129.norm.y", 6, true);
		testYaccGrammar("grammars/grammar130.norm.y", 11, false);
		testYaccGrammar("grammars/grammar131.norm.y", 11, false); // XXX is actually ambiguous, but at length 13 (> 15 hrs)
		testYaccGrammar("grammars/grammar136.norm.y", 1, true); // XXX actually at 0, but that crashes :-)
		testYaccGrammar("grammars/grammar137.norm.y", 11, true);
		testYaccGrammar("grammars/grammar138.norm.y", 9, true);
		testYaccGrammar("grammars/grammar139.norm.y", 7, true);
	}
	
	public void testYaccGrammar(String grammar, int len, boolean ambiguous) {
		for (int i = 1; i <= len; i++) {
			execute(new String[] {"-ndf" , "-pgp", "-p", "1", "-k", String.valueOf(i), grammar}, i == len && ambiguous);
		}
	}

	public void execute(String[] cmd, boolean ambiguous) {
		Main m = new Main(new ConsoleMonitor());
		m.executeArguments(cmd);
		int ambiguities = ParallelDerivationGenerator.ambiguities.size();
		if (ambiguous) {
			assertTrue(ambiguities > 0);
		} else {
			assertTrue(ambiguities == 0);
		}
	}
}
