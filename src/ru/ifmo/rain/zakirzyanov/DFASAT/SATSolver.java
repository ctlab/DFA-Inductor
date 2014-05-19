package ru.ifmo.rain.zakirzyanov.DFASAT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	private Map<String, Integer>[][] y;
	// z[i] - vertex with color i is Acceptable
	private int[] z;
	// e[i][j] - exist edge from vertex with color i to vertex with color j
	private int[][] e;
	// m[i][j][k] - minimal symbol k
	private Map<String, Integer>[][] m;
	// p[i][k] - parent
	private int[][] p;
	private Set<String> alphabet;
	private IProblem problem;
	private String DimacsFile = "dimacsFile.cnf";
	private String tmpFile = "tmp";
	private String satSolverFile = null;
	private String ansLine = null;
	private PrintWriter pwDF;
	private int countClauses;

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors)
			throws ContradictionException, ParseFormatException, IOException {
		init(apta, cg, colors, null);
		problem = build();
	}

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors,
			String satSolverFile) throws ContradictionException,
			ParseFormatException, IOException {
		init(apta, cg, colors, satSolverFile);
	}

	@SuppressWarnings("unchecked")
	private void init(APTA apta, ConsistencyGraph cg, int colors,
			String satSolverFile) throws IOException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.pwDF = new PrintWriter(this.DimacsFile);
		this.satSolverFile = satSolverFile;
		this.alphabet = apta.getAlphabet();
		this.x = new int[vertices][colors];
		this.y = new HashMap[colors][colors];
		this.z = new int[colors];
		this.e = new int[colors][colors];
		this.m = new HashMap[colors][colors];
		this.p = new int[colors][colors];

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
						String label = e.getKey();
						if (y[i][j] == null) {
							y[i][j] = new HashMap<>();
						}
						y[i][j].put(label, maxVar++);
					}
				}
			}
		}
		for (int i = 0; i < colors; i++) {
			for (int j = i + 1; j < colors; j++) {
				e[i][j] = maxVar++;
			}
		}

		for (int i = 0; i < colors; i++) {
			for (int j = i + 1; j < colors; j++) {
				m[i][j] = new HashMap<>();
				for (String label : alphabet) {
					m[i][j].put(label, maxVar++);
				}
			}
		}

		for (int i = 1; i < colors; i++) {
			for (int j = 0; j < i; j++) {
				p[i][j] = maxVar++;
			}
		}
		generateFile();
	}

	public int[][] getX() {
		return x;
	}

	public Map<String, Integer>[][] getY() {
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
			// Process process = new ProcessBuilder(satSolverFile + " " +
			// DimacsFile).start();
			Process process = Runtime.getRuntime().exec(
					satSolverFile + " " + DimacsFile);
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
//		printSBPEdgeExist(tmpPW);
//		printSBPMinimalSymbol(tmpPW);
//		printSBPParent(tmpPW);
//		printSBPChildrenOrder(tmpPW);
//		printSBPOrderByChildrenSymbol(tmpPW);
//		printSBPOrderInLayer(tmpPW);
//		printSBPParentExist(tmpPW);
		tmpPW.close();

		pwDF.print("p cnf " + (maxVar - 1) + " " + countClauses + "\n");

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(tmp)));

		String aLine = null;
		while ((aLine = in.readLine()) != null) {
			pwDF.print(aLine + "\n");
		}

		in.close();
		pwDF.close();

		tmp.delete();
	}

	private IProblem build() throws ContradictionException,
			ParseFormatException, IOException {
		ISolver solver = SolverFactory.newDefault();
		Reader reader = new DimacsReader(solver);
		IProblem problem = reader.parseInstance(DimacsFile);
		return problem;
	}

	// Each vertex has at least one color.
	// x_{v,1} or x_{v,2} or ... or x_{v, |C|}
	private void printOneAtLeast(PrintWriter pw) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				pw.print(x[v][i] + " ");
			}
			pw.print("0\n");
			countClauses++;
		}

		// root has 0 color
		pw.print(x[0][0] + " 0\n");
		countClauses++;
	}

	// Accepting vertices cannot have the same color as rejecting vertices
	// (!x_{v,i} or z_i) and (!x_{w,i} or !z_i), where v is acc, w is rej
	private void printAccVertDiffColorRej(PrintWriter pw) {
		for (int i = 0; i < colors; i++) {
			for (Integer acc : apta.getAcceptableNodes()) {
				pw.print(-x[acc][i] + " " + z[i] + " 0\n");
				countClauses++;
			}
			for (Integer rej : apta.getRejectableNodes()) {
				pw.print(-x[rej][i] + " " + -z[i] + " 0\n");
				countClauses++;
			}
		}
	}

	// A parent relation is set when a vertex and its parent are colored
	// (y_{i,j,a} or !x_{p(v),i} or !x_{v,i})
	private void printParrentRelationIsSet(PrintWriter pw) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						pw.print(y[i][j].get(e.getKey()) + " "
								+ -x[e.getValue().getNumber()][i] + " "
								+ -x[v][j] + " 0\n");
						countClauses++;
					}
				}
			}
		}
	}

	// each parent relation can target at most one color
	// (!y_{i,h,a} or !y_{i,j,a}) where a in Alphabet, h < j
	private void printParrentRelationAtMostOneColor(PrintWriter pw) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					for (int h = 0; h < j; h++) {
						pw.print(-y[i][h].get(st) + " " + -y[i][j].get(st)
								+ " 0\n");
						countClauses++;
					}
				}
			}
		}
	}

	// each vertex has at most one color
	// (!x_{v,i} or !x_{v,j}) where i < j
	private void printOneAtMost(PrintWriter pw) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					pw.print(-x[v][i] + " " + -x[v][j] + " 0\n");
					countClauses++;
				}
			}
		}
	}

	// each parent relation must target at least one color
	// (!y_{i,h,a} or !y_{i,j,a}) where a in Alphabet, h < j
	private void printParrentRelationAtLeastOneColor(PrintWriter pw) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					pw.print(y[i][j].get(st) + " ");
				}
				pw.print("0\n");
				countClauses++;
			}
		}
	}

	// a parent relation forces a vertex once the parent is colored
	// (!y_{i,j,l(v)} or !x_{p(v),i} or x_{v,i})
	private void printParrentRelationForces(PrintWriter pw) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						pw.print(-y[i][j].get(e.getKey()) + " "
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
	private void printConflictsFromCG(PrintWriter pw) {
		for (Entry<Integer, Set<Integer>> e : cg.getEdges().entrySet()) {
			int v = e.getKey();
			for (int w : e.getValue()) {
				if (w > v) {
					continue;
				}
				for (int i = 0; i < colors; i++) {
					pw.print(-x[v][i] + " " + -x[w][i] + " 0\n");
					countClauses++;
				}
			}
		}
	}

	// SBP

	// e_{i,j} <=> y_{i,j,k_1} or ... or y_{i,j,k_n}
	private void printSBPEdgeExist(PrintWriter pw) {
		for (int i = 0; i < colors; i++) {
			for (int j = i + 1; j < colors; j++) {
				int eij = e[i][j];
				StringBuilder tmp = new StringBuilder(-eij + " ");
				for (String label : alphabet) {
					pw.print(eij + " " + -y[i][j].get(label) + " 0\n");
					countClauses++;
					tmp.append(y[i][j].get(label) + " ");
				}
				tmp.append("0\n");
				pw.print(tmp);
				countClauses++;
			}
		}
	}

	// m_{i,j,c_k} <=> e_{i,j} and y_{i,j,c_k} and !y_{i,j,c_(k-1)} and ... and
	// !y_{i,j,c_1}
	private void printSBPMinimalSymbol(PrintWriter pw) {
		for (int i = 0; i < colors; i++) {
			for (int j = i + 1; j < colors; j++) {
				for (String label : alphabet) {
					int curM = m[i][j].get(label);

					pw.print(-curM + " " + e[i][j] + " 0\n");
					countClauses++;

					pw.print(-curM + " " + y[i][j].get(label) + " 0\n");
					countClauses++;

					StringBuilder tmp = new StringBuilder(curM + " " + -e[i][j]
							+ " " + -y[i][j].get(label) + " ");
					for (String prevLabel : alphabet) {
						if (prevLabel == label) {
							break;
						}
						pw.print(-curM + " " + -y[i][j].get(prevLabel) + " 0\n");
						countClauses++;

						tmp.append(y[i][j].get(prevLabel) + " ");
					}
					tmp.append("0\n");
					pw.print(tmp);
					countClauses++;
				}
			}
		}
	}

	// p_{i,j} <=> e_{j,i} and !e{j-1,i} and ... and !e{0, i}
	private void printSBPParent(PrintWriter pw) {
		for (int i = 1; i < colors; i++) {
			for (int j = 0; j < i; j++) {
				StringBuilder tmp = new StringBuilder(p[i][j] + " " + -e[j][i]
						+ " ");

				pw.print(-p[i][j] + " " + e[j][i] + " 0\n");
				countClauses++;

				for (int k = 0; k < j; k++) {
					pw.print(-p[i][j] + " " + -e[k][i] + " 0\n");
					countClauses++;

					tmp.append(e[k][i] + " ");
				}
				tmp.append("0\n");
				pw.print(tmp);
				countClauses++;
			}
		}
	}

	// p_{i,j} and !p_{i+1,j} => !p_{i+q, j}
	private void printSBPChildrenOrder(PrintWriter pw) {
		for (int i = 1; i < colors; i++) {
			for (int j = 0; j < i; j++) {
				for (int k = i + 2; k < colors; k++) {
					pw.print(-p[i][j] + " " + p[i + 1][j] + " " + -p[k][j]
							+ " 0\n");
					countClauses++;
				}
			}
		}
	}

	// p_{i,j} and p_{i+1,j} and m_{j,i,c_k} => !m_{j,i+1,c_(k-q)}
	private void printSBPOrderByChildrenSymbol(PrintWriter pw) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (String label : alphabet) {
					for (String prevLabel : alphabet) {
						pw.print(-p[i][j] + " " + -p[i + 1][j] + " "
								+ -m[j][i].get(label) + " "
								+ -m[j][i + 1].get(prevLabel) + " 0\n");
						countClauses++;
					}
				}
			}
		}
	}
	
	//p_{i,j} => !p_{i+1,j-q}
	private void printSBPOrderInLayer(PrintWriter pw) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (int k = 0; k < j; k++) {
					pw.print(-p[i][j] + " " + -p[i+1][k] + " 0\n");
					countClauses++;
				}
			}
		}
	}
	
	//p_{i,j} or ... or p_{i,i-1}
	private void printSBPParentExist(PrintWriter pw) {
		for (int i = 1; i < colors; i++) {
			StringBuilder tmp = new StringBuilder();
			for (int j = 0; j < i; j++) {
				tmp.append(p[i][j] + " ");
			}
			tmp.append("0\n");
			pw.print(tmp);
			countClauses++;
		}
	}

}
