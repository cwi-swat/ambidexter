package nl.cwi.sen1.AmbiDexter.grammar;

import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.util.LinkedList;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Relation;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;


public class FollowRestrictions {

	CharacterClass fr1 = new CharacterClass();
	ShareableHashSet<LinkedList<CharacterClass>> frLonger = new ShareableHashSet<LinkedList<CharacterClass>>();
	
	// if this set of follow restricions used to simulate the Rascal 'follow' condition,
	// then this field contains the maximum length of all strings that must follow.
	public int mustFollowLength = 0;
	
	public FollowRestrictions() {
	}
	
	public FollowRestrictions(FollowRestrictions fr) { // copy constructor
		fr1.add(fr.fr1);
		frLonger.addAll(fr.frLonger);
		mustFollowLength = fr.mustFollowLength;
	}
	
	/**
	 * Returns whether this object represents a restriction or an 'obligation'
	 */
	public boolean mustFollow() {
		return mustFollowLength > 0;
	}
	
	// pre: s is not a NonTerminal
	public boolean canShift(Symbol s) {
		if (s instanceof CharacterClass) {
			return ((CharacterClass) s).differenceNonEmpty(fr1);
		} else {
			return !fr1.contains(s);
		}
	}

	public FollowRestrictions getNextAfterReduce(NonTerminal n) {
		if (n.followRestrictions == null) {
			return this;
		}
		FollowRestrictions next = new FollowRestrictions(this);
		next.add(n.followRestrictions);
		return next;
	}
	
	public FollowRestrictions getNextAfterShift(Symbol s) {
		FollowRestrictions next = new FollowRestrictions();
		for (LinkedList<CharacterClass> l : frLonger) {
			if (s instanceof CharacterClass) {
				if (((CharacterClass) s).differenceNonEmpty(l.elem)) {
					continue;
				}
			} else { // s is a Character
				if (!l.elem.contains(s)) {
					continue;
				}
			}
			
			next.add(l.next);
		}
		if (mustFollowLength > 0) {
			next.mustFollowLength = mustFollowLength - 1;
		}
		return next.isEmpty() ? null : next;
	}
	
	// pre: canShift(s) == true
	public Pair<CharacterClass, ShareableHashMap<CharacterClass, FollowRestrictions>> getShiftPossibilities(Symbol s) {
		if (!(s instanceof CharacterClass)) {
			throw new RuntimeException("not implemented yet");
		}
		
		// first subtract fr1 from shiftable character class
		CharacterClass sc = (CharacterClass) s;
		sc = sc.subtract(fr1);
		
		// because of precondition, sc is not empty

		// get overlaps of sc and the first character classes of all longer restrictions  
		Set<CharacterClass> ccs = new ShareableHashSet<CharacterClass>();
		ccs.add(sc);
		for (LinkedList<CharacterClass> l : frLonger) {
			ccs.add(l.elem);
		}
		
		LinkedList<Pair<Integer, Set<CharacterClass>>> overlaps = CharacterClass.getOverlaps(ccs);
		Relation<CharacterClass, LinkedList<CharacterClass>> r = new Relation<CharacterClass, LinkedList<CharacterClass>>();
		CharacterClass nsc = new CharacterClass();
		
		LinkedList<Pair<Integer, Set<CharacterClass>>> l = overlaps;
		while (l.next != null) {
			CharacterClass n = new CharacterClass();
			n.add(l.elem.a, l.next.elem.a - 1);
			
			boolean containsSC = false;
			for (CharacterClass c : l.elem.b) {
				if (c.equals(sc)) {
					containsSC = true;
					break;
				}
			}
			if (containsSC) {
				boolean onlySC = true;
				for (CharacterClass c : l.elem.b) {
					for (LinkedList<CharacterClass> fr : frLonger) {
						if (fr.elem.equals(c)) {
							r.add(n, fr.next);
							onlySC = false;
						}						
					}
				}
				if (onlySC) {
					nsc.add(n);
				}
			}
			
			l = l.next;
		}
		
		// combine all linked lists into FollowRestrictions objects
		Relation<CharacterClass, FollowRestrictions> fr = new Relation<CharacterClass, FollowRestrictions>();
		for (Entry<CharacterClass, Set<LinkedList<CharacterClass>>> e : r.m) {
			FollowRestrictions f = new FollowRestrictions();
			for (LinkedList<CharacterClass> fl : e.getValue()) {
				f.add(fl);
			}
			fr.add(e.getKey(), f);
		}
		
		// reverse r to minimize range-restriction pairs
		Relation<FollowRestrictions, CharacterClass> frr = fr.reverse();

		// build final relation from char ranges to restrictions
		ShareableHashMap<CharacterClass, FollowRestrictions> result = new ShareableHashMap<CharacterClass, FollowRestrictions>();
		
		// combine all ranges with the same follow restrictions into one class
		for (Entry<FollowRestrictions, Set<CharacterClass>> e : frr.m) {
			CharacterClass c = new CharacterClass();
			for (CharacterClass ec : e.getValue()) {
				c.add(ec);
			}
			
			result.put(c, e.getKey());
		}			
		
		return new Pair<CharacterClass, ShareableHashMap<CharacterClass,FollowRestrictions>>(nsc, result);	
	}
	
	public CharacterClass getNextCharClassAfterShift(CharacterClass cc) {
		if (fr1 == null) {
			return cc;
		} else {
			return cc.subtract(fr1);
		}
	}

	public void add(LinkedList<CharacterClass> l) {
		if (l.next == null) {
			fr1.add(l.elem);
		} else {
			frLonger.add(l);
		}
	}
	
	public void add(FollowRestrictions fr) {
		fr1.add(fr.fr1);
		frLonger.addAll(fr.frLonger);
		
		mustFollowLength = Math.max(mustFollowLength, fr.mustFollowLength);
	}
	
	public boolean isEmpty() {
		return fr1.size() == 0 && frLonger.size() == 0;
	}

	@Override
	public String toString() {
		String s = (fr1.size() > 0 ? fr1.toString() : null);
		for (LinkedList<CharacterClass> l : frLonger) {
			if (s == null) {
				s = l.toString();
			} else {
				s += " | " + l.toString();
			}
		}
		return s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fr1.hashCode();
		result = prime * result	+ frLonger.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof FollowRestrictions))
			return false;
		FollowRestrictions other = (FollowRestrictions) obj;
		if (!fr1.equals(other.fr1))
			return false;
		if (!frLonger.equals(other.frLonger))
			return false;
		return mustFollowLength == other.mustFollowLength;
	}

	public int size() {
		return (fr1.size() == 0 ? 0 : 1) + frLonger.size();
	}
	
	public int length() {
		if (frLonger.size() > 0) {
			int max = 0;
			for (LinkedList<CharacterClass> l : frLonger) {
				int len = l.size();
				if (len > max) {
					max = len;
				}
			}
			return max;
		} else {
			return fr1.size() > 0 ? 1 : 0;
		}
	}

	// returns true if ss is not restricted by this
	public boolean check(SymbolString ss) {
		if (ss.size() == 0) {
			return true;
		}
		Symbol s = ss.get(0);
		if (s instanceof CharacterClass) {
			if (!((CharacterClass) s).differenceNonEmpty(fr1)) {
				return false;
			}
		} else {
			if (fr1.contains(s)) {
				return false;
			}
		}		
		
		for (LinkedList<CharacterClass> l : frLonger) {
			if (l.size() <= ss.size()) {
				int i = 0;
				while (l != null) {
					s = ss.get(i++);
					boolean completeMatch = false;
					if (s instanceof CharacterClass) {
						if (!((CharacterClass) s).differenceNonEmpty(fr1)) {
							completeMatch = true;
						}
					} else {
						if (fr1.contains(s)) {
							completeMatch = true;
						}
					}
					if (completeMatch) {
						if (l.next == null) {
							return false;
						} // else advance
					} else {
						break;
					}
					l = l.next;
				}
			}
		}
		
		return true;
	}

	public void intersect(FollowRestrictions f) {
		fr1 = fr1.intersect(f.fr1);
		
		ShareableHashSet<LinkedList<CharacterClass>> newLonger = new ShareableHashSet<LinkedList<CharacterClass>>();;
		
		for (LinkedList<CharacterClass> l1 : frLonger) {
			for (LinkedList<CharacterClass> l2 : f.frLonger) {
				if (l1.size() == l2.size()){
					// intersect
					LinkedList<CharacterClass> l3 = null;
					while (l1 != null) {
						CharacterClass i = l1.elem.intersect(l2.elem);
						if (i.isEmpty()) {
							l3 = null;
							break;
						}
						
						if (l3 == null) {
							l3 = new LinkedList<CharacterClass>(i);
						} else {
							l3.append(i);
						}
						l1 = l1.next;
						l2 = l2.next;
					}
					
					if (l3 != null) {
						newLonger.add(l3);
					}
				}
			}
		}
		
		frLonger = newLonger;
	}
}
