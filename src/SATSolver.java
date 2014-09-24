import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class SATSolver {

	private APTA apta;
	private int colors;
	private int vertices;
	private IProblem problem;
	private String dimacsFile = null;
	private String satSolverFile = null;
	private String timeoutString = " ";
	private String ansLine = null;
	private int countClauses;
	private int countVars;
	private int timeout = 0;

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors,
			String dimacsFile) throws ContradictionException,
			ParseFormatException, IOException {
		init(apta, cg, colors, dimacsFile, null);
	}

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors,
			String dimacsFile, int timeout) throws ContradictionException,
			ParseFormatException, IOException {
	    this.timeout = timeout;
		init(apta, cg, colors, dimacsFile, null);
	}

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors,
			String dimacsFile, String satSolverFile)
			throws ContradictionException, ParseFormatException, IOException {
		init(apta, cg, colors, dimacsFile, satSolverFile);
	}

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors,
			String dimacsFile, int timeout, String satSolverFile)
			throws ContradictionException, IOException {
		init(apta, cg, colors, dimacsFile, satSolverFile);
		timeoutString = " -t " + timeout + " ";
	}

	private void init(APTA apta, ConsistencyGraph cg, int colors,
			String dimacsFile, String satSolverFile) throws IOException {
		this.apta = apta;
		this.vertices = apta.getSize();
		this.dimacsFile = dimacsFile;
		this.colors = colors;
		this.satSolverFile = satSolverFile;
		BufferedReader br = new BufferedReader(new FileReader(dimacsFile));
		String line;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("p cnf ")) {
				String[] tmp = line.split(" ");
				countVars = Integer.parseInt(tmp[2]);
				countClauses = Integer.parseInt(tmp[3]);
				break;
			}
		}
		br.close();
	}

	private IProblem build() throws ContradictionException,
			ParseFormatException, IOException {
		ISolver solver = SolverFactory.newDefault();
		if (timeout > 0) {
			solver.setTimeout(timeout);
		}
		Reader reader = new DimacsReader(solver);
		IProblem problem = reader.parseInstance(dimacsFile);
		return problem;
	}

	public boolean problemIsSatisfiable() throws TimeoutException, IOException, ParseFormatException, ContradictionException {
		if (satSolverFile == null) {
			problem = build();
			return problem.isSatisfiable();
		} else {
			Process process = new ProcessBuilder(
					(satSolverFile + timeoutString + dimacsFile).split(" "))
					.start();
			// Process process = Runtime.getRuntime().exec(
			// satSolverFile + timeoutString + dimacsFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.equals("s SATISFIABLE")) {
					ansLine = "";
				}
				if (line.charAt(0) == 'v') {
					ansLine += line.substring(2, line.length()) + " ";
				}
				if (line.contains("c time limit") && line.contains("reached")) {
					throw new TimeoutException();
				}
			}
			br.close();
			if (ansLine != null) {
				return true;
			} else {
				return false;
			}
		}
	}

	// must be called after problemIsSatisfiable
	public Automaton getModel() {
		int[][] x = new int[vertices][colors];
		int curVar = 1;
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				x[v][i] = curVar++;
			}
		}

		Automaton automaton = new Automaton(colors);
		// map[vertex][color]
		Map<Integer, Integer> colorsOfNodes = new HashMap<>();
		int[] model;
		if (satSolverFile == null) {
			model = problem.model();
		} else {
			String[] strings = ansLine.split(" ");
			model = new int[strings.length];
			for (int i = 0; i < strings.length; i++) {
				model[i] = Integer.parseInt(strings[i]);
			}
		}

		for (int i = 0; i < colors; i++) {
			for (int v = 0; v < vertices; v++) {
				if (model[x[v][i] - 1] > 0) {
					colorsOfNodes.put(v, i);
				}
			}
		}
		for (Entry<Integer, Integer> e : colorsOfNodes.entrySet()) {
			int vertex = e.getKey();
			int color = e.getValue();
			Node vertexNode = apta.getNode(vertex);

			if (vertexNode.isAcceptable()) {
				automaton.getState(color).setStatus(Node.Status.ACCEPTABLE);
			} else if (vertexNode.isRejectable()) {
				automaton.getState(color).setStatus(Node.Status.REJECTABLE);
			}

			for (Entry<String, Node> entry : apta.getNode(vertex).getChildren()
					.entrySet()) {
				String label = entry.getKey();
				int to = entry.getValue().getNumber();
				automaton.addTransition(color, colorsOfNodes.get(to), label);
			}
		}
		return automaton;
	}

	public int nVars() {
		return countVars;
	}

	public int nConstraints() {
		return countClauses;
	}

}
