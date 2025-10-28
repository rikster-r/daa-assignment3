import java.io.*;
import java.util.*;
import org.json.*;

public class Kruskal {
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

    static class UnionFind {
        Map<String, String> parent;
        Map<String, Integer> rank;
        int operationsCount;

        UnionFind(List<String> nodes) {
            parent = new HashMap<>();
            rank = new HashMap<>();
            operationsCount = 0;

            for (String node : nodes) {
                parent.put(node, node);
                rank.put(node, 0);
            }
        }

        String find(String node) {
            operationsCount++; // Count find operation
            if (!parent.get(node).equals(node)) {
                parent.put(node, find(parent.get(node))); // Path compression
            }
            return parent.get(node);
        }

        boolean union(String node1, String node2) {
            operationsCount++; // Count union operation
            String root1 = find(node1);
            String root2 = find(node2);

            if (root1.equals(root2)) {
                return false; // Already in same set
            }

            // Union by rank
            int rank1 = rank.get(root1);
            int rank2 = rank.get(root2);

            if (rank1 < rank2) {
                parent.put(root1, root2);
            } else if (rank1 > rank2) {
                parent.put(root2, root1);
            } else {
                parent.put(root2, root1);
                rank.put(root1, rank1 + 1);
            }

            return true;
        }
    }

    static class KruskalResult {
        List<Edge> mstEdges;
        int totalCost;
        int operationsCount;
        double executionTimeMs;

        KruskalResult(List<Edge> mstEdges, int totalCost, int operationsCount, double executionTimeMs) {
            this.mstEdges = mstEdges;
            this.totalCost = totalCost;
            this.operationsCount = operationsCount;
            this.executionTimeMs = executionTimeMs;
        }
    }

    static KruskalResult kruskal(List<String> nodes, List<Edge> edges) {
        long startTime = System.nanoTime();

        List<Edge> mstEdges = new ArrayList<>();
        int totalCost = 0;

        // Sort edges by weight
        Collections.sort(edges);
        int sortOperations = edges.size() * (int)(Math.log(edges.size()) / Math.log(2)); // Approximation

        UnionFind uf = new UnionFind(nodes);

        for (Edge edge : edges) {
            uf.operationsCount++; // Count edge consideration
            if (uf.union(edge.from, edge.to)) {
                mstEdges.add(edge);
                totalCost += edge.weight;

                if (mstEdges.size() == nodes.size() - 1) {
                    break; // MST complete
                }
            }
        }

        long endTime = System.nanoTime();
        double executionTimeMs = (endTime - startTime) / 1_000_000.0;

        int totalOperations = uf.operationsCount + sortOperations;

        return new KruskalResult(mstEdges, totalCost, totalOperations, executionTimeMs);
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

                // Run Kruskal's algorithm
                KruskalResult kruskalResult = kruskal(nodes, edges);

                // Build result JSON
                JSONObject result = new JSONObject();
                result.put("graph_id", graphId);

                JSONObject inputStats = new JSONObject();
                inputStats.put("vertices", nodes.size());
                inputStats.put("edges", edges.size());
                result.put("input_stats", inputStats);

                JSONObject kruskalJson = new JSONObject();
                JSONArray mstEdgesArray = new JSONArray();
                for (Edge edge : kruskalResult.mstEdges) {
                    JSONObject edgeJson = new JSONObject();
                    edgeJson.put("from", edge.from);
                    edgeJson.put("to", edge.to);
                    edgeJson.put("weight", edge.weight);
                    mstEdgesArray.put(edgeJson);
                }
                kruskalJson.put("mst_edges", mstEdgesArray);
                kruskalJson.put("total_cost", kruskalResult.totalCost);
                kruskalJson.put("operations_count", kruskalResult.operationsCount);
                kruskalJson.put("execution_time_ms",
                        Math.round(kruskalResult.executionTimeMs * 100.0) / 100.0);

                result.put("kruskal", kruskalJson);
                results.put(result);
            }

            outputJson.put("results", results);

            // Write output JSON file
            try (FileWriter file = new FileWriter("output.json")) {
                file.write(outputJson.toString(4));
                System.out.println("Results written to output.json");
            }

            // Print summary to console
            System.out.println("\n=== MST Results (Kruskal's Algorithm) ===");
            for (int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                System.out.println("\nGraph " + result.getInt("graph_id") + ":");
                System.out.println("  Vertices: " + result.getJSONObject("input_stats").getInt("vertices"));
                System.out.println("  Edges: " + result.getJSONObject("input_stats").getInt("edges"));

                JSONObject kruskal = result.getJSONObject("kruskal");
                System.out.println("  Total Cost: " + kruskal.getInt("total_cost"));
                System.out.println("  Operations: " + kruskal.getInt("operations_count"));
                System.out.println("  Execution Time: " + kruskal.getDouble("execution_time_ms") + " ms");
                System.out.println("  MST Edges:");
                JSONArray mstEdges = kruskal.getJSONArray("mst_edges");
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