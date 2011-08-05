package nl.cwi.sen1.AmbiDexter.grammar;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;


@SuppressWarnings("serial")
public class SymbolBitSet extends BitSet implements SymbolSet {

	public SymbolBitSet() {
		super(Symbol.getNumberOfSymbols());
	}
	
	public boolean add(Symbol s) {
		if (s instanceof CharacterClass) {
			CharacterClass cc = (CharacterClass) s;
			for (int i = 0; i < cc.size / 2; i++) {
				set(cc.ranges[i * 2], cc.ranges[i * 2 + 1] + 1);
			}
		} else {
			set(s.id);
		}
		return false; // just add
	}

	public boolean addAll(Collection<? extends Symbol> c) {
		if (c instanceof BitSet) {
			or((BitSet) c);
		} else {
			for (Symbol s : c) {
				add(s);
			}
		}
		return true;
	}

	// TODO does not work with characterclasses
	public boolean contains(Object o) {
		if (o instanceof Symbol) {
			return get(((Symbol)o).id);
		} else {
			return false;
		}
	}

	public boolean containsAll(Collection<?> c) {
		if (c instanceof SymbolBitSet) {
			BitSet copy = (BitSet) this.clone();
			copy.and((SymbolBitSet) c);
			return copy.equals(c);
		} else {
			for (Object s : c) {
				if (!contains(s)) {
					return false;
				}
			}
			return true;
		}
	}

	public Iterator<Symbol> iterator() {
		return new SymbolBitSetIterator(this);
	}
	
	public Symbol removeOne() {
		int id = nextSetBit(0);
		clear(id);
		return Symbol.getSymbol(id);
	}

	public boolean remove(Object o) {
		if (o instanceof Symbol) {
			clear(((Symbol) o).id);
		}
		return false;
	}

	public boolean removeAll(Collection<?> c) {
		if (c instanceof SymbolBitSet) {
			andNot((SymbolBitSet) c);
		} else {
			for (Object o : c) {
				remove(o);
			}
		}
		return true;
	}

	public boolean retainAll(Collection<?> c) {
		if (c instanceof SymbolBitSet) {
			and((SymbolBitSet)c);
		} else {
			SymbolBitSet copy = (SymbolBitSet) this.clone();
			clear();
			for (Object o : c) {
				if (copy.contains(o)) {
					set(((Symbol)o).id);
				}
			}
		}
		return false;
	}

	public int size() {
		return cardinality();
	}

	public Object[] toArray() {
		int size = size();
		Object a[] = new Object[size];
		int id = 0;
		for (int i = 0; i < size; i++) {
			id = nextSetBit(id);
			a[i] = Symbol.getSymbol(id);
		}
		return a;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		if (a instanceof Symbol[]) {
			return (T[]) toArray();	 
		} else {
			return null;
		}		
	}
	
	public boolean intersects(SymbolSet set) {
		if (set instanceof SymbolBitSet) {
			return intersects((BitSet)set);
		} else {
			throw new UnsupportedOperationException("not implemented");
			/*for (Symbol s : set) {
				if (contains(s)) {
					return true;
				}
			}
			return false;*/
		}
	}
	
	@Override
	public String toString() {
		String result = "{";
		
		SymbolSet s = new SymbolBitSet();
		s.addAll(this);
		boolean first = true;
		
		while (s.size() > 0) {
			// find smallest terminal
			Symbol st = null;
			for (Symbol t : s) {
				if (st == null) {
					st = t;
				} else {
					if (t.s.compareTo(st.s) < 0) {
						st = t;
					}
				}
			}
			
			if (!first) {
				result = result + ", ";
			} else {
				first = false;
			}
			result = result + st.s;

			s.remove(st);
		}
	
		return result + "}";
	}

	static class SymbolBitSetIterator implements Iterator<Symbol> {

		SymbolBitSet set;
		int pos;
		
		public SymbolBitSetIterator(SymbolBitSet set) {
			this.set = set;
			pos = set.nextSetBit(0);
		}
		
		public boolean hasNext() {
			return pos != -1;
		}

		public Symbol next() {
			Symbol s = Symbol.getSymbol(pos);
			pos = set.nextSetBit(pos + 1);
			return s;
		}

		public void remove() {
			set.clear(pos);
			pos = set.nextSetBit(pos + 1);
		}
	}

	public int compareTo(SymbolSet other) {
		if (this.size() != other.size()) {
			return this.size() - other.size();
		}
		if (!(other instanceof SymbolBitSet)) {
			return 1;
		}
		SymbolBitSet that = (SymbolBitSet) other;
		int i1 = this.nextSetBit(0);
		int i2 = that.nextSetBit(0);
		while (i1 == i2 && i1 != -1) {
			i1 = this.nextSetBit(i1 + 1);
			i2 = that.nextSetBit(i2 + 1);
		}
		return i1 - i2; // i1 == -1 implies i2 == -1 implies equality
	}
}
