/*******************************************************************************
 * This file is part of the AbcDatalog project.
 *
 * Copyright (c) 2016, Harvard University
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the BSD License which accompanies this distribution.
 *
 * The development of the AbcDatalog project has been supported by the 
 * National Science Foundation under Grant Nos. 1237235 and 1054172.
 *
 * See README for contributors.
 ******************************************************************************/
package abcdatalog.util.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import abcdatalog.util.Box;

public class Digraph<V, E extends DirectedEdge<V>> {
	private Map<V, List<E>> graph = new HashMap<>();
	
	public void addEdge(E edge) {
		List<E> neighbors = this.graph.get(edge.getSource());
		if (neighbors == null) {
			neighbors = new LinkedList<>();
			this.graph.put(edge.getSource(), neighbors);
		}
		neighbors.add(edge);
	}
	
	public void addVertex(V vertex) {
		List<E> neighbors = this.graph.get(vertex);
		if (neighbors == null) {
			neighbors = new LinkedList<>();
			this.graph.put(vertex, neighbors);
		}
	}
	
	public Iterable<E> getOutgoingEdges(V source) {
		List<E> outgoing = this.graph.get(source);
		if (outgoing == null) {
			return Collections.emptyList();
		}
		return outgoing;
	}
	
	public Set<V> getVertices() {
		return this.graph.keySet();
	}
	
	public Digraph<V, E> getTranspose(Function<E, E> reverseEdge) {
		Digraph<V, E> t = new Digraph<>();
		for (Map.Entry<V, List<E>> e : this.graph.entrySet()) {
			t.addVertex(e.getKey());
			for (E edge : e.getValue()) {
				t.addEdge(reverseEdge.apply(edge));
			}
		}
		return t;
	}
	
	public List<Set<V>> getStronglyConnectedComponents(Function<E, E> reverseEdge) {
		Box<Integer> time = new Box<>();
		Set<V> visited = new HashSet<>();
		Map<V, Integer> finishingTimes = new HashMap<>();
		Box<Consumer<V>> dfs = new Box<>();
		dfs.value = v -> {
			++time.value;
			visited.add(v);
			for (E edge : this.getOutgoingEdges(v)) {
				V dest = edge.getDest();
				if (!visited.contains(dest)) {
					dfs.value.accept(dest);
				}
			}
			finishingTimes.put(v, ++time.value);
		};
		
		time.value = 0;
		for (V vertex : this.getVertices()) {
			if (!visited.contains(vertex)) {
				dfs.value.accept(vertex);
			}
		}
		
		Digraph<V, E> transpose = this.getTranspose(reverseEdge);
		Box<BiConsumer<V, Set<V>>> dfs2 = new Box<>();
		dfs2.value = (v, curComponent) -> {
			visited.add(v);
			curComponent.add(v);
			for (E edge : transpose.getOutgoingEdges(v)) {
				V dest = edge.getDest();
				if (!visited.contains(dest)) {
					dfs2.value.accept(dest, curComponent);
				}
			}
		};
		
		List<V> orderedVertices = new ArrayList<>();
		finishingTimes.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.forEachOrdered(e -> orderedVertices.add(e.getKey()));
		List<Set<V>> components = new ArrayList<>();
		visited.clear();
		for (V vertex : orderedVertices) {
			if (!visited.contains(vertex)) {
				Set<V> curComponent = new HashSet<>();
				dfs2.value.accept(vertex, curComponent);
				components.add(curComponent);
			}
		}
		
		return components;
	}
	
	public static void main(String[] args) {
		class CharEdge implements DirectedEdge<Character> {
			private final char source;
			private final char dest;
			
			public CharEdge(char source, char dest) {
				this.source = source;
				this.dest = dest;
			}

			@Override
			public Character getSource() {
				return this.source;
			}

			@Override
			public Character getDest() {
				return this.dest;
			}
			
			public CharEdge reverse() {
				return new CharEdge(this.dest, this.source);
			}
			
		}
		
		Digraph<Character, CharEdge> graph = new Digraph<>();
		graph.addEdge(new CharEdge('a', 'b'));
		graph.addEdge(new CharEdge('b', 'c'));
		graph.addEdge(new CharEdge('b', 'e'));
		graph.addEdge(new CharEdge('b', 'f'));
		graph.addEdge(new CharEdge('c', 'd'));
		graph.addEdge(new CharEdge('c', 'g'));
		graph.addEdge(new CharEdge('d', 'c'));
		graph.addEdge(new CharEdge('d', 'h'));
		graph.addEdge(new CharEdge('e', 'a'));
		graph.addEdge(new CharEdge('e', 'f'));
		graph.addEdge(new CharEdge('f', 'g'));
		graph.addEdge(new CharEdge('g', 'f'));
		graph.addEdge(new CharEdge('g', 'h'));
		graph.addEdge(new CharEdge('h', 'h'));
		
		// Components (in topological order) should be:
		// [a, b, e]
		// [c, d]
		// [f, g]
		// [h]
		List<Set<Character>> components = graph.getStronglyConnectedComponents(e -> e.reverse());
		for (Set<Character> component : components) {
			System.out.println(component);
		}
	}
}
