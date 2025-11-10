import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generador de casos de prueba para el Problema de Enrutamiento de Vehículos (VRP).
 * Esta es una traducción de un script en C.
 */
public class GeneradorVRP {

    // Constantes (Asumidas del código C, como MAX_COORD)
    // MAX_COORD se infiere de rand() % (MAX_COORD + 1)
    private static final int MAX_COORD = 1000; 

    // --- Clases anidadas para emular 'structs' ---

    /**
     * Representa un Nodo (Depósito, Hub, o Cliente)
     */
    static class Nodo {
        int id;
        int x;
        int y;

        Nodo(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Representa una Arista (conexión entre dos nodos)
     */
    static class Arista {
        int u;
        int v;
        double peso; // Distancia

        Arista(int u, int v, double peso) {
            this.u = u;
            this.v = v;
            this.peso = peso;
        }
    }

    // --- Función de utilidad (portada de C) ---

    /**
     * Calcula la distancia Euclidiana entre dos nodos.
     */
    private static double calcularDistancia(Nodo n1, Nodo n2) {
        // Math.pow(base, exp) es la función de potencia
        // Math.sqrt(val) es la raíz cuadrada
        return Math.sqrt(Math.pow(n1.x - n2.x, 2) + Math.pow(n1.y - n2.y, 2));
    }

    // --- Lógica Principal ---

    public static void generarCaso(String[] args) {
        
        // --- 1. CONFIGURACIÓN Y PARSEO ---
        int numNodos = 10;
        int numHubs = 3;
        int numPaquetes = 5;
        int capacidadCamion = 8;
        int depositoId = 0; // Por convención, el depósito es el nodo 0
        long seed = 123; 

        // Parseo de argumentos de línea de comandos (args)
        for (int i = 0; i < args.length; i++) {
            // En Java, se usa .equals() para comparar strings
            if (args[i].equals("--nodos")) {
                numNodos = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--hubs")) {
                numHubs = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--paquetes")) {
                numPaquetes = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--seed")) {
                // Usamos Long.parseLong() para la semilla
                seed = Long.parseLong(args[++i]);
            }
        }

        // Validaciones
        if (numHubs >= numNodos) {
            System.out.println("Error: El número de hubs debe ser menor que el número total de nodos.");
            return; // Termina el programa
        }
        if ((numNodos - numHubs - 1) <= 0) {
            System.out.println("Error: No hay nodos disponibles para entregas (numNodos - numHubs - 1 <= 0).");
            return;
        }

        // Inicialización del generador de números aleatorios (RNG)
        if (seed == 0) {
            seed = System.currentTimeMillis(); // Equivalente a time(NULL)
        }
        Random rand = new Random(seed); // Equivalente a srand(seed)
        System.out.println("Generador iniciado con semilla: " + seed);

        // Medición de tiempo (Java usa System.nanoTime() para mayor precisión)
        long inicio = System.nanoTime();

        // --- 2. LÓGICA DE GENERACIÓN ---

        // Matriz de adyacencia para chequear conexiones existentes.
        // 'boolean' es más semántico que 'int'. Se inicializa en 'false' por defecto.
        // No necesitamos 'malloc' o 'free', el Garbage Collector (GC) se encarga.
        boolean[][] conectados = new boolean[numNodos][numNodos];

        // Arrays para Nodos y Aristas
        Nodo[] nodos = new Nodo[numNodos];
        
        // Usamos ArrayList para las aristas, es más flexible que un array de tamaño fijo
        List<Arista> aristas = new ArrayList<>();

        // Generación de Nodos
        for (int i = 0; i < numNodos; i++) {
            // rand() % (MAX_COORD + 1) -> rand.nextInt(MAX_COORD + 1)
            nodos[i] = new Nodo(
                i,
                rand.nextInt(MAX_COORD + 1),
                rand.nextInt(MAX_COORD + 1)
            );
        }

        // Generación de aristas (primero un camino para asegurar conectividad)
        for (int i = 0; i < numNodos - 1; i++) {
            double peso = calcularDistancia(nodos[i], nodos[i + 1]);
            // .add() en lugar de aristas[num_aristas++]
            aristas.add(new Arista(i, i + 1, peso));
            conectados[i][i+1] = true;
            conectados[i+1][i] = true;
        }

        // Generación de aristas adicionales
        int aristasAdicionales = numNodos / 2;
        for (int i = 0; i < aristasAdicionales; i++) {
            int u = rand.nextInt(numNodos); // rand() % num_nodos
            int v = rand.nextInt(numNodos);

            if (u != v && !conectados[u][v]) {
                double peso = calcularDistancia(nodos[u], nodos[v]);
                aristas.add(new Arista(u, v, peso));
                conectados[u][v] = true;
                conectados[v][u] = true;
            }
        }

        // --- 3. ESCRITURA DEL ARCHIVO DE SALIDA ---

        // Usamos try-with-resources (Java 7+), que cierra el archivo automáticamente
        // (equivale a la combinación de fopen, fprintf y fclose)
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("caso.txt")))) {

            out.println("// --- CONFIGURACION ---");
            // out.printf() funciona igual que fprintf()
            out.printf("NODOS %d\nHUBS %d\nPAQUETES %d\nCAPACIDAD_CAMION %d\nDEPOSITO_ID %d\n\n",
                    numNodos, numHubs, numPaquetes, capacidadCamion, depositoId);

            out.println("// --- NODOS (ID X Y) ---");
            for (int i = 0; i < numNodos; i++) {
                out.printf("%d %d %d", nodos[i].id, nodos[i].x, nodos[i].y);
                if (i == depositoId) out.println(" // Deposito");
                else if (i <= numHubs) out.println(" // Hub");
                else out.println(" // Entrega");
            }

            out.println("\n// --- HUBS (ID COSTO_ACTIVACION) ---");
            for (int i = 1; i <= numHubs; i++) {
                // (double)(rand() % 401) -> (double)rand.nextInt(401)
                double costoActivacion = 100 + (double)rand.nextInt(401);
                out.printf("%d %.2f\n", i, costoActivacion);
            }

            out.println("\n// --- PAQUETES (ID NODO_ORIGEN NODO_DESTINO) ---");
            for (int i = 0; i < numPaquetes; i++) {
                // Lógica de selección de nodo de entrega
                int numNodosEntrega = numNodos - numHubs - 1;
                int nodoEntrega = (numHubs + 1) + rand.nextInt(numNodosEntrega);
                out.printf("%d %d %d\n", i, depositoId, nodoEntrega);
            }

            out.println("\n// --- ARISTAS (NODO1 NODO2 PESO) ---");
            // Iteramos sobre el ArrayList de aristas
            for (Arista a : aristas) {
                out.printf("%d %d %.2f\n", a.u, a.v, a.peso);
            }

        } catch (IOException e) {
            // Manejo de error si no se puede escribir el archivo
            System.out.println("Error al abrir o escribir el archivo!");
            e.printStackTrace();
            return;
        }

        // No es necesario 'free(conectados)', el GC de Java lo maneja.

        // Cálculo del tiempo de ejecución
        long fin = System.nanoTime();
        // Convertimos nanosegundos a segundos
        double tiempoCpuUsado = (fin - inicio) / 1_000_000_000.0; 

        System.out.println("Archivo 'caso.txt' generado con éxito.");
        System.out.printf("Tiempo de generación: %f segundos.\n", tiempoCpuUsado);
    }
}