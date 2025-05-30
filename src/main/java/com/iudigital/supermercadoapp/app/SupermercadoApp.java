package com.iudigital.supermercadoapp.app;

import com.iudigital.supermercadoapp.model.Cliente;
import com.iudigital.supermercadoapp.model.Producto;
import com.iudigital.supermercadoapp.service.Cajera;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SupermercadoApp extends Application {

    private TextArea logArea;
    private Button startButton;
    private Label totalSimulationTimeLabel;
    private TextField numCajerosField;
    private TextField numClientesField;

    private BlockingQueue<Cliente> colaClientes;
    private List<Cajera> cajeras;
    private ExecutorService executorService;

    @Override
    public void start(Stage primaryStage) {
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(400);

        startButton = new Button("Iniciar Simulación");
        totalSimulationTimeLabel = new Label("Tiempo total de simulación: --");

        numCajerosField = new TextField("2");
        numCajerosField.setPromptText("Número de Cajeros");
        numCajerosField.setPrefWidth(100);

        numClientesField = new TextField("10");
        numClientesField.setPromptText("Número de Clientes");
        numClientesField.setPrefWidth(100);

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(5);
        inputGrid.setPadding(new Insets(10));

        inputGrid.add(new Label("Número de Cajeros:"), 0, 0);
        inputGrid.add(numCajerosField, 1, 0);
        inputGrid.add(new Label("Número de Clientes:"), 0, 1);
        inputGrid.add(numClientesField, 1, 1);

        startButton.setOnAction(event -> iniciarSimulacion());

        VBox root = new VBox(10, inputGrid, startButton, totalSimulationTimeLabel, logArea);
        Scene scene = new Scene(root, 600, 600);
        primaryStage.setTitle("Simulación de Cobro en Supermercado");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        });
    }

    private void iniciarSimulacion() {
        int numCajeros;
        try {
            numCajeros = Integer.parseInt(numCajerosField.getText());
            if (numCajeros <= 0) {
                mostrarAlerta("Error de Entrada", "El número de cajeros debe ser mayor que cero.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error de Entrada", "Por favor, ingrese un número válido para los cajeros.");
            return;
        }

        int numClientes;
        try {
            numClientes = Integer.parseInt(numClientesField.getText());
            if (numClientes <= 0) {
                mostrarAlerta("Error de Entrada", "El número de clientes debe ser mayor que cero.");
                return;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Error de Entrada", "Por favor, ingrese un número válido para los clientes.");
            return;
            }

        startButton.setDisable(true);
        logArea.clear();
        totalSimulationTimeLabel.setText("Tiempo total de simulación: Calculando...");

        // La cola debe tener capacidad para todos los clientes + las "poison pills" (una por cajera)
        colaClientes = new ArrayBlockingQueue<>(numClientes + numCajeros);
        cajeras = new ArrayList<>();
        executorService = Executors.newFixedThreadPool(numCajeros);

        // Crear cajeras y asignarlas al pool de hilos
        for (int i = 0; i < numCajeros; i++) {
            Cajera cajera = new Cajera("Cajera " + (i + 1), colaClientes, logArea);
            cajeras.add(cajera);
            executorService.execute(cajera);
        }

        final int finalNumClientes = numClientes;
        final int finalNumCajeros = numCajeros;

        new Thread(() -> {
            // 1. Generar y encolar a todos los clientes reales
            List<Cliente> clientesGenerados = generarClientes(finalNumClientes);
            for (Cliente cliente : clientesGenerados) {
                try {
                    colaClientes.put(cliente);
                    Platform.runLater(() -> logArea.appendText("Cliente " + cliente.getId() + " ha entrado en la cola.\n"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> logArea.appendText("Error al añadir cliente a la cola: " + e.getMessage() + "\n"));
                    break;
                }
            }

            // 2. Enviar una "poison pill" por cada cajera
            // Esto asegura que cada cajera reciba una señal para terminar
            for (int i = 0; i < finalNumCajeros; i++) {
                try {
                    colaClientes.put(Cliente.POISON_PILL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(() -> logArea.appendText("Error al enviar señal de terminación a cajera: " + e.getMessage() + "\n"));
                    break;
                }
            }

            // 3. Iniciar el apagado del ExecutorService
            executorService.shutdown();
            try {
                // Esperar a que todos los hilos terminen.
                boolean terminated = executorService.awaitTermination(1, TimeUnit.HOURS);
                if (!terminated) {
                    Platform.runLater(() -> logArea.appendText("Advertencia: La simulación no terminó en el tiempo esperado. Forzando cierre.\n"));
                    executorService.shutdownNow(); // Forzar el cierre si no termina
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Platform.runLater(() -> logArea.appendText("Simulación interrumpida durante el apagado.\n"));
            }

            // 4. Calcular y mostrar el tiempo total de simulación
            long tiempoTotalSimulacion = 0;
            for (Cajera cajera : cajeras) {
                tiempoTotalSimulacion += cajera.getTiempoTotalCobro();
            }

            final long finalTiempoTotalSimulacion = tiempoTotalSimulacion;

            Platform.runLater(() -> {
                logArea.appendText("\n--- Simulación Finalizada ---\n");
                for (Cajera cajera : cajeras) {
                    logArea.appendText(cajera.getNombre() + " tiempo total de cobro: " + cajera.getTiempoTotalCobro() + "ms\n");
                }
                totalSimulationTimeLabel.setText("Tiempo total de simulación (sumado de cajeras): " + finalTiempoTotalSimulacion + "ms");
                startButton.setDisable(false);
            });

        }).start();
    }

    private List<Cliente> generarClientes(int numClientes) {
        List<Cliente> clientes = new ArrayList<>();
        Random random = new Random();
        String[] nombresProductos = {"Leche", "Pan", "Huevos", "Manzanas", "Arroz", "Pasta", "Jabon", "Shampoo", "Galletas", "Refresco"};

        for (int i = 1; i <= numClientes; i++) {
            List<Producto> carrito = new ArrayList<>();
            int numProductos = 1 + random.nextInt(5);
            for (int j = 0; j < numProductos; j++) {
                String nombreProducto = nombresProductos[random.nextInt(nombresProductos.length)];
                double precio = 1.0 + random.nextDouble() * 1000.0;
                int tiempoProc = 50 + random.nextInt(200);
                carrito.add(new Producto(nombreProducto, precio, tiempoProc));
            }
            clientes.add(new Cliente(i, carrito));
        }
        return clientes;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
