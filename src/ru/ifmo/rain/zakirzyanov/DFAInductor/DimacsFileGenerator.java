package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DimacsFileGenerator {
	
		private APTA apta;
		private ConsistencyGraph cg;
		private int colors;
		private int maxVar;
		private int vertices;
		private PrintWriter pwDF;
		private Set<String> alphabet;
		private int[][] x;
		private Map<String, Integer>[][] y;
		private int[] z;
		private int[][] e;
		private Map<String, Integer>[][] m;
		private int[][] p;
		private String tmpFile = "tmp";
		private String dimacsFile;
		private int countClauses = 0;

		public DimacsFileGenerator(APTA apta, ConsistencyGraph cg, int colors) throws IOException {
			init(apta, cg, colors, "dimacsFile.cnf");
		}
		
		public DimacsFileGenerator(APTA apta, ConsistencyGraph cg, int colors, String dimacsFile) throws IOException {
			init(apta, cg, colors, dimacsFile);
		}
		
	@SuppressWarnings("unchecked")
	private void init(APTA apta, ConsistencyGraph cg, int colors,
			String dimacsFile) throws IOException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.dimacsFile = dimacsFile;
		this.pwDF = new PrintWriter(dimacsFile);
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
	}
	
	public String generateFile() throws IOException {

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
		printSBPEdgeExist(tmpPW);
		printSBPMinimalSymbol(tmpPW);
		printSBPParent(tmpPW);
		printSBPChildrenOrder(tmpPW);
		printSBPOrderByChildrenSymbol(tmpPW);
		printSBPOrderInLayer(tmpPW);
		printSBPParentExist(tmpPW);
		tmpPW.close();

		pwDF.print("p cnf " + (maxVar - 1) + " " + countClauses  + "\n");

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(tmp)));

		String aLine = null;
		while ((aLine = in.readLine()) != null) {
			pwDF.print(aLine + "\n");
		}

		in.close();
		pwDF.close();

		tmp.delete();
		
		return dimacsFile;
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
						if (label == prevLabel) {
							break;
						}
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
