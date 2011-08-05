package nl.cwi.sen1.AmbiDexter.parse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nl.cwi.sen1.AmbiDexter.grammar.Character;
import nl.cwi.sen1.AmbiDexter.grammar.CharacterClass;
import nl.cwi.sen1.AmbiDexter.grammar.Production;
import nl.cwi.sen1.AmbiDexter.grammar.Symbol;
import nl.cwi.sen1.AmbiDexter.grammar.SymbolString;
import nl.cwi.sen1.AmbiDexter.util.Pair;
import nl.cwi.sen1.AmbiDexter.util.Queue;
import nl.cwi.sen1.AmbiDexter.util.ShareableHashSet;
import nl.cwi.sen1.AmbiDexter.util.Util;

public class ParseTree {
	
	public ParseTreeNode top;
	public int nrAmbiguities;
	
	public ParseTree(ParseTreeNode top, int nrAmbiguities) {
		this.top = top;
		this.nrAmbiguities = nrAmbiguities;
	}	
	
	public Pair<Symbol, SymbolString> getAmbiguousCore() {
		AmbNode amb = getMinimalParseTree();
		
		if (amb == null) {
			String s = "No ambiguity node found for " + top.getRootSymbol() + " : " + top.yield().prettyPrint();
			System.out.println(s);
			//throw new RuntimeException(s);
			return new Pair<Symbol, SymbolString>(top.getRootSymbol(), top.yield());
		}

		amb.removeNestedAmbiguities();
		amb.liftToCharacterClasses();
		amb.addPosInfo(0);
		
		// remove identical nodes from bottom of all alternatives
		boolean changed;
		do {
			changed = false;
			Queue<List<ParseTreeNode>> q = new Queue<List<ParseTreeNode>>();
			for (int i = 0; i < amb.children.size(); ++i) {
				List<ParseTreeNode> y = new ArrayList<ParseTreeNode>();
				amb.children.get(i).getBottom(y);
				q.add(y);
			}
			
 			Set<ParseTreeNode> remove = new ShareableHashSet<ParseTreeNode>();
 			List<ParseTreeNode> y = q.get(0);
 			for (ParseTreeNode n : y) {
 				Set<ParseTreeNode> equalNodes = new ShareableHashSet<ParseTreeNode>();
 				for (int i = 1; i < q.size(); ++i) {
 					for (ParseTreeNode n2 : q.get(i)) {
 						if (n.equalTo(n2) && !remove.contains(n2)) {
 							equalNodes.add(n2);
 							break;
 						}
 					}
 				}
 				if (equalNodes.size() == q.size() - 1) {
 					remove.addAll(equalNodes);
 					remove.add(n);
 				}
 			}
 			
			amb.removeNodes(remove);
			changed = remove.size() > 0;
		} while (changed);
		
		amb.reconstructChildren();
		
		return new Pair<Symbol, SymbolString>(amb.getRootSymbol(), amb.yield());
	}

	public AmbNode getMinimalParseTree() {
		// breadth first search for first ambiguity node
		List<ParseTreeNode> l = new ArrayList<ParseTreeNode>();
		l.add(top);
		AmbNode amb = null;
		do {
			for (int i = 0; i < l.size(); i++) {
				if (l.get(i) instanceof AmbNode) {
					amb = (AmbNode) l.get(i);
					break;
				}
			}
			
			List<ParseTreeNode> l2 = new ArrayList<ParseTreeNode>();
			for (int i = 0; i < l.size(); i++) {
				l2.addAll(l.get(i).children);
			}
			
			l = l2;
		} while (amb == null && l.size() > 0);
		return amb;
	}

	public static abstract class ParseTreeNode {
		protected List<ParseTreeNode> children = new LinkedList<ParseTreeNode>();
		private static int ids = 0;
		protected int id = ids++;
		int from, to; // to is exclusive (so from == to means empty)
		
		public int getNumChildren() {
			return children.size();
		}
		
		public ParseTreeNode getChild(int i) {
			return children.get(i);
		}
		
		// prepend!
		public void addChild(ParseTreeNode n) {
			children.add(0, n);
		}
		
		public String toDot() {
			return "digraph G {\n" + dotString() + "}";
		}
		
		protected String dotString() {
			String s = "";
			for (ParseTreeNode c : children) {
				s += "" + id + " -> " + c.id + "\n";
				s += c.dotString();
			}
			return s;
		}
		
		public String prettyPrint() {
			return prettyPrint("\n", "  ");
		}
		
		public String prettyPrint(String indent, String line) {
			String s = indent + "+-" + printNode();
			indent += line;
			for (int i = 0; i < children.size(); i++) {
				final ParseTreeNode c = children.get(i);
				if (c == null) {
					s += indent + "+- null";
				} else {
					if (i == children.size() - 1) {
						s += c.prettyPrint(indent, "  ");
					} else {
						s += c.prettyPrint(indent, "| ");
					}
				}
			}
			return s;
		}
		
		public void liftToCharacterClasses() {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				c.liftToCharacterClasses();
			}
		}

		public void getBottom(List<ParseTreeNode> l) {
			if (children.size() == 0) {
				l.add(this);
			} else {
				boolean allNull = true;
				for (int i = 0; i < children.size(); ++i) {
					ParseTreeNode c = children.get(i);
					if (c != null) {
						allNull = false;
						c.getBottom(l);
					}
				}
				if (allNull) {
					l.add(this);
				}
			}
		}

