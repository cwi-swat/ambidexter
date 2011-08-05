package nl.cwi.sen1.AmbiDexter.util;

public class LinkedList<T> {
	public T elem = null;
	public LinkedList<T> next = null;

	// empty
	public LinkedList() {
	}

	// initial first element
	public LinkedList(T t) {
		elem = t;
	}
	
	// prefix t to l
	public LinkedList(T t, LinkedList<T> l) {
		elem = t;
		next = l;
	}
	
	public void append(T t) {
		if (next == null) {
			if (elem == null) {
				elem = t;
			} else {
				next = new LinkedList<T>(t);
			}
		} else {
			next.append(t);
		}
	}

	public LinkedList<T> insert(T t) {
		LinkedList<T> l = new LinkedList<T>(t);
		l.next = next;
		next = l;
		return l;
	}
	
	@Override
	public String toString() {
		String s = "[" + elem;
		if (next != null) {
			return s + "," + next.toString2();
		} else {
			return s + "]";
		}
	}
	
	private String toString2() {
		if (next != null) {
			return "" + elem + "," + next.toString2(); 
		} else {
			return "" + elem + "]";
		}
	}
	
	public int size() {
		int s = elem == null ? 0 : 1;
		if (next != null) {
			s += next.size();
		}
		return s;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elem == null) ? 0 : elem.hashCode());
		result = prime * result + ((next == null) ? 0 : next.hashCode());
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof LinkedList))
			return false;
		LinkedList other = (LinkedList) obj;
		if (elem == null) {
			if (other.elem != null)
				return false;
		} else if (!elem.equals(other.elem))
			return false;
		if (next == null) {
			if (other.next != null)
				return false;
		} else if (!next.equals(other.next))
			return false;
		return true;
	}
}
