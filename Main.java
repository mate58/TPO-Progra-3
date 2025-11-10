/**
 * Punto de entrada principal del TPO.
 * Orquesta: Lector -> FloydWarshall -> Solver
 * (Corregido para usar Lector.java)
 */
public class Main {

    public static void main(String[] args) {
        
        System.out.println("--- Iniciando TPO de Programación III ---");
        String nombreArchivo = "caso.txt";

        try {
            // --- 1. Lectura del Problema (USANDO Lector) ---
            System.out.println("Leyendo archivo: " + nombreArchivo + " ...");
            // Aquí estaba el error -> Lector.Problema y Lector.leerArchivo
            Lector.Problema problema = Lector.leerArchivo(nombreArchivo);
            
            if (problema == null) {
                System.err.println("ERROR FATAL: No se pudo leer el problema.");
                return;
            }
            
            // --- 2. Pre-procesamiento (Floyd-Warshall) ---
            System.out.println("Ejecutando pre-procesamiento (Floyd-Warshall)...");
            FloydWarshall.calcularCaminosMinimos(problema);
            System.out.println("¡Pre-procesamiento completado!");

            // Aquí estaba el error -> Lector.imprimirProblema
            Lector.imprimirProblema(problema);


            // --- 3. Resolver el Problema ---
            System.out.println("\nIniciando Solver...");
            Solver solver = new Solver(problema);
            Solucion solucionOptima = solver.encontrarMejorSolucion();

            // --- 4. Imprimir la Solución Final ---
            if (solucionOptima != null) {
                solucionOptima.imprimir();
            } else {
                System.out.println("\nNo se encontró ninguna solución.");
            }


        } catch (Exception e) {
            System.err.println("ERROR FATAL: Ocurrió un error inesperado en Main.");
            e.printStackTrace();
        }

        System.out.println("\n--- Ejecución Finalizada ---");
    }
}