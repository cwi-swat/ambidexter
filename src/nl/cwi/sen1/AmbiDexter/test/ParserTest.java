package nl.cwi.sen1.AmbiDexter.test;

import junit.framework.TestCase;
import nl.cwi.sen1.AmbiDexter.automata.LR0NFA;
import nl.cwi.sen1.AmbiDexter.automata.NFA;
import nl.cwi.sen1.AmbiDexter.grammar.Grammar;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;
import nl.cwi.sen1.AmbiDexter.grammar.importexport.YaccImporter;
import nl.cwi.sen1.AmbiDexter.parse.IParser;
import nl.cwi.sen1.AmbiDexter.parse.ParseTree;
import nl.cwi.sen1.AmbiDexter.parse.SimpleSGLRParser;

public class ParserTest extends TestCase {

	static int testNR = 0;
	public void test1()	{ // simple 1 char with dying stack
		String s[] = {"'a'"};
		testParser(" %% S : A | B ; A : 'a' ; B : 'b' ; ", s);
	}
	
	public void test2()	{ // vertical ambiguity
		String s[] = {"'a'"};
		testParser(" %% S : A | B ; A : 'a' ; B : 'a' ; ", s);
	}
	
	public void test3()	{ // horizontal ambiguity
		String s[] = {"'a'", "'b'", "'c'"};
		testParser(" %% S : A B ; A : 'a' | 'a' 'b' ; B : 'b' 'c' | 'c' ; ", s);
	}
	
	public void test4()	{ // right recursion
		String s[] = {"'a'", "'a'", "'a'"};
		testParser(" %% S : 'a' S | 'a' ; ", s);
	}
	
	public void test5()	{ // left recursion
		String s[] = {"'a'", "'a'", "'a'"};
		testParser(" %% S : S 'a' | 'a' ; ", s);
	}
	
	public void test6()	{ // hidden right recursion
		String s[] = {"'a'", "'a'", "'b'"};
		testParser(" %% S : 'a' S B | 'b' ; B : ; ", s);
	}
	
	public void test7()	{ // hidden left recursion
		String s[] = {"'b'", "'a'", "'a'"};
		testParser(" %% S : B S 'a' | 'b' ; B : ; ", s);
	}
	
	public void test8()	{ // hidden empty right recursion (ambiguous)
		String s[] = {"'a'"};
		testParser(" %% S : S B | 'a' ; B : ; ", s);
	}
	
	public void test9()	{ // hidden empty left recursion (ambiguous)
		String s[] = {"'a'"};
		testParser(" %% S : B S | 'a' ; B : ; ", s);
	}	
	
	public void test10()	{ // horizontal ambiguity
		String s[] = {"'a'"};
		testParser(" %% S : A B ; A : 'a' | ; B : 'a' | ; ", s);
	}
	
	public void test11()	{ // horizontal ambiguity empty
		String s[] = {};
		testParser(" %% S : S S | ; ", s);
	}
		
	public void test12()	{ // highly exponential
		int size = 4;
		String s[] = new String[size];
		for (int i = 0; i < size; i++) {
			s[i] = "'a'";
		}
		testParser(" %% S : 'a' | S S | S S S ; ", s);
	}
	
	public void test13()	{ // highly exponential, provided by Dick Grune
		int size = 4;
		String s[] = new String[size];
		for (int i = 0; i < size; i++) {
			s[i] = "'a'";
		}
		testParser(" %% S : 'a' | S T U | ; T : T U S | ; U : U S T | ; ", s);
	}

	public static void testParser(String yacc, String sentence[]) {
		String name = "ParserTest" + testNR++;
		Grammar g = new YaccImporter().importYacc(yacc, name);
		g.finish();
		g.verify();
		
		NFA nfa = new LR0NFA(g);
		nfa.build(true, false);
		nfa.finish();
		nfa.optimize(true);
		
		IParser p = new SimpleSGLRParser(nfa);
		SymbolString s = new SymbolString(sentence);
		
		System.out.println("Parsing: " + s);
		long ms = System.currentTimeMillis();
		ParseTree result = p.parse(s);
		
		ms = System.currentTimeMillis() - ms;
		System.out.println("Time: " + ms /*+ ", ambiguities: " + result.b*/);
		System.out.println(result.top.prettyPrint());
		System.out.println();

		//Util.writeTextFile(name + ".dot", result.a.toDot());
	}
}
