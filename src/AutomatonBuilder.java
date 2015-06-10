import java.util.HashMap;
import java.util.Map;

public class AutomatonBuilder {

	public static Automaton build(int[] model, DimacsFileGenerator dfg, APTA apta, int colors, boolean noisyMode) {
		int vertices = apta.getSize();
		int[][] x = dfg.getX();
		Map<Integer, Integer> f = null;
		if (noisyMode) {
			f = dfg.getF();
		}
		Automaton automaton = new Automaton(colors);
		// map[vertex][color]
		Map<Integer, Integer> colorsOfNodes = new HashMap<>();
		for (int i = 0; i < colors; i++) {
			for (int v = 0; v < vertices; v++) {
				if (model[x[v][i] - 1] > 0) {
					colorsOfNodes.put(v, i);
				}
			}
		}
		if (colorsOfNodes.get(0) != 0) {
			int changeColor = colorsOfNodes.get(0);
			for (int v = 0; v < vertices; v++) {
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

			for (Map.Entry<String, Node> entry : apta.getNode(vertex).getChildren()
					.entrySet()) {
				String label = entry.getKey();
				int to = entry.getValue().getNumber();
				automaton.addTransition(color, colorsOfNodes.get(to), label);
			}
		}
		return automaton;
	}

}
