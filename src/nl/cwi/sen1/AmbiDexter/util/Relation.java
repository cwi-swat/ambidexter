package nl.cwi.sen1.AmbiDexter.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;


public class Relation<A, B> implements Collection<Pair<A, B>> {
	public ShareableHashMap<A, Set<B>> m;
	
	public Relation() {
		m = new ShareableHashMap<A, Set<B>>();
	}
	
	public Relation(ShareableHashMap<A, Set<B>> m) {
		this.m = m;
	}
	
	public Relation(Set<Pair<A, B>> s) {
		m = new ShareableHashMap<A, Set<B>>();
		for (Pair<A, B> p : s) {
			add(p.a, p.b);
		}
	}
	
	public boolean add(A a, B b) {
		Set<B> s = m.get(a);
		if (s == null) {
			s = new ShareableHashSet<B>();
			m.put(a, s);
		}
		return s.add(b);
	}
	
	public Set<B> get(A a) {
		Set<B> s = m.get(a);
		if (s == null) {
			s = new ShareableHashSet<B>();
			m.put(a, s);
		}
		return s;
	}
	
	public void addAll(A a, Set<B> bs) {
		Set<B> s = m.get(a);
		if (s == null) {
			s = new ShareableHashSet<B>();
			m.put(a, s);
		}
		s.addAll(bs);
	}
	
	public void addDomain(Collection<A> c) {
		for (A a : c) {
			Set<B> s = m.get(a);
			if (s == null) {
				s = new ShareableHashSet<B>();
				m.put(a, s);
			}
		}	
	}
	
	public void put(A a, Set<B> bs) {
		m.put(a, bs);
	}
	
	public boolean contains(A a, B b) {
		Set<B> s = m.get(a);
		if (s == null) {
			return false;
		}
		return s.contains(b);
	}
	
	public boolean remove(A a, B b) {
		Set<B> s = m.get(a);
		if (s != null) {
			return s.remove(b);
		}
		return false;
	}
	
	public int size() {
		int i = 0;
		for (Entry<A, Set<B>> e : m) {
			i += e.getValue().size();
		}
		return i;
	}
	
	public Iterator<Entry<A, Set<B>>> entryIterator() {
		return m.iterator();
	}
	
	public String toString() {
		String s = "{";
		boolean first = true;
		for (Pair<A, B> e : this) {
			if (first) {
				first = false;
			} else {
				s += ", ";
			}
			s += e.toString();
		}
		return s + "}";
	}

	public boolean add(Pair<A, B> o) {
		return add(o.a, o.b);
	}

	@SuppressWarnings("unchecked")
	public boolean addAll(Collection<? extends Pair<A, B>> c) {
		if (c instanceof Relation) {
			for (Entry<A, Set<B>> e : ((Relation<A, B>)c).m) {
				addAll(e.getKey(), e.getValue());
			}
			return true;
		} else {
			boolean changed = false;
			for (Pair<A, B> p : c) {
				changed |= add(p);
			}
			return changed;
		}
	}

	public void clear() {
		m = new ShareableHashMap<A, Set<B>>();
	}

	@SuppressWarnings("unchecked")
	public boolean contains(Object o) {
		if (o instanceof Pair) {
			Pair<A,B> p = (Pair) o;
			return contains(p.a, p.b);
		}
		return false;
	}
	
	public boolean inDomain(A a) {
		return m.containsKey(a);
	}

	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	public boolean isEmpty() {
		if (m.size() == 0) {
			return true;
		}
		for (Set<B> s : m.values()) {
			if (s.size() > 0) {
				return false;
			}
		}
		return true;
	}

	public Iterator<Pair<A, B>> iterator() {
		return new RelationIterator(this);
	}

	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		if (o instanceof Pair) {
			Pair<A,B> p = (Pair) o;
			return remove(p.a, p.b);
		}
		return false;
	}

	public boolean removeAll(Collection<?> c) {
		boolean changed = false;
		for (Object o : c) {
			changed |= remove(o);
		}
		return changed;
	}
	
	public void removeAll(A a, Collection<? extends B> bs) {
		Set<B> s = m.get(a);
		if (s != null) {
			s.removeAll(bs);
		}
	}

	public boolean retainAll(Collection<?> c) {
		Relation<A, B> r2 = new Relation<A, B>();
		boolean changed = false;
		for (Pair<A, B> p : this) {
			if (c.contains(p)) {
				r2.add(p);
			} else {
				changed = true;
			}
		}
		m = r2.m;
		return changed;
	}
	
	public Set<A> domain() {
		return m.keySet();
	}
	
	public Relation<B, A> reverse() {
		Relation<B, A> r = new Relation<B, A>();
		
		for (Entry<A, Set<B>> e : m) {
			for (B b : e.getValue()) {
				r.add(b, e.getKey());
			}
		}		
		
		return r;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if (o instanceof Relation) {
			return m.equals(((Relation) o).m);
		} else {
			return false;
		}
	}
	
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public class RelationIterator implements Iterator<Pair<A, B>> {
		Relation<A, B> r;
		Iterator<Entry<A, Set<B>>> mapIter;
		Iterator<B> setIter;
		A currentKey;
		
		public RelationIterator(Relation<A, B> r) {
			this.r = r;
			mapIter = r.m.iterator();
			advance();
		}

		private void advance() {
			setIter = null;
			while (mapIter.hasNext()) {
				Entry<A, Set<B>> e = mapIter.next();
				currentKey = e.getKey();
				setIter = e.getValue().iterator();
				if (setIter.hasNext()) {
					break;
				}
			}
		}
		
		public boolean hasNext() {
			if (setIter != null) {
				if (setIter.hasNext()) {
					return true;
				}
				advance();
				return (setIter != null && setIter.hasNext());
			}
			return false;
		}

		public Pair<A, B> next() {
			return new Pair<A, B>(currentKey, setIter.next());
		}

		public void remove() {
			setIter.remove();			
		}		
	}
}
