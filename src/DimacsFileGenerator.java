import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class DimacsFileGenerator {

	enum SBStrategy {
		WITHOUT_SB, BFS_SB, CLIQUE_SB
	}

	// Exception??

	public SBStrategy getSBStrategyByNum(int num) {
		switch (num) {
			case 0:
				return SBStrategy.WITHOUT_SB;
			case 2:
				return SBStrategy.CLIQUE_SB;
			default:
				return SBStrategy.BFS_SB;
		}
	}

	private APTA apta;
	private ConsistencyGraph cg;
	private int colors;
	private int maxVar;
	private int vertices;
	private Set<String> alphabet;
	private int[][] x;
	private Map<String, Integer>[][] y;
	private int[] z;
	private int[][] e;
	private Map<String, Integer>[][] m;
	private int[][] p;
	private List<List<Integer>> n;
	private List<List<Integer>> o;
	private List<Integer> f;
	private String tmpFile = "tmp";
	private String dimacsFile;
	private SBStrategy SB;
	private int noisyP;
	private int noisySize;
	private Set<Integer> acceptableClique;
	private Set<Integer> rejectableClique;
	private int color = 0;
	private List<Integer> ends;


	public DimacsFileGenerator(APTA apta, ConsistencyGraph cg, int colors,
	                           int SB, int noisyP, String dimacsFile) throws IOException {
		init(apta, cg, colors, getSBStrategyByNum(SB), noisyP, dimacsFile);
	}

	@SuppressWarnings("unchecked")
	private void init(APTA apta, ConsistencyGraph cg, int colors,
	                  SBStrategy SB, int noisyP, String dimacsFile) throws IOException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.SB = SB;
		this.noisyP = noisyP;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.dimacsFile = dimacsFile;
		this.alphabet = apta.getAlphabet();
		this.ends = new ArrayList<>();

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

		for (int i = 0; i < colors; i++) {
			for (int j = 0; j < colors; j++) {
				y[i][j] = new HashMap<>();
				for (String label : alphabet) {
					y[i][j].put(label, maxVar++);
				}
			}
		}

		if (SB == SBStrategy.BFS_SB) {
			this.e = new int[colors][colors];
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					e[i][j] = maxVar++;
				}
			}

			this.p = new int[colors][colors];
			for (int i = 1; i < colors; i++) {
				for (int j = 0; j < i; j++) {
					p[i][j] = maxVar++;
				}
			}

			this.m = new HashMap[colors][colors];
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					m[i][j] = new HashMap<>();
					for (String label : alphabet) {
						m[i][j].put(label, maxVar++);
					}
				}
			}
		}

		if (SB == SBStrategy.CLIQUE_SB) {
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

		if (noisyP > 0) {
			ends.addAll(apta.getAcceptableNodes());
			ends.addAll(apta.getRejectableNodes());
			Collections.sort(ends);

			noisySize = (int) Math.round((ends.size() / 100.0) * noisyP);

			n = new ArrayList<>();
			for (int i = 0; i < noisySize; i++) {
				n.add(new ArrayList<Integer>());
				for (int v : ends) {
					n.get(i).add(maxVar++);
				}
			}

			o = new ArrayList<>();
			for (int i = 0; i < noisySize; i++) {
				o.add(new ArrayList<Integer>());
				for (int v : ends) {
					o.get(i).add(maxVar++);
				}
				o.get(i).add(maxVar++);
			}

			f = new ArrayList<>();
			for (int v : ends) {
				f.add(maxVar++);
			}
		}
	}

	public String generateFile() throws IOException {
		File tmp = new File(tmpFile);
		try (PrintWriter tmpPW = new PrintWriter(tmp)) {
			Buffer buffer = new Buffer(tmpPW);
			try (PrintWriter pwDF = new PrintWriter(dimacsFile)) {

				printOneAtLeast(buffer);
				printOneAtMost(buffer);
				printParrentRelationIsSet(buffer);
				printParrentRelationAtMostOneColor(buffer);
				printParrentRelationAtLeastOneColor(buffer);
				printParrentRelationForces(buffer);

				if (SB == SBStrategy.BFS_SB) {
					//  root has 0 color
					buffer.addClause(x[0][0]);
					printSBPEdgeExist(buffer);
					printSBPMinimalSymbol(buffer);
					printSBPParent(buffer);
					// printSBPChildrenOrder(buffer);
					if (apta.getAlphaSize() == 2) {
						printSBPOrderByChildrenSymbolForSizeTwo(buffer);
					} else {
						printSBPOrderByChildrenSymbol(buffer);
					}
					printSBPOrderInLayer(buffer);
					printSBPParentExist(buffer);
				}
				if (SB == SBStrategy.CLIQUE_SB) {
					printAcceptableCliqueSB(buffer);
					printRejectableCliqueSB(buffer);
				}

				if (noisyP > 0) {
//			        printOneAtLeastInNoisy(buffer);
//			        printOneAtMostInNoisy(buffer);
//			        printNoisyOrdered(buffer);
					printNoisyNProxy(buffer);
					printNoisyOneUnderOne(buffer);
					printNoisyOrderingDiagonal(buffer);
					printFProxy(buffer);
					printAccVertDiffColorRejNoisy(buffer);

				} else {
					printAccVertDiffColorRej(buffer);
					printConflictsFromCG(buffer);
				}
				int countClauses = buffer.nClauses();

				pwDF.print("p cnf " + (maxVar - 1) + " " + countClauses + "\n");
				tmpPW.flush();
				try (BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(tmp)))) {

					String aLine;
					while ((aLine = in.readLine()) != null) {
						pwDF.print(aLine + "\n");
					}
				}
			}
		}
		tmp.delete();
		return dimacsFile;
	}

	public void banSolution(Automaton automaton) throws IOException {
		List<String> cache = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(dimacsFile))) {
			String line = br.readLine();
			if (line.startsWith("p cnf")) {
				String[] tmp = line.split("\\s+");
				tmp[3] = String.valueOf(Integer.parseInt(tmp[3]) + 1) + "\n";
				cache.add(tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + tmp[3]);
			} //else - throw ex
			while ((line = br.readLine()) != null) {
				cache.add(line);
			}
		}
		try (PrintWriter pwDF = new PrintWriter(new BufferedWriter(new FileWriter(dimacsFile)))) {
			for (String s : cache) {
				pwDF.println(s);
			}
			Buffer buffer = new Buffer(pwDF);
			StringBuilder sb = new StringBuilder();
			for (Node state : automaton.getStates()) {
				for (Entry<String, Node> e : state.getChildren().entrySet()) {
					sb.append(-y[state.getNumber()][e.getValue().getNumber()].get(e.getKey())).append(" ");
				}
			}
			buffer.addClause(sb);
			buffer.flush();
		}
	}

	private int findNeighbourWithHighestDegree(Set<Integer> cur, int v, boolean acceptable) {
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
			// check if other vertices in cur connected with u
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
				sb.append(x[v][i]).append(" ");
			}
			buffer.addClause(sb);
		}
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
	//(y_{i,1,a} or ... or y_{i,|C|,a})
	private void printParrentRelationAtLeastOneColor(Buffer buffer) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < colors; j++) {
					sb.append(y[i][j].get(st)).append(" ");
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
					tmp.append(y[i][j].get(label)).append(" ");
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
						if (prevLabel.equals(label)) {
							break;
						}
						buffer.addClause(-curM, -y[i][j].get(prevLabel));

						tmp.append(y[i][j].get(prevLabel)).append(" ");
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

					tmp.append(e[k][i]).append(" ");
				}
				buffer.addClause(tmp);
			}
		}
		buffer.flush();
	}

