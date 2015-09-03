import java.io.*;
import java.util.*;
import java.util.Map.Entry;

public class DimacsFileGenerator {

	enum SBStrategy {
		WITHOUT_SB, BFS_SB, DFS_SB, CLIQUE_SB
	}

	// Exception??

	public SBStrategy getSBStrategyByNum(int num) {
		switch (num) {
			case 0:
				return SBStrategy.WITHOUT_SB;
			case 1:
				return SBStrategy.BFS_SB;
			case 2:
				return SBStrategy.DFS_SB;
			case 3:
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
				x[v][i] = newVariable();
			}
		}
		for (int i = 0; i < colors; i++) {
			z[i] = newVariable();
		}

		for (int i = 0; i < colors; i++) {
			for (int j = 0; j < colors; j++) {
				y[i][j] = new HashMap<>();
				for (String label : alphabet) {
					y[i][j].put(label, newVariable());
				}
			}
		}

		if (SB == SBStrategy.BFS_SB || SB == SBStrategy.DFS_SB) {
			this.e = new int[colors][colors];
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					e[i][j] = newVariable();
				}
			}

			this.p = new int[colors][colors];
			for (int i = 1; i < colors; i++) {
				for (int j = 0; j < i; j++) {
					p[i][j] = newVariable();
				}
			}

			this.m = new HashMap[colors][colors];
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					m[i][j] = new HashMap<>();
					for (String label : alphabet) {
						m[i][j].put(label, newVariable());
					}
				}
			}
		}

		if (SB == SBStrategy.CLIQUE_SB) {
			acceptableClique = cg.getAcceptableClique();
			rejectableClique = cg.getRejectableClique();
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
					n.get(i).add(newVariable());
				}
			}

			o = new ArrayList<>();
			for (int i = 0; i < noisySize; i++) {
				o.add(new ArrayList<Integer>());
				for (int v : ends) {
					o.get(i).add(newVariable());
				}
				o.get(i).add(newVariable());
			}

			f = new ArrayList<>();
			for (int v : ends) {
				f.add(newVariable());
			}
		}
	}

	public String generateFile(boolean isBimander) throws IOException {
		File tmp = new File(tmpFile);
		try (PrintWriter tmpPW = new PrintWriter(tmp)) {
			Buffer buffer = new Buffer(tmpPW);
			try (PrintWriter pwDF = new PrintWriter(dimacsFile)) {

				printOneAtLeast(buffer);
				printOneAtMost(buffer, isBimander);
				printParentRelationIsSet(buffer);
				printParentRelationAtMostOneColor(buffer, isBimander);
				printParentRelationAtLeastOneColor(buffer);
				printParentRelationForces(buffer);

				if (SB == SBStrategy.BFS_SB || SB == SBStrategy.DFS_SB) {
					//  root has 0 color
					buffer.addClause(x[0][0]);
					printSBPEdgeExist(buffer);
					printSBPMinimalSymbol(buffer);
					// printSBPChildrenOrder(buffer);

					printSBPParentExist(buffer);
					if (SB == SBStrategy.BFS_SB) {
						printSBPParentBFS(buffer);
						printSBPOrderInLayerBFS(buffer);
						if (apta.getAlphaSize() == 2) {
							printSBPOrderByChildrenSymbolForSizeTwoBFS(buffer);
						} else {
							printSBPOrderByChildrenSymbolBFS(buffer);
						}
					} else {
						printSBPParentDFS(buffer);
						printSBPSubtreeNotIntersectDFS(buffer);
						if (apta.getAlphaSize() == 2) {
							printSBPOrderByChildrenSymbolForSizeTwoDFS(buffer);
						} else {
							printSBPOrderByChildrenSymbolDFS(buffer);
						}
					}
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

	public void banSolution(Automaton automaton, int[] model) throws IOException {
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
//			for (Node state : automaton.getStates()) {
//				for (Entry<String, Node> e : state.getChildren().entrySet()) {
//					sb.append(-y[state.getNumber()][e.getValue().getNumber()].get(e.getKey())).append(" ");
//				}
//			}
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					for (String label : alphabet) {
						if (model[y[i][j].get(label) - 1] > 0) {
							sb.append(-y[i][j].get(label)).append(" ");
						}
					}
				}
			}
			buffer.addClause(sb);
		}
	}

	public int[][] getX() {
		return x;
	}

	public Map<Integer, Integer> getF() {
		if (noisyP > 0) {
			Map<Integer, Integer> ff = new HashMap<>();
			for (int i = 0; i < ends.size(); i++) {
				ff.put(ends.get(i), f.get(i));
			}
			return ff;
		} else {
			throw new NullPointerException("F is undefined in the noiseless mode.");
		}
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
	}

	// A parent relation is set when a vertex and its parent are colored
	// (y_{i,j,a} or !x_{p(v),i} or !x_{v,i})
	private void printParentRelationIsSet(Buffer buffer) {
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
	}

	// each parent relation can target at most one color
	// (!y_{i,h,a} or !y_{i,j,a}) where a in Alphabet, h < j
	private void printParentRelationAtMostOneColor(Buffer buffer, boolean isBimander) {
		if (isBimander) {
			List<Integer> yList = new ArrayList<>();
			for (String st : apta.getAlphabet()) {
				for (int i = 0; i < colors; i++) {
					yList.clear();
					for (int j = 0; j < colors; j++) {
						yList.add(y[i][j].get(st));
					}
					atMostOneBimander(buffer, (int) Math.ceil(Math.sqrt((double) colors)), yList);
				}
			}
		} else {
			for (String st : apta.getAlphabet()) {
				for (int i = 0; i < colors; i++) {
					for (int j = 0; j < colors; j++) {
						for (int h = 0; h < j; h++) {
							buffer.addClause(-y[i][h].get(st), -y[i][j].get(st));
						}
					}
				}
			}
		}
	}

	// each vertex has at most one color
	// (!x_{v,i} or !x_{v,j}) where i < j
	private void printOneAtMost(Buffer buffer, boolean isBimander) {
		if (isBimander && colors > 6) {
			List<Integer> xList = new ArrayList<>();
			for (int v = 0; v < vertices; v++) {
				xList.clear();
				for (int i : x[v]) {
					xList.add(i);
				}
				atMostOneBimander(buffer, (int) Math.ceil(Math.sqrt((double) colors)), xList);
			}
		} else {
			for (int v = 0; v < vertices; v++) {
				for (int i = 0; i < colors; i++) {
					for (int j = i + 1; j < colors; j++) {
						buffer.addClause(-x[v][i], -x[v][j]);
					}
				}
			}
		}
	}

	// each parent relation must target at least one color
	//(y_{i,1,a} or ... or y_{i,|C|,a})
	private void printParentRelationAtLeastOneColor(Buffer buffer) {
		for (String st : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < colors; j++) {
					sb.append(y[i][j].get(st)).append(" ");
				}
				buffer.addClause(sb);
			}
		}
	}

	// a parent relation forces a vertex once the parent is colored
	// (!y_{i,j,l(v)} or !x_{p(v),i} or x_{v,i})
	private void printParentRelationForces(Buffer buffer) {
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
	}

	// p_{i,j} <=> e_{j,i} and !e{j-1,i} and ... and !e{0, i}
	private void printSBPParentBFS(Buffer buffer) {
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
	}

	// p_{i,j} <=> e_{j,i} and !e{j+1,i} and ... and !e{i-1, i}
	private void printSBPParentDFS(Buffer buffer) {
		for (int i = 1; i < colors; i++) {
			for (int j = 0; j < i; j++) {
				StringBuilder tmp = new StringBuilder(p[i][j] + " " + -e[j][i]
						+ " ");
				buffer.addClause(-p[i][j], e[j][i]);

				for (int k = j + 1; k < i; k++) {
					buffer.addClause(-p[i][j], -e[k][i]);

					tmp.append(e[k][i]).append(" ");
				}
				buffer.addClause(tmp);
			}
		}
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
// 	}

	// if alphabet size greater then 2 BFS
	// p_{i,j} and p_{i+1,j} and m_{j,i,c_k} => !m_{j,i+1,c_(k-q)}
	private void printSBPOrderByChildrenSymbolBFS(Buffer buffer) {
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
	}

	// if alphabet size greater then 2 DFS
	// p_{i,j} and p_{i+t,j} and m_{j,i,c_k} => !m_{j,i+t,c_(k-q)}
	private void printSBPOrderByChildrenSymbolDFS(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (int s = i + 1; s < colors; s++) {
					for (String label : alphabet) {
						for (String prevLabel : alphabet) {
							if (label.equals(prevLabel)) {
								break;
							}
							buffer.addClause(-p[i][j], -p[s][j],
									-m[j][i].get(label),
									-m[j][s].get(prevLabel));
						}
					}
				}
			}
		}
	}

	// if alphabet size equal to 2 BFS
	// p_{i,j} and p_{i+1,j} => y_{j,i,0} and y_{j,i+1,1}
	private void printSBPOrderByChildrenSymbolForSizeTwoBFS(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				buffer.addClause(-p[i][j], -p[i + 1][j], y[j][i].get("0"));
				buffer.addClause(-p[i][j], -p[i + 1][j], y[j][i + 1].get("1"));
			}
		}
	}

	// if alphabet size equal to 2 DFS
	// p_{i,j} and p_{i+q,j} => y_{j,i,0} and y_{j,i+q,1}
	private void printSBPOrderByChildrenSymbolForSizeTwoDFS(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (int s = i + 1; s < colors; s++) {
					buffer.addClause(-p[i][j], -e[j][s], y[j][i].get("0"));
					buffer.addClause(-p[i][j], -e[j][s], y[j][s].get("1"));
				}
			}
		}
	}

	// p_{i,j} => !p_{i+1,j-q}
	private void printSBPOrderInLayerBFS(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (int k = 0; k < j; k++) {
					buffer.addClause(-p[i][j], -p[i + 1][k]);
				}
			}
		}
	}

	// p_{i,j} => !t_{i+k,j-q}
	private void printSBPSubtreeNotIntersectDFS(Buffer buffer) {
		for (int i = 1; i < colors - 1; i++) {
			for (int j = 0; j < i; j++) {
				for (int t = j + 1; t < i; t++) {
					for (int s = i + 1; s < colors; s++) {
						buffer.addClause(-p[i][j], -e[t][s]);
					}
				}
			}
		}
	}

	// p_{i,1} or ... or p_{i,i-1}
	private void printSBPParentExist(Buffer buffer) {
		for (int i = 1; i < colors; i++) {
			StringBuilder tmp = new StringBuilder();
			for (int j = 0; j < i; j++) {
				tmp.append(p[i][j]).append(" ");
			}
			buffer.addClause(tmp);
		}
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
// 	}
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
//	}

	//n_{q,i} <=> o_{q,i} /\ ~o_{q,i+1}
	private void printNoisyNProxy(Buffer buffer) {
		for (int q = 0; q < noisySize; q++) {
			List<Integer> nq = n.get(q);
			List<Integer> oq = o.get(q);
			for (int i = 0; i < nq.size(); i++) {
				int nqi = nq.get(i);
				buffer.addClause(nqi, -oq.get(i), oq.get(i + 1));
				buffer.addClause(-nqi, oq.get(i));
				buffer.addClause(-nqi, -oq.get(i + 1));
			}
			buffer.addClause(oq.get(0));
			buffer.addClause(-oq.get(ends.size()));
		}
	}

	//o_{q,i} => o_{q,i-1}
	private void printNoisyOneUnderOne(Buffer buffer) {
		for (int q = 0; q < noisySize; q++) {
			List<Integer> oq = o.get(q);
			for (int i = 1; i < oq.size(); i++) {
				buffer.addClause(-oq.get(i), oq.get(i - 1));
			}
		}
	}

	//o_{q,i} => o_{q+1,i+1}
	private void printNoisyOrderingDiagonal(Buffer buffer) {
		for (int q = 0; q < noisySize - 1; q++) {
			for (int i = 0; i < o.get(q).size() - 1; i++) {
				buffer.addClause(-o.get(q).get(i), o.get(q + 1).get(i + 1));
			}
		}
	}

	// f_v <=> n_{1,v} \/ ... \/ n_{k, v}.
	private void printFProxy(Buffer buffer) {
		for (int v = 0; v < f.size(); v++) {
			int fv = f.get(v);
			StringBuilder tmp = new StringBuilder(-fv + " ");
			for (int q = 0; q < noisySize; q++) {
				int nqv = n.get(q).get(v);
				buffer.addClause(fv, -nqv);
				tmp.append(nqv).append(" ");
			}
			buffer.addClause(tmp);
		}
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
	}

	private void atMostOneBimander(Buffer buffer, int m, List<Integer> vars) {
		int n = vars.size();
		int g = (int) Math.ceil((double) n / m);
		int[] b;
		int k = log2(m);
		BitMask bm = new BitMask(k);
		int curGfrom = 0;
		int curGto = g;
		//first part of bimander. AMO in groups
		while (curGfrom != n) {
			for (int i = curGfrom; i < curGto - 1; i++) {
				for (int j = i + 1; j < curGto; j++) {
					buffer.addClause(-vars.get(i), -vars.get(j));
				}
			}
			curGfrom = curGto;
			curGto = Math.min(curGto + g, n);
		}
		//redundant vars
		b = new int[k];
		for (int i = 0; i < k; i++) {
			b[i] = newVariable();
		}
		curGfrom = 0;
		curGto = g;
		//second part of bimander
		while (curGfrom != n) {
			for (int i = curGfrom; i < curGto; i++) {
				for (int j = 0; j < k; j++) {
					int sign = bm.get(j) ? 1 : -1;
					buffer.addClause(-vars.get(i), sign * b[j]);
				}
			}
			curGfrom = curGto;
			curGto = Math.min(curGto + g, n);
			bm.next();
		}
	}

	private static int log2(int n) {
		if (n <= 0) {
			throw new IllegalArgumentException();
		}
		return 31 - Integer.numberOfLeadingZeros(n);
	}

	private class BitMask {
		boolean[] ar;
		int n;

		BitMask(int n) {
			this.n = n;
			ar = new boolean[n];
		}

		void next() {
			for (int i = 0; i < n; i++) {
				if (ar[i]) {
					ar[i] = false;
				} else {
					ar[i] = true;
					break;
				}
			}
		}

		boolean get(int i) {
			return ar[i];
		}

		void clear() {
			ar = new boolean[n];
		}
	}

	private int newVariable() {
		return maxVar++;
	}
}
