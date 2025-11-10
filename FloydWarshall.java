/**
 * Implementación del algoritmo de Floyd-Warshall.
 * Se utiliza para pre-procesar la matriz de adyacencia leída
 * y convertirla en una matriz de caminos mínimos, como pide la consigna.
 */
public class FloydWarshall {

    /**
     * Ejecuta el algoritmo de Floyd-Warshall sobre la matriz de distancias
     * del problema.
     * * IMPORTANTE: Este método MODIFICA la matriz 'problema.grafoDistancias'.
     *
     * @param problema El objeto LectorJava.Problema que contiene la matriz 
     * de adyacencia (aristas directas).
     */
    public static void calcularCaminosMinimos(Lector.Problema problema) {
        
        int n = problema.numNodos;
        double[][] dist = problema.grafoDistancias; // 'dist' es un alias a la matriz del problema

        // --- 1. Inicialización ---
        // El 'LectorJava' pone 0.0 donde NO hay arista.
        // Floyd-Warshall necesita Infinito (Double.POSITIVE_INFINITY)
        // para representar "sin conexión".
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    // La distancia de un nodo a sí mismo es siempre 0.
                    dist[i][j] = 0.0;
                } else if (dist[i][j] == 0.0) {
                    // Si es 0.0 y no es la diagonal, significa que no hay
                    // arista directa. Lo ponemos como Infinito.
                    dist[i][j] = Double.POSITIVE_INFINITY;
                }
            }
        }

        // --- 2. Algoritmo Principal de Floyd-Warshall ---
        // "k" es el nodo intermedio que probamos
        for (int k = 0; k < n; k++) {
            // "i" es el nodo de origen
            for (int i = 0; i < n; i++) {
                // "j" es el nodo de destino
                for (int j = 0; j < n; j++) {
                    
                    // Calculamos la distancia de i -> k -> j
                    double dist_i_k_j = dist[i][k] + dist[k][j];

                    // Si el camino pasando por 'k' es más corto que el
                    // camino directo (o infinito) que teníamos...
                    if (dist_i_k_j < dist[i][j]) {
                        // ...lo actualizamos.
                        dist[i][j] = dist_i_k_j;
                    }
                }
            }
        }

        // ¡Listo! En este punto, 'problema.grafoDistancias' ya no contiene
        // solo las aristas, sino los caminos MÍNIMOS entre todos los nodos.
    }
}