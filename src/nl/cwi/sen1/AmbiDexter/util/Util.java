package nl.cwi.sen1.AmbiDexter.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.Map.Entry;


public abstract class Util {
	
	public static void writeTextFile(String filename, String contents) {
		BufferedWriter out = null;
		try {
			FileWriter fstream = new FileWriter(filename);
			out = new BufferedWriter(fstream);
			out.write(contents);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) { }
			}
		}
	}

	public static String readTextFile(String filename) {
		String s = "";
		BufferedReader in = null;
		
		try {
			FileReader fstream = new FileReader(filename);
			in = new BufferedReader(fstream);
			while (in.ready()) {
				s += in.readLine() + "\n";
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) { }
			}
		}
		
		return s;
	}
	
	public static String[] readTextFileIntoArray(String filename) {
		String s[];
		int size = 0;;
		BufferedReader in = null;
		
		try {
			FileReader fstream = new FileReader(filename);
			in = new BufferedReader(fstream);
			in.mark(0);
			while (in.ready()) {
				in.readLine();
				size++;
			}
			
			s = new String[size];
			fstream = new FileReader(filename);
			in = new BufferedReader(fstream);
			int i = 0;
			while (in.ready()) {
				s[i++] = in.readLine();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) { }
			}
		}
		
		return s;
	}

	public static int hash32shift(int key)
	{
		// Thomas Wang
		// http://www.concentric.net/~Ttwang/tech/inthash.htm
		
		key = ~key + (key << 15); // key = (key << 15) - key - 1;
		key = key ^ (key >>> 12);
		key = key + (key << 2);
		key = key ^ (key >>> 4);
		key = key * 2057; // key = (key + (key << 3)) + (key << 11);
		key = key ^ (key >>> 16);
		return key;
	}

	public static <T> ShareableHashMap<T, Set<T>> stronglyConnectedComponents2(final ShareableHashMap<T, Set<T>> r) {
		class ItemInfo {
			int index, lowlink;
			boolean onStack;
			
			public ItemInfo(int index) {
				this.index = lowlink = index;
				onStack = true;
			}
		}
	
		final ShareableHashMap<T, ItemInfo> info = new ShareableHashMap<T, ItemInfo>();
		final ShareableHashMap<T, Set<T>> scc = new ShareableHashMap<T, Set<T>>();
		final FragmentStack<T> s = new FragmentStack<T>();
	
		class Visitor {
			int index = 0;
			
			private void visit(T v) {
				ItemInfo vinf = new ItemInfo(index);
				info.put(v, vinf);
				++index;
				s.add(v);
				
				Set<T> ts = r.get(v);
				if (ts != null) {
					for (T w : ts) {
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
					}
				}
				
				if (vinf.index == vinf.lowlink) {
					Set<T> vscc = new ShareableHashSet<T>();
					T w;					
					do {
						w = s.pop();
						ItemInfo winf = info.get(w);
						winf.onStack = false;
						
						vscc.add(w);
						scc.put(w, vscc);
					} while (w != v);
				}
			}
		}
		
		Visitor v = new Visitor();
		for (Entry<T, Set<T>> e : r) {
			T t = e.getKey();
			if (!info.contains(t)) {
				v.visit(t);
			}
		}
		
		return scc;
	}

	public static <T> ShareableHashMap<Set<T>, Set<Set<T>>> lift(ShareableHashMap<T, Set<T>> s, ShareableHashMap<T, Set<T>> member) {
		
		ShareableHashMap<Set<T>, Set<Set<T>>> l = new ShareableHashMap<Set<T>, Set<Set<T>>>();
		
		for (Entry<T, Set<T>> e : s) {
			T k = e.getKey();
			Set<T> mk = member.get(k);
			
			Set<Set<T>> lk = l.get(mk);
			if (lk == null) {
				lk = new ShareableHashSet<Set<T>>();
				l.put(mk, lk);
			}
			
			for (T v : e.getValue()) {
				if (v != k) {
					Set<T> mv = member.get(v);
					lk.add(mv);
				}
			}
		}
		
		return l;
	}
	
	public static <T> ShareableHashMap<T, Set<T>> transitiveClosure2(final ShareableHashMap<T, Set<T>> r) {
		
		class ItemInfo {
			int index, lowlink;
			boolean onStack;
			
			public ItemInfo(int index) {
				this.index = lowlink = index;
				onStack = true;
			}
		}

		final ShareableHashMap<T, ItemInfo> info = new ShareableHashMap<T, ItemInfo>();
		final ShareableHashMap<T, Set<T>> tc = new ShareableHashMap<T, Set<T>>();
		final FragmentStack<T> s = new FragmentStack<T>();

		class Visitor {
			int index = 0;
			
			private void visit(T v) {
				ItemInfo vinf = new ItemInfo(index);
				info.put(v, vinf);
				++index;
				s.add(v);
				
				Set<T> vtc = new ShareableHashSet<T>();
				tc.put(v, vtc);
				
				Set<T> ts = r.get(v);
				if (ts != null) {
					for (T w : ts) {
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
						Set<T> wtc = tc.get(w);
						if (wtc != null && wtc != vtc) {
							vtc.addAll(wtc);
						}
						vtc.add(w);
					}
				}
				
				if (vinf.index == vinf.lowlink) {
					T w;
					do {
						w = s.pop();
						ItemInfo winf = info.get(w);
						winf.onStack = false;
						
						if (w != v) {
							vtc.add(w);						
							vtc.addAll(tc.get(w));						
							tc.put(w, vtc);
						}
					} while (w != v);
				}
			}
		}
		
		Visitor v = new Visitor();
		for (Entry<T, Set<T>> e : r) {
			T t = e.getKey();
			if (!info.contains(t)) {
				v.visit(t);
			}
		}
		
		return tc;
	}
	
	public static String dotId(Object o) {
		return "\"" + o.toString().replace("\\", "|").replace("\"", "\\\"") + "\"";
	}
}
