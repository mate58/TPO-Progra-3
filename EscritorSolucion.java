import java.io.PrintWriter;
import java.io.IOException;
import java.util.stream.Collectors;


public class EscritorSolucion {

    /**
     * @param sol La solución óptima encontrada.
     * @param tiempoEjecucion El tiempo que tardó el solver (en segundos).
     */
    public static void escribir(Solucion sol, double tiempoEjecucion) {
        try (PrintWriter out = new PrintWriter("solucion.txt")) {

            // --- 1. HUBS ACTIVADOS ---
            out.println("// HUBS ACTIVADOS");
            if (sol.hubsActivados.isEmpty()) {
                out.println("Ninguno");
            } else {
                for (Lector.Hub hub : sol.hubsActivados) {
                    out.println(hub.idNodo());
                }
            }

            // --- 2. RUTA ÓPTIMA ---
            out.println("\n// RUTA OPTIMA");
            StringBuilder rutaCompleta = new StringBuilder();
            for (int i = 0; i < sol.rutas.size(); i++) {
                Solucion.Ruta r = sol.rutas.get(i);
                String rutaParcial = r.nodosVisitados.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(" -> "));
                
                if (i == 0) {
                    rutaCompleta.append(rutaParcial);
                } else {
                    rutaCompleta.append(" -> ")
                                .append(rutaParcial.substring(rutaParcial.indexOf(" -> ") + 4));
                }
            }
            out.println(rutaCompleta.toString());

            // --- 3. MÉTRICAS ---
            out.println("\n// METRICAS");
            out.printf("COSTO_TOTAL: %.2f\n", sol.getCostoTotal());
            out.printf("DISTANCIA_RECORRIDA: %.2f\n", sol.costoTotalDistancia);
            out.printf("COSTO_HUBS: %.2f\n", sol.costoTotalActivacion);
            out.printf("TIEMPO_EJECUCION: %f segundos\n", tiempoEjecucion);

        } catch (IOException e) {
            System.err.println("Error al escribir el archivo de solución: " + e.getMessage());
        }
    }
}