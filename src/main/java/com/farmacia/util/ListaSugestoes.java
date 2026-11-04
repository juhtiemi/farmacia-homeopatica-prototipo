package com.farmacia.util;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import javafx.util.Duration;

import java.util.function.Function;

// Lista de sugestões (autocomplete) como sobreposição: o ListView é managed=false no FXML, então não altera o layout
// do formulário, mas o contêiner também não o dimensiona; por isso o tamanho é definido aqui.
public final class ListaSugestoes {

    private static final double ALTURA_LINHA = 24.0;

    private ListaSugestoes() {
    }

    public static void exibir(ListView<?> lista, Node campo) {
        double desejada = lista.getItems().size() * ALTURA_LINHA + 4;
        lista.setPrefHeight(Math.min(desejada, espacoAbaixo(campo)));
        lista.setVisible(true);
        lista.autosize();
    }

    public static void ocultar(ListView<?> lista) {
        lista.setVisible(false);
    }

    // Texto da sugestão com reticências no meio, para o final (ex.: o RG que distingue homônimos) continuar visível,
    // e sem alargar o ListView (o que criaria uma barra de rolagem horizontal sobre as linhas).
    public static <T> Callback<ListView<T>, ListCell<T>> fabricaCelulas(Function<T, String> texto) {
        return lista -> new ListCell<T>() {
            {
                setPrefWidth(0);
                setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
            }

            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : texto.apply(item));
            }
        };
    }

    // Fecha a lista quando o foco sai do campo, mas só se também não estiver na própria lista: um clique mantido ou o uso
    // da barra de rolagem da lista não pode fechá-la. O pequeno atraso deixa o clique na sugestão ser processado primeiro.
    public static void ocultarAoPerderFoco(TextField campo, ListView<?> lista) {
        Runnable agendar = () -> {
            PauseTransition atraso = new PauseTransition(Duration.millis(150));
            atraso.setOnFinished(e -> {
                if (!campo.isFocused() && !lista.isFocused() && !lista.isPressed()) {
                    ocultar(lista);
                }
            });
            atraso.play();
        };
        campo.focusedProperty().addListener((obs, antes, agora) -> {
            if (!agora) {
                agendar.run();
            }
        });
        lista.focusedProperty().addListener((obs, antes, agora) -> {
            if (!agora) {
                agendar.run();
            }
        });
    }

    // Espaço entre a base do campo e a base da área visível do ScrollPane do formulário (a sobreposição é recortada por ele).
    private static double espacoAbaixo(Node campo) {
        if (campo.getScene() == null) {
            return Double.MAX_VALUE;
        }
        for (Node n = campo.getParent(); n != null; n = n.getParent()) {
            if (n instanceof ScrollPane painel) {
                double baseVisivel = painel.localToScene(painel.getBoundsInLocal()).getMaxY();
                double baseCampo = campo.localToScene(campo.getBoundsInLocal()).getMaxY();
                return Math.max(2 * ALTURA_LINHA, baseVisivel - baseCampo - 4);
            }
        }
        return Double.MAX_VALUE;
    }
}