//	// p_{i,j} and !p_{i+1,j} => !p_{i+q, j}
//	private void printSBPChildrenOrder(Buffer buffer) {
//		for (int i = 1; i < colors; i++) {
//			for (int j = 0; j < i; j++) {
//				for (int k = i + 2; k < colors; k++) {
//					buffer.addClause(-p[i][j], p[i + 1][j], -p[k][j]);
//				}
//			}
//		}
//		buffer.flush();
//	}

	// if alphabet size greater then 2
	// p_{i,j} and p_{i+1,j} and m_{j,i,c_k} => !m_{j,i+1,c_(k-q)}
	private void printSBPOrderByChildrenSymbol(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (String label : alphabet) {
					for (String prevLabel : alphabet) {
						if (label.equals(prevLabel)) {
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

	// if alphabet size equal to 2
	// p_{i,j} and p_{i+1,j} => y_{j, i, 0} and y_{j, i + 1, 1}
	private void printSBPOrderByChildrenSymbolForSizeTwo(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				buffer.addClause(-p[i][j], -p[i + 1][j], y[j][i].get("0"));
				buffer.addClause(-p[i][j], -p[i + 1][j], y[j][i + 1].get("1"));
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
				tmp.append(p[i][j]).append(" ");
			}
			buffer.addClause(tmp);
		}
		buffer.flush();
	}

	private void printAcceptableCliqueSB(Buffer buffer) {
		for (int i : acceptableClique) {
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

//	// n_{q,1} \/ n_{q,2} \/ ... \/ n_{q,|V|}
//	private void printOneAtLeastInNoisy(Buffer buffer) {
//		for (int q = 0; q < noisySize; q++) {
//			StringBuilder sb = new StringBuilder();
//			for (int i = 0; i < vertices; i++) {
//				if (ends.contains(i)) {
//					sb.append(n[q][i] + " ");
//				}
//			}
//			buffer.addClause(sb);
//		}
//		buffer.flush();
//	}
//
//	// ~n[q,i] \/ ~n[q,j]
//	private void printOneAtMostInNoisy(Buffer buffer) {
//		for (int q = 0; q < noisySize; q++) {
//			for (int i = 0; i < vertices; i++) {
//				if (ends.contains(i)) {
//					for (int j = i + 1; j < vertices; j++) {
//						if (ends.contains(j)) {
//							buffer.addClause(-n[q][i], -n[q][j]);
//						}
//					}
//				}
//			}
//		}
//		buffer.flush();
//	}
//
//	// n_{q,i} => ~n_{q+1,i-j}
//	private void printNoisyOrdered(Buffer buffer) {
//		for (int q = 0; q < noisySize - 1; q++) {
//			for (int i = 0; i < vertices; i++) {
//				if (ends.contains(i)) {
//					for (int j = 0; j < i; j++) {
//						if (ends.contains(j)) {
//							buffer.addClause(-n[q][i], -n[q + 1][j]);
//						}
//					}
//				}
//			}
//		}
//		buffer.flush();
//	}

	//n_{q,i} <=> o_{q,i} /\ ~o_{q,i+1}
	private void printNoisyNProxy(Buffer buffer) {
		for (int q = 0; q < noisySize; q++) {
			for (int i = 0; i < n.get(q).size(); i++) {
				buffer.addClause(n.get(q).get(i), -o.get(q).get(i), o.get(q).get(i + 1));
				buffer.addClause(-n.get(q).get(i), o.get(q).get(i));
				buffer.addClause(-n.get(q).get(i), -o.get(q).get(i + 1));
			}
			buffer.addClause(o.get(q).get(0));
			buffer.addClause(-o.get(q).get(ends.size()));
		}
		buffer.flush();
	}

	//o_{q,i} => o_{q,i-1}
	private void printNoisyOneUnderOne(Buffer buffer) {
		for (int q = 0; q < noisySize; q++) {
			for (int i = 1; i < o.get(q).size(); i++) {
				buffer.addClause(-o.get(q).get(i), o.get(q).get(i - 1));
			}
		}
		buffer.flush();
	}

	//o_{q,i} => o_{q+1,i+1}
	private void printNoisyOrderingDiagonal(Buffer buffer) {
		for (int q = 0; q < noisySize - 1; q++) {
			for (int i = 0; i < o.get(q).size() - 1; i++) {
				buffer.addClause(-o.get(q).get(i), o.get(q + 1).get(i + 1));
			}
		}
		buffer.flush();
	}

	// f_v <=> n_{1,v} \/ ... \/ n_{k, v}.
	private void printFProxy(Buffer buffer) {
		for (int v = 0; v < f.size(); v++) {
			int fv = f.get(v);
			StringBuilder tmp = new StringBuilder(-fv + " ");
			for (int q = 0; q < noisySize; q++) {
				buffer.addClause(fv, -n.get(q).get(v));
				tmp.append(n.get(q).get(v)).append(" ");
			}
			buffer.addClause(tmp);
		}
		buffer.flush();
	}

	// (f_v \/ ~x_{v,i} \/ z_i) /\ (f_w \/~x_{w,i} \/ ~z_i)
	private void printAccVertDiffColorRejNoisy(Buffer buffer) {
		for (int i = 0; i < colors; i++) {
			for (int v = 0; v < f.size(); v++) {
				if (apta.getAcceptableNodes().contains(ends.get(v))) {
					buffer.addClause(f.get(v), -x[ends.get(v)][i], z[i]);
				} else {
					buffer.addClause(f.get(v), -x[ends.get(v)][i], -z[i]);
				}
			}
		}
		buffer.flush();
	}

}
