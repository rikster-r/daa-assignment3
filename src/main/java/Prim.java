import java.io.*;
import java.util.*;
import org.json.*;

public class Prim {
    static class Edge implements Comparable<Edge> {
        String from;
        String to;
        int weight;

        Edge(String from, String to, int weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        @Override
        public int compareTo(Edge other) {
            return this.weight - other.weight;
        }
    }

    static class PrimResult {
        List<Edge> mstEdges;
        int totalCost;
        int operationsCount;
        double executionTimeMs;

        PrimResult(List<Edge> mstEdges, int totalCost, int operationsCount, double executionTimeMs) {
            this.mstEdges = mstEdges;
            this.totalCost = totalCost;
            this.operationsCount = operationsCount;
            this.executionTimeMs = executionTimeMs;
        }
    }

    static PrimResult prim(List<String> nodes, List<Edge> edges) {
        long startTime = System.nanoTime();

        List<Edge> mstEdges = new ArrayList<>();
        int totalCost = 0;
        int operationsCount = 0;

        // Build adjacency list
        Map<String, List<Edge>> graph = new HashMap<>();
        for (String node : nodes) {
            graph.put(node, new ArrayList<>());
        }
        for (Edge edge : edges) {
            graph.get(edge.from).add(edge);
            graph.get(edge.to).add(new Edge(edge.to, edge.from, edge.weight));
            operationsCount++; // Count graph construction
        }

        // Priority queue to store edges with minimum weight
        PriorityQueue<Edge> pq = new PriorityQueue<>();
        Set<String> visited = new HashSet<>();

        // Start from the first node
        String startNode = nodes.get(0);
        visited.add(startNode);

        // Add all edges from start node
        for (Edge edge : graph.get(startNode)) {
            pq.offer(edge);
            operationsCount++; // Count edge insertion
        }

        // Build MST
        while (!pq.isEmpty() && visited.size() < nodes.size()) {
            Edge minEdge = pq.poll();
            operationsCount++; // Count edge extraction

            // Skip if destination already visited
            if (visited.contains(minEdge.to)) {
                continue;
            }

            // Add edge to MST
            mstEdges.add(minEdge);
            totalCost += minEdge.weight;
            visited.add(minEdge.to);

            // Add all edges from newly added vertex
            for (Edge edge : graph.get(minEdge.to)) {
                operationsCount++; // Count edge consideration
                if (!visited.contains(edge.to)) {
                    pq.offer(edge);
                }
            }
        }

        long endTime = System.nanoTime();
        double executionTimeMs = (endTime - startTime) / 1_000_000.0;

        return new PrimResult(mstEdges, totalCost, operationsCount, executionTimeMs);
    }

    public static void main(String[] args) {
        try {
            // Read input JSON file
            String inputContent = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get("input.json")));
            JSONObject inputJson = new JSONObject(inputContent);
            JSONArray graphs = inputJson.getJSONArray("graphs");

            JSONObject outputJson = new JSONObject();
            JSONArray results = new JSONArray();

            // Process each graph
            for (int i = 0; i < graphs.length(); i++) {
                JSONObject graph = graphs.getJSONObject(i);
                int graphId = graph.getInt("id");

                // Parse nodes
                JSONArray nodesArray = graph.getJSONArray("nodes");
                List<String> nodes = new ArrayList<>();
                for (int j = 0; j < nodesArray.length(); j++) {
                    nodes.add(nodesArray.getString(j));
                }

                // Parse edges
                JSONArray edgesArray = graph.getJSONArray("edges");
                List<Edge> edges = new ArrayList<>();
                for (int j = 0; j < edgesArray.length(); j++) {
                    JSONObject edgeObj = edgesArray.getJSONObject(j);
                    edges.add(new Edge(
                            edgeObj.getString("from"),
                            edgeObj.getString("to"),
                            edgeObj.getInt("weight")
                    ));
                }

                // Run Prim's algorithm
                PrimResult primResult = prim(nodes, edges);

                // Build result JSON
                JSONObject result = new JSONObject();
                result.put("graph_id", graphId);

                JSONObject inputStats = new JSONObject();
                inputStats.put("vertices", nodes.size());
                inputStats.put("edges", edges.size());
                result.put("input_stats", inputStats);

                JSONObject primJson = new JSONObject();
                JSONArray mstEdgesArray = new JSONArray();
                for (Edge edge : primResult.mstEdges) {
                    JSONObject edgeJson = new JSONObject();
                    edgeJson.put("from", edge.from);
                    edgeJson.put("to", edge.to);
                    edgeJson.put("weight", edge.weight);
                    mstEdgesArray.put(edgeJson);
                }
                primJson.put("mst_edges", mstEdgesArray);
                primJson.put("total_cost", primResult.totalCost);
                primJson.put("operations_count", primResult.operationsCount);
                primJson.put("execution_time_ms",
                        Math.round(primResult.executionTimeMs * 100.0) / 100.0);

                result.put("prim", primJson);
                results.put(result);
            }

            outputJson.put("results", results);

            // Write output JSON file
            try (FileWriter file = new FileWriter("output_prim.json")) {
                file.write(outputJson.toString(4));
                System.out.println("Results written to output_prim.json");
            }

            // Print summary to console
            System.out.println("\n=== MST Results (Prim's Algorithm) ===");
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                System.out.println("\nGraph " + result.getInt("graph_id") + ":");
                System.out.println("  Vertices: " + result.getJSONObject("input_stats").getInt("vertices"));
                System.out.println("  Edges: " + result.getJSONObject("input_stats").getInt("edges"));

                JSONObject prim = result.getJSONObject("prim");
                System.out.println("  Total Cost: " + prim.getInt("total_cost"));
                System.out.println("  Operations: " + prim.getInt("operations_count"));
                System.out.println("  Execution Time: " + prim.getDouble("execution_time_ms") + " ms");
                System.out.println("  MST Edges:");
                JSONArray mstEdges = prim.getJSONArray("mst_edges");
                for (int j = 0; j < mstEdges.length(); j++) {
                    JSONObject edge = mstEdges.getJSONObject(j);
                    System.out.println("    " + edge.getString("from") + " -> " +
                            edge.getString("to") + " (weight: " + edge.getInt("weight") + ")");
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}