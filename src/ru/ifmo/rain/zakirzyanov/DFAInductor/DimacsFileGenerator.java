package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DimacsFileGenerator {

	final static int WITHOUT_SB = 0;
	final static int BFS_SB = 1;
	final static int CLIQUE_SB = 2;

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
	private int SB;
	private Set<Integer> acceptableClique;
	private Set<Integer> rejectableClique;
	private int color = 1;

	public DimacsFileGenerator(APTA apta, ConsistencyGraph cg, int colors,
			int SB) throws IOException {
		init(apta, cg, colors, SB, "dimacsFile.cnf");
	}

	public DimacsFileGenerator(APTA apta, ConsistencyGraph cg, int colors,
			int SB, String dimacsFile) throws IOException {
		init(apta, cg, colors, SB, dimacsFile);
	}

	@SuppressWarnings("unchecked")
	private void init(APTA apta, ConsistencyGraph cg, int colors, int SB,
			String dimacsFile) throws IOException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.SB = SB;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.dimacsFile = dimacsFile;
		this.pwDF = new PrintWriter(dimacsFile);
		this.alphabet = apta.getAlphabet();

		this.x = new int[vertices][colors];
		this.y = new HashMap[colors][colors];
		this.z = new int[colors];
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
						if (!y[i][j].containsKey(label)) {
							y[i][j].put(label, maxVar++);
						}
					}
				}
			}
		}

		if (SB == BFS_SB) {
			this.e = new int[colors][colors];
			this.m = new HashMap[colors][colors];
			this.p = new int[colors][colors];
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

		if (SB == CLIQUE_SB) {
			int maxDegree = 0;
			int maxV = -1;
			acceptableClique = new HashSet<>();
			for (int candidate : apta.getAcceptableNodes()) {
				int candidateDegree = cg.getEdges().get(candidate).size();
				if (candidateDegree > maxDegree) {
					maxDegree = candidateDegree;
					maxV = candidate;
				}
			}
			int last = maxV;
			if (last != -1) {
				acceptableClique.add(last);
				int anotherOne = findNeighbourWithHighestDegree(
						acceptableClique, last, true);
				while (anotherOne != -1) {
					acceptableClique.add(anotherOne);
					last = anotherOne;
					anotherOne = findNeighbourWithHighestDegree(
							acceptableClique, last, true);
				}
			}

			maxDegree = 0;
			maxV = -1;
			rejectableClique = new HashSet<>();
			for (int candidate : apta.getRejectableNodes()) {
				int candidateDegree = cg.getEdges().get(candidate).size();
				if (candidateDegree > maxDegree) {
					maxDegree = candidateDegree;
					maxV = candidate;
				}
			}
			last = maxV;
			if (last != -1) {
				rejectableClique.add(last);
				int anotherOne = findNeighbourWithHighestDegree(
						rejectableClique, last, false);
				while (anotherOne != -1) {
					rejectableClique.add(anotherOne);
					last = anotherOne;
					anotherOne = findNeighbourWithHighestDegree(
							rejectableClique, last, false);
				}
			}

		}
	}

	public String generateFile() throws IOException {

		File tmp = new File(tmpFile);
		PrintWriter tmpPW = new PrintWriter(tmp);
		Buffer buffer = new Buffer(tmpPW);

		if (SB == CLIQUE_SB) {
			printAcceptableCliqueSB(buffer);
			printRejectableCliqueSB(buffer);
		}
		printOneAtLeast(buffer);
		printAccVertDiffColorRej(buffer);
		printParrentRelationIsSet(buffer);
		printParrentRelationAtMostOneColor(buffer);
		printOneAtMost(buffer);
		printParrentRelationAtLeastOneColor(buffer);
		printParrentRelationForces(buffer);
		printConflictsFromCG(buffer);
		if (SB == BFS_SB) {
			printSBPEdgeExist(buffer);
			printSBPMinimalSymbol(buffer);
			printSBPParent(buffer);
			printSBPChildrenOrder(buffer);
			printSBPOrderByChildrenSymbol(buffer);
			printSBPOrderInLayer(buffer);
			printSBPParentExist(buffer);
		}
		
		tmpPW.close();
		countClauses = buffer.nClauses();

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

		return dimacsFile;
	}

	private int findNeighbourWithHighestDegree(Set<Integer> cur, int v,
			boolean acceptable) {
		int maxDegree = 0;
		int maxNeighbour = -1;
		// uv - edge
		for (int u : cg.getEdges().get(v)) {
			if (acceptable && !apta.isAcceptable(u)) {
				continue;
			}
			if (!acceptable && !apta.isRejectable(u)) {
				continue;
			}
			boolean uInClique = true;
			// check if other edges in cur connected with u
			for (int w : cur) {
				if (w != v) {
					if (!cg.getEdges().get(w).contains(u)) {
						uInClique = false;
						break;
					}
				}
			}
			if (uInClique) {
				int uDegree = cg.getEdges().get(u).size();
				if (uDegree > maxDegree) {
					maxDegree = uDegree;
					maxNeighbour = u;
				}
			}
		}
		return maxNeighbour;
	}

	// Each vertex has at least one color.
	// x_{v,1} or x_{v,2} or ... or x_{v, |C|}
	private void printOneAtLeast(Buffer buffer) {
		for (int v = 0; v < vertices; v++) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < colors; i++) {
				sb.append(x[v][i] + " ");
			}
			buffer.addClause(sb);
		}

		// root has 0 color
		buffer.addClause(x[0][0]);
		buffer.flush();
	}

	// Accepting vertices cannot have the same color as rejecting vertices
	// (!x_{v,i} or z_i) and (!x_{w,i} or !z_i), where v is acc, w is rej
	private void printAccVertDiffColorRej(Buffer buffer) {
		for (int i = 0; i < colors; i++) {
			for (Integer acc : apta.getAcceptableNodes()) {
				buffer.addClause(-x[acc][i], z[i]);
			}
			for (Integer rej : apta.getRejectableNodes()) {
				buffer.addClause(-x[rej][i], -z[i]);
			}
		}
		buffer.flush();
	}

	// A parent relation is set when a vertex and its parent are colored
	// (y_{i,j,a} or !x_{p(v),i} or !x_{v,i})
	private void printParrentRelationIsSet(Buffer buffer) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						buffer.addClause(y[i][j].get(e.getKey()), -x[e
								.getValue().getNumber()][i], -x[v][j]);
					}
				}
			}
		}
		buffer.flush();
	}

	// each parent relation can target at most one color
	// (!y_{i,h,a} or !y_{i,j,a}) where a in Alphabet, h < j
	private void printParrentRelationAtMostOneColor(Buffer buffer) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					for (int h = 0; h < j; h++) {
						buffer.addClause(-y[i][h].get(st), -y[i][j].get(st));
					}
				}
			}
		}
		buffer.flush();
	}

	// each vertex has at most one color
	// (!x_{v,i} or !x_{v,j}) where i < j
	private void printOneAtMost(Buffer buffer) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					buffer.addClause(-x[v][i], -x[v][j]);
				}
			}
		}
		buffer.flush();
	}

	// each parent relation must target at least one color
	// (!y_{i,h,a} or !y_{i,j,a}) where a in Alphabet, h < j
	private void printParrentRelationAtLeastOneColor(Buffer buffer) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < colors; j++) {
					sb.append(y[i][j].get(st) + " ");
				}
				buffer.addClause(sb);
			}
		}
		buffer.flush();
	}

	// a parent relation forces a vertex once the parent is colored
	// (!y_{i,j,l(v)} or !x_{p(v),i} or x_{v,i})
	private void printParrentRelationForces(Buffer buffer) {
		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						buffer.addClause(-y[i][j].get(e.getKey()), -x[e
								.getValue().getNumber()][i], x[v][j]);
					}
				}
			}
		}
		buffer.flush();
	}

	// all determinization conflicts explicitly added as clauses
	// (!x_{v,i} or !x_{w,i}) where (v,w) - edge from cg
	private void printConflictsFromCG(Buffer buffer) {
		for (Entry<Integer, Set<Integer>> e : cg.getEdges().entrySet()) {
			int v = e.getKey();
			for (int w : e.getValue()) {
				if (w > v) {
					continue;
				}
				for (int i = 0; i < colors; i++) {
					buffer.addClause(-x[v][i], -x[w][i]);
				}
			}
		}
		buffer.flush();
	}

	// SBP

	// e_{i,j} <=> y_{i,j,k_1} or ... or y_{i,j,k_n}
	private void printSBPEdgeExist(Buffer buffer) {
		for (int i = 0; i < colors; i++) {
			for (int j = i + 1; j < colors; j++) {
				int eij = e[i][j];
				StringBuilder tmp = new StringBuilder(-eij + " ");
				for (String label : alphabet) {
					buffer.addClause(eij, -y[i][j].get(label));
					tmp.append(y[i][j].get(label) + " ");
				}
				buffer.addClause(tmp);
			}
		}
		buffer.flush();
	}

	// m_{i,j,c_k} <=> e_{i,j} and y_{i,j,c_k} and !y_{i,j,c_(k-1)} and ... and
	// !y_{i,j,c_1}
	private void printSBPMinimalSymbol(Buffer buffer) {
		for (int i = 0; i < colors; i++) {
			for (int j = i + 1; j < colors; j++) {
				for (String label : alphabet) {
					int curM = m[i][j].get(label);

					buffer.addClause(-curM, e[i][j]);
					buffer.addClause(-curM, y[i][j].get(label));

					StringBuilder tmp = new StringBuilder(curM + " " + -e[i][j]
							+ " " + -y[i][j].get(label) + " ");
					for (String prevLabel : alphabet) {
						if (prevLabel == label) {
							break;
						}
						buffer.addClause(-curM, -y[i][j].get(prevLabel));

						tmp.append(y[i][j].get(prevLabel) + " ");
					}
					buffer.addClause(tmp);
				}
			}
		}
		buffer.flush();
	}

	// p_{i,j} <=> e_{j,i} and !e{j-1,i} and ... and !e{0, i}
	private void printSBPParent(Buffer buffer) {
		for (int i = 1; i < colors; i++) {
			for (int j = 0; j < i; j++) {
				StringBuilder tmp = new StringBuilder(p[i][j] + " " + -e[j][i]
						+ " ");

				buffer.addClause(-p[i][j], e[j][i]);

				for (int k = 0; k < j; k++) {
					buffer.addClause(-p[i][j], -e[k][i]);

					tmp.append(e[k][i] + " ");
				}
				buffer.addClause(tmp);
			}
		}
		buffer.flush();
	}

	// p_{i,j} and !p_{i+1,j} => !p_{i+q, j}
	private void printSBPChildrenOrder(Buffer buffer) {
		for (int i = 1; i < colors; i++) {
			for (int j = 0; j < i; j++) {
				for (int k = i + 2; k < colors; k++) {
					buffer.addClause(-p[i][j], p[i + 1][j], -p[k][j]);
				}
			}
		}
		buffer.flush();
	}

	// p_{i,j} and p_{i+1,j} and m_{j,i,c_k} => !m_{j,i+1,c_(k-q)}
	private void printSBPOrderByChildrenSymbol(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (String label : alphabet) {
					for (String prevLabel : alphabet) {
						if (label == prevLabel) {
							break;
						}
						buffer.addClause(-p[i][j], -p[i + 1][j],
								-m[j][i].get(label),
								-m[j][i + 1].get(prevLabel));
					}
				}
			}
		}
		buffer.flush();
	}

	// p_{i,j} => !p_{i+1,j-q}
	private void printSBPOrderInLayer(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (int k = 0; k < j; k++) {
					buffer.addClause(-p[i][j], -p[i + 1][k]);
				}
			}
		}
		buffer.flush();
	}

	// p_{i,j} or ... or p_{i,i-1}
	private void printSBPParentExist(Buffer buffer) {
		for (int i = 1; i < colors; i++) {
			StringBuilder tmp = new StringBuilder();
			for (int j = 0; j < i; j++) {
				tmp.append(p[i][j] + " ");
			}
			buffer.addClause(tmp);
		}
		buffer.flush();
	}

	private void printAcceptableCliqueSB(Buffer buffer) {
		for (int i : acceptableClique) {
			if (i == 0) {
				continue;
			}
			if (color < colors) {
				buffer.addClause(x[i][color]);
				buffer.addClause(z[color]);
				color++;
			} else {
				break;
			}
		}
		buffer.flush();
	}

	private void printRejectableCliqueSB(Buffer buffer) {
		for (int i : rejectableClique) {
			if (i == 0) {
				continue;
			}
			if (color < colors) {
				buffer.addClause(x[i][color]);
				buffer.addClause(-z[color]);
				color++;
			} else {
				break;
			}
		}
		buffer.flush();
	}

}
