import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Lector {

    // --- ESTRUCTURAS DE DATOS ---
    public record Nodo(int id, int x, int y) {}
    public record Hub(int idNodo, double costoActivacion) {}
    public record Paquete(int id, int idNodoOrigen, int idNodoDestino) {}

    public static class Problema {
        public int numNodos = 0;
        public int numHubs = 0;
        public int numPaquetes = 0;
        public int capacidadCamion = 0;
        public int depositoId = 0;

        public List<Nodo> nodos = new ArrayList<>();
        public List<Hub> hubs = new ArrayList<>();
        public List<Paquete> paquetes = new ArrayList<>();
        public double[][] grafoDistancias;
    }

    // --- LÓGICA DE PARSEO ---
    private static String eliminarComentario(String linea) {
        int commentIndex = linea.indexOf("//");
        if (commentIndex != -1) {
            linea = linea.substring(0, commentIndex);
        }
        return linea.trim();
    }

 
    //Lee un archivo de problema y retorna un objeto Problema.
    public static Problema leerArchivo(String nombreArchivo) {
        Problema p = new Problema();

        enum Seccion { CONFIG, NODOS, HUBS, PAQUETES, ARISTAS };
        Seccion seccionActual = Seccion.CONFIG; 
        String rutaCompleta = "Output/" + nombreArchivo;

        try (BufferedReader br = new BufferedReader(new FileReader(rutaCompleta))) {
            String lineaOriginal;
            
            while ((lineaOriginal = br.readLine()) != null) {
                
                // 1. Limpiamos la línea para chequearla
                String lineaTrimmed = lineaOriginal.trim();
                
                // 2. Si la línea está vacía, la saltamos
                if (lineaTrimmed.isEmpty()) {
                    continue;
                }

                // --- 3. DETECCIÓN DE CAMBIO DE SECCIÓN ---
                if (lineaTrimmed.startsWith("// --- NODOS")) {
                    seccionActual = Seccion.NODOS;
                    continue;
                }
                if (lineaTrimmed.startsWith("// --- HUBS")) {
                    seccionActual = Seccion.HUBS;
                    continue; 
                }
                if (lineaTrimmed.startsWith("// --- PAQUETES")) {
                    seccionActual = Seccion.PAQUETES;
                    continue;
                }
                if (lineaTrimmed.startsWith("// --- ARISTAS")) {
                    seccionActual = Seccion.ARISTAS;
                    continue;
                }
                
                // --- 4. PROCESADO DE LÍNEA DE DATOS ---
                String lineaDeDatos = eliminarComentario(lineaOriginal);

                if (lineaDeDatos.isEmpty()) {
                    continue;
                }

                String[] partes = lineaDeDatos.split("\\s+");
                if (partes.length == 0 || (partes.length == 1 && partes[0].isEmpty())) {
                    continue;
                }

                try {
                    switch (seccionActual) {
                        case CONFIG:
                            if (partes.length < 2) continue;
                            if (partes[0].equals("NODOS")) p.numNodos = Integer.parseInt(partes[1]);
                            else if (partes[0].equals("HUBS")) p.numHubs = Integer.parseInt(partes[1]);
                            else if (partes[0].equals("PAQUETES")) p.numPaquetes = Integer.parseInt(partes[1]);
                            else if (partes[0].equals("CAPACIDAD_CAMION")) p.capacidadCamion = Integer.parseInt(partes[1]);
                            else if (partes[0].equals("DEPOSITO_ID")) {
                                p.depositoId = Integer.parseInt(partes[1]);
                                p.grafoDistancias = new double[p.numNodos][p.numNodos];
                            }
                            break;
                        
                        case NODOS:
                            if (partes.length >= 3 && p.nodos.size() < p.numNodos) {
                                p.nodos.add(new Nodo(
                                    Integer.parseInt(partes[0]),
                                    Integer.parseInt(partes[1]),
                                    Integer.parseInt(partes[2])
                                ));
                            }
                            break;
                        
                        case HUBS:
                            if (partes.length >= 2 && p.hubs.size() < p.numHubs) {
                                // Reemplazamos la coma por un punto
                                String costoConPunto = partes[1].replace(',', '.');
                                p.hubs.add(new Hub(
                                    Integer.parseInt(partes[0]),
                                    Double.parseDouble(costoConPunto)
                                ));
                            }
                            break;

                        case PAQUETES:
                            if (partes.length >= 3 && p.paquetes.size() < p.numPaquetes) {
                                p.paquetes.add(new Paquete(
                                    Integer.parseInt(partes[0]),
                                    Integer.parseInt(partes[1]),
                                    Integer.parseInt(partes[2])
                                ));
                            }
                            break;

                        case ARISTAS:
                                if (partes.length >= 3) {
                                String pesoConPunto = partes[2].replace(',', '.');
                                int u = Integer.parseInt(partes[0]);
                                int v = Integer.parseInt(partes[1]);
                                double peso = Double.parseDouble(pesoConPunto);
                                if (u < p.numNodos && v < p.numNodos) {
                                    p.grafoDistancias[u][v] = peso;
                                    p.grafoDistancias[v][u] = peso;
                                }
                            }
                            break;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Advertencia: Se saltó una línea mal formada: " + lineaOriginal);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("Advertencia: Se saltó una línea con datos incompletos: " + lineaOriginal);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: No se pudo abrir el archivo '" + nombreArchivo + "'");
            return null;
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
            return null;
        }

        // Verificación final
        if (p.nodos.size() != p.numNodos) {
            System.err.println("Advertencia: Se esperaban " + p.numNodos + " nodos, pero se leyeron " + p.nodos.size());
        }
        if (p.hubs.size() != p.numHubs) {
            System.err.println("Advertencia: Se esperaban " + p.numHubs + " hubs, pero se leyeron " + p.hubs.size());
        }
        if (p.paquetes.size() != p.numPaquetes) {
            System.err.println("Advertencia: Se esperaban " + p.numPaquetes + " paquetes, pero se leyeron " + p.paquetes.size());
        }


        return p;
    }


    public static void imprimirProblema(Problema p) {
        System.out.println("\n================================ RESUMEN DEL PROBLEMA CARGADO ===============================");
        System.out.println("\n--- CONFIGURACION ---");
        System.out.printf("Total de Nodos:\t\t%d\n", p.numNodos);
        System.out.printf("Total de Hubs:\t\t%d\n", p.numHubs);
        System.out.printf("Total de Paquetes:\t%d\n", p.numPaquetes);
        System.out.printf("Capacidad del Camión:\t%d\n", p.capacidadCamion);
        System.out.printf("ID del Depósito:\t\t%d\n", p.depositoId);

        System.out.println("\n--- NODOS ---");
        for (Nodo nodo : p.nodos) {
            System.out.printf("   Nodo %2d: (x=%4d, y=%4d)\n", nodo.id(), nodo.x(), nodo.y());
        }

        System.out.println("\n--- HUBS ---");
        for (Hub hub : p.hubs) {
            System.out.printf("   Hub en Nodo %2d: Costo de Activación = %.2f\n", hub.idNodo(), hub.costoActivacion());
        }

        System.out.println("\n--- PAQUETES ---");
        for (Paquete paquete : p.paquetes) {
            System.out.printf("   Paquete %2d: Origen=%d -> Destino=%d\n", paquete.id(), paquete.idNodoOrigen(), paquete.idNodoDestino());
        }

        System.out.println("\n--- MUESTRA DEL GRAFO (MATRIZ DE ADYACENCIA) ---");
        int tamMuestra = Math.min(10, p.numNodos);
        System.out.print("        ");
        for (int j = 0; j < tamMuestra; j++) System.out.printf("%7d ", j);
        System.out.println();
        System.out.print("----");
        for (int j = 0; j < tamMuestra; j++) System.out.print("--------");
        System.out.println();
        for (int i = 0; i < tamMuestra; i++) {
            System.out.printf("%4d| ", i);
            for (int j = 0; j < tamMuestra; j++) {
                System.out.printf("%7.2f ", p.grafoDistancias[i][j]);
            }
            System.out.println();
        }
        System.out.println("======================================================================================\n");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java Lector <nombre_del_archivo.txt>");
            System.exit(1);
        }
        String nombreArchivo = args[0];
        System.out.println("Leyendo el archivo de problema: " + nombreArchivo);
        Problema problema = leerArchivo(nombreArchivo);
        if (problema == null) {
            System.out.println("\n>> Hubo un error al leer o procesar el archivo.");
            System.exit(1);
        }
        System.out.println("\n¡Archivo leído y procesado con éxito!");
        imprimirProblema(problema);
        System.out.println("Memoria gestionada por el recolector de basura.");
    }
}