		public void removeNodes(Set<ParseTreeNode> remove) {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				if (c != null){ 
					if (remove.contains(c)) {
						children.set(i, null);
					} else {
						c.removeNodes(remove);
					}
				}
			}
		}

		public void removeNestedAmbiguities() {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				if (c instanceof AmbNode) {
					c = c.getChild(0);
					children.set(i, c);
				}
				c.removeNestedAmbiguities();
			}
		}

		public boolean equalTo(ParseTreeNode n) {
			return from == n.from && to == n.to;
		}

		protected abstract String printNode();
		public abstract SymbolString yield();
		public abstract boolean yieldsEmpty();
		public abstract Symbol getRootSymbol();
		public abstract int addPosInfo(int start);
		public abstract void reconstructChildren();
	}

	static class ProdNode extends ParseTreeNode {
		Production prod;
		
		public ProdNode(Production prod) {
			this.prod = prod;
		}
		
		public String toString() {
			return "prod(" + prod + "," + children + ")";
		}
		
		protected String dotString() {
			return "" + id + " [label=" + Util.dotId(prod) + "]\n"
				+ super.dotString();
		}

		@Override
		protected String printNode() {
			return prod.toString();
		}
		
		@Override
		public SymbolString yield() {
			SymbolString s = new SymbolString();
			for (ParseTreeNode n : children) {
				s.addAll(n.yield());
			}
			return s;
		}
		
		@Override
		public boolean yieldsEmpty() {
			for (ParseTreeNode n : children) {
				if (!n.yieldsEmpty()) {
					return false;
				}
			}
			return true;
		}
		
		@Override
		public void liftToCharacterClasses() {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				if (c instanceof LeafNode) {
					LeafNode l = (LeafNode) c;
					if (l.s instanceof Character || 
					   (l.s instanceof CharacterClass && !l.s.equals(prod.getSymbolAt(i)))) {
						children.set(i, new LeafNode(prod.getSymbolAt(i)));
					}
				} else {
					c.liftToCharacterClasses();	
				} 
			}
		}

		@Override
		public Symbol getRootSymbol() {
			return prod.lhs;
		}

		@Override
		public int addPosInfo(int start) {
			if (children.size() == 0) {
				from = to = start;
			} else {
				from = start;
				for (int i = 0; i < children.size(); ++i) {
					ParseTreeNode c = children.get(i);
					start = c.addPosInfo(start);
				}
				to = start;
			}
			
			return start;
		}

		@Override
		public boolean equalTo(ParseTreeNode n) {
			if (super.equalTo(n) && n instanceof ProdNode) {
				return ((ProdNode) n).prod.equals(prod);
			}
			return false;
		}

		@Override
		public void reconstructChildren() {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				if (c == null) {
					children.set(i, new LeafNode(prod.getSymbolAt(i)));
				} else {
					c.reconstructChildren();
				}
			}
		}
	}

	static class LeafNode extends ParseTreeNode {
		Symbol s;
		
		public LeafNode(Symbol s) {
			this.s = s;
		}
		
		public void addChild(ParseTreeNode n) {
		}
		
		public String toString() {
			return s.toString();
		}
		
		protected String dotString() {
			return "" + id + " [label=" + Util.dotId(s) + "]\n";
		}

		@Override
		protected String printNode() {
			return s.prettyPrint();
		}

		@Override
		public SymbolString yield() {
			SymbolString ss = new SymbolString(1);
			ss.add(s);
			return ss;
		}

		@Override
		public boolean yieldsEmpty() {
			return false;
		}

		@Override
		public Symbol getRootSymbol() {
			return s;
		}

		@Override
		public int addPosInfo(int start) {
			from = start;
			to = ++start;
			return start;
		}

		@Override
		public boolean equalTo(ParseTreeNode n) {
			if (super.equalTo(n) && n instanceof LeafNode) {
				return ((LeafNode) n).s.equals(s);
			}
			return false;
		}

		@Override
		public void reconstructChildren() { }
	}
	
	static class AmbNode extends ParseTreeNode {
		boolean horizontal = false;
		
		public AmbNode() {}		

		public String toString() {
			return "amb(" + children + ")";
		}
		
		protected String dotString() {
			return "" + id + " [label=\"amb\", shape=diamond]\n"
				+ super.dotString();
		}

		@Override
		protected String printNode() {
			return "AMBIGUITY";
		}
		
		@Override
		public SymbolString yield() {
			SymbolString s = null;
			for (ParseTreeNode c : children) {
				if (s == null) {
					s = c.yield();
				} else {
					SymbolString y = c.yield();
					if (s.containsOnlyCharClasses() && y.containsOnlyCharClasses()) {
						s.intersect(y);
					}
				}
			}
			return s;
		}
		
		@Override
		public boolean yieldsEmpty() {
			return children.get(0).yieldsEmpty();
		}

		@Override
		public Symbol getRootSymbol() {
			return children.get(0).getRootSymbol();
		}

		@Override
		public int addPosInfo(int start) {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				c.addPosInfo(start);
			}
			from = start;
			to = children.get(0).to;
			return to;
		}

		@Override
		public boolean equalTo(ParseTreeNode n) {
			throw new RuntimeException("Should not be called");
		}

		@Override
		public void reconstructChildren() {
			for (int i = 0; i < children.size(); ++i) {
				ParseTreeNode c = children.get(i);
				c.reconstructChildren();
			}
		}
	}
}
