   
 module com.iudigital.supermercadoapp {
        // Exporta el paquete donde se encuentra tu clase principal (SupermercadoApp)
        exports com.iudigital.supermercadoapp.app;

        // Requiere los módulos de JavaFX que tu aplicación utiliza
        requires javafx.controls;
        requires javafx.fxml; // Descomenta si usas FXML (aunque sea solo la plantilla)
        requires javafx.graphics; // Generalmente requerido por javafx.controls
        requires javafx.base;    // Generalmente requerido por otros módulos de JavaFX
    }
