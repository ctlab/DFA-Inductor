package algorithms;

import misc.Settings;
import structures.APTA;
import structures.Automaton;
import structures.Node;

import java.util.Map;

public abstract class AutomatonBuilderByY {

    private AutomatonBuilderByY() {}

    public static Automaton build(int[] model, DimacsFileGenerator dfg, APTA apta, int colors) {
        Map<String, Integer>[][] y = dfg.getY();
        Map<String, Integer>[][] y2sink = dfg.getY2sink();
        int[] z = dfg.getZ();

        Automaton automaton = new Automaton(colors, Settings.getSinksAmount());

        for (int from = 0; from < colors; from++) {
            for (int to = 0; to < colors; to++) {
                for (Map.Entry<String, Integer> e : y[from][to].entrySet()) {
                    if (model[e.getValue() - 1] > 0) {
                        automaton.addTransition(from, to, e.getKey());
                    }
                }
            }
        }

        for (int from = 0; from < colors; from++) {
            for (int to = 0; to < Settings.getSinksAmount(); to++) {
                for (Map.Entry<String, Integer> e : y2sink[from][to].entrySet()){
                    if (model[e.getValue() - 1] > 0) {
                        automaton.addTransition2Sink(from, to, e.getKey());
                    }
                }
            }
        }

        for (int i = 0; i < colors; i++) {
            if (model[z[i] - 1] > 0) {
                automaton.getState(i).setStatus(Node.Status.ACCEPTABLE);
            } else {
                automaton.getState(i).setStatus(Node.Status.REJECTABLE);
            }
        }

        for (int i = 0; i < Settings.getSinksAmount(); i++) {
            automaton.getSink(i).setStatus(Node.Status.SINK);
        }

        return automaton;
    }
}
