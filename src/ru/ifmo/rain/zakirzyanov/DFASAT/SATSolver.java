package ru.ifmo.rain.zakirzyanov.DFASAT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
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

	public SATSolver(APTA apta, ConsistencyGraph cg, int colors)
			throws ContradictionException {
		this.apta = apta;
		this.cg = cg;
		this.colors = colors;
		this.maxVar = 1;
		this.vertices = apta.getSize();
		this.x = new int[vertices][colors];
		this.y = new HashMap<String, int[][]>();
		this.z = new int[colors];
		problem = build();
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
		return problem.nVars();
	}

	public int nConstraints() {
		return problem.nConstraints();
	}

	public boolean problemIsSatisfiable() throws TimeoutException {
		return problem.isSatisfiable();
	}

	// must be called after problemIsSatisfiable
	public Automat getModel() {
		Automat automat = new Automat(colors);
		// map[vertex][color]
		Map<Integer, Integer> colorsOfNodes = new HashMap<>();
		int[] model = problem.model();
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

	private ISolver build() throws ContradictionException {
		ISolver solver = SolverFactory.newDefault();
		List<VecInt> forAdding;

		forAdding = getOneAtLeast();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getAccVertDiffColorRej();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getParrentRelationIsSet();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getParrentRelationAtMostOneColor();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getOneAtMost();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getParrentRelationAtLeastOneColor();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getParrentRelationForces();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		forAdding = getConflictsFromCG();
		for (VecInt vecInt : forAdding) {
			solver.addClause(vecInt);
		}

		return solver;
	}

	// Each vertex has at least one color.
	// x_{v,1} or x_{v,2} or ... or x_{v, |C|}
	private List<VecInt> getOneAtLeast() {
		List<VecInt> ans = new ArrayList<>();

		for (int v = 0; v < vertices; v++) {
			VecInt vecInt = new VecInt();
			for (int i = 0; i < colors; i++) {
				x[v][i] = maxVar++;
				vecInt.push(x[v][i]);
			}
			ans.add(vecInt);
		}

		VecInt tmp = new VecInt();
		tmp.push(x[0][0]);
		ans.add(tmp);

		return ans;
	}

	// Accepting vertices cannot have the same color as rejecting vertices
	// (!x_{v,i} or z_i) and (!x_{w,i} or !z_i), where v is acc, w is rej
	private List<VecInt> getAccVertDiffColorRej() {
		List<VecInt> ans = new ArrayList<>();

		for (int i = 0; i < colors; i++) {
			z[i] = maxVar++;
			for (Integer acc : apta.getAcceptableNodes()) {
				VecInt vecInt = new VecInt();
				vecInt.push(-x[acc][i]);
				vecInt.push(z[i]);
				ans.add(vecInt);
			}
			for (Integer rej : apta.getRejectableNodes()) {
				VecInt vecInt = new VecInt();
				vecInt.push(-x[rej][i]);
				vecInt.push(-z[i]);
				ans.add(vecInt);
			}
		}

		return ans;
	}

	// A parent relation is set when a vertex and its parent are colored
	// (y_{l(v),i,j} or !x_{p(v),i} or !x_{v,i})
	private List<VecInt> getParrentRelationIsSet() {
		List<VecInt> ans = new ArrayList<>();

		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						if (!y.containsKey(e.getKey())) {
							y.put(e.getKey(), new int[colors][colors]);
						}
						y.get(e.getKey())[i][j] = maxVar++;
						VecInt vecInt = new VecInt();
						vecInt.push(y.get(e.getKey())[i][j]);
						vecInt.push(-x[e.getValue().getNumber()][i]);
						vecInt.push(-x[v][j]);
						ans.add(vecInt);
					}
				}
			}
		}

		return ans;
	}

	// each parent relation can target at most one color
	// (!y_{a,i,h} or !y_{a,i,j}) where a in Alphabet, h < j
	private List<VecInt> getParrentRelationAtMostOneColor() {
		List<VecInt> ans = new ArrayList<>();

		for (String s : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					for (int h = 0; h < j; h++) {
						VecInt vecInt = new VecInt();
						vecInt.push(-y.get(s)[i][h]);
						vecInt.push(-y.get(s)[i][j]);
						ans.add(vecInt);
					}
				}
			}
		}

		return ans;
	}

	// each vertex has at most one color
	// (!x_{v,i} or !x_{v,j}) where i < j
	private List<VecInt> getOneAtMost() {
		List<VecInt> ans = new ArrayList<>();

		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = i + 1; j < colors; j++) {
					VecInt vecInt = new VecInt();
					vecInt.push(-x[v][i]);
					vecInt.push(-x[v][j]);
					ans.add(vecInt);
				}
			}
		}

		return ans;
	}

	// each parent relation must target at least one color
	// (!y_{a,i,h} or !y_{a,i,j}) where a in Alphabet, h < j
	private List<VecInt> getParrentRelationAtLeastOneColor() {
		List<VecInt> ans = new ArrayList<>();

		for (String s : apta.getAlphabet()) {
			for (int i = 0; i < colors; i++) {
				VecInt vecInt = new VecInt();
				for (int j = 0; j < colors; j++) {
					vecInt.push(y.get(s)[i][j]);
				}
				ans.add(vecInt);
			}
		}

		return ans;
	}

	// a parent relation forces a vertex once the parent is colored
	// (!y_{l(v),i,j} or !x_{p(v),i} or x_{v,i})
	private List<VecInt> getParrentRelationForces() {
		List<VecInt> ans = new ArrayList<>();

		for (int v = 0; v < vertices; v++) {
			for (int i = 0; i < colors; i++) {
				for (int j = 0; j < colors; j++) {
					Node cur = apta.getNode(v);
					for (Entry<String, Node> e : cur.getParents().entrySet()) {
						VecInt vecInt = new VecInt();
						vecInt.push(-y.get(e.getKey())[i][j]);
						vecInt.push(-x[e.getValue().getNumber()][i]);
						vecInt.push(x[v][j]);
						ans.add(vecInt);
					}
				}
			}
		}

		return ans;
	}

	// all determinization conflicts explicitly added as clauses
	// (!x_{v,i} or !x_{w,i}) where (v,w) - edge from cg
	private List<VecInt> getConflictsFromCG() {
		List<VecInt> ans = new ArrayList<>();

		for (Entry<Integer, Set<Integer>> e : cg.getEdges().entrySet()) {
			int v = e.getKey();
			for (int w : e.getValue()) {
				if (w > v) {
					continue;
				}
				for (int i = 0; i < colors; i++) {
					VecInt vecInt = new VecInt();
					vecInt.push(-x[v][i]);
					vecInt.push(-x[w][i]);
					ans.add(vecInt);
				}
			}
		}

		return ans;
	}

}
