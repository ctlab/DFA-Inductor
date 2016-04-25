package structures;

import algorithms.CGStateMerger;
import algorithms.StateMerger;
import misc.Settings;

import java.util.*;

public class ConsistencyGraph {
	private Map<Integer, Set<Integer>> RBedges;
	private Map<Integer, Set<Integer>> BBedges;
	private APTA apta;
	private Set<Integer> acceptableClique;
	private Set<Integer> rejectableClique;
	private Set<Integer> commonClique;

	public ConsistencyGraph(APTA apta) {
		RBedges = new HashMap<>();
		BBedges = new HashMap<>();
		this.apta = apta;

		if (!Settings.NOISY_MODE) {
			for (int i = 0; i < apta.getRealSize(); i++) {
				RBedges.put(i, new HashSet<Integer>());
				BBedges.put(i, new HashSet<Integer>());
			}
			Set<Integer> acceptableNodes = apta.getAcceptableNodes();
			Set<Integer> rejectableNodes = apta.getRejectableNodes();

//			for (Node first : apta.getRedNodes()) {
//				for (Node second : apta.getRedNodes()) {
//					if (first.getNumber() <= second.getNumber()) {
//						continue;
//					}
//					edges.get(first.getNumber()).add(second.getNumber());
//					edges.get(second.getNumber()).add(first.getNumber());
//				}
//			}
//
			ConsistencyGraphWorker cgWorker = new ConsistencyGraphWorker();
//
//			for (Node red : apta.getRedNodes()) {
//				for (Node notRed : apta.getNotRedNodes()) {
//					if (red.isAcceptable() && notRed.isRejectable() || red.isRejectable() && notRed.isAcceptable()) {
//						continue;
//					}
//					if (cgWorker.mergeAndUndo(red, notRed) < 0) {
//						edges.get(red.getNumber()).add(notRed.getNumber());
//						edges.get(notRed.getNumber()).add(red.getNumber());
//					}
//				}
//			}
			for (Node red : apta.getRedNodes()) {
				for (Node notRed : apta.getNotRedNotSinkNodes()) {
					if (red.getNumber() >= notRed.getNumber()) {
						continue;
					}
					if (red.isAcceptable() && notRed.isRejectable() || red.isRejectable() && notRed.isAcceptable() || cgWorker.mergeAndUndo(red, notRed) < 0) {
						RBedges.get(red.getNumber()).add(notRed.getNumber());
						RBedges.get(notRed.getNumber()).add(red.getNumber());
					}
				}
			}

			for (Node notRed1 : apta.getNotRedNotSinkNodes()) {
				for (Node notRed2 : apta.getNotRedNotSinkNodes()) {
					if (notRed1.getNumber() >= notRed2.getNumber()) {
						continue;
					}
					if (notRed1.isAcceptable() && notRed2.isRejectable() || notRed1.isRejectable() && notRed2.isAcceptable() || cgWorker.mergeAndUndo(notRed1, notRed2) < 0) {
						BBedges.get(notRed1.getNumber()).add(notRed2.getNumber());
						BBedges.get(notRed2.getNumber()).add(notRed1.getNumber());
					}
				}
			}
		}
	}

	public Map<Integer, Set<Integer>> getRBEdges() {
		return RBedges;
	}

	public Map<Integer, Set<Integer>> getBBEdges() {
		return BBedges;
	}

	public Map<Integer, Set<Integer>> getEdges() {
		Map<Integer, Set<Integer>> map = new HashMap<>();
		map.putAll(BBedges);
		map.putAll(RBedges);
		return map;
	}

	private class ConsistencyGraphWorker {
		private StateMerger merger;

		public ConsistencyGraphWorker() {
			merger = new CGStateMerger(apta);
		}

		private int mergeAndUndo(Node red, Node blue) {
			merger.resetScore();
			merger.merge(red, blue);
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
		commonClique = new HashSet<>();
		if (apta.getRedNodes().size() > acceptableClique.size() + rejectableClique.size()) {
			acceptableClique.clear();
			rejectableClique.clear();
			for (Node node : apta.getRedNodes()) {
				switch (node.getStatus()) {
					case ACCEPTABLE:
						acceptableClique.add(node.getNumber());
						break;
					case COMMON:
						commonClique.add(node.getNumber());
						break;
					case REJECTABLE:
						rejectableClique.add(node.getNumber());
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

	public Set<Integer> getCommonClique() {
		return commonClique;
	}

	public int getCliqueSize() {
		return acceptableClique.size() + rejectableClique.size() + commonClique.size();
	}
}
