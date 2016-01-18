package structures;

import algorithms.CGStateMerger;
import algorithms.StateMerger;

import java.util.*;

public class ConsistencyGraph {
	private Map<Integer, Set<Integer>> edges;
	private APTA apta;
	private Set<Integer> acceptableClique;
	private Set<Integer> rejectableClique;

	public ConsistencyGraph(APTA apta, boolean noisyMode) {
		edges = new HashMap<>();
		this.apta = apta;

		if (!noisyMode) {
			for (int i = 0; i < apta.getSize(); i++) {
				edges.put(i, new HashSet<Integer>());
			}
			Set<Integer> acceptableNodes = apta.getAcceptableNodes();
			Set<Integer> rejectableNodes = apta.getRejectableNodes();

			for (int i : acceptableNodes) {
				for (int j : rejectableNodes) {
					edges.get(i).add(j);
					edges.get(j).add(i);
				}
			}

			for (Node first : apta.getRedNodes()) {
				for (Node second : apta.getRedNodes()) {
					if (first.getNumber() <= second.getNumber()) {
						continue;
					}
					edges.get(first.getNumber()).add(second.getNumber());
					edges.get(second.getNumber()).add(first.getNumber());
				}
			}

			ConsistencyGraphWorker cgWorker = new ConsistencyGraphWorker();

			for (Node red : apta.getRedNodes()) {
				for (Node notRed : apta.getNotRedNodes()) {
					if (red.isAcceptable() && notRed.isRejectable() || red.isRejectable() && notRed.isAcceptable()) {
						continue;
					}
					if (cgWorker.mergeAndUndo(red, notRed) < 0) {
						edges.get(red.getNumber()).add(notRed.getNumber());
						edges.get(notRed.getNumber()).add(red.getNumber());
					}
				}
			}

			for (Node notRed1 : apta.getNotRedNodes()) {
				for (Node notRed2 : apta.getNotRedNodes()) {
					if (notRed1.getNumber() <= notRed2.getNumber()) {
						continue;
					}
					if (notRed1.isAcceptable() && notRed2.isRejectable() || notRed1.isRejectable() && notRed2.isAcceptable()) {
						continue;
					}
					if (cgWorker.mergeAndUndo(notRed1, notRed2) < 0) {
						edges.get(notRed1.getNumber()).add(notRed2.getNumber());
						edges.get(notRed2.getNumber()).add(notRed1.getNumber());
					}
				}
			}
		}
	}

	public Map<Integer, Set<Integer>> getEdges() {
		return edges;
	}

	private class ConsistencyGraphWorker {
		private StateMerger merger;

		public ConsistencyGraphWorker() {
			merger = new CGStateMerger(apta);
		}

		private int mergeAndUndo(Node red, Node blue) {
			merger.resetScore();
			merger.merge(red, blue, false);
			int res = merger.getScore();
			merger.undoMerge(red, blue);
			return res;
		}
	}

	public void findClique() {
		int maxDegree = 0;
		int maxV = -1;
		acceptableClique = new HashSet<>();
		for (int candidate : apta.getAcceptableNodes()) {
			int candidateDegree = getEdges().get(candidate).size();
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
				if (last == anotherOne) {
					break;
				}
			}
		}

		maxDegree = 0;
		maxV = -1;
		rejectableClique = new HashSet<>();
		for (int candidate : apta.getRejectableNodes()) {
			int candidateDegree = getEdges().get(candidate).size();
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
				if (last == anotherOne) {
					break;
				}
			}
		}
	}

	private int findNeighbourWithHighestDegree(Set<Integer> cur, int v, boolean acceptable) {
		int maxDegree = 0;
		int maxNeighbour = -1;
		// uv - edge
		for (int u : getEdges().get(v)) {
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
					if (!getEdges().get(w).contains(u)) {
						uInClique = false;
						break;
					}
				}
			}
			if (uInClique) {
				int uDegree = getEdges().get(u).size();
				if (uDegree > maxDegree) {
					maxDegree = uDegree;
					maxNeighbour = u;
				}
			}
		}
		return maxNeighbour;
	}

	public Set<Integer> getAcceptableClique() {
		return acceptableClique;
	}

	public Set<Integer> getRejectableClique() {
		return rejectableClique;
	}

	public int getCliqueSize() {
		return acceptableClique.size() + rejectableClique.size();
	}
}
