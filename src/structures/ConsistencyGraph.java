package structures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ConsistencyGraph {
	private Map<Integer, Set<Integer>> edges;
	private APTA apta;
	Map<Integer, Triple> merged = new HashMap<>();
	Map<Integer, Triple> mergedInit = new HashMap<>();
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

			for (int i = 0; i < apta.getSize(); i++) {
				mergedInit.put(i,
						new Triple(i, apta.isAcceptable(i), apta.isRejectable(i)));
			}

			findConsistentEdges(apta.getRoot());
		}
	}

	public Map<Integer, Set<Integer>> getEdges() {
		return edges;
	}

	private void findConsistentEdges(Node node) {
		int thisNumber = node.getNumber();
		for (Entry<String, Node> entry : node.getChildren().entrySet()) {
			findConsistentEdges(entry.getValue());
		}
		// for (Entry<String, Node> entry : node.getChildren().entrySet()) {
		// String label = entry.getKey();
		// Node child = entry.getValue();
		// int childNumber = child.getNumber();
		// boolean isAcceptable = apta.isAcceptable(thisNumber);
		// boolean isRejectable = apta.isRejectable(thisNumber);
		// Set<Integer> inequalityWithChild = new HashSet<>(
		// edges.get(childNumber));
		// for (int i : inequalityWithChild) {
		// Node compareWith = apta.getNode(i);
		// Node parent = compareWith.getParent(label);
		// if (compareWith.getNumber() != childNumber
		// && edges.get(childNumber).contains(
		// compareWith.getNumber()) && parent != null) {
		// edges.get(thisNumber).add(parent.getNumber());
		// edges.get(parent.getNumber()).add(thisNumber);
		// }
		// }
		// Set<Integer> buffer = new HashSet<>();
		// buffer.add(childNumber);
		// boolean isInequality = false;
		// while (child.getChildren().containsKey(label)) {
		// child = child.getChildren().get(label);
		// childNumber = child.getNumber();
		// if (isAcceptable != apta.isAcceptable(childNumber)
		// && isRejectable != apta.isRejectable(childNumber)) {
		// buffer.add(childNumber);
		// isInequality = true;
		// }
		// }
		// if (isInequality) {
		// for (int number : buffer) {
		// edges.get(thisNumber).add(number);
		// edges.get(number).add(thisNumber);
		// }
		// }
		// }

		for (int otherNumber = thisNumber + 1; otherNumber < apta.getSize(); otherNumber++) {
			if (otherNumber != thisNumber) {
				Node other = apta.getNode(otherNumber);
				if (other.isAcceptable() != node.isAcceptable()
						&& other.isRejectable() != node.isRejectable()) {
					// I added this pair when initialized edges[][]
					continue;
				}
				merged = new HashMap<>(mergedInit);
				if (!testMerge(node, other)) {
					edges.get(thisNumber).add(otherNumber);
					edges.get(otherNumber).add(thisNumber);
				}
			}
		}
	}

	// node number < other number
	private boolean testMerge(Node node, Node other) {
		Triple nodeRep = merged.get(node.getNumber());
		while (nodeRep != merged.get(nodeRep.num)) {
			nodeRep = merged.get(nodeRep.num);
		}
		Triple otherRep = merged.get(other.getNumber());
		while (otherRep != merged.get(otherRep.num)) {
			otherRep = merged.get(otherRep.num);
		}
		if (nodeRep.isAcc != otherRep.isAcc && nodeRep.isRej != otherRep.isRej) {
			return false;
		}
		Triple newRep = new Triple(Math.min(nodeRep.num, otherRep.num),
				nodeRep.isAcc || otherRep.isAcc,
				nodeRep.isRej || otherRep.isRej);
		merged.put(nodeRep.num, newRep);
		merged.put(node.getNumber(), newRep);
		merged.put(otherRep.num, newRep);
		merged.put(other.getNumber(), newRep);
		for (String label : node.getChildren().keySet()) {
			if (other.getChildren().containsKey(label)) {
				if (!testMerge(node.getChild(label), other.getChild(label))) {
					return false;
				}
			}
		}
		return true;
	}

	private class Triple {
		int num;
		boolean isAcc;
		boolean isRej;

		public Triple(int num, boolean isAcc, boolean isRej) {
			this.num = num;
			this.isAcc = isAcc;
			this.isRej = isRej;
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
