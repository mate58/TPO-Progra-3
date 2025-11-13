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

    // --- INICIO DE MODIFICACIÓN (PODA HEURÍSTICA) ---
    /**
     * Poda heurística: en lugar de explorar N! permutaciones de entrega,
     * en cada paso solo exploramos las K opciones más cercanas.
     * Esto reduce el factor de ramificación de N a K.
     * 5 es un valor balanceado: rápido y con buena probabilidad de
     * encontrar una solución cercana a la óptima.
     */
    private static final int K_VECINOS_CERCANOS = 5;
    // --- FIN DE MODIFICACIÓN ---

    // --- Estado para UNA combinación de Hubs ---
    private double mejorCostoDistanciaVRP;
    private List<Solucion.Ruta> mejorRutaVRP;
    
    // --- Estado Global para el Backtracking (Modificado y Restaurado) ---
    private Map<Integer, Integer> paquetesPendientesEstado;
    private int numTotalPaquetesPendientesEstado;
    
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
        
        // --- Branch and Bound (Cálculo de solución base) ---
        System.out.println("Calculando una primera solución 'base' (sin hubs)...");
        Set<Integer> soloDeposito = new HashSet<>();
        soloDeposito.add(this.depositoId);
        evaluarCombinacion(0.0, soloDeposito, new ArrayList<>());
        
        // Este es tu 'sout' de debug
        if (this.mejorSolucionGlobal != null) {
            System.out.printf("Solución base encontrada. Costo: %.2f. Usando para poda.\n", this.costoMinimoGlobal);
        } else {
            System.out.println("No se encontró solución base (raro), continuando...");
        }
        // --- Fin ---

        int numCombinaciones = 1 << hubs.size();
        // Este es tu 'sout' de debug
        System.out.printf("Total de combinaciones de Hubs a probar: %d\n", numCombinaciones); 

        for (int i = 0; i < numCombinaciones; i++) {
            
            // --- Combinación 0 (sin hubs) ya fue procesada ---
            if (i == 0) {
                 System.out.printf("\n--- Probando Combinación %d / %d (Caso Base, ya calculado) ---\n", (i + 1), numCombinaciones);
                 continue; // Ya hicimos este cálculo
            }

            // Este es tu 'sout' de debug
            System.out.printf("\n--- Probando Combinación %d / %d ---\n", (i + 1), numCombinaciones);
            Solucion solucionParcial = new Solucion();
            Set<Integer> puntosDeRecarga = new HashSet<>();
            puntosDeRecarga.add(this.depositoId);
            double costoHubsActual = 0.0;

            // Construir el subconjunto de hubs
            for (int j = 0; j < hubs.size(); j++) {
                if ((i & (1 << j)) > 0) {
                    Lector.Hub hub = hubs.get(j);
                    costoHubsActual += hub.costoActivacion();
                    solucionParcial.hubsActivados.add(hub);
                    puntosDeRecarga.add(hub.idNodo());
                }
            }
            
            // --- PODA Nivel 1 (Branch & Bound Global) ---
            if (costoHubsActual >= this.costoMinimoGlobal) {
                System.out.println("  -> PODADO (Nivel 1): Costo de hubs es mayor que la mejor solución.");
                continue; 
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
        this.mejorRutaVRP = null; 
        
        this.rutasActuales = new ArrayList<>();
        this.rutaParcialActual = new Solucion.Ruta();
        this.rutaParcialActual.nodosVisitados.add(this.depositoId);

        // 2. Iniciar la recursión
        backtrackRecursivo(
            this.depositoId,
            this.capacidadCamion,
            0.0, 
            costoHubs,
            puntosDeRecarga
        );

        // 3. Evaluar el resultado de esta combinación
        if (this.mejorRutaVRP == null) {
            // No se encontró una ruta válida para esta combinación
             System.out.println("  -> No se encontró solución VRP para esta combinación.");
            return;
        }

        double costoTotalCombinacion = this.mejorCostoDistanciaVRP + costoHubs;

        if (costoTotalCombinacion < this.costoMinimoGlobal) {
            this.costoMinimoGlobal = costoTotalCombinacion;
            
            this.mejorSolucionGlobal = new Solucion();
            this.mejorSolucionGlobal.costoTotalActivacion = costoHubs;
            this.mejorSolucionGlobal.costoTotalDistancia = this.mejorCostoDistanciaVRP;
            this.mejorSolucionGlobal.hubsActivados = hubsActivos;
            this.mejorSolucionGlobal.rutas = this.mejorRutaVRP; 

            // Este es tu 'sout' de debug
            System.out.printf("  -> NUEVA MEJOR SOLUCIÓN GLOBAL! Costo: %.2f (Dist: %.2f + Hubs: %.2f) [Hubs: %s]\n",
                 this.costoMinimoGlobal,
                 this.mejorCostoDistanciaVRP,
                 costoHubs,
                 hubsActivos.stream().map(Lector.Hub::idNodo).collect(Collectors.toList()));
        }
    }


    private void backtrackRecursivo(int nodoActual, int capacidadRestante,
                                    double costoDistanciaAcumulado,
                                    double costoHubs,
                                    Set<Integer> puntosDeRecarga) {
        
        // --- PODA Nivel 2 (Branch & Bound Global) ---
        if (costoDistanciaAcumulado + costoHubs >= this.costoMinimoGlobal) {
            return; // PODADO (Global)
        }

        // --- PODA Nivel 3 (Branch & Bound Local del VRP) ---
        if (costoDistanciaAcumulado >= this.mejorCostoDistanciaVRP) {
            return; // PODADO (Local)
        }

        // --- CASO BASE (ÉXITO) ---
        if (this.numTotalPaquetesPendientesEstado == 0) {
            int nodoRetorno = encontrarRecargaMasCercana(nodoActual, puntosDeRecarga);
            double costoRetorno = (nodoRetorno != -1) ? distancias[nodoActual][nodoRetorno] : 0.0;
            double costoVRPFinal = costoDistanciaAcumulado + costoRetorno;

            if (costoVRPFinal < this.mejorCostoDistanciaVRP) {
                this.mejorCostoDistanciaVRP = costoVRPFinal;
                
                Solucion.Ruta rutaFinal = new Solucion.Ruta(this.rutaParcialActual);
                if (nodoRetorno != -1) {
                     rutaFinal.nodosVisitados.add(nodoRetorno);
                }
                rutaFinal.costoDistanciaRuta += costoRetorno;
                
                this.mejorRutaVRP = new ArrayList<>(this.rutasActuales);
                this.mejorRutaVRP.add(rutaFinal);
            }
            return; 
        }

        // --- PASO RECURSIVO ---

        // Opción 1: Entregar un paquete (si tenemos capacidad)
        if (capacidadRestante > 0) {
            
            // --- INICIO DE MODIFICACIÓN (PODA HEURÍSTICA) ---
            // En lugar de iterar sobre TODOS los clientes pendientes,
            // iteramos solo sobre los K más cercanos.
            List<Integer> clientesCercanos = encontrarClientesMasCercanos(
                nodoActual,
                this.paquetesPendientesEstado.keySet(),
                K_VECINOS_CERCANOS
            );

            for (int idCliente : clientesCercanos) {
            // --- FIN DE MODIFICACIÓN ---

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
        
        // --- INICIO DE MODIFICACIÓN (LÓGICA DE RECARGA) ---
        // Se puede recargar si NO estamos llenos, O si
        // estamos vacíos Y AÚN QUEDAN paquetes por entregar (recarga obligatoria).
        boolean esRecargaOpcional = (capacidadRestante < this.capacidadCamion);
        boolean esRecargaObligatoria = (capacidadRestante == 0 && this.numTotalPaquetesPendientesEstado > 0);
        
        if (esRecargaOpcional || esRecargaObligatoria) {
        // --- FIN DE MODIFICACIÓN ---
            
            for (int idRecarga : puntosDeRecarga) {
                if (idRecarga == nodoActual) continue; 

                double costoViaje = distancias[nodoActual][idRecarga];

                // 1. MODIFICAR ESTADO
                Solucion.Ruta rutaAnterior = this.rutaParcialActual; 
                this.rutasActuales.add(rutaAnterior); 
                
                this.rutaParcialActual = new Solucion.Ruta();
                this.rutaParcialActual.nodosVisitados.add(idRecarga);

                // 2. RECURSAR
                backtrackRecursivo(
                    idRecarga,
                    this.capacidadCamion, // Capacidad reseteada
                    costoDistanciaAcumulado + costoViaje,
                    costoHubs,
                    puntosDeRecarga
                );

                // 3. DESHACER
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

    // --- INICIO DE NUEVA FUNCIÓN HELPER ---
    /**
     * Encuentra los K clientes más cercanos a un nodo dado, de una lista de pendientes.
     * @param nodoActual El nodo donde está el camión.
     * @param clientesPendientes El Set de IDs de clientes que faltan entregar.
     * @param k El número de vecinos a retornar.
     * @return Una Lista de los K IDs de clientes más cercanos al nodoActual.
     */
    private List<Integer> encontrarClientesMasCercanos(int nodoActual, Set<Integer> clientesPendientes, int k) {
        
        // Convertimos el Set a una Lista para poder ordenarla
        List<Integer> listaClientes = new ArrayList<>(clientesPendientes);

        // Ordenamos la lista comparando la distancia de dos clientes (c1, c2)
        // al nodoActual
        listaClientes.sort((c1, c2) -> 
            Double.compare(distancias[nodoActual][c1], distancias[nodoActual][c2])
        );

        // Devolvemos solo los primeros K elementos (los más cercanos)
        // Usamos Math.min para evitar un error si hay menos de K clientes
        return listaClientes.subList(0, Math.min(listaClientes.size(), k));
    }
    // --- FIN DE NUEVA FUNCIÓN HELPER ---
}