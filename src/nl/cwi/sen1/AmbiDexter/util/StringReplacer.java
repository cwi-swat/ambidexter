package nl.cwi.sen1.AmbiDexter.util;

public class StringReplacer {

	public static String replaceAll(String s, String search, String replace) {
		int p = s.indexOf(search);
		while (p != -1) {
			s = s.substring(0, p) + replace + s.substring(p + search.length());
			p = s.indexOf(search, p);
		}
		return s;
	}
	
	public static String replaceRegions(String s, String open, String close, String r) {
		int p = s.indexOf(open, 0);
		while (p != -1) {
			int q = s.indexOf(close, p);
			if (q > p) {
				s = s.substring(0, p) + r + s.substring(q + close.length());
				p = s.indexOf(open, p);
			} else {
				throw new RuntimeException("Parse error, unmatched " + open + " and " + close);
			}
		}
		return s;
	}
	
	public static String replaceRegionsNested(String s, String open, String close, String r) {
		int p = s.indexOf(open, 0);
		while (p > 0) {
			int q = match(s, open, close, p);
			s = s.substring(0, p) + r + s.substring(q + close.length());
			p = s.indexOf(open, p);
		}
		return s;
	}

	// hierarchical
	private static int match(String s, String open, String close, int p) {
		int q = s.indexOf(close, p + 1);
		int p2 = s.indexOf(open, p + 1);
		
		while (p2 != -1 && p2 < q) {
			int q2 = match(s, open, close, p2);
			q = s.indexOf(close, q2 + 1);
			p2 = s.indexOf(open, q2 + 1);
		}

		if (q == -1) {
			throw new RuntimeException("Parse error, unmatched " + open + " and " + close);						
		}

		return q;
	}
	
	public static String strip(String s) {
		return replaceAll(replaceAll(replaceAll(s, " ", ""), "\t", ""), "\n", "");
	}
}
