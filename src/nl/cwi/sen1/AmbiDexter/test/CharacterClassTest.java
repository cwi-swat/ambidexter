package nl.cwi.sen1.AmbiDexter.test;

import java.util.Set;

import junit.framework.TestCase;

import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class CharacterClassTest extends TestCase {
	
	public void test1() {
		Symbol.resetSymbolCache();
		
		int i1[] = {0, 5, 10, 15};
		int i2[] = {4, 12};
		int i3[] = {0, 15};
		CharacterClass c1 = new CharacterClass(i1);		
		CharacterClass c2 = new CharacterClass(i2);
		CharacterClass c3 = new CharacterClass(i3);

		if (!c1.intersect(c3).equals(c1)) {
			throw new RuntimeException("CharacterClass test 1d failed");
		}
		
		if (!c3.intersect(c1).equals(c1)) {
			throw new RuntimeException("CharacterClass test 1e failed");
		}
		
		if (!c1.intersect(c1).equals(c1)) {
			throw new RuntimeException("CharacterClass test 1f failed");
		}
		
		Set<CharacterClass> ccs = new ShareableHashSet<CharacterClass>();
		ccs.add(c1);
		ccs.add(c2);
		ccs.add(c3);			
		ESet<Symbol> commons = CharacterClass.getCommonShiftables(ccs);
		if (commons.size() != 1) {
			throw new RuntimeException("CharacterClass test 1c failed");
		}
		
		c1.add(c2);		
		if (!c1.equals(c3)) {
			throw new RuntimeException("CharacterClass test 1a failed");
		}
		
		if (!c2.intersects(c3)) {
			throw new RuntimeException("CharacterClass test 1b failed");
		}
	}
	
	public void test2() {
		Symbol.resetSymbolCache();
		
		int i1[] = {0, 5, 10, 15};
		int i2[] = {7, 7};
		int i3[] = {0, 5, 7, 7, 10, 15};
		CharacterClass c1 = new CharacterClass(i1);		
		CharacterClass c2 = new CharacterClass(i2);
		CharacterClass c3 = new CharacterClass(i3);
		
		if (!c3.intersect(c2).equals(c2)) {
			throw new RuntimeException("CharacterClass test 2c failed");
		}
		
		if (!c2.intersect(c3).equals(c2)) {
			throw new RuntimeException("CharacterClass test 2d failed");
		}
		
		if (!c1.intersect(c3).equals(c1)) {
			throw new RuntimeException("CharacterClass test 2e failed");
		}
		
		if (!c3.intersect(c1).equals(c1)) {
			throw new RuntimeException("CharacterClass test 2f failed");
		}
		
		c1.add(c2);		
		if (!c1.equals(c3)) {
			throw new RuntimeException("CharacterClass test 2a failed");
		}

		if (!c2.intersects(c3)) {
			throw new RuntimeException("CharacterClass test 2b failed");
		}
	}
	
	public void test3() {
		Symbol.resetSymbolCache();
		
		int i1[] = {0, 5, 10, 15};
		int i2[] = {6, 9};
		int i3[] = {0, 15};
		CharacterClass c1 = new CharacterClass(i1);		
		CharacterClass c2 = new CharacterClass(i2);
		CharacterClass c3 = new CharacterClass(i3);
		
		if (c1.intersects(c2)) {
			throw new RuntimeException("CharacterClass test 3a failed");
		}

		c1.add(c2);
		if (!c1.equals(c3)) {
			throw new RuntimeException("CharacterClass test 3b failed");
		}
	}
	
	public void test4() {
		Symbol.resetSymbolCache();
		
		// this happened once: [0-38,40-92,93-255]
		int[] i1 = {0,38,40,91,93,255};
		int[] i2 = {0,38,40,255};
		CharacterClass c1 = new CharacterClass(i1);
		CharacterClass c2 = new CharacterClass(i2);
		c1.add(92, 92);
		if (!c1.equals(c2)) {
			throw new RuntimeException("CharacterClass test 4a failed");
		}
		
		int[] i3 = {0,38,40,92};
		int[] i4 = {0,38,40,255};
		CharacterClass c3 = new CharacterClass(i3);
		CharacterClass c4 = new CharacterClass(i4);
		c3.add(93, 255);
		if (!c3.equals(c4)) {
			throw new RuntimeException("CharacterClass test 4b failed");
		}
	}
	
	public void test5() {
		Symbol.resetSymbolCache();
		
		int[] i1 = {0,33,35,60};
		int[] i2 = {67,67};
		int[] i3 = {34,34};
		CharacterClass c1 = new CharacterClass(i1);		
		CharacterClass c2 = new CharacterClass(i2);
		CharacterClass c3 = new CharacterClass(i3);
		Set<CharacterClass> ccs = new ShareableHashSet<CharacterClass>();
		ccs.add(c1); ccs.add(c2); ccs.add(c3);
		
		ESet<Symbol> common = CharacterClass.getCommonShiftables(ccs);
		System.out.println(common);
	}
}
