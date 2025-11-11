import java.util.ArrayList;
import java.util.List;


public class Solucion {

    public static class Ruta {
        public List<Integer> nodosVisitados = new ArrayList<>();
        public double costoDistanciaRuta = 0.0;
        public int paquetesEntregados = 0;

        @Override
        public String toString() {
            String formato = "  Ruta (Paquetes: %d, Dist: %.2f): %s";
            return String.format(formato, paquetesEntregados, costoDistanciaRuta, nodosVisitados);
        }
    }

    // --- Campos de la Solución ---
    public List<Ruta> rutas = new ArrayList<>();
    public List<Lector.Hub> hubsActivados = new ArrayList<>();
    
    public double costoTotalDistancia = 0.0;
    public double costoTotalActivacion = 0.0;
    
    //Calcula el costo total combinado de la solución.
    public double getCostoTotal() {
        return costoTotalDistancia + costoTotalActivacion;
    }

    //Imprime la solución de forma legible.
    public void imprimir() {
        System.out.println("\n========= SOLUCIÓN ÓPTIMA ==========");
        System.out.printf("COSTO TOTAL: %.2f\n", getCostoTotal());
        System.out.printf(" (Distancia: %.2f + Activación Hubs: %.2f)\n",
                costoTotalDistancia, costoTotalActivacion);
        
        System.out.printf("\nHubs Activados (%d):\n", hubsActivados.size());
        if (hubsActivados.isEmpty()) {
            System.out.println("  Ninguno");
        } else {
            for (Lector.Hub hub : hubsActivados) {
                System.out.printf("  - Nodo %d (Costo: %.2f)\n", hub.idNodo(), hub.costoActivacion());
            }
        }

        System.out.printf("\nRutas de Camiones (%d):\n", rutas.size());
        if (rutas.isEmpty() && costoTotalDistancia > 0) {
             System.out.println("  (Solución de backtracking aún no implementada, solo se muestra costo de hubs)");
        } else {
            for (Ruta r : rutas) {
                System.out.println(r);
            }
        }
        System.out.println("=====================================");
    }
}