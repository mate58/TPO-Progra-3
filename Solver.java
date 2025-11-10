import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cerebro del TPO. Resuelve el problema.
 * (Versión Completa con Backtracking Interno VRP)
 */
public class Solver {

    private Lector.Problema problema;
    private double[][] distancias;
    private Map<Integer, Integer> demandasPorNodo; // Demanda original
    private int capacidadCamion;

    private Solucion mejorSolucionGlobal;
    private double costoMinimoGlobal = Double.POSITIVE_INFINITY;

    // Memoization (Caché) para el backtracking interno (VRP)
    // La clave será un String que representa el estado (ej: "5_3_{12=1, 26=4}")
    private Map<String, Double> memoVRP;

    /**
     * Constructor del Solver.
     */
    public Solver(Lector.Problema problema) {
        this.problema = problema;
        this.distancias = problema.grafoDistancias; // Ya procesada por Floyd-Warshall
        this.capacidadCamion = problema.capacidadCamion;
        
        this.demandasPorNodo = new HashMap<>();
        for (Lector.Paquete p : problema.paquetes) {
            int idDestino = p.idNodoDestino();
            this.demandasPorNodo.put(idDestino, this.demandasPorNodo.getOrDefault(idDestino, 0) + 1);
        }
        System.out.println("Demandas pre-procesadas (NodoDestino -> Cantidad):");
        System.out.println(this.demandasPorNodo);
    }

    /**
     * Punto de entrada público.
     */
    public Solucion encontrarMejorSolucion() {
        System.out.println("\nIniciando búsqueda de la mejor combinación de Hubs...");
        
        List<Lector.Hub> hubs = problema.hubs;
        boolean[] hubsActivos = new boolean[hubs.size()]; 

        buscarCombinacionHubs(0, hubs, hubsActivos);

        return this.mejorSolucionGlobal;
    }

    /**
     * BACKTRACKING EXTERNO (Combinatoria de Hubs)
     */
    private void buscarCombinacionHubs(int hubIndex, List<Lector.Hub> hubs, boolean[] hubsActivos) {
        
        if (hubIndex == hubs.size()) {
            evaluarCombinacion(hubs, hubsActivos);
            return;
        }
        hubsActivos[hubIndex] = false;
        buscarCombinacionHubs(hubIndex + 1, hubs, hubsActivos);
        
        hubsActivos[hubIndex] = true;
        buscarCombinacionHubs(hubIndex + 1, hubs, hubsActivos);
    }

    /**
     * Evalúa el costo de una combinación específica de hubs.
     */
    private void evaluarCombinacion(List<Lector.Hub> hubs, boolean[] hubsActivos) {
        
        Solucion solucionParcial = new Solucion();
        Set<Integer> puntosDeRecarga = new HashSet<>();
        puntosDeRecarga.add(problema.depositoId);

        for (int i = 0; i < hubs.size(); i++) {
            if (hubsActivos[i]) {
                Lector.Hub hub = hubs.get(i);
                solucionParcial.costoTotalActivacion += hub.costoActivacion();
                solucionParcial.hubsActivados.add(hub);
                puntosDeRecarga.add(hub.idNodo());
            }
        }

        // --- INICIO DE LA SECCIÓN ACTUALIZADA ---
        
        // 3. Resolver el VRP (Backtracking Interno)
        
        // Inicializamos la caché de memoization para esta combinación de hubs
        this.memoVRP = new HashMap<>();
        
        // Creamos la copia inicial de paquetes pendientes
        Map<Integer, Integer> paquetesPendientes = new HashMap<>(this.demandasPorNodo);

        // La primera llamada al backtracking empieza en el Depósito,
        // con el camión lleno y todos los paquetes pendientes.
        double costoDistancia = backtrackRuteo(
            problema.depositoId,
            this.capacidadCamion,
            paquetesPendientes,
            puntosDeRecarga
        );
        
        solucionParcial.costoTotalDistancia = costoDistancia;
        
        // --- FIN DE LA SECCIÓN ACTUALIZADA ---


        // 4. Comparar y guardar si es la mejor solución global
        if (solucionParcial.getCostoTotal() < this.costoMinimoGlobal) {
            this.costoMinimoGlobal = solucionParcial.getCostoTotal();
            this.mejorSolucionGlobal = solucionParcial;
            
            // (Ya no imprimimos el placeholder, sino el costo real)
            System.out.printf("  -> Nueva mejor solución! Costo: %.2f (Dist: %.2f + Hubs: %.2f) [Hubs: %s]\n",
                 this.costoMinimoGlobal,
                 costoDistancia,
                 solucionParcial.costoTotalActivacion,
                 solucionParcial.hubsActivados.stream().map(Lector.Hub::idNodo).collect(Collectors.toList()));
        }
    }

