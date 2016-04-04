package algorithms;

import misc.Settings;
import structures.APTA;
import structures.Automaton;
import structures.Node;

import org.sat4j.specs.TimeoutException;

import java.util.*;

public class BacktrackingSolver {

	private APTA apta;
	private int size;
	private long startTime;
	private long timeout;
	private boolean findAllMode;
	private Set<Automaton> answer;

	public BacktrackingSolver(APTA apta, int size, long timeout) {
		this.apta = apta;
		this.size = size;
		this.startTime = System.currentTimeMillis();
		this.timeout = timeout;
		this.findAllMode = Settings.FIND_ALL_MODE;
	}

	public boolean problemIsBacktrackinging() throws TimeoutException {
		List<Transition> frontier = frontierInit();
		Automaton automaton = new Automaton(size);
		answer = backtracking(automaton, frontier);
		return !answer.isEmpty();
	}

	public Set<Automaton> getAnswer() {
		return answer;
	}

	private Set<Automaton> backtracking(Automaton automaton, List<Transition> frontier) throws TimeoutException {
		Set<Automaton> automatons = new HashSet<>();
		if ((System.currentTimeMillis() - startTime) / 1000. > timeout) {
			throw new TimeoutException();
			//return automatons;
		}
		Transition tr = frontier.get(0);
		for (int destination = 0; destination < size; destination++) {
			if (destination > 1 && automaton.getState(destination - 1).getParents().isEmpty()) {
				break;
			}
			Automaton newAutomaton = new Automaton(automaton);
			newAutomaton.getState(tr.stateFromInDFA).addChild(tr.label, newAutomaton.getState(destination));
			if (newAutomaton.getState(destination).getStatus() == Node.Status.COMMON) {
				newAutomaton.getState(destination).setStatus(tr.status);
			} else {
				if (newAutomaton.getState(destination).isAcceptable() && tr.status == Node.Status.REJECTABLE) {
					continue;
				}
				if (newAutomaton.getState(destination).isRejectable() && tr.status == Node.Status.ACCEPTABLE) {
					continue;
				}
			}

			List<Transition> newFrontier = findNewFrontier(frontier, newAutomaton);
			if (newFrontier != null) {
				if (newFrontier.isEmpty()) {
					automatons.addAll(makeComplete(newAutomaton));
					if (!findAllMode) {
						return automatons;
					}
				} else {
					Set<Automaton> newAutomatons = backtracking(newAutomaton, newFrontier);
					if (!newAutomatons.isEmpty()) {
						if (!findAllMode) {
							return newAutomatons;
						}
						automatons.addAll(newAutomatons);
					}
				}
			}
		}
		return automatons;
	}

	private Set<Automaton> makeComplete(Automaton automaton) {
		Set<Automaton> ans = new HashSet<>();
		Automaton newAutomaton = new Automaton(automaton);
		boolean complete = true;
		for (Node node : newAutomaton.getStates()) {
			for (String label : apta.getAlphabet()) {
				if (node.getChild(label) == null) {
					complete = false;
//					for (int i = 0; i < newAutomaton.size(); i++) {
//						node.addChild(label, newAutomaton.getState(i));q
//						ans.addAll(makeComplete(newAutomaton));
//					}
					node.addChild(label, node);
					ans.add(newAutomaton);
				}
			}
		}
		if (complete) {
			ans.add(newAutomaton);
		}
		return ans;
	}

	private List<Transition> frontierInit() {
		List<Transition> frontier = new ArrayList<>();
		Node root = apta.getRoot();
		String label;
		Node child;
		for (Map.Entry<String, Node> childEntry : root.getChildren().entrySet()) {
			label = childEntry.getKey();
			child = childEntry.getValue();
			frontier.add(new Transition(root.getNumber(), child.getNumber(), label, 0, child.getStatus()));
		}
		return frontier;
	}

	private List<Transition> findNewFrontier(List<Transition> frontier, Automaton automaton) {
		List<Transition> newFrontier = new ArrayList<>();
		List<Transition> newFrontierPart;
		Node fromDFA;
		Node toDFA;
		Node toAPTA;
		for (Transition tr : frontier) {
			fromDFA = automaton.getState(tr.stateFromInDFA);
			toDFA = fromDFA.getChild(tr.label);
			toAPTA = apta.getNode(tr.to);
			if (toDFA != null) {
				if (toDFA.getStatus() == Node.Status.COMMON) {
					toDFA.setStatus(toAPTA.getStatus());
				} else {
					if (toDFA.isAcceptable() && toAPTA.isRejectable()) {
						return null;
					}
					if (toDFA.isRejectable() && toAPTA.isAcceptable()) {
						return null;
					}
				}
				newFrontierPart = findNewFrontierDFS(toAPTA, toDFA);
				if (newFrontierPart == null) {
					return null;
				}
				newFrontier.addAll(newFrontierPart);
			} else {
				newFrontier.add(tr);
			}
		}
		return newFrontier;
	}

	private List<Transition> findNewFrontierDFS(Node APTANode, Node DFANode) {
		List<Transition> frontierPart = new ArrayList<>();
		String label;
		Node APTAto;
		Node DFAto;
		for (Map.Entry<String, Node> APTANodeEntry : APTANode.getChildren().entrySet()) {
			label = APTANodeEntry.getKey();
			APTAto = APTANodeEntry.getValue();
			DFAto = DFANode.getChild(label);
			if (DFAto == null) {
				frontierPart.add(new Transition(APTANode.getNumber(), APTAto.getNumber(), label, DFANode.getNumber(), APTAto.getStatus()));
				continue;
			}
			if (DFAto.getStatus() == Node.Status.COMMON) {
				DFAto.setStatus(APTAto.getStatus());
			} else {
				if (DFAto.isAcceptable() && APTAto.isRejectable()) {
					return null;
				}
				if (DFAto.isRejectable() && APTAto.isAcceptable()) {
					return null;
				}
			}
			List<Transition> recFrontier = findNewFrontierDFS(APTAto, DFAto);
			if (recFrontier == null) {
				return null;
			}
			frontierPart.addAll(recFrontier);
		}
		return frontierPart;
	}

	private class Transition {
		int from;
		int to;
		String label;
		int stateFromInDFA;
		Node.Status status;

		Transition(int from, int to, String label, int stateFromInDFA, Node.Status status) {
			this.from = from;
			this.to = to;
			this.label = label;
			this.stateFromInDFA = stateFromInDFA;
			this.status = status;
		}
	}
}
