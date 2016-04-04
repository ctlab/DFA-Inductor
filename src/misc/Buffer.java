package misc;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class Buffer {
	private PrintWriter pw;
	private int countClauses;
	private Set<Integer> trueVars;
	private Set<Integer> falseVars;

	public Buffer(PrintWriter pw) {
		this.pw = pw;
		this.countClauses = 0;
		this.trueVars = new HashSet<>();
		this.falseVars = new HashSet<>();
	}

	public void addClause(String s) {
		s = simplify(s);
		if (s.isEmpty() || isConstant(s)) {
			return;
		}
		if (!s.endsWith(" ")) {
			s += " ";
		}
		s += "0";
		pw.println(s);
		countClauses++;
	}

	public void addClause(StringBuilder s) {
		s = new StringBuilder(simplify(s));
		if (s.length() == 0 || isConstant(s)) {
			return;
		}
		if (!s.substring(s.length() - 2).equals(" ")) {
			s.append(" ");
		}
		s.append("0");
		pw.println(s);
		countClauses++;
	}

	public void addClause(int... literals) {
		StringBuilder sb = new StringBuilder();
		for (int literal : literals) {
			sb.append(literal).append(" ");
		}
		sb = new StringBuilder(simplify(sb));
		if (sb.length() == 0 || isConstant(sb)) {
			return;
		}
		sb.append("0");
		pw.println(sb);
		countClauses++;
	}

	public int nClauses() {
		return countClauses;
	}


	private <T extends CharSequence> String simplify(T s) {
		int i;
		String ans = "";
		for (String var: s.toString().split(" ")) {
			i = Integer.valueOf(var);
			if (trueVars.contains(i)) {
				if (i > 0) {
					return "";
				} else {
					continue;
				}
			}
			if (falseVars.contains(i)) {
				if (i < 0) {
					return "";
				} else {
					continue;
				}
			}
			ans += var + " ";
		}
		return ans;
	}

	private <T extends CharSequence> boolean isConstant(T s) {
		String str = s.toString();
		if (str.split(" ").length == 1) {
			int c = Integer.valueOf(str.trim());
			if (c > 0) {
				if (!trueVars.contains(c)) {
					trueVars.add(c);
					return false;
				}
			} else {
				if (!falseVars.contains(c)) {
					falseVars.add(c);
					return false;
				}
			}
			return true;
		}
		return false;
	}
}
