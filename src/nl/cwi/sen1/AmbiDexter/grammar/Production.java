/**
 * 
 */
package nl.cwi.sen1.AmbiDexter.grammar;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.util.Relation;


public class Production {
	public int nr;
	public NonTerminal lhs;
	public ArrayList<Symbol> rhs;
	public Derive derivation;
	public Reduce reduction;
	private int hash;
	public Relation<Integer, Production> deriveRestrictions;
	public boolean reject = false, prefer = false, avoid = false;
	public boolean reachable = false;
	public boolean usedForReject = false;
	public boolean reconstructed = false;
	
	{
		rhs = new ArrayList<Symbol>();
		deriveRestrictions = new Relation<Integer, Production>();
	}
	
	public Production(NonTerminal lhs, int nr) {
		this.lhs = lhs;
		this.nr = nr;
	}
	
	// clone
	public Production(Production p, int nr) {
		this.lhs = p.lhs;
		this.nr = nr;
		for (Symbol s : p.rhs) {
			rhs.add(s);
		}
		for (Entry<Integer, Set<Production>> e : p.deriveRestrictions.m) {
			deriveRestrictions.put(e.getKey(), e.getValue()); // watch out, does not clone!
		}
		reject = p.reject;
		avoid = p.avoid;
		prefer = p.prefer;
		
		reachable = p.reachable;
		usedForReject = p.usedForReject;
		reconstructed = p.reconstructed;
	}
	
	public boolean isEmpty() {
		return rhs.size() == 0;
	}

	public void addSymbol(Symbol s) {
		rhs.add(s);
	}
	
	public void done() {
		derivation = new Derive(this);
		reduction = new Reduce(this);
		hash = calcHashCode();
		lhs.productions.add(this);
	}

	public boolean isInjection() {
		return rhs.size() == 1 && rhs.get(0) instanceof NonTerminal;
	}

	@Override
	public int hashCode() {
		return hash;
	}
	
	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
		result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Production))
			return false;
		Production other = (Production) obj;
		if (lhs == null) {
			if (other.lhs != null)
				return false;
		} else if (!lhs.equals(other.lhs))
			return false;
		if (rhs == null) {
			if (other.rhs != null)
				return false;
		} else if (!rhs.equals(other.rhs))
			return false;
		//return origin == other.origin;
		//return true;
		return nr == other.nr;
	}

	public String toString() {
		return toString(-1);
	}

	public String toString(int position) {
		String s = lhs.s;

//		if (origin != null) {
//			s += " #";
//		}
		
		s += " ->";
		
		for (int i = 0; i < rhs.size(); i++) {
			if (i == position)
				s += " *";
			s += " " + rhs.get(i);
		}
		if (position == rhs.size())
			s += " *";
		
		return s;
	}

	public SymbolString rhsToSymbolString() {
		SymbolString s = new SymbolString(rhs.size());
		s.addAll(rhs);
		return s;
	}

	public boolean isCfToLex() {
		if (rhs.size() == 1) {
			Symbol s = rhs.get(0);
			return s.s != null && s.s.equals(lhs.s.replace("cf(", "lex("));
		}
		return false;
	}
	
	public boolean containsNonTerminals() {
		for (int i = rhs.size() - 1; i >= 0; --i) {
			if (rhs.get(i) instanceof NonTerminal) {
				return true;
			}
		}
		return false;
	}

	public boolean isDerivationAllowed(Production prod, int i) {
		// check if we are allowed to derive to production p from position i.
		return !deriveRestrictions.contains(i, prod);
	}
	
	public void addDeriveRestriction(int pos, Production prod) {
		deriveRestrictions.add(pos, prod);
	}	
	
	public Symbol getSymbolAt(int i) {
		return rhs.get(i);
	}
	
	public int getLength() {
		return rhs.size();
	}
	
	public void setSymbol(int pos, Symbol s) {
		deriveRestrictions.m.remove(pos);
		rhs.set(pos, s);
	}
	
	@SuppressWarnings("unused")
	private void insertSymbol(int pos, Symbol s) {
		Relation<Integer, Production> newDR = new Relation<Integer, Production>();
		for (int i = 0; i <= rhs.size(); i++) {
			if (i < pos) {
				Set<Production> ps = deriveRestrictions.m.get(i);
				if (ps != null) {
					newDR.put(i, ps);
				}
			} else {
				Set<Production> ps = deriveRestrictions.m.get(i);
				if (ps != null) {
					newDR.put(i + 1, ps);
				}
			}
		}
		deriveRestrictions = newDR;
		
		rhs.add(pos, s);
	}

	@SuppressWarnings("unused")
	private void removeSymbol(int pos) {
		Relation<Integer, Production> newDR = new Relation<Integer, Production>();
		for (int i = 0; i <= rhs.size(); i++) {
			if (i < pos) {
				Set<Production> s = deriveRestrictions.m.get(i);
				if (s != null) {
					newDR.put(i, s);
				}
			} else if (i > pos) {
				Set<Production> s = deriveRestrictions.m.get(i);
				if (s != null) {
					newDR.put(i - 1, s);
				}
			}
		}
		deriveRestrictions = newDR;
		
		rhs.remove(pos);
	}

	public void serialize(Writer w) throws IOException {
		w.write(lhs.s + "\n");
		w.write(rhs.size() + "\n");
		for (int i = 0; i < rhs.size(); i++) {
			Symbol s = rhs.get(i);
			if (s instanceof CharacterClass) {
				w.write(((CharacterClass) s).serialize() + "\n");
			} else {
				w.write(s.s);
			}
		}
	}
	
	public int deserialize(String[] s, int pos, Grammar g) {
		// lhs has been read already
		int length = Integer.parseInt(s[pos++]);
		for (int i = 0; i < length; i++) {
			rhs.add(g.getSymbol(s[pos++]));
		}
		return pos;
	}
}