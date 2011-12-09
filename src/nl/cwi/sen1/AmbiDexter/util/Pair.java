package nl.cwi.sen1.AmbiDexter.util;

public class Pair<A, B> {
	
	public A a;
	public B b;
	
	public Pair(A a, B b) {
		this.a = a;
		this.b = b;
	}
		
	public String toString() {
		String s = "< ";
		s += (a == null ? "_" : a.toString());
		s += " , ";
		s += (b == null ? "_" : b.toString());
		return s + " >";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((a == null) ? 0 : a.hashCode());
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?, ?> other = (Pair<?, ?>) obj;
		if (a == null) {
			if (other.a != null)
				return false;
		} else if (!a.equals(other.a))
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		return true;
	}

	
	/*public boolean equalsAssoc(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Pair))
			return false;
		
		Pair other = (Pair) obj;
		
		if (a != null && a.equals(other.a) && b != null && b.equals(other.b))
			return true;
		
		if (a != null && a.equals(other.b) && b != null && b.equals(other.a))
			return true;

		if (a == null && other.a == null && b != null && b.equals(other.b))
			return true;

		if (a == null && other.b == null && b != null && b.equals(other.a))
			return true;

		if (a != null && a.equals(other.a) && b == null && other.b == null)
			return true;

		if (a != null && a.equals(other.b) && b == null && other.a == null)
			return true;

		if (a == null && other.a == null && b == null && other.b == null)
			return true;

		return false;
	}*/
}
