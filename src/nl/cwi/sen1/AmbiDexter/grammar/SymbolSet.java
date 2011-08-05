package nl.cwi.sen1.AmbiDexter.grammar;

import nl.cwi.sen1.AmbiDexter.util.ESet;


public interface SymbolSet extends ESet<Symbol> {
	/*public boolean add(Symbol s);
	public boolean addAll(Collection<? extends Symbol> c);
	public boolean contains(Object o);
	public boolean containsAll(Collection<?> c);
	public Iterator<Symbol> iterator();
	public Symbol removeOne();
	public boolean remove(Object o);
	public boolean removeAll(Collection<?> c);
	public boolean retainAll(Collection<?> c);
	public int size();
	public Object[] toArray();
	public <T> T[] toArray(T[] a);*/
	public boolean intersects(SymbolSet lookahead);
	public int compareTo(SymbolSet other);
}
