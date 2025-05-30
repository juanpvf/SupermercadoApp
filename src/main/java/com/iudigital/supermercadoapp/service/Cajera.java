package com.iudigital.supermercadoapp.service;

import com.iudigital.supermercadoapp.model.Cliente;
import com.iudigital.supermercadoapp.model.Producto;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.util.concurrent.BlockingQueue;

public class Cajera implements Runnable {
    private String nombre;
    private BlockingQueue<Cliente> colaClientes;
    private TextArea logArea;
    private long tiempoTotalCobro; // en milisegundos

    public Cajera(String nombre, BlockingQueue<Cliente> colaClientes, TextArea logArea) {
        this.nombre = nombre;
        this.colaClientes = colaClientes;
        this.logArea = logArea;
        this.tiempoTotalCobro = 0;
    }

    @Override
    public void run() {
        Platform.runLater(() -> logArea.appendText(nombre + " está lista para atender clientes.\n"));
        try {
            while (true) {
                Cliente cliente = colaClientes.take(); // Espera si no hay clientes

                if (cliente == Cliente.POISON_PILL) {
                    Platform.runLater(() -> logArea.appendText(nombre + " ha recibido la señal de terminación.\n"));
                    colaClientes.put(Cliente.POISON_PILL);
                    break; // Sale del bucle y termina el hilo de la cajera
                }

                long inicioCobroCliente = System.currentTimeMillis();

                Platform.runLater(() -> logArea.appendText(nombre + " está atendiendo al Cliente " + cliente.getId() + "\n"));

                double costoTotalCliente = 0;

                for (Producto producto : cliente.getCarritoDeCompras()) {
                    final Producto productoActual = producto;
                    Platform.runLater(() -> logArea.appendText("  Procesando producto: " + productoActual.getNombre() + " (Tiempo: " + productoActual.getTiempoProcesamiento() + "ms)\n"));
                    Thread.sleep(producto.getTiempoProcesamiento());
                    costoTotalCliente += producto.getPrecio();
                    producto.getTiempoProcesamiento();
                }

                long finCobroCliente = System.currentTimeMillis();
                long duracionCobroCliente = finCobroCliente - inicioCobroCliente;
                tiempoTotalCobro += duracionCobroCliente;

                final double finalCostoTotalCliente = costoTotalCliente;
                final long finalDuracionCobroCliente = duracionCobroCliente;
                final int finalClienteId = cliente.getId();

                Platform.runLater(() -> {
                    logArea.appendText("  Cliente " + finalClienteId + " - Costo Total: $" + String.format("%.2f", finalCostoTotalCliente) + "\n");
                    logArea.appendText("  Cliente " + finalClienteId + " - Tiempo Procesado: " + finalDuracionCobroCliente + "ms\n");
                    logArea.appendText(nombre + " ha terminado con el Cliente " + finalClienteId + "\n");
                });
            }
        } catch (InterruptedException e) {
            Platform.runLater(() -> logArea.appendText(nombre + " ha sido interrumpida.\n"));
            Thread.currentThread().interrupt();
        }
    }

    public long getTiempoTotalCobro() {
        return tiempoTotalCobro;
    }

    public String getNombre() {
        return nombre;
    }
}