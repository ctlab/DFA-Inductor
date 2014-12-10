import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
	private boolean problemIsSatisfiableCalled = false;

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile) throws ContradictionException,
			ParseFormatException, IOException {
		init(apta, colors, dimacsFile, null);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, int timeout) throws ContradictionException,
			ParseFormatException, IOException {
		this.timeout = timeout;
		init(apta, colors, dimacsFile, null);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, String satSolverFile)
			throws ContradictionException, ParseFormatException, IOException {
		init(apta, colors, dimacsFile, satSolverFile);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, int timeout, String satSolverFile)
			throws ContradictionException, IOException {
		init(apta, colors, dimacsFile, satSolverFile);
		timeoutString = " -t " + timeout + " ";
	}

	private void init(APTA apta, int colors,
	                  String dimacsFile, String satSolverFile) throws IOException {
		this.apta = apta;
		this.vertices = apta.getSize();
		this.dimacsFile = dimacsFile;
		this.colors = colors;
		this.satSolverFile = satSolverFile;
		try (BufferedReader br = new BufferedReader(new FileReader(dimacsFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("p cnf ")) {
					String[] tmp = line.split(" ");
					countVars = Integer.parseInt(tmp[2]);
					countClauses = Integer.parseInt(tmp[3]);
					break;
				}
			}
		}
	}

	private IProblem build() throws ContradictionException,
			ParseFormatException, IOException {
		ISolver solver = SolverFactory.newDefault();
		if (timeout > 0) {
			solver.setTimeout(timeout);
		}
		Reader reader = new DimacsReader(solver);
		return problem = reader.parseInstance(dimacsFile);
	}

	public boolean problemIsSatisfiable() throws TimeoutException, IOException, ParseFormatException, ContradictionException {
		problemIsSatisfiableCalled = true;
		if (satSolverFile == null) {
			problem = build();
			return problem.isSatisfiable();
		} else {
			Process process = new ProcessBuilder(
					(satSolverFile + timeoutString + dimacsFile).split(" "))
					.start();
			// Process process = Runtime.getRuntime().exec(
			// satSolverFile + timeoutString + dimacsFile);
			try (BufferedReader br = new BufferedReader(new InputStreamReader(
					process.getInputStream()))) {
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
				return ansLine != null;
			}
		}
	}

	// must be called after problemIsSatisfiable
	public int[] getModel() throws Exception {
		if (!problemIsSatisfiableCalled) {
			throw new Exception("You should call problemIsSatisfiable first");
		}
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
		return model;
	}

	public int nVars() {
		return countVars;
	}

	public int nConstraints() {
		return countClauses;
	}

}
