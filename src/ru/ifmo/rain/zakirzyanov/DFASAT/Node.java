package ru.ifmo.rain.zakirzyanov.DFASAT;

import java.util.HashMap;
import java.util.Map;

public class Node {
	private int number;
	private Map<String, Node> children;
	private Map<String, Node> parents;
	//1 - reject, 0 - doesn't matter, 1 - accept
	private int status;
	
	public Node(int number) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.status = 0;
	}
	
	public Node(int number, int status) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.status = status;
	}
	
	public Node(int number, String label, Node father) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.parents.put(label, father);
		this.status = 0;
	}
	
	public Node(int number, String label, Node father, int status) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.parents.put(label, father);
		this.status = status;
	}
	
	public int getNumber() {
		return number;
	}
	
	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}

	public Map<String, Node> getParents() {
		return parents;
	}
	
	public Node getParent(String s) {
		if (parents.containsKey(s)) {
			return parents.get(s);
		} else {
			return null;
		}
	}
	
	public Map<String, Node> getChildren() {
		return children;
	}
	
	public void addChild(String s, Node child) {
		children.put(s, child);
	}
	
	public void addParent(String s, Node child) {
		parents.put(s, child);
	}
}
