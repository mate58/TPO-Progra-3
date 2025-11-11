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

    private Solucion mejorSolucionGlobal;
    private double costoMinimoGlobal = Double.POSITIVE_INFINITY;

    private Map<String, Double> memoVRP;
    private Map<String, Integer> memoPath; 

    public Solver(Lector.Problema problema) {
        this.problema = problema;
        this.distancias = problema.grafoDistancias;
        this.capacidadCamion = problema.capacidadCamion;
        
        this.demandasPorNodo = new HashMap<>();
        for (Lector.Paquete p : problema.paquetes) {
            int idDestino = p.idNodoDestino();
            this.demandasPorNodo.put(idDestino, this.demandasPorNodo.getOrDefault(idDestino, 0) + 1);
        }
        System.out.println("Demandas pre-procesadas (NodoDestino -> Cantidad):");
        System.out.println(this.demandasPorNodo);
    }

    public Solucion encontrarMejorSolucion() {
        System.out.println("\nIniciando búsqueda de la mejor combinación de Hubs...");
        List<Lector.Hub> hubs = problema.hubs;
        boolean[] hubsActivos = new boolean[hubs.size()]; 
        buscarCombinacionHubs(0, hubs, hubsActivos);
        return this.mejorSolucionGlobal;
    }

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

        this.memoVRP = new HashMap<>();
        this.memoPath = new HashMap<>();
        Map<Integer, Integer> paquetesPendientes = new HashMap<>(this.demandasPorNodo);

        double costoDistancia = backtrackRuteo(
            problema.depositoId,
            this.capacidadCamion,
            paquetesPendientes,
            puntosDeRecarga
        );
        
        solucionParcial.costoTotalDistancia = costoDistancia;

        if (solucionParcial.getCostoTotal() < this.costoMinimoGlobal) {
            this.costoMinimoGlobal = solucionParcial.getCostoTotal();

            reconstruirRutas(solucionParcial, puntosDeRecarga);
            this.mejorSolucionGlobal = solucionParcial;
            
            System.out.printf("  -> Nueva mejor solución! Costo: %.2f (Dist: %.2f + Hubs: %.2f) [Hubs: %s]\n",
                 this.costoMinimoGlobal,
                 costoDistancia,
                 solucionParcial.costoTotalActivacion,
                 solucionParcial.hubsActivados.stream().map(Lector.Hub::idNodo).collect(Collectors.toList()));
        }
    }

    private double backtrackRuteo(int nodoActual, int capacidadRestante,
                                  Map<Integer, Integer> paquetesPendientes,
                                  Set<Integer> puntosDeRecarga) {
        
        if (paquetesPendientes.isEmpty()) {
            return encontrarDistanciaARecargaMasCercana(nodoActual, puntosDeRecarga);
        }

        String estadoKey = nodoActual + "_" + capacidadRestante + "_" + paquetesPendientes.toString();
        if (memoVRP.containsKey(estadoKey)) {
            return memoVRP.get(estadoKey);
        }

        double costoMinimo = Double.POSITIVE_INFINITY;
        int mejorDecision = -1; 

        if (capacidadRestante > 0) {
            for (int idCliente : new ArrayList<>(paquetesPendientes.keySet())) {
                double costoViaje = distancias[nodoActual][idCliente];
                Map<Integer, Integer> proximoEstadoPaquetes = new HashMap<>(paquetesPendientes);
                int demandaRestante = proximoEstadoPaquetes.get(idCliente) - 1;
                
                if (demandaRestante == 0) proximoEstadoPaquetes.remove(idCliente);
                else proximoEstadoPaquetes.put(idCliente, demandaRestante);

                double costoFuturo = backtrackRuteo(
                    idCliente, capacidadRestante - 1, proximoEstadoPaquetes, puntosDeRecarga);
                
                double costoTotal = costoViaje + costoFuturo;
                if (costoTotal < costoMinimo) {
                    costoMinimo = costoTotal;
                    mejorDecision = idCliente;
                }
            }
        }

        if (capacidadRestante == 0 && !paquetesPendientes.isEmpty()) {
            double costoMinimoRecarga = Double.POSITIVE_INFINITY;
            int mejorPuntoRecarga = -1;
            
            for (int idRecarga : puntosDeRecarga) {
                double costoViaje = distancias[nodoActual][idRecarga];
                double costoFuturo = backtrackRuteo(
                    idRecarga, this.capacidadCamion, paquetesPendientes, puntosDeRecarga);
                
                double costoTotalRecarga = costoViaje + costoFuturo;
                if (costoTotalRecarga < costoMinimoRecarga) {
                    costoMinimoRecarga = costoTotalRecarga;
                    mejorPuntoRecarga = idRecarga;
                }
            }
            costoMinimo = costoMinimoRecarga;
            mejorDecision = mejorPuntoRecarga;
        }


        if (paquetesPendientes.isEmpty()) {
            mejorDecision = encontrarRecargaMasCercana(nodoActual, puntosDeRecarga);
            costoMinimo = distancias[nodoActual][mejorDecision];
        }

        memoVRP.put(estadoKey, costoMinimo);
        memoPath.put(estadoKey, mejorDecision);
        return costoMinimo;
    }


    private void reconstruirRutas(Solucion sol, Set<Integer> puntosDeRecarga) {
        sol.rutas = new ArrayList<>();
        Map<Integer, Integer> paquetesPendientes = new HashMap<>(this.demandasPorNodo);
        
        int nodoActual = problema.depositoId;
        int capacidadActual = this.capacidadCamion;
        
        Solucion.Ruta rutaActual = new Solucion.Ruta();
        rutaActual.nodosVisitados.add(nodoActual);

        while (!paquetesPendientes.isEmpty()) {
            String estadoKey = nodoActual + "_" + capacidadActual + "_" + paquetesPendientes.toString();
            int proximoNodo = memoPath.getOrDefault(estadoKey, -1);

            if (proximoNodo == -1) {
                System.err.println("Error: No se pudo reconstruir la ruta. Estado no encontrado en memoPath: " + estadoKey);
                break;
            }

            double costoViaje = distancias[nodoActual][proximoNodo];
            rutaActual.costoDistanciaRuta += costoViaje;

            nodoActual = proximoNodo;
            rutaActual.nodosVisitados.add(nodoActual);
            
            if (puntosDeRecarga.contains(nodoActual)) {
                if(rutaActual.paquetesEntregados > 0) {
                    sol.rutas.add(rutaActual);
                }
                rutaActual = new Solucion.Ruta();
                rutaActual.nodosVisitados.add(nodoActual);
                capacidadActual = this.capacidadCamion;
            
            } else if (paquetesPendientes.containsKey(nodoActual)) {
                capacidadActual--;
                rutaActual.paquetesEntregados++;
                
                int demandaRestante = paquetesPendientes.get(nodoActual) - 1;
                if (demandaRestante == 0) paquetesPendientes.remove(nodoActual);
                else paquetesPendientes.put(nodoActual, demandaRestante);
            }
        }
        

        if(rutaActual.nodosVisitados.size() > 1) {
            if (!puntosDeRecarga.contains(nodoActual)) {
                int recargaFinal = encontrarRecargaMasCercana(nodoActual, puntosDeRecarga);
                rutaActual.costoDistanciaRuta += distancias[nodoActual][recargaFinal];
                rutaActual.nodosVisitados.add(recargaFinal);
            }
            sol.rutas.add(rutaActual);
        }
    }

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
    
    private double encontrarDistanciaARecargaMasCercana(int nodoActual, Set<Integer> puntosDeRecarga) {
        int idRecarga = encontrarRecargaMasCercana(nodoActual, puntosDeRecarga);
        return (idRecarga != -1) ? distancias[nodoActual][idRecarga] : 0.0;
    }
}