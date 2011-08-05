package nl.cwi.sen1.AmbiDexter.util;

import java.util.Collection;
import java.util.Iterator;

public class FragmentStack<E> implements Stack<E> {

	private Object[][] data;
	private int size;
	private int fragmentSize;
	
	public FragmentStack() {
		this(1024);
	}
	
	public FragmentStack(int size) {
		fragmentSize = size;
		clear();
	}
	
	private void grow(int fragments) {
		Object[][] olddata = data;
		data = new Object[fragments][];
		for (int i = 0; i < olddata.length; ++i) {
			data[i] = olddata[i];
		}
		for (int i = olddata.length; i < fragments; ++i) {
			data[i] = new Object[fragmentSize];
		}
	}
		
	@SuppressWarnings("unchecked")
	public E get(int i) {
		if (i >= size) {
			return null;
		}
		return (E) data[i / fragmentSize][i % fragmentSize];
	}

	public E pop() {
		E e = get(size - 1);
		--size;
		return e;
	}
	
	public E peek() {
		return get(size - 1);
	}

	public void set(int i, E e) {
		if (i >= data.length * fragmentSize) {
			grow((i / fragmentSize) + 1);
		}
		data[i / fragmentSize][i % fragmentSize] = e;
		if (i >= size) {
			size = i + 1;
		}
	}

	public boolean add(E o) {
		if (size == data.length * fragmentSize) {
			grow(data.length * 2);
		}
		data[size / fragmentSize][size % fragmentSize] = o;
		++size;
		return true;
	}

	public boolean addAll(Collection<? extends E> c) {
		// TODO do arraycopy if c instanceof queue
		for (E e : c) {
			add(e);
		}
		return true;
	}

	public void clear() {
		data = new Object[2][];
		for (int i = 0; i < data.length; ++i) {
			data[i] = new Object[fragmentSize];
		}
		size = 0;		
	}

	public boolean contains(Object o) {
		int k = 0;
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < fragmentSize; ++j) {
				if (k++ == size) {
					return false;
				}
				if (data[i][j].equals(o)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsRef(Object o) {
		int k = 0;
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < fragmentSize; ++j) {
				if (k++ == size) {
					return false;
				}
				if (data[i][j] == o) {
					return true;
				}
			}
		}
		return false;
	}

	public int size() {
		return size;
	}
	
	public int allocatedSize() {
		return data.length * fragmentSize;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean remove(Object o) {
		int k = 0;
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < fragmentSize; ++j) {
				if (k == size) {
					return false;
				}
				if (data[i][j].equals(o)) {
					remove(k);
					return true;
				}
				++k;
			}
		}
		return false;
	}

	public void remove(int i) {
		--size;
		data[i / fragmentSize][i % fragmentSize] = data[size / fragmentSize][size % fragmentSize];
	}
	
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

}
