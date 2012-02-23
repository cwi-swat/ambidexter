/**
 * 
 */
package nl.cwi.sen1.AmbiDexter.grammar;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.util.ESet;
import nl.cwi.sen1.AmbiDexter.util.LinkedList;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class CharacterClass extends Symbol implements SymbolSet {
	
	// pairwise lower and higher bound
	// single characters have equal lower and higher bound
	public int ranges[];
	int size = 0; // nr of positions used in ranges
	
	public CharacterClass() {
		ranges = new int[2];
	}
	
	public CharacterClass(int size, int id) {
		ranges = new int[size];
		this.id = id;
	}
	
	public CharacterClass(int r[]) {
		ranges = r;
		size = r.length;
	}
	
	public CharacterClass(String s) { // deserialize
		String[] a = s.substring(2).split(",");
		ranges = new int[a.length];
		for (int i = 0; i < a.length; ++i) {
			ranges[i] = Integer.parseInt(a[i]);
		}
		size = a.length;
	}
	
	public boolean canShiftWith(Symbol s) {
		if (s instanceof CharacterClass) {
			return intersects((SymbolSet) s);
		} else if (s instanceof Character) {
			return contains(s);
		}
		return false;
	}
	
	public void append(int c) {
		append(c, c);
	}
	
	public void append(int c1, int c2) {
		if (size == ranges.length) {
			grow(size * 2);
		}
		ranges[size++] = c1;
		ranges[size++] = c2;
	}
	
	boolean contains(int c) {
		for (int i = 0; i < size / 2; i++) {
			if (ranges[i * 2] >= c && ranges[i*2 + 1] <= c) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		String s = "[";
		boolean first = true;
		for (int i = 0; i < size; i++) {
			if (first) {
				if (i != 0) {
					s += ",";
				}
				s += ranges[i] >= 0 ? ranges[i] : Symbol.getSymbol(-ranges[i]);
			} else {
				if (ranges[i] != ranges[i-1]) {
					s += "-" + (ranges[i] >= 0 ? ranges[i] : Symbol.getSymbol(-ranges[i]));
				}
			}
			first = !first;
		}
		return s + "]";
	}
	
	public String serialize() {
		String s = "[]";
		for (int i = 0; i < size; ++i) {
			if (i == 0) {
				s += ranges[i];
			} else {
				s += "," + ranges[i];
			}
		}
		return s;
	}
	
	@Override
	public String prettyPrint() {
		String s = "[";
		boolean first = true;
		for (int i = 0; i < size; i++) {
			if (first) {
				if (i != 0) {
					s += ",";
				}
				//s += ranges[i] >= 0 ? ranges[i] : Symbol.getSymbol(-ranges[i]).prettyPrint();
				s += Symbol.getSymbol(-ranges[i]).prettyPrint();
			} else {
				if (ranges[i] != ranges[i-1]) {
					//s += "-" + (ranges[i] >= 0 ? ranges[i] : Symbol.getSymbol(-ranges[i]).prettyPrint());
					s += "-" + Symbol.getSymbol(-ranges[i]).prettyPrint();
				}
			}
			first = !first;
		}
		return s + "]";
	}

	public boolean add(Symbol o) {
		if (o instanceof CharacterClass) {
			add((CharacterClass) o);
		} else if (o instanceof Terminal && !(o == Grammar.empty || o == Grammar.endmarker)) {
			throw new UnsupportedOperationException("not implemented");
		} else { // Chacter
			add(-o.id, -o.id);
		}
		return false;
	}
	
	private void grow(int newLength) {
		int t[] = new int[newLength];
		for (int i = 0; i < size; i++) {
			t[i] = ranges[i];
		}
		ranges = t;
	}
	
	public boolean add(CharacterClass c) {
		if (c.size == 0) {
			return false;
		}
		
		// copy this one's array into a bigger one
		grow(size + c.size);
		
		// add c's ranges one by one
		for (int i = 0; i < c.size / 2; i++) {
			add(c.ranges[i * 2], c.ranges[i * 2 + 1]);			
		}

		return false;
	}
	
	public void add(int c1, int c2) {
		int i = 0;
		while (i < size && ranges[i] < c1) {
			i += 1;
		}
		
		if (i == size) {
			if (i > 0 && ranges[i-1] == c1 - 1) {
				// t: ...--i>
				// c:        <--... 
				ranges[i-1] = c2;
			} else {
				if (ranges.length == size) {
					grow(size * 2);
				}
				ranges[size++] = c1;
				ranges[size++] = c2;
			}
		} else {
			if (i % 2 == 0) {			
				// t:    <i--...
				// c: <--...
				if (i > 0 && ranges[i-1] == c1 - 1) {
					// t: ..-->   <i--...
					// c:      <--...
					ranges[i-1] = c2;
					optimize(i-1);
				} else {				
					if (c2 >= ranges[i] - 1) {
						// t:    <i----...
						// c: <----->
						
						// overwrite range at i
						ranges[i] = c1;
						if (c2 > ranges[i + 1]) {
							// t:   <i-->
							// c: <------->
							ranges[i + 1] = c2;					
							optimize(i + 1);
						} else {
							// t:   <i-->
							// c: <--->
							
							// do nothing
						}
					} else {
						// t:      <i----...
						// c: <-->
				
						// insert new range before i
						if (ranges.length == size) {
							grow(size * 2);
						}
						
						for (int j = size - 1; j >= i; j--) {
							ranges[j + 2] = ranges[j];
						}
						ranges[i] = c1;
						ranges[i+1] = c2;
						size += 2;
					}
				}
			} else {
				// t: <-----i>
				// c:   <--... 
				if (c2 > ranges[i]) {
					// t: <----i>
					// c:   <-----> 
					ranges[i] = c2;
					optimize(i);
				} else {
					// t: <-----i>
					// c:   <-->
					
					// do nothing
				}
			}
		}		
	}
	
	private void optimize(int i) {
		// look to see if there is overlap like this:
		// t: <---i>
		// t:   <----->
		if (i == size - 1) {
			return;
		}
		
		int j = i + 1;
		while (j < size && ranges[j] <= ranges[i]) {
			j += 1;
		}
		if (j == size) {
			size = i + 1;
		} else {
			int d = j - i - 1;
			if (d % 2 == 0) {
				// t: <----i>
				// t:   ...
				// t:          <j----> 	

				if (ranges[j] == ranges[i] + 1) {
					ranges[i] = ranges[j + 1];
					j += 2;
					d += 2;
				}
				// shrink below
			} else {
				// t: <----i>
				// t:   ...
				// t:     <----j> 	

				ranges[i] = ranges[j];
				j++;
				d++;
			}
			
			// shrink
			for (int k = j; k < size; k++) {
				ranges[k - d] = ranges[k];
			}
			size -= d;
		}
	}
	
	public boolean addAll(Collection<? extends Symbol> c) {
		if (c instanceof CharacterClass) {
			add((CharacterClass) c);
		} else {
			for (Symbol s : c) {
				add(s);
			}		
		}
		return false;
	}

	public void clear() {
		size = 0;		
	}

	public CharacterClass getSmallestSubset() {
		CharacterClass cc = new CharacterClass();
		if (size > 0) {
			cc.append(ranges[0]);
		}
		return cc;
	}
	
	public boolean contains(Object o) {
		if (o instanceof Symbol) {
			if (o instanceof CharacterClass) {
				throw new UnsupportedOperationException("really?");
			} else if (o instanceof Terminal && !(o == Grammar.empty || o == Grammar.endmarker)) {
				throw new UnsupportedOperationException("not implemented");
			} else {
				return containsChar(-((Symbol) o).id);
			}
		}
		return false;
	}
	
	private boolean containsChar(int c) {
		for (int i = 0; i < size / 2; i++) {
			if (ranges[i * 2] <= c && ranges[i * 2 + 1] >= c) {
				return true;
			}
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("not implemented");
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public Iterator<Symbol> iterator() {
		return new CharacterClassIterator(this);
	}

	public boolean remove(Object o) {
		if (o instanceof CharacterClass) {
			throw new UnsupportedOperationException("why do we need this?");
		} else if (o instanceof Terminal && !(o == Grammar.empty || o == Grammar.endmarker)) {
			throw new UnsupportedOperationException("not implemented");
		} else {
			removeChar(-((Symbol) o).id);
		}
		return false;
	}

	private void removeChar(int c) {
		for (int i = 0; i < size / 2; i++) {
			if (ranges[i * 2] <= c && ranges[i * 2 + 1] >= c) {
				if (ranges[i * 2] == c) {
					if (ranges[i * 2 + 1] == c) {
						for (int j = i * 2 + 2; j < size; j++) { // shift left
							ranges[j - 2] = ranges[j];
						}
						size -= 2;
					} else {
						ranges[i * 2] += 1;
					}
				} else {
					if (ranges[i * 2 + 1] == c) {
						ranges[i * 2 + 1] -= 1;
					} else {
						// split
						if (ranges.length == size) {
							int t[] = new int[size * 2]; // resize
							for (int j = 0; j <= i * 2 + 1; j++) {
								t[j] = ranges[j];
							}
							for (int j = i * 2; j < size; j++) {
								t[j + 2] = ranges[j];
							}
							ranges = t;
						} else {
							for (int j = size - 1; j >= i * 2; j--) { // shift right
								ranges[j + 2] = ranges[j];
							}
						}
						ranges[i * 2 + 1] = c - 1;
						ranges[i * 2 + 2] = c + 1;
						size += 2;
					}
				}
				return;
			}
		}
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("not implemented");
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("not implemented");
	}

	public int size() {
		int s = 0;
		for (int i = 0; i < size / 2; i++) {
			s += ranges[i * 2 + 1] - ranges[i * 2] + 1;
		}
		return s;
	}

	public Object[] toArray() {
		throw new UnsupportedOperationException("not implemented");
	}

	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Symbol removeOne() {
		throw new UnsupportedOperationException("not implemented");
	}

	public boolean intersects(SymbolSet set) {
		if (set instanceof CharacterClass) {
			return intersects((CharacterClass)set);
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
	
	public boolean intersects(CharacterClass cc) {
		int i1 = 0;
		
		for (int i2 = 0; i2 < cc.size / 2; i2++) {
			int h2 = cc.ranges[i2 * 2];
			int t2 = cc.ranges[i2 * 2 + 1];

			while (i1 < size / 2 && ranges[i1 * 2 + 1] < h2) {
				i1++;
			}
			
			if (i1 == size / 2) {
				return false;
			}
			
			int h1 = ranges[i1 * 2];
			//int t1 = ranges[i1 * 2 + 1];
			
			// here holds: t1 >= h2			
			// 1: ...--->
			// 2:    <---...
			
			if (h1 <= t2) {
				return true;
			}
		}
		return false;
	}
	
	public CharacterClass intersect(CharacterClass cc) {
		CharacterClass i = new CharacterClass();
		
		int p2 = 0;
		
		for (int p1 = 0; p1 < size / 2; ++p1) {
			int f1 = ranges[p1 * 2];
			int t1 = ranges[p1 * 2 + 1];
			
			while (true) {
				if (p2 >= cc.size / 2) {
					return i; // done
				}
				
				int f2 = cc.ranges[p2 * 2];
				int t2 = cc.ranges[p2 * 2 + 1];
				
				if (f2 > t1) {
					// this: -->
					// cc:        <--
					break; // compare next range in this to current cc range
				}
				
				if (f1 > t2) {
					// this:      <--
					// cc:   -->
					++p2;
					continue; // compare next cc range to current range of this
				}
				
				if (f1 > f2) {
					if (t1 > t2) {
						// this:   <--->
						// cc:   <--->
						i.add(f1, t2);
						++p2; // compare next range in cc to current range of this
					} else {
						// this:   <-->
						// cc:   <------>
						// right sides can be equal
						i.add(f1, t1);
						break; // compare next range in this to current cc range
					}
				} else {
					if (t1 > t2) {
						// this: <------>
						// cc:     <-->
						// left sides can be equal
						i.add(f2, t2);
						++p2; // compare next range in cc to current range of this
					} else {
						// this: <---> 
						// cc:     <--->
						// right and left sides can be equal
						i.add(f2, t1);
						break; // compare next range in this to current cc range
					}
				}
			}
		}
		
		return i;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = prime * size;
		for (int i = size - 1; i >= 0; --i) {
			result = prime * result + ranges[i];
		}
		return result;
	}
	
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof CharacterClass) {
			CharacterClass c = (CharacterClass) o;
			
			if (size == c.size) {
				for (int i = 0; i < size; i++) {
					if (ranges[i] != c.ranges[i]) {
						return false;
					}
				}
				return true;
			}
		}
		
		return false;
	}

	// returns: this \ cc != {}
	public boolean differenceNonEmpty(CharacterClass cc) {
		int i2 = 0;
		
		// check for each range in this if it has non-overlapping parts with cc
		for (int i1 = 0; i1 < size / 2; i1++) {
			int h1 = ranges[i1 * 2];
			int t1 = ranges[i1 * 2 + 1];
			int h2 = cc.ranges[i2]; // here i2 should mark a beginning of a range
			int t2 = cc.ranges[i2 + 1];

			if (h1 < h2) {
				return true;
			}
			if (h1 == h2) {
				if (t1 > t2) {
					return true;
				}
			} else {
				while (i2 < cc.size && cc.ranges[i2] < h1) {
					i2++;
				}
				
				if (i2 == cc.size) {
					return true;
				}
				
				if (i2 % 2 == 0) {
					// 1:  <--...
					// 2:    <i2--...
					if (cc.ranges[i2] > h1) {
						return true;
					}
					// 1:  <--...
					// 2:  <i2--...
					if (cc.ranges[i2+1] < t1) {
						return true;
					}
					// 1 is completely covered by 2
					// re-compare current 2 for next 1
				} else {
					// 1:       <-----
					// 2:  ...--i2>
					
					if (t1 > cc.ranges[i2]) {
						return true;
					} else {
						// 1 is completely covered by 2
						// re-compare current 2 for next 1
						i2--;
					}
				}				
			}
		}		
		
		return false;
	}
	
	public int getMaxCharacter() {
		return ranges[size - 1];
	}
		
	static class CharacterClassIterator implements Iterator<Symbol> {

		CharacterClass set;
		int range;
		int character; // next character to return
		
		public CharacterClassIterator(CharacterClass set) {
			this.set = set;
			range = 0;
			if (set.size > 0) {
				character = set.ranges[0];
			}
		}
		
		public boolean hasNext() {
			return range < set.size / 2;
		}

		public Symbol next() {
			Symbol s = Symbol.getSymbol(-character);

			if (set.ranges[range * 2 + 1] > character) {
				character++;
			} else {
				range++;
				if (hasNext()) {
					character = set.ranges[range * 2];
				}
			}

			return s;
		}

		public void remove() {
			throw new UnsupportedOperationException("not yet implemented");
		}
	}
	
	// post: ccs is empty // TODO why?
	public static ESet<Symbol> getCommonShiftables(Set<CharacterClass> ccs) {
		
		/* divide entire character class into subranges where classes in ccs overlap,
		 * and store for each range which classes are 'in' there.
		 * example:
		 * 
		 *           1     2     3     4     5     6
		 * [1,4]     |  x  |  x  |  x  |  x  |
		 * [1,2]     |  x  |  x  |
		 * [1,2,5]   |  x  |  x  |           |  x  |
		 * [2,3]           |  x  |  x  |
		 * [4]                         |  x  |
		 *      
		 */
		
		/* XXX the current code might find non optimal solution
		 consider:
		 [1,4]
		 [1,2]
		 [1,2,5]
		 [2,3]
		 [4]
		 The current algorithm would find [1,2,4], while [2,4] is optimal.
		 b/c if there are more than one character with the same appearance rate, we should check them all
		 the problem is closely related to SAT, which is NP complete, I think this one also is.
		*/
		
		// TODO check out whether the extra costs of finding the optimal solution make a difference in derivation generation performance

		// XXX memoization of sets gives hopeless performance
		
		{ // first test whether or not there actually is overlap between the character classes
			Iterator<CharacterClass> it = ccs.iterator();
			boolean overlap = false;
			if (it.hasNext()) {
				CharacterClass c1 = new CharacterClass();
				c1.add(it.next());
				while (it.hasNext()) {
					CharacterClass c2 = it.next();
					if (c1.intersects(c2)) {
						overlap = true;
						break;
					}
					c1.add(c2);
				}
			}
			
			if (!overlap) {
				ESet<Symbol> result = new ShareableHashSet<Symbol>();
				for (CharacterClass c : ccs) {
					result.add(c.getOne());
				}
				return result;
			}
		}
		
		LinkedList<Pair<Integer, Set<CharacterClass>>> list = getOverlaps(ccs);
		
		ESet<Symbol> result = new ShareableHashSet<Symbol>();
		
		// removeAll is 8%
		
		while (ccs.size() > 0) {
			LinkedList<Pair<Integer, Set<CharacterClass>>> l = list;
			Pair<Integer, Set<CharacterClass>> max = l.elem;
			LinkedList<Pair<Integer, Set<CharacterClass>>> maxpre = null;
			while (l.next != null) {
				if (l.next.elem.b.size() > max.b.size()) {
					max = l.next.elem;
					maxpre = l;
				}				
				l = l.next;
			}
			
			//result.add(max);
			result.add(Character.getSymbol(-max.a));
			ccs.removeAll(max.b);

			// remove max from list
			// remove currently handled list elem
			if (maxpre == null) {
				list = list.next;
				if (list == null) {
					// list is empty, we're done
					break;
				}
			} else {
				maxpre.next = maxpre.next.next;
			}
			
			// remove max from other list elems
			l = list;
			l.elem.b.removeAll(max.b);
			while (l.next != null) {
				l.next.elem.b.removeAll(max.b);
				if (l.next.elem.b.size() == 0) {
					l.next = l.next.next;
				} else {
					l = l.next;
				}
			}
		}
		
		// TODO: make sure all overlaps of classes are covered!!!!
		/*{
			for (CharacterClass c1 : ccs) {
				for (CharacterClass c2 : ccs) {
					if (c1 != c2 && c1.intersects(c2)) {
						CharacterClass c3 = c1.intersect(c2);
						boolean covered = false;
						for (Symbol s : result) {
							if (c3.contains(s)) {
								covered = true;
								break;
							}
						}
						if (!covered) {
							throw new RuntimeException("Uncovered intersection between " + c1 + " and " + c2);
						}
					}
				}
			}
		}*/		
		
		return result;
	}

	public static LinkedList<Pair<Integer, Set<CharacterClass>>> getOverlaps(Set<CharacterClass> ccs) {
		LinkedList<Pair<Integer, Set<CharacterClass>>> overlaps = new LinkedList<Pair<Integer,Set<CharacterClass>>>(new Pair<Integer, Set<CharacterClass>>(0, new ShareableHashSet<CharacterClass>()));
		
		// insert all ranges of all characterclasses into the list
		for (CharacterClass cc : ccs) {
			LinkedList<Pair<Integer, Set<CharacterClass>>> l = overlaps;
			for (int i = 0; i < cc.size; ) {
				int c1 = cc.ranges[i++];
				int c2 = cc.ranges[i++];
				
				// from
				while (l.next != null && l.next.elem.a <= c1) {
					l = l.next;
				}
				
				if (l.elem.a == c1) {
					// do nothing
				} else {
					Pair<Integer, Set<CharacterClass>> p = new Pair<Integer, Set<CharacterClass>>(c1, new ShareableHashSet<CharacterClass>());
					p.b.addAll(l.elem.b);
					l = l.insert(p);
				}
				
				// to
				while (l.next != null && l.next.elem.a <= c2) {
					l.elem.b.add(cc);
					l = l.next;
				}
				
				if (l.next == null) {
					l.insert(new Pair<Integer, Set<CharacterClass>>(c2 + 1, new ShareableHashSet<CharacterClass>()));
				} else if (l.next.elem.a == c2 + 1) {
					// do nothing
				} else {
					Pair<Integer, Set<CharacterClass>> p = new Pair<Integer, Set<CharacterClass>>(c2 + 1, new ShareableHashSet<CharacterClass>());
					p.b.addAll(l.elem.b);
					l.insert(p);
				}
				l.elem.b.add(cc);
			}
		}
		return overlaps;
	}

	public int compareTo(SymbolSet other) {
		if (this.size() != other.size()) {
			return this.size() - other.size();
		}
		if (!(other instanceof CharacterClass)) {
			return 1;
		}
		CharacterClass that = (CharacterClass) other;
		for (int i = 0; i < size; i++) {
			if (this.ranges[i] != that.ranges[i]) {
				return this.ranges[i] - that.ranges[i];
			}
		}
		return 0;
	}

	public Character getOne() {
		if (size > 0) {
			return (Character) Symbol.getSymbol(-ranges[0]);
		} else {
			return null;
		}
	}

	public CharacterClass subtract(CharacterClass cc) {
		// TODO write proper function!!
		
		CharacterClass result = new CharacterClass();
		result.add(this);
		
		for (Symbol c : cc) {
			result.remove(c);
		}
		
		return result;
	}
	
	public CharacterClass invert() {
		// TODO write proper function!!
		CharacterClass all = new CharacterClass();
		all.add(0, 65536);
		return all.subtract(this);
	}
}