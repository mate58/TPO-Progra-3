import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Solver {

    private Lector.Problema problema;
    private double[][] distancias;
    private Map<Integer, Integer> demandasPorNodo;
    private int capacidadCamion;
    private int depositoId;

    // --- Solución Global ---
    private Solucion mejorSolucionGlobal;
    private double costoMinimoGlobal = Double.POSITIVE_INFINITY;

    // --- Estado para UNA combinación de Hubs ---
    // Guardamos el mejor VRP encontrado para la combinación de hubs actual
    private double mejorCostoDistanciaVRP;
    private List<Solucion.Ruta> mejorRutaVRP;
    
    // --- Estado Global para el Backtracking (Modificado y Restaurado) ---
    // Esto evita el OutOfMemoryError
    private Map<Integer, Integer> paquetesPendientesEstado;
    private int numTotalPaquetesPendientesEstado;
    
    // Para construir la ruta sobre la marcha
    private List<Solucion.Ruta> rutasActuales;
    private Solucion.Ruta rutaParcialActual;


    public Solver(Lector.Problema problema) {
        this.problema = problema;
        this.distancias = problema.grafoDistancias;
        this.capacidadCamion = problema.capacidadCamion;
        this.depositoId = problema.depositoId;
        
        this.demandasPorNodo = new HashMap<>();
        for (Lector.Paquete p : problema.paquetes) {
            int idDestino = p.idNodoDestino();
            this.demandasPorNodo.put(idDestino, this.demandasPorNodo.getOrDefault(idDestino, 0) + 1);
        }
    }

    public Solucion encontrarMejorSolucion() {
        System.out.println("\nIniciando búsqueda de la mejor combinación de Hubs...");
        List<Lector.Hub> hubs = problema.hubs;
        
        // --- Branch and Bound
        System.out.println("Calculando una primera solución 'base' (sin hubs)...");
        Set<Integer> soloDeposito = new HashSet<>();
        soloDeposito.add(this.depositoId);
        evaluarCombinacion(0.0, soloDeposito, new ArrayList<>());
        
        if (this.mejorSolucionGlobal != null) {
            System.out.printf("Solución base encontrada. Costo: %.2f. Usando para poda.\n", this.costoMinimoGlobal);
        } else {
            System.out.println("No se encontró solución base (raro), continuando...");
        }
    // --- Fin

        // Iteramos 2^N_HUBS. Para 15 hubs (caso grande) son ~32k, es muy rápido.
        int numCombinaciones = 1 << hubs.size();
        //Para debug en la terminal
        System.out.printf("Total de combinaciones de Hubs a probar: %d\n", numCombinaciones); 

        for (int i = 0; i < numCombinaciones; i++) {
            //Para debug en la terminal
            System.out.printf("\n--- Probando Combinación %d / %d ---\n", (i + 1), numCombinaciones);
            Solucion solucionParcial = new Solucion();
            Set<Integer> puntosDeRecarga = new HashSet<>();
            puntosDeRecarga.add(this.depositoId);
            double costoHubsActual = 0.0;

            // Construir el subconjunto de hubs para esta iteración
            for (int j = 0; j < hubs.size(); j++) {
                // Chequeamos si el j-ésimo bit está encendido
                if ((i & (1 << j)) > 0) {
                    Lector.Hub hub = hubs.get(j);
                    costoHubsActual += hub.costoActivacion();
                    solucionParcial.hubsActivados.add(hub);
                    puntosDeRecarga.add(hub.idNodo());
                }
            }
            
            // --- PODA Nivel 1 (Branch & Bound Global) ---
            // Si activar estos hubs ya cuesta más que la mejor solución encontrada,
            // ni siquiera intentamos calcular la ruta VRP.
            if (costoHubsActual >= this.costoMinimoGlobal) {
                continue; // Podamos esta combinación de hubs
            }

            // --- Resolver el VRP para esta combinación de hubs ---
            evaluarCombinacion(costoHubsActual, puntosDeRecarga, solucionParcial.hubsActivados);
        }

        return this.mejorSolucionGlobal;
    }


    private void evaluarCombinacion(double costoHubs, Set<Integer> puntosDeRecarga, List<Lector.Hub> hubsActivos) {
        
        // 1. Inicializar el estado para el backtracking
        this.paquetesPendientesEstado = new HashMap<>(this.demandasPorNodo);
        this.numTotalPaquetesPendientesEstado = this.problema.numPaquetes;
        this.mejorCostoDistanciaVRP = Double.POSITIVE_INFINITY;
        this.mejorRutaVRP = null; // Aún no encontramos ruta para esta comb.
        
        this.rutasActuales = new ArrayList<>();
        this.rutaParcialActual = new Solucion.Ruta();
        this.rutaParcialActual.nodosVisitados.add(this.depositoId);

        // 2. Iniciar la recursión
        backtrackRecursivo(
            this.depositoId,
            this.capacidadCamion,
            0.0, // costoDistanciaAcumulado
            costoHubs,
            puntosDeRecarga
        );

        // 3. Evaluar el resultado de esta combinación
        double costoTotalCombinacion = this.mejorCostoDistanciaVRP + costoHubs;

        if (costoTotalCombinacion < this.costoMinimoGlobal) {
            this.costoMinimoGlobal = costoTotalCombinacion;
            
            // Construimos el objeto Solucion final
            this.mejorSolucionGlobal = new Solucion();
            this.mejorSolucionGlobal.costoTotalActivacion = costoHubs;
            this.mejorSolucionGlobal.costoTotalDistancia = this.mejorCostoDistanciaVRP;
            this.mejorSolucionGlobal.hubsActivados = hubsActivos;
            this.mejorSolucionGlobal.rutas = this.mejorRutaVRP; // Guardamos la mejor ruta VRP encontrada

            System.out.printf("  -> NUEVA MEJOR SOLUCIÓN GLOBAL! Costo: %.2f (Dist: %.2f + Hubs: %.2f) [Hubs: %s]\n",
                 this.costoMinimoGlobal,
                 this.mejorCostoDistanciaVRP,
                 costoHubs,
                 hubsActivos.stream().map(Lector.Hub::idNodo).collect(Collectors.toList()));
        }
    }

    /**
     * Función de backtracking principal. No retorna nada (void), sino que
     * modifica el estado global 'mejorCostoDistanciaVRP' y 'mejorRutaVRP'
     * si encuentra una solución VRP completa y mejor.
     */
    private void backtrackRecursivo(int nodoActual, int capacidadRestante,
                                    double costoDistanciaAcumulado,
                                    double costoHubs,
                                    Set<Integer> puntosDeRecarga) {
        
        // --- PODA Nivel 2 (Branch & Bound Global) ---
        // Si la distancia que ya recorrimos + hubs es peor que la mejor
        // SOLUCIÓN TOTAL, esta rama es inútil.
        if (costoDistanciaAcumulado + costoHubs >= this.costoMinimoGlobal) {
            return; // PODADO (Global)
        }

        // --- PODA Nivel 3 (Branch & Bound Local del VRP) ---
        // Si la distancia que ya recorrimos es peor que la mejor
        // RUTA VRP (para esta comb. de hubs), esta rama es inútil.
        if (costoDistanciaAcumulado >= this.mejorCostoDistanciaVRP) {
            return; // PODADO (Local)
        }

        // --- CASO BASE (ÉXITO) ---
        // No quedan paquetes por entregar
        if (this.numTotalPaquetesPendientesEstado == 0) {
            // Encontramos una solución VRP completa.
            // Calculamos el costo de volver al punto de recarga más cercano.
            int nodoRetorno = encontrarRecargaMasCercana(nodoActual, puntosDeRecarga);
            double costoRetorno = (nodoRetorno != -1) ? distancias[nodoActual][nodoRetorno] : 0.0;
            double costoVRPFinal = costoDistanciaAcumulado + costoRetorno;

            // ¿Es la mejor solución VRP *para esta combinación de hubs*?
            if (costoVRPFinal < this.mejorCostoDistanciaVRP) {
                this.mejorCostoDistanciaVRP = costoVRPFinal;
                
                // Guardamos la ruta completa
                // (Necesitamos un constructor de copia en Solucion.Ruta)
                Solucion.Ruta rutaFinal = new Solucion.Ruta(this.rutaParcialActual);
                rutaFinal.nodosVisitados.add(nodoRetorno);
                rutaFinal.costoDistanciaRuta += costoRetorno;
                
                this.mejorRutaVRP = new ArrayList<>(this.rutasActuales);
                this.mejorRutaVRP.add(rutaFinal);
            }
            return; // Fin de esta rama recursiva
        }


        // --- PASO RECURSIVO ---

        // Opción 1: Entregar un paquete (si tenemos capacidad)
        if (capacidadRestante > 0) {
            // Usamos una copia de las keys para evitar ConcurrentModificationException
            // al modificar el map 'paquetesPendientesEstado'
            Set<Integer> clientesPendientes = new HashSet<>(this.paquetesPendientesEstado.keySet());

            for (int idCliente : clientesPendientes) {
                double costoViaje = distancias[nodoActual][idCliente];

                // 1. MODIFICAR ESTADO
                this.rutaParcialActual.nodosVisitados.add(idCliente);
                this.rutaParcialActual.costoDistanciaRuta += costoViaje;
                this.rutaParcialActual.paquetesEntregados++;
                
                int demandaRestante = this.paquetesPendientesEstado.get(idCliente) - 1;
                if (demandaRestante == 0) this.paquetesPendientesEstado.remove(idCliente);
                else this.paquetesPendientesEstado.put(idCliente, demandaRestante);
                
                this.numTotalPaquetesPendientesEstado--;

                // 2. RECURSAR
                backtrackRecursivo(
                    idCliente,
                    capacidadRestante - 1,
                    costoDistanciaAcumulado + costoViaje,
                    costoHubs,
                    puntosDeRecarga
                );

                // 3. DESHACER (El Backtrack)
                this.numTotalPaquetesPendientesEstado++;
                this.paquetesPendientesEstado.put(idCliente, this.paquetesPendientesEstado.getOrDefault(idCliente, 0) + 1);
                
                this.rutaParcialActual.paquetesEntregados--;
                this.rutaParcialActual.costoDistanciaRuta -= costoViaje;
                this.rutaParcialActual.nodosVisitados.remove(this.rutaParcialActual.nodosVisitados.size() - 1);
            }
        }

        // Opción 2: Ir a recargar (a un Hub o al Depósito)
        // El camión siempre tiene la opción de recargar, a menos que
        // ya esté en un punto de recarga con el tanque lleno.
        boolean puedeRecargar = (capacidadRestante < this.capacidadCamion);
        if (puedeRecargar) {
            for (int idRecarga : puntosDeRecarga) {
                if (idRecarga == nodoActual) continue; // No recargar donde ya estamos

                double costoViaje = distancias[nodoActual][idRecarga];

                // 1. MODIFICAR ESTADO (Guardamos ruta parcial, empezamos una nueva)
                Solucion.Ruta rutaAnterior = this.rutaParcialActual; // Guardamos para el backtrack
                this.rutasActuales.add(rutaAnterior); // "Bancamos" la ruta
                
                this.rutaParcialActual = new Solucion.Ruta();
                this.rutaParcialActual.nodosVisitados.add(idRecarga);
                // (El costo de esta ruta parcial es 0 por ahora)

                // 2. RECURSAR
                backtrackRecursivo(
                    idRecarga,
                    this.capacidadCamion, // Capacidad reseteada
                    costoDistanciaAcumulado + costoViaje,
                    costoHubs,
                    puntosDeRecarga
                );

                // 3. DESHACER (Restaurar estado de la ruta)
                this.rutaParcialActual = rutaAnterior;
                this.rutasActuales.remove(this.rutasActuales.size() - 1);
            }
        }
    }


    // --- Funciones Helper ---

    private int encontrarRecargaMasCercana(int nodoActual, Set<Integer> puntosDeRecarga) {
        double distMinima = Double.POSITIVE_INFINITY;
        int idRecargaMasCercana = -1;
        for (int idRecarga : puntosDeRecarga) {
            double dist = distancias[nodoActual][idRecarga];
            if (dist < distMinima) {
                distMinima = dist;
                idRecargaMasCercana = idRecarga;
            }
        }
        return idRecargaMasCercana;
    }
}