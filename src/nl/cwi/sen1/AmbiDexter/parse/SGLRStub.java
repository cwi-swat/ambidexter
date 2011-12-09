package nl.cwi.sen1.AmbiDexter.parse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import nl.cwi.sen1.AmbiDexter.grammar.Character;
import nl.cwi.sen1.AmbiDexter.grammar.ListNonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.NonTerminal;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;

public class SGLRStub implements IParser {

	String parseTableFile;
	
	public SGLRStub(String parseTableFile) {
		this.parseTableFile = parseTableFile;
	}
	
	@Override
	public ParseTree parse(SymbolString s) {
		return null;
	}

	@Override
	public ParseTree parse(SymbolString s, NonTerminal nt) {
		return null;
	}

	public boolean parseSGLR(SymbolString ss, NonTerminal n) {
		String s = "";
		for (int z = 0; z < ss.size(); ++z) {
			Character c = (Character)ss.get(z);
			s += c.toAscii();				
		}
	
		if (n.asfixName == null) {
			System.out.println("Could not parse " + n + ": " + s);
			return false;
		}

		//System.out.println("Parsing " + n + ": " + s);

		String startSymbol = n.asfixName;
		
		if (startSymbol.startsWith("lex") && !n.usedInLayout) {
			startSymbol = startSymbol.replaceFirst("lex", "cf");
		}
				
		boolean ambig = false;
		ambig = parseSGLR(s, startSymbol);
		
		// sometimes a + list does not appear in original grammar, but is introduced by normalization (b/c of *)
		// if the string is not ambiguous, try the * list
		// TODO does not work for nested lists
		if (!ambig && n instanceof ListNonTerminal) {
			ListNonTerminal ln = (ListNonTerminal) n;
			if (!ln.star) {
				startSymbol = startSymbol.replaceFirst("iter\\(", "iter-star\\(");
				ambig = parseSGLR(s, startSymbol);
			}
		}
		
		return ambig;
	}

	private boolean parseSGLR(String s, String startSymbol) {
		// TODO fix case where startSymbol is "start"
		String cmd = "sglr -t -p " + parseTableFile + " -S " + startSymbol;
		String tree = execSGLR(s, cmd);
	    boolean ambig = !tree.equals("") && !tree.endsWith(",0)") && !tree.startsWith("summary");
	    return ambig;
	}
	
	private String execSGLR(String s, String cmd) {
		try {
			Process parser = Runtime.getRuntime().exec(cmd);
			OutputStream stdin = parser.getOutputStream();
			InputStream stderr = parser.getErrorStream();
			InputStream stdout = parser.getInputStream();
		    
		    stdin.write(s.getBytes());
		    stdin.flush();
		    stdin.close();

		    String line, tree = "";
		    BufferedReader parserOut = new BufferedReader(new InputStreamReader(stdout));
		    while ((line = parserOut.readLine()) != null) {
		    	tree += line;
		    }
		    parserOut.close();
		    stderr.close();

		    //System.out.println(tree);
		    
		    if (tree.equals("summary(\"parser\",\"sglr\",[error(\"could not open parse table file\",[])])")) {
		    	throw new RuntimeException(tree);
		    }
		    
		    return tree;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
