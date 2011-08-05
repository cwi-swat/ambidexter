/**
 * 
 */
package nl.cwi.sen1.AmbiDexter.grammar;

import java.util.Map;

import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;


public class Symbol {
	public String s;
	public int id;
	
	private static int idCounter;
	private static Queue<Symbol> symbols;
	private static Map<Integer, Character> characters;
	
	public Symbol() {
		// used for character classes
		// does not have id
	}
	
	public Symbol(String s, int id) {
		// for characters
		this.s = s;
		this.id = id;
		symbols.add(this);
	}
	
	public Symbol(String s) {
		this.s = s;
		id = idCounter++;
		symbols.add(this);
	}
	
	public boolean canShiftWith(Symbol s) {
		return this == s; 
	}
	
	public String toString() {
		return s;
	}

	@Override
	public int hashCode() {
		/*final int prime = 31;
		int result = 1;
		result = prime * result + ((s == null) ? 0 : s.hashCode());
		return result;*/
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		return this == obj;
		/*if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Symbol other = (Symbol) obj;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		return true;*/
	}

	public String prettyPrint() {
		return toString();
	}
	
	//============ Static functions ======================
	
	public static int getNumberOfSymbols() {
		return idCounter;
	}
	
	public static Symbol getSymbol(int id) {
		Symbol s = null;
		if (id > 0) {
			s = symbols.get(id - 1);			
		} else {
			id = -id;
			s = characters.get(id);
			if (s == null) {
				s = new Character(id);
				characters.put(id, (Character) s);
			}
		}
		return s;
	}
	
	public static void resetSymbolCache() {
		idCounter = 1;
		symbols = new Queue<Symbol>(1024);
		characters = new ShareableHashMap<Integer, Character>();
	}
}