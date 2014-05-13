package ru.ifmo.rain.zakirzyanov.DFASAT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javafx.animation.Transition;

public class Automat {
	private Node start;
	List<Node> states;
	
	public Automat() {
		this.start = new Node(0);
		this.states = new ArrayList<>();
		this.states.add(start);
	}
	
	public Automat(int size) {
		int cur = 0;
		this.start = new Node(cur++);
		this.states = new ArrayList<>();
		this.states.add(start);
		
		while (cur < size) {
			this.states.add(new Node(cur++));
		}
	}
	
	public Node getStart() {
		return start;
	}

	public Node getState(int i) {
		return states.get(i);
	}
	
	public List<Node> getStates() {
		return states;
	}
	
	public int size() {
		return states.size();
	}
	
	public void addTransition(int from, int to, String label) {
		Node fromNode = states.get(from);
		Node toNode = states.get(to);
		
		fromNode.addChild(label, toNode);
		toNode.addParent(label, fromNode);
	}
	
	public void addChildren(int num, Map<String, Node> children) {
		Node numNode = states.get(num);
		numNode.getChildren().putAll(children);
	}
	
	public void addParents(int num, Map<String, Node> children) {
		Node numNode = states.get(num);
		numNode.getParents().putAll(children);
	}
	
	public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("digraph Automat {\n");
        s.append("    node [shape = circle];\n");
        s.append("    0 [style = \"bold\"];\n");

        for (Node state : states) {
        	for (Entry<String, Node> e : state.getChildren().entrySet()) {
        		s.append("    ");
        		s.append(state.getNumber());
        		s.append(" -> ");
        		s.append(e.getValue().getNumber());
        		s.append(" [label = \"");
        		s.append(e.getKey());
        		s.append("\"];\n");
        	}
        }
        s.append("}");
        
        return s.toString();
    }
}
