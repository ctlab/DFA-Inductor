import java.io.PrintWriter;

public class Buffer {
	private StringBuilder sb;
	private PrintWriter pw;
	private int countClauses;

	public Buffer(PrintWriter pw) {
		this.pw = pw;
		this.sb = new StringBuilder();
		this.countClauses = 0;
	}

	public void addClause(String s) {
		if (!s.endsWith(" ")) {
			s += " ";
		}
		s += "0\n";
		sb.append(s);
		countClauses++;
		if (needFlush()) {
			flush();
		}
	}

	public void addClause(StringBuilder s) {
		if (!s.substring(s.length() - 2).equals(" ")) {
			s.append(" ");
		}
		s.append("0\n");
		sb.append(s);
		countClauses++;
		if (needFlush()) {
			flush();
		}
	}

	public void addClause(int... literals) {
		for (int literal : literals) {
			sb.append(literal + " ");
		}
		sb.append("0\n");
		countClauses++;
		if (needFlush()) {
			flush();
		}
	}

	public void flush() {
		pw.print(sb);
		sb = new StringBuilder();
	}

	public int nClauses() {
		return countClauses;
	}

	private boolean needFlush() {
		return countClauses % 100000 == 0;
	}
}
