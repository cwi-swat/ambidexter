package nl.cwi.sen1.AmbiDexter.util;

import java.util.Collection;

public interface Stack<E> extends Collection<E> {

	public E get(int i);	
	public void set(int i, E e);
	public E pop();
	public E peek();
	public boolean containsRef(Object o);
	public void remove(int i);
	
	public int allocatedSize();
}
