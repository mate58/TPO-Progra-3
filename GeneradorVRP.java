import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Generador de casos de prueba para el Problema de Enrutamiento de Vehículos (VRP).
public class GeneradorVRP {
    // MAX_COORD se infiere de rand() % (MAX_COORD + 1)
    private static final int MAX_COORD = 1000; 

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

    static class Arista {
        int u;
        int v;
        double peso;
        Arista(int u, int v, double peso) {
            this.u = u;
            this.v = v;
            this.peso = peso;
        }
    }

    private static double calcularDistancia(Nodo n1, Nodo n2) {
        return Math.sqrt(Math.pow(n1.x - n2.x, 2) + Math.pow(n1.y - n2.y, 2));
    }

    public static void generarArchivoCaso(String[] args) {
        
        // --- 1. CONFIGURACIÓN Y PARSEO
        int numNodos = 10;
        int numHubs = 3;
        int numPaquetes = 5;
        int capacidadCamion = 8;
        int depositoId = 0; 
        long seed = 123; 

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--nodos")) {
                numNodos = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--hubs")) {
                numHubs = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--paquetes")) {
                numPaquetes = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--seed")) {
                seed = Long.parseLong(args[++i]);
            }
        }

        if (numHubs >= numNodos) {
            System.out.println("Error: El número de hubs debe ser menor que el número total de nodos.");
            return; 
        }
        if ((numNodos - numHubs - 1) <= 0) {
            System.out.println("Error: No hay nodos disponibles para entregas (numNodos - numHubs - 1 <= 0).");
            return;
        }

        if (seed == 0) {
            seed = System.currentTimeMillis(); 
        }
        Random rand = new Random(seed); 
        System.out.println("Generador iniciado con semilla: " + seed);

        long inicio = System.nanoTime();

        // --- 2. LÓGICA DE GENERACIÓN
        boolean[][] conectados = new boolean[numNodos][numNodos];

        Nodo[] nodos = new Nodo[numNodos];
        
        List<Arista> aristas = new ArrayList<>();

        for (int i = 0; i < numNodos; i++) {
            nodos[i] = new Nodo(
                i,
                rand.nextInt(MAX_COORD + 1),
                rand.nextInt(MAX_COORD + 1)
            );
        }

        for (int i = 0; i < numNodos - 1; i++) {
            double peso = calcularDistancia(nodos[i], nodos[i + 1]);
            // .add() en lugar de aristas[num_aristas++]
            aristas.add(new Arista(i, i + 1, peso));
            conectados[i][i+1] = true;
            conectados[i+1][i] = true;
        }

        int aristasAdicionales = numNodos / 2;
        for (int i = 0; i < aristasAdicionales; i++) {
            int u = rand.nextInt(numNodos);
            int v = rand.nextInt(numNodos);

            if (u != v && !conectados[u][v]) {
                double peso = calcularDistancia(nodos[u], nodos[v]);
                aristas.add(new Arista(u, v, peso));
                conectados[u][v] = true;
                conectados[v][u] = true;
            }
        }

        // --- 3. ESCRITURA DEL ARCHIVO DE SALIDA
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("Output/caso.txt")))) {

            out.println("// --- CONFIGURACION ---");
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
                double costoActivacion = 100 + (double)rand.nextInt(401);
                out.printf("%d %.2f\n", i, costoActivacion);
            }

            out.println("\n// --- PAQUETES (ID NODO_ORIGEN NODO_DESTINO) ---");
            for (int i = 0; i < numPaquetes; i++) {
                int numNodosEntrega = numNodos - numHubs - 1;
                int nodoEntrega = (numHubs + 1) + rand.nextInt(numNodosEntrega);
                out.printf("%d %d %d\n", i, depositoId, nodoEntrega);
            }

            out.println("\n// --- ARISTAS (NODO1 NODO2 PESO) ---");
            for (Arista a : aristas) {
                out.printf("%d %d %.2f\n", a.u, a.v, a.peso);
            }

        } catch (IOException e) {
            System.out.println("Error al abrir o escribir el archivo!");
            e.printStackTrace();
            return;
        }

        long fin = System.nanoTime();
        double tiempoCpuUsado = (fin - inicio) / 1_000_000_000.0; 

        System.out.println("Archivo 'caso.txt' generado con éxito en la carpeta Output");
        System.out.printf("Tiempo de generación: %f segundos.\n", tiempoCpuUsado);
    }
}