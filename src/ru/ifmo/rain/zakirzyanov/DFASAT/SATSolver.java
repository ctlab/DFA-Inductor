package ru.ifmo.rain.zakirzyanov.DFASAT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sat4j.core.VecInt;
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
	private ConsistencyGraph cg;
	private int colors;
	private int vertices;
	private int maxVar;
	// x[i][j] - i-th vertex has color j
	private int[][] x;
	// y[i][j][k] - from vertex with color i by symbol k to vertex with color j
	private Map<String, int[][]> y;
	// z[i] - vertex with color i is Acceptable
	private int[] z;
	private IProblem problem;
	private String DimacsFile = "dimacsFile.cnf";
	private String tmpFile = "tmp";
	private String satSolverFile = null;
	private String ansLine = null;
	private PrintWriter pwDF;
	private int countClauses;

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors)
			throws ContradictionException, ParseFormatException, IOException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.x = new int[vertices][colors];
		this.y = new HashMap<String, int[][]>();
		this.z = new int[colors];
		this.countClauses = 0;
		this.pwDF = new PrintWriter(this.DimacsFile);
		initXYZ();
		generateFile();
		problem = build();
	}

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors,
			String satSolverFile) throws ContradictionException,
			ParseFormatException, IOException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.x = new int[vertices][colors];
		this.y = new HashMap<String, int[][]>();
		this.z = new int[colors];
		this.pwDF = new PrintWriter(this.DimacsFile);
		this.satSolverFile = satSolverFile;
		initXYZ();
		generateFile();
	}

		public int[][] getX() {
		return x;
	}

	public Map<String, int[][]> getY() {
		return y;
	}

	public int[] getZ() {
		return z;
	}

	public int nVars() {
		return maxVar - 1;
	}

	public int nConstraints() {
		return countClauses;
	}

	public boolean problemIsSatisfiable() throws TimeoutException, IOException {
		if (satSolverFile == null) {
			return problem.isSatisfiable();
		} else {
			//Process process = new ProcessBuilder(satSolverFile + " " + DimacsFile).start();
			Process process = Runtime.getRuntime().exec(satSolverFile + " " + DimacsFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while((line = br.readLine()) != null) {
				if (line.equals("s SATISFIABLE")) {
					ansLine = "";
				}
				if (line.charAt(0) == 'v') {
					ansLine += line.substring(2, line.length()) + " ";
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
	public Automat getModel() {
		Automat automat = new Automat(colors);
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
				automat.getState(color).setStatus(Node.ACCEPTABLE);
			} else if (vertexNode.isRejectable()) {
				automat.getState(color).setStatus(Node.REJECTABLE);
			}

			for (Entry<String, Node> entry : apta.getNode(vertex).getChildren()
					.entrySet()) {
				String label = entry.getKey();
				int to = entry.getValue().getNumber();
				automat.addTransition(color, colorsOfNodes.get(to), label);
			}
		}
		return automat;
	}
	
	private void initXYZ() {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				x[v][i] = maxVar++;
			}
		}
		for (int i = 0; i < colors; i++) {
			z[i] = maxVar++;
		}
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						if (!y.containsKey(e.getKey())) {
							y.put(e.getKey(), new int[colors][colors]);
						}
						y.get(e.getKey())[i][j] = maxVar++;
					}
				}
			}
		}
	}

	private void generateFile() throws IOException {
		
		File tmp = new File(tmpFile);
		PrintWriter tmpPW = new PrintWriter(tmp);

		printOneAtLeast(tmpPW);
		printAccVertDiffColorRej(tmpPW);
		printParrentRelationIsSet(tmpPW);
		printParrentRelationAtMostOneColor(tmpPW);
		printOneAtMost(tmpPW);
		printParrentRelationAtLeastOneColor(tmpPW);
		printParrentRelationForces(tmpPW);
		printConflictsFromCG(tmpPW);
		tmpPW.close();

		pwDF.print("p cnf " + (maxVar - 1) + " " + countClauses + "\n");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tmp)));
 		
		String aLine = null;
		while ((aLine = in.readLine()) != null) {
			pwDF.print(aLine + "\n");
		}
 
		in.close();
 		pwDF.close();
 		
 		tmp.delete();
	}

	private IProblem build() throws ContradictionException, ParseFormatException, IOException {
		ISolver solver = SolverFactory.newDefault();
		Reader reader = new DimacsReader(solver);
		IProblem problem = reader.parseInstance(DimacsFile);
		return problem;
	}
	
	
	// Each vertex has at least one color.
	// x_{v,1} or x_{v,2} or ... or x_{v, |C|}
	private void printOneAtLeast(PrintWriter tmpPW) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				tmpPW.print(x[v][i] + " ");
			}
			tmpPW.print("0\n");
			countClauses++;
		}

		// root has 0 color
		tmpPW.print(x[0][0] + " 0\n");
		countClauses++;
	}

	// Accepting vertices cannot have the same color as rejecting vertices
	// (!x_{v,i} or z_i) and (!x_{w,i} or !z_i), where v is acc, w is rej
	private void printAccVertDiffColorRej(PrintWriter tmpPW) {
		for (int i = 0; i < colors; i++) {
			for (Integer acc : apta.getAcceptableNodes()) {
				tmpPW.print(-x[acc][i] + " " + z[i] + " 0\n");
				countClauses++;
			}
			for (Integer rej : apta.getRejectableNodes()) {
				tmpPW.print(-x[rej][i] + " " + -z[i] + " 0\n");
				countClauses++;
			}
		}
	}

	// A parent relation is set when a vertex and its parent are colored
	// (y_{l(v),i,j} or !x_{p(v),i} or !x_{v,i})
	private void printParrentRelationIsSet(PrintWriter tmpPW) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						tmpPW.print(y.get(e.getKey())[i][j] + " "
								+ -x[e.getValue().getNumber()][i] + " "
								+ -x[v][j] + " 0\n");
						countClauses++;
					}
				}
			}
		}
	}

	// each parent relation can target at most one color
	// (!y_{a,i,h} or !y_{a,i,j}) where a in Alphabet, h < j
	private void printParrentRelationAtMostOneColor(PrintWriter tmpPW) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					for (int h = 0; h < j; h++) {
						tmpPW.print(-y.get(st)[i][h] + " " + -y.get(st)[i][j]
								+ " 0\n");
						countClauses++;
					}
				}
			}
		}
	}

	// each vertex has at most one color
	// (!x_{v,i} or !x_{v,j}) where i < j
	private void printOneAtMost(PrintWriter tmpPW) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					tmpPW.print(-x[v][i] + " " + -x[v][j] + " 0\n");
					countClauses++;
				}
			}
		}
	}

	// each parent relation must target at least one color
	// (!y_{a,i,h} or !y_{a,i,j}) where a in Alphabet, h < j
	private void printParrentRelationAtLeastOneColor(PrintWriter tmpPW) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				VecInt vecInt = new VecInt();
				for (int j = 0; j < colors; j++) {
					vecInt.push(y.get(st)[i][j]);
					tmpPW.print(y.get(st)[i][j] + " ");
				}
				tmpPW.print("0\n");
				countClauses++;
			}
		}
	}

	// a parent relation forces a vertex once the parent is colored
	// (!y_{l(v),i,j} or !x_{p(v),i} or x_{v,i})
	private void printParrentRelationForces(PrintWriter tmpPW) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						tmpPW.print(-y.get(e.getKey())[i][j] + " "
								+ -x[e.getValue().getNumber()][i] + " "
								+ x[v][j] + " 0\n");
						countClauses++;
					}
				}
			}
		}
	}

	// all determinization conflicts explicitly added as clauses
	// (!x_{v,i} or !x_{w,i}) where (v,w) - edge from cg
	private void printConflictsFromCG(PrintWriter tmpPW) {
		for (Entry<Integer, Set<Integer>> e : cg.getEdges().entrySet()) {
			int v = e.getKey();
			for (int w : e.getValue()) {
				if (w > v) {
					continue;
				}
				for (int i = 0; i < colors; i++) {
					tmpPW.print(-x[v][i] + " " + -x[w][i] + " 0\n");
					countClauses++;
				}
			}
		}
	}

}
