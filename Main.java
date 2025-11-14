//Generador -> Lector -> FloydWarshall -> Solver -> Escritor
public class Main {

    public static void main(String[] args) {

        // --- 1. GENERAR CASO DE PRUEBA ---
        System.out.println("--- Generando un caso de prueba ---");
        

        String[] argsGenerador = new String[] {
             "--nodos", "20", "--hubs", "3", "--paquetes", "10", "--seed", "20241"
        };
        
        GeneradorVRP.generarArchivoCaso(argsGenerador);
        System.out.println("--- 'caso.txt' generado ---");

        System.out.println("--- Iniciando TPO de Programación III ---");
        String nombreArchivo = "caso.txt"; 

        try {
            // --- 2. Lectura y Pre-procesamiento ---
            Lector.Problema problema = Lector.leerArchivo(nombreArchivo);
            if (problema == null) return;
            FloydWarshall.calcularCaminosMinimos(problema);
            Lector.imprimirProblema(problema);

            // --- 3. Resolver el Problema (Medir Tiempo) ---
            System.out.println("\nIniciando Solver (Backtracking)...");
            long inicioSolver = System.nanoTime(); // Iniciar timer

            Solver solver = new Solver(problema);
            Solucion solucionOptima = solver.encontrarMejorSolucion();

            long finSolver = System.nanoTime(); // Detener timer
            double tiempoEjecucion = (finSolver - inicioSolver) / 1_000_000_000.0; // Convertir a segundos

            // --- 4. Imprimir y Escribir la Solución Final ---
            if (solucionOptima != null) {
                solucionOptima.imprimir();
                System.out.printf("\nSolver finalizado en %.6f segundos.\n", tiempoEjecucion);
                
                EscritorSolucion.escribir(solucionOptima, tiempoEjecucion);
                System.out.println("Archivo 'solucion.txt' generado.");
                
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