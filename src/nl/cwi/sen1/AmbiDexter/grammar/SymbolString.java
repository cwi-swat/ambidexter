package nl.cwi.sen1.AmbiDexter.grammar;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class SymbolString extends ArrayList<Symbol> {
	
	public SymbolString() {
		
	}
	
	public SymbolString(int len) {
		super(len);
	}
	
	public SymbolString(String s[]) {
		super(s.length);
		
		for (int i = 0; i < s.length; i++) {
			this.add(Grammar.getInstance().getTerminal(s[i]));
		}
	}
	
	public SymbolString(SymbolString s) {
		super(s); // copy constructor
	}
	
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < size(); i++) {
			if (i != 0) {
				s += " " + get(i);
			} else {
				s += get(i);
			}
		}
		return s;
	}
	
	public String prettyPrint() {
		String s = "";
		for (int i = 0; i < size(); i++) {
			if (i != 0) {
				s += " " + get(i).prettyPrint();
			} else {
				s += get(i).prettyPrint();
			}
		}
		return s;
	}

	public void revert() {
		int s = size();
		for (int i = 0; i < s / 2; i++) {
			Symbol t = get(i);
			set(i, get(s - i - 1));
			set(s - i - 1, t);
		}
	}

	public boolean endsWith(Symbol s) {
		int size = size();
		return size > 0 && get(size - 1) == s;
	}

	public void copy(SymbolString ss, int index) {
		// overwrite contents from position i with ss.
		ensureCapacity(ss.size() + index);
		int size = size();
		for (Symbol s : ss) {
			if (index < size) {
				set(index++, s);
			} else {
				add(s);
			}
		}		
	}

	public void intersect(SymbolString y) {
		for (int i = size() - 1; i >= 0; --i) {
			Symbol s = get(i);
			if (s instanceof CharacterClass) {
				final Symbol ys = y.get(i);
				if (!(ys instanceof CharacterClass)) {
					System.out.println(this);
					System.out.println(y);
				}
				set(i, ((CharacterClass) s).intersect((CharacterClass) ys));
			}
		}		
	}

	public boolean containsReconstructedCharacters() {
		for (int i = size() - 1; i >= 0; --i) {
			Symbol s = get(i);
			if (s instanceof Character) {
				if (((Character) s).reconstructed()) {
					return true;
				}
			}
		}
		return false;
	}

	public String toAscii() {
		String str = "";
		for (int i = 0; i < size(); i++) {
			Symbol s = get(i);
			if (s instanceof Character) {
				str += ((Character) s).toAscii();
			}
		}
		return str;
	}

	public boolean containsOnlyCharClasses() {
		for (int i = size() - 1; i >= 0; --i) {
			Symbol s = get(i);
			if (!(s instanceof CharacterClass)) {
				return false;
			}
		}
		return true;
	}
}
