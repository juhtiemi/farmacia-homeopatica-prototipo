package com.farmacia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = Objects.requireNonNull(getClass().getResource("/fxml/ClienteView.fxml"),
                "FXML não encontrado no classpath: /fxml/ClienteView.fxml");

        Scene scene = new Scene(FXMLLoader.load(fxml));

        stage.setTitle("Farmácia Homeopática — Gerenciamento de Clientes");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