    /**
     * BACKTRACKING INTERNO (VRP) - El núcleo del TPO.
     *
     * @param nodoActual El ID del nodo donde está el camión.
     * @param capacidadRestante Paquetes que el camión aún puede cargar (0 a N).
     * @param paquetesPendientes Mapa de {NodoID -> Cantidad} que FALTA entregar.
     * @param puntosDeRecarga Set de {NodoID} (Depósito + Hubs activos).
     * @return El costo MÍNIMO de distancia para entregar todos los paquetes
     * pendientes desde este estado.
     */
    private double backtrackRuteo(int nodoActual, int capacidadRestante,
                                  Map<Integer, Integer> paquetesPendientes,
                                  Set<Integer> puntosDeRecarga) {
        
        // --- CASO BASE ---
        if (paquetesPendientes.isEmpty()) {
            // ¡Éxito! Todos los paquetes entregados.
            // Volvemos al depósito (o la recarga más cercana) para terminar.
            return encontrarDistanciaARecargaMasCercana(nodoActual, puntosDeRecarga);
        }

        // --- MEMOIZATION (CACHE) ---
        // Creamos una clave única para este estado.
        String estadoKey = nodoActual + "_" + capacidadRestante + "_" + paquetesPendientes.toString();
        if (memoVRP.containsKey(estadoKey)) {
            return memoVRP.get(estadoKey);
        }

        double costoMinimo = Double.POSITIVE_INFINITY;

        // --- DECISIÓN 1: Visitar a un cliente ---
        // (Iteramos sobre una copia para evitar ConcurrentModificationException)
        for (int idCliente : new ArrayList<>(paquetesPendientes.keySet())) {
            
            int demandaCliente = paquetesPendientes.get(idCliente);
            
            // ¿Tengo capacidad para este cliente?
            // (Simplificación: asumimos que entregamos 1 paquete a la vez)
            // (Para una versión avanzada, aquí se manejaría 'demandaCliente')
            if (capacidadRestante > 0) {
                
                // 1. "Visitar" al cliente
                double costoViaje = distancias[nodoActual][idCliente];
                
                // 2. Actualizar estado
                Map<Integer, Integer> proximoEstadoPaquetes = new HashMap<>(paquetesPendientes);
                int demandaRestante = proximoEstadoPaquetes.get(idCliente) - 1;
                
                if (demandaRestante == 0) {
                    proximoEstadoPaquetes.remove(idCliente); // Cliente completado
                } else {
                    proximoEstadoPaquetes.put(idCliente, demandaRestante); // Cliente parcialmente completado
                }

                // 3. Llamada recursiva
                double costoFuturo = backtrackRuteo(
                    idCliente, // Nueva ubicación
                    capacidadRestante - 1, // Nueva capacidad
                    proximoEstadoPaquetes,
                    puntosDeRecarga
                );
                
                costoMinimo = Math.min(costoMinimo, costoViaje + costoFuturo);
            }
        }

        // --- DECISIÓN 2: Ir a un punto de recarga ---
        // (Solo si NO estamos ya en un punto de recarga)
        if (!puntosDeRecarga.contains(nodoActual)) {
            // Encontramos el punto de recarga más cercano para reabastecer
            double costoViajeARecarga = encontrarDistanciaARecargaMasCercana(nodoActual, puntosDeRecarga);
            
            // NOTA: El nodo "desde" el que partimos después de recargar es
            // el de la recarga. Esto es una simplificación.
            // Una versión más compleja probaría *todas* las recargas.
            // Aquí asumimos que vamos a la más cercana y seguimos desde ahí.
            // (Esto es una Heurística + Backtracking)
            
            // TODO: Esta parte es compleja. ¿Desde qué hub continuamos?
            // Por simplicidad, asumamos que "mágicamente" nos reabastecemos
            // y continuamos desde nuestra posición actual, pero con el
            // camión lleno.
            
            // *** Simplificación (Opción 2 de la consigna) ***
            // "Los hubs activos actúan como 'clones' del depósito"
            // "Se permite recargar paquetes en cualquier hub activo"
            // Esto significa que si capacidadRestante == 0, DEBEMOS ir a recargar.

            if (capacidadRestante == 0) {
                // Estamos "vacíos" y necesitamos entregar más.
                // Debemos forzar un viaje a la recarga más cercana.
                
                double costoRecarga = encontrarDistanciaARecargaMasCercana(nodoActual, puntosDeRecarga);

                // IMPORTANTE: ¿Desde dónde continuamos?
                // Lo más simple: continuamos desde la recarga más cercana.
                // (Esta lógica es compleja, la dejaremos para una mejora,
                // por ahora, el "Visitar a un cliente" se bloquea si
                // capacidadRestante = 0, forzando este camino).
                
                // --- RE-DISEÑO SIMPLE ---
                // Si me quedo sin capacidad, mi *única* opción es ir a recargar.
                // Y *luego* de recargar, volver a tomar la decisión 1.
                // El código actual ya maneja esto:
                // Si capacidadRestante = 0, el bucle "DECISIÓN 1" nunca se ejecuta.
                // Entonces, necesitamos una "DECISIÓN 2" que SÍ se ejecute.
            }
        }
        
        // --- DECISIÓN 2 (Refinada): Ir a recargar (si es necesario o beneficioso) ---
        // Si no podemos visitar a ningún cliente (capacidad == 0)
        // O simplemente decidimos "volver a base"
        if (capacidadRestante == 0 && !paquetesPendientes.isEmpty()) {
            // FORZADO a recargar
            double costoMinimoRecarga = Double.POSITIVE_INFINITY;
            
            // Probamos ir a CADA punto de recarga
            for (int idRecarga : puntosDeRecarga) {
                double costoViaje = distancias[nodoActual][idRecarga];
                
                // Después de recargar, estamos en 'idRecarga', con camión lleno
                double costoFuturo = backtrackRuteo(
                    idRecarga, // Nueva ubicación
                    this.capacidadCamion, // Camión lleno
                    paquetesPendientes, // Mismos paquetes pendientes
                    puntosDeRecarga
                );
                
                costoMinimoRecarga = Math.min(costoMinimoRecarga, costoViaje + costoFuturo);
            }
            costoMinimo = Math.min(costoMinimo, costoMinimoRecarga);
        }


        // Guardar resultado en caché y devolver
        memoVRP.put(estadoKey, costoMinimo);
        return costoMinimo;
    }

    /**
     * Función helper para encontrar la distancia al hub/depósito más cercano.
     */
    private double encontrarDistanciaARecargaMasCercana(int nodoActual, Set<Integer> puntosDeRecarga) {
        double distMinima = Double.POSITIVE_INFINITY;
        for (int idRecarga : puntosDeRecarga) {
            distMinima = Math.min(distMinima, distancias[nodoActual][idRecarga]);
        }
        return distMinima;
    }
}