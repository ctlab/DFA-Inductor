package ru.ifmo.rain.zakirzyanov.DFAInductor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ConsistencyGraph {
	private Map<Integer, Set<Integer>> edges;
	private APTA apta;

	public ConsistencyGraph(APTA apta) {
		edges = new HashMap<>();
		this.apta = apta;

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

		findConsistentEdges(apta.getRoot());
	}

	public Map<Integer, Set<Integer>> getEdges() {
		return edges;
	}

	private void findConsistentEdges(Node node) {
		int thisNumber = node.getNumber();
		for (Entry<String, Node> entry : node.getChildren().entrySet()) {
			findConsistentEdges(entry.getValue());
		}
		for (Entry<String, Node> entry : node.getChildren().entrySet()) {
			String label = entry.getKey();
			Node child = entry.getValue();
			int childNumber = child.getNumber();
			boolean isAcceptable = apta.isAcceptable(thisNumber);
			boolean isRejectable = apta.isRejectable(thisNumber);
			Set<Integer> inequalityWithChild = new HashSet<>(
					edges.get(childNumber));
			for (int i : inequalityWithChild) {
				Node compareWith = apta.getNode(i);
				Node parent = compareWith.getParent(label);
				if (compareWith.getNumber() != childNumber
						&& edges.get(childNumber).contains(
								compareWith.getNumber()) && parent != null) {
					edges.get(thisNumber).add(parent.getNumber());
					edges.get(parent.getNumber()).add(thisNumber);
				}
			}
			Set<Integer> buffer = new HashSet<>();
			buffer.add(childNumber);
			boolean isInequality = false;
			while (child.getChildren().containsKey(label)) {
				child = child.getChildren().get(label);
				childNumber = child.getNumber();
				if (isAcceptable != apta.isAcceptable(childNumber)
						&& isRejectable != apta.isRejectable(childNumber)) {
					buffer.add(childNumber);
					isInequality = true;
				}
			}
			if (isInequality) {
				for (int number : buffer) {
					edges.get(thisNumber).add(number);
					edges.get(number).add(thisNumber);
				}
			}
		}
	}
}
