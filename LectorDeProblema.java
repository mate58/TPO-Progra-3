import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

/**
 * Lector/Parser para el archivo de problema VRP.
 * Lee un archivo 'caso.txt' y lo carga en un objeto 'Problema'.
 * Incluye un main() para probar la carga e impresión.
 */
public class LectorDeProblema {

    // ==================================================================
    // 1. CLASES DE DATOS (Equivalente a 'struct')
    // ==================================================================

    /**
     * Contenedor principal para todos los datos del problema.
     */
    static class Problema {
        int num_nodos, num_hubs, num_paquetes, capacidad_camion, deposito_id;
        Nodo[] nodos;
        Hub[] hubs;
        Paquete[] paquetes;
        double[][] grafo_distancias; // Matriz de adyacencia
    }

    static class Nodo {
        int id;
        int x;
        int y;
    }

    static class Hub {
        int id_nodo;
        double costo_activacion;
    }

    static class Paquete {
        int id;
        int id_nodo_origen;
        int id_nodo_destino;
    }

    // ==================================================================
    // 3. FUNCIÓN MAIN (Punto de entrada para probar)
    // ==================================================================

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java LectorDeProblema <nombre_del_archivo.txt>");
            return;
        }

        String nombreArchivo = args[0];
        System.out.println("Leyendo el archivo de problema: " + nombreArchivo);

        try {
            Problema problema = leerArchivo(nombreArchivo);
            
            System.out.println("\n¡Archivo leído y procesado con éxito!");
            imprimirProblema(problema);
            
            // No se necesita liberar_memoria(problema)
            // El Garbage Collector (GC) de Java lo hace automáticamente.
            System.out.println("\nMemoria gestionada automáticamente por el GC de Java.");

        } catch (FileNotFoundException e) {
            System.err.println("Error: Archivo no encontrado '" + nombreArchivo + "'");
        } catch (Exception e) {
            System.err.println("\n>> Hubo un error al leer o procesar el archivo. Revisa el formato.");
            e.printStackTrace();
        }
    }

    // ==================================================================
    // 4. IMPLEMENTACIÓN DE LAS FUNCIONES
    // ==================================================================

    /**
     * Lee el archivo de problema y devuelve un objeto Problema.
     * @param nombreArchivo El path al archivo (ej. "caso.txt")
     * @return Un objeto Problema con todos los datos cargados.
     * @throws FileNotFoundException Si el archivo no se encuentra.
     */
    public static Problema leerArchivo(String nombreArchivo) throws FileNotFoundException {
        // Usamos Scanner para leer el archivo. Es el análogo a fscanf.
        // Usamos Locale.US para que lea los decimales con "." (ej. 123.45)
        Scanner scanner = new Scanner(new File(nombreArchivo));
        scanner.useLocale(Locale.US);

        Problema p = new Problema();

        // --- Lectura de la Configuración ---
        scanner.nextLine(); // Descarta "// --- CONFIGURACION ---"
        
        // Leemos cada valor uno por uno
        scanner.next(); // Descarta "NODOS"
        p.num_nodos = scanner.nextInt();
        
        scanner.next(); // Descarta "HUBS"
        p.num_hubs = scanner.nextInt();
        
        scanner.next(); // Descarta "PAQUETES"
        p.num_paquetes = scanner.nextInt();
        
        scanner.next(); // Descarta "CAPACIDAD_CAMION"
        p.capacidad_camion = scanner.nextInt();
        
        scanner.next(); // Descarta "DEPOSITO_ID"
        p.deposito_id = scanner.nextInt();

        // --- Asignación de Memoria (Creación de Arrays) ---
        p.nodos = new Nodo[p.num_nodos];
        p.hubs = new Hub[p.num_hubs];
        p.paquetes = new Paquete[p.num_paquetes];
        // En Java, los arrays numéricos se inicializan en 0.0 (como calloc)
        p.grafo_distancias = new double[p.num_nodos][p.num_nodos]; 

        scanner.nextLine(); // Consumir el resto de la línea de DEPOSITO_ID
        scanner.nextLine(); // Consumir la línea en blanco

        // --- Lectura de Nodos ---
        scanner.nextLine(); // Consumir comentario "// --- NODOS..."
        for (int i = 0; i < p.num_nodos; i++) {
            p.nodos[i] = new Nodo();
            p.nodos[i].id = scanner.nextInt();
            p.nodos[i].x = scanner.nextInt();
            p.nodos[i].y = scanner.nextInt();
            scanner.nextLine(); // Consumir el comentario (ej. "// Deposito")
        }

        scanner.nextLine(); // Consumir línea en blanco
        scanner.nextLine(); // Consumir comentario "// --- HUBS..."
        for (int i = 0; i < p.num_hubs; i++) {
            p.hubs[i] = new Hub();
            p.hubs[i].id_nodo = scanner.nextInt();
            p.hubs[i].costo_activacion = scanner.nextDouble();
        }

        scanner.nextLine(); // Consumir línea en blanco
        scanner.nextLine(); // Consumir comentario "// --- PAQUETES..."
        for (int i = 0; i < p.num_paquetes; i++) {
            p.paquetes[i] = new Paquete();
            p.paquetes[i].id = scanner.nextInt();
            p.paquetes[i].id_nodo_origen = scanner.nextInt();
            p.paquetes[i].id_nodo_destino = scanner.nextInt();
        }

        scanner.nextLine(); // Consumir línea en blanco
        scanner.nextLine(); // Consumir comentario "// --- ARISTAS..."
        
        // Leemos aristas hasta que no haya más enteros
        while (scanner.hasNextInt()) {
            int u = scanner.nextInt();
            int v = scanner.nextInt();
            double peso = scanner.nextDouble();
            
            if (u < p.num_nodos && v < p.num_nodos) {
                p.grafo_distancias[u][v] = peso;
                p.grafo_distancias[v][u] = peso; // Asumimos grafo no dirigido
            }
        }

        scanner.close(); // Equivalente a fclose(fp)
        return p;
    }

    /**
     * Imprime un resumen de los datos del problema en la consola.
     * @param p El objeto Problema cargado.
     */
    public static void imprimirProblema(Problema p) {
        System.out.println("\n============== RESUMEN DEL PROBLEMA CARGADO ===============");
        System.out.println("\n--- CONFIGURACION ---");
        System.out.printf("Total de Nodos:\t\t%d\n", p.num_nodos);
        System.out.printf("Total de Hubs:\t\t%d\n", p.num_hubs);
        System.out.printf("Total de Paquetes:\t%d\n", p.num_paquetes);
        System.out.printf("Capacidad del Camión:\t%d\n", p.capacidad_camion);
        System.out.printf("ID del Depósito:\t\t%d\n", p.deposito_id);
        
        System.out.println("\n--- NODOS ---");
        for (int i = 0; i < p.num_nodos; i++) {
            System.out.printf("  Nodo %2d: (x=%4d, y=%4d)\n", p.nodos[i].id, p.nodos[i].x, p.nodos[i].y);
        }
        
        System.out.println("\n--- HUBS ---");
        for (int i = 0; i < p.num_hubs; i++) {
            System.out.printf("  Hub en Nodo %2d: Costo de Activación = %.2f\n", p.hubs[i].id_nodo, p.hubs[i].costo_activacion);
        }
        
        System.out.println("\n--- PAQUETES ---");
        for (int i = 0; i < p.num_paquetes; i++) {
            System.out.printf("  Paquete %2d: Origen=%d -> Destino=%d\n", p.paquetes[i].id, p.paquetes[i].id_nodo_origen, p.paquetes[i].id_nodo_destino);
        }
        
        System.out.println("\n--- MUESTRA DEL GRAFO (MATRIZ DE ADYACENCIA) ---");
        System.out.printf("       ");
        for (int j = 0; j < 10 && j < p.num_nodos; j++) System.out.printf("  %4d   ", j);
        System.out.println("\n----");
        for (int j = 0; j < 10 && j < p.num_nodos; j++) System.out.printf("--------");
        System.out.println();
        
        for (int i = 0; i < 10 && i < p.num_nodos; i++) {
            System.out.printf("%4d| ", i);
            for (int j = 0; j < 10 && j < p.num_nodos; j++) {
                System.out.printf("%7.2f ", p.grafo_distancias[i][j]);
            }
            System.out.println();
        }
        System.out.println("===========================================================");
    }
}
