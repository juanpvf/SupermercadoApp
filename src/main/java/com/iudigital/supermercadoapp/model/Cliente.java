package com.iudigital.supermercadoapp.model;

import java.util.ArrayList;
import java.util.List;

public class Cliente {
    private int id;
    private List<Producto> carritoDeCompras;

    // Instancia estática para la "píldora venenosa"
    // Usamos un ID negativo (-1) para distinguirlo de clientes reales
    public static final Cliente POISON_PILL = new Cliente(-1, new ArrayList<>());

    public Cliente(int id, List<Producto> carritoDeCompras) {
        this.id = id;
        this.carritoDeCompras = carritoDeCompras;
    }

    // Getters
    public int getId() { return id; }
    public List<Producto> getCarritoDeCompras() { return carritoDeCompras; }

    public double getTotalCompra() {
        return carritoDeCompras.stream().mapToDouble(Producto::getPrecio).sum();
    }

    public int getTiempoTotalProcesamiento() {
        return carritoDeCompras.stream().mapToInt(Producto::getTiempoProcesamiento).sum();
    }
}