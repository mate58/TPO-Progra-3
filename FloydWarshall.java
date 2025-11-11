public class FloydWarshall {

    /**
     * @param problema 
     */
    public static void calcularCaminosMinimos(Lector.Problema problema) {
        
        int n = problema.numNodos;
        double[][] dist = problema.grafoDistancias; // 'dist' es un alias a la matriz del problema

        // --- 1. Inicialización ---
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    dist[i][j] = 0.0;
                } else if (dist[i][j] == 0.0) {
                    dist[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }

        // --- 2. Algoritmo Principal de Floyd-Warshall ---
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double dist_i_k_j = dist[i][k] + dist[k][j];
                    if (dist_i_k_j < dist[i][j]) {
                        dist[i][j] = dist_i_k_j;
                    }
                }
            }
        }
    }
}