package nl.cwi.sen1.AmbiDexter.automata;

import java.util.Set;
import java.util.Map.Entry;

import nl.cwi.sen1.AmbiDexter.automata.NFA.Item;
import nl.cwi.sen1.AmbiDexter.automata.NFA.Transition;
import nl.cwi.sen1.AmbiDexter.util.FragmentStack;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashMap;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;

public class NFASCC {
	
	private ShareableHashMap<Item, Set<Item>> scc = new ShareableHashMap<Item, Set<Item>>();
	private ShareableHashMap<Item, ItemInfo> info = new ShareableHashMap<Item, ItemInfo>();
	private FragmentStack<Item> s = new FragmentStack<Item>();
	private int index = 0;
	private Set<Item> ends = new ShareableHashSet<Item>(); // states that are initially alive
	private boolean visitDerives, visitShifts, visitReduces, collectSCCs;
	
	public ShareableHashMap<Item, Set<Item>> getStronglyConnectedComponents(Item startItem) {
		visitDerives = visitShifts = true;
		visitReduces = false;
		collectSCCs = true;
		visit(startItem);
		return scc;
	}
	
	public Set<Item> getDeadItems(Item startItem, Item endItem) {
		ends.add(endItem);
		visitDerives = visitShifts = visitReduces = true;
		collectSCCs = false;
		
		visit(startItem);
		
		Set<Item> dead = new ShareableHashSet<Item>();
		for (Entry<Item, ItemInfo> e : info) {
			if (!e.getValue().alive) {
				dead.add(e.getKey());
			}
		}
		return dead;
	}
	
	public Set<Item> getItemsFromIncompleteProductions(Set<Item> items) {
		Set<Item> begins = new ShareableHashSet<Item>();
		
		for (Item i : items) {
			if (i.atBegin()) {
				begins.add(i);
			} // no else here! to make items of empty productions alive as well
			if (i.atEnd()) {
				ends.add(i);
			}
		}
		
		visitDerives = visitReduces = false;
		visitShifts = true;
		collectSCCs = false;
		
		for (Item i : begins) {
			visit(i);
		}
		
		Set<Item> dead = new ShareableHashSet<Item>();
		for (Entry<Item, ItemInfo> e : info) {
			if (!e.getValue().alive) {
				dead.add(e.getKey());
			}
		}
		return dead;
	}
	
	/*Wikipedia:
	  
	  function strongconnect(v)
	    // Set the depth index for v to the smallest unused index
	    v.index := index
	    v.lowlink := index
	    index := index + 1
	    S.push(v)
	
	    // Consider successors of v
	    for each (v, w) in E do
	      if (w.index is undefined) then
	        // Successor w has not yet been visited; recurse on it
	        strongconnect(w)
	        v.lowlink := min(v.lowlink, w.lowlink)
	      else if (w is in S) then
	        // Successor w is in stack S and hence in the current SCC
	        v.lowlink := min(v.lowlink, w.index)
	      end if
	    repeat
	
	    // If v is a root node, pop the stack and generate an SCC
	    if (v.lowlink = v.index) then
	      start a new strongly connected component
	      repeat
	        w := S.pop()
	        add w to current strongly connected component
	      until (w = v)
	      output the current strongly connected component
	    end if
	  end function
	*/
	
	private void visit(Item v) {
		ItemInfo vinf = new ItemInfo(index);
		if (ends.contains(v)) {
			vinf.alive = true;
		}
		info.put(v, vinf);
		++index;
		s.add(v);
		
		if (visitDerives) {
			for (Transition t : v.derives) {
				Item w = t.target;
				visitTrans(w, vinf);
			}
		}
		if (visitShifts) {
			if (v.shift != null) {
				Item w = v.shift.target;
				visitTrans(w, vinf);
			}
			if (v.shifts != null) {
				for (Transition t : v.shifts) {
					Item w = t.target;
					visitTrans(w, vinf);
				}
			}
		}
		if (visitReduces) {
			for (Transition t : v.reduces) {
				Item w = t.target;
				visitTrans(w, vinf);
			}
		}
		
		if (vinf.index == vinf.lowlink) {
			Set<Item> vscc = new ShareableHashSet<Item>();
			Item w;
			do {
				w = s.pop();
				ItemInfo winf = info.get(w);
				winf.onStack = false;
				
				if (collectSCCs) {
					vscc.add(w);
					scc.put(w, vscc);
				}
				
				if (vinf.alive) {
					winf.alive = true;
				}
			} while (w != v);
		}
	}

	private void visitTrans(Item w, ItemInfo vinf) {
		ItemInfo winf = info.get(w);
		if (winf == null) {
			visit(w);
			winf = info.get(w);
			if (winf.lowlink < vinf.lowlink) {
				vinf.lowlink = winf.lowlink;
			}
		} else if (winf.onStack) {
			if (winf.lowlink < vinf.lowlink) {
				vinf.lowlink = winf.lowlink;
			}
		}
		if (winf.alive) {
			vinf.alive = true;
		}
	}
	
	private static class ItemInfo {
		int index, lowlink;
		boolean onStack, alive;
		
		public ItemInfo(int index) {
			this.index = lowlink = index;
			onStack = true;
		}
	}	
}
