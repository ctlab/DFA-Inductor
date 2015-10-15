package misc;

import java.io.PrintWriter;

public class Buffer {
	private PrintWriter pw;
	private int countClauses;

	public Buffer(PrintWriter pw) {
		this.pw = pw;
		this.countClauses = 0;
	}

	public void addClause(String s) {
		if (!s.endsWith(" ")) {
			s += " ";
		}
		s += "0";
		pw.println(s);
		countClauses++;
	}

	public void addClause(StringBuilder s) {
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
		sb.append("0");
		pw.println(sb);
		countClauses++;
	}

	public int nClauses() {
		return countClauses;
	}
}
