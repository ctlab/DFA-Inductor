package algorithms;

import structures.APTA;

import org.sat4j.minisat.SolverFactory;
import org.sat4j.reader.DimacsReader;
import org.sat4j.reader.ParseFormatException;
import org.sat4j.reader.Reader;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

import java.io.*;
import java.util.Scanner;

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
	private boolean iterativeMode;
	private boolean iterativeSolver;
	private Process process;
	private boolean first;
	Scanner sc;
	BufferedWriter bw;

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile) throws ContradictionException,
			ParseFormatException, IOException {
		init(apta, colors, dimacsFile, null, false, false);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, int timeout) throws ContradictionException,
			ParseFormatException, IOException {
		this.timeout = timeout;
		init(apta, colors, dimacsFile, null, false, false);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, String satSolverFile)
			throws ContradictionException, ParseFormatException, IOException {
		init(apta, colors, dimacsFile, satSolverFile, false, false);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, String satSolverFile, boolean iterativeMode, boolean iterativeSolver)
			throws ContradictionException, ParseFormatException, IOException {
		init(apta, colors, dimacsFile, satSolverFile, iterativeMode, iterativeSolver);
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, int timeout, String satSolverFile)
			throws ContradictionException, IOException {
		init(apta, colors, dimacsFile, satSolverFile, false, false);
		timeoutString = " -t " + timeout + " ";
	}

	public SATSolver(APTA apta, int colors,
	                 String dimacsFile, int timeout, String satSolverFile, boolean iterativeMode, boolean iterativeSolver)
			throws ContradictionException, IOException {
		init(apta, colors, dimacsFile, satSolverFile, iterativeMode, iterativeSolver);
		timeoutString = " -t " + timeout + " ";
	}

	private void init(APTA apta, int colors, String dimacsFile,
	                  String satSolverFile, boolean iterativeMode, boolean iterativeSolver) throws IOException {
		this.apta = apta;
		this.vertices = apta.getSize();
		this.dimacsFile = dimacsFile;
		this.colors = colors;
		this.satSolverFile = satSolverFile;
		this.iterativeMode = iterativeMode;
		this.iterativeSolver = iterativeSolver;
		this.first = true;
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
			if (!(iterativeMode || iterativeSolver)) {
				process = new ProcessBuilder((satSolverFile + timeoutString + dimacsFile).split(" ")).start();
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
			} else {
				if (first) {
					process = new ProcessBuilder((satSolverFile + " " + countVars).split(" ")).start();
					sc = new Scanner(new InputStreamReader(process.getInputStream()));
					bw = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
					try (
							BufferedReader brDimacs = new BufferedReader(new FileReader(dimacsFile))
					) {
						String line;
						while ((line = brDimacs.readLine()) != null) {
							if (line.startsWith("p cnf ")) {
								continue;
							}
							bw.write(line + "\n");
						}
						bw.write("solve " + timeout + "\n");
						bw.flush();
						line = sc.nextLine();
						if (line.equals("SAT")) {
							line = sc.nextLine();
							ansLine = line.substring(2, line.length()) + " ";
							if (iterativeSolver) {
								bw.write("halt\n");
								bw.flush();
							}
						} else if (line.equals("UNKNOWN")) {
							bw.write("halt\n");
							bw.flush();
							throw new TimeoutException();
						} else if (line.equals("UNSAT")) {
							bw.write("halt\n");
							bw.flush();
						}
						first = false;
						return ansLine != null;
					}
				} else {
					try (
							BufferedReader brDimacs = new BufferedReader(new FileReader(dimacsFile))
					) {
						ansLine = null;
						String prevLine = "";
						String line = brDimacs.readLine();
						while (line != null) {
							prevLine = line;
							line = brDimacs.readLine();
						}
						bw.write(prevLine + "\n");
						bw.write("solve " + timeout + "\n");
						bw.flush();
						line = sc.nextLine();
						if (line.equals("SAT")) {
							line = sc.nextLine();
							ansLine = line.substring(2, line.length()) + " ";
						} else if (line.equals("UNKNOWN")) {
							bw.write("halt\n");
							bw.flush();
							throw new TimeoutException();
						} else if (line.equals("UNSAT")) {
							bw.write("halt\n");
							bw.flush();
						}
						return ansLine != null;
					}
				}
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

	public void updateTL(int timeout) {
		this.timeout = timeout;
	}

	public int nVars() {
		return countVars;
	}

	public int nConstraints() {
		return countClauses;
	}

}
