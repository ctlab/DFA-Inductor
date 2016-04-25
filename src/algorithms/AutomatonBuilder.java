package algorithms;

import misc.Settings;
import structures.APTA;
import structures.Automaton;
import structures.Node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AutomatonBuilder {

	private AutomatonBuilder() {}

	public static Automaton build(int[] model, DimacsFileGenerator dfg, APTA apta, int colors) {
		Set<Node> redNodes = apta.getRedNodes();
		Set<Node> notRedNotSinkNodes = apta.getNotRedNotSinkNodes();

		Map<Integer, int[]> x = dfg.getX();
		Map<Integer, Integer> sinks = dfg.getSinks();
		Map<Integer, Integer> f = null;
		if (Settings.NOISY_MODE) {
			f = dfg.getF();
		}
		Automaton automaton = new Automaton(colors, Settings.getSinksAmount());
		// map[vertex][color]
		Map<Integer, Integer> colorsOfNodes = new HashMap<>();
		Set<Integer> sinksInAutomaton = new HashSet<>();

		for (Node node : redNodes) {
			for (int i = 0; i < colors; i++) {
				if (model[x.get(node.getNumber())[i] - 1] > 0) {
					colorsOfNodes.put(node.getNumber(), i);
				}
			}
		}
		for (Node node : notRedNotSinkNodes) {
			for (int i = 0; i < colors; i++) {
				if (model[x.get(node.getNumber())[i]] > 0) {
					colorsOfNodes.put(node.getNumber(), i);
				}
				if (model[sinks.get(node.getNumber()) - 1] > 0) {
					sinksInAutomaton.add(node.getNumber());
				}
			}
		}

		if (colorsOfNodes.get(0) != 0) {
			int changeColor = colorsOfNodes.get(0);
			for (int v : colorsOfNodes.keySet()) {
				if (colorsOfNodes.get(v) == changeColor) {
					colorsOfNodes.put(v, 0);
				} else if (colorsOfNodes.get(v) == 0) {
					colorsOfNodes.put(v, changeColor);
				}
			}
		}

		for (Map.Entry<Integer, Integer> e : colorsOfNodes.entrySet()) {
			int vertex = e.getKey();
			int color = e.getValue();
			Node vertexNode = apta.getNode(vertex);
			if (vertexNode.isAcceptable() && !(f != null && model[f.get(vertex) - 1] > 0)) {
				automaton.getState(color).setStatus(Node.Status.ACCEPTABLE);
			} else if (vertexNode.isRejectable() && !(f != null && model[f.get(vertex) - 1] > 0)) {
				automaton.getState(color).setStatus(Node.Status.REJECTABLE);
			}
			if (sinksInAutomaton.contains(vertex)) {
				automaton.getState(color).setStatus(Node.Status.SINK);
			}

			for (Map.Entry<String, Node> entry : apta.getNode(vertex).getChildren()
					.entrySet()) {
				String label = entry.getKey();
				if (entry.getValue().getSinkType() == Node.SINK_TYPE.NON_SINK) {
					int to = entry.getValue().getNumber();
					if (colorsOfNodes.get(to) != null) {
						automaton.addTransition(color, colorsOfNodes.get(to), label);
					}
				} else {
					for (int sinkNumber : sinksInAutomaton) {
						if (entry.getValue().getSinkType() == apta.getNode(sinkNumber).getSinkType()) {
							automaton.addTransition2Sink(color, entry.getValue().getSinkNumber(), label);
						}
					}
				}
			}
		}
		return automaton;
	}

}
