package com.farmacia.ui;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

// Atalhos de teclado comuns às listas: Ctrl N, Ctrl F, "/", Enter, Delete, Esc, F1 (e Ctrl S nos formulários).
public final class AtalhosLista {

    private AtalhosLista() {
    }

    public static <T> void instalar(StackPane host, TextField busca, TableView<T> tabela, Runnable nova,
                                    Consumer<T> editar, Consumer<T> excluir, Runnable ajuda) {
        host.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (Sobreposicao.temCaixaAberta(host)) {
                return;
            }
            Node foco = host.getScene() == null ? null : host.getScene().getFocusOwner();
            boolean digitando = foco instanceof TextInputControl
                    || (foco != null && foco.getParent() instanceof ComboBox<?> cb && cb.isEditable());
            boolean ctrl = e.isControlDown() || e.isMetaDown();
            T selecionado = tabela.getSelectionModel().getSelectedItem();

            if (e.getCode() == KeyCode.F1) {
                e.consume();
                ajuda.run();
            } else if (ctrl && e.getCode() == KeyCode.N) {
                e.consume();
                nova.run();
            } else if (ctrl && e.getCode() == KeyCode.F) {
                e.consume();
                focar(busca);
            } else if (!ctrl && !digitando && e.getCode() == KeyCode.SLASH) {
                e.consume();
                focar(busca);
            } else if (e.getCode() == KeyCode.ESCAPE && foco == busca && !busca.getText().isEmpty()) {
                e.consume();
                busca.clear();
            } else if (foco == tabela && selecionado != null) {
                if (e.getCode() == KeyCode.ENTER) {
                    e.consume();
                    editar.accept(selecionado);
                } else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                    e.consume();
                    excluir.accept(selecionado);
                }
            }
        });
    }

    // Formulários: Ctrl S salva, Esc cancela (quando não há caixa aberta).
    public static void instalarFormulario(StackPane host, Runnable salvar, Runnable cancelar) {
        host.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (Sobreposicao.temCaixaAberta(host)) {
                return;
            }
            boolean ctrl = e.isControlDown() || e.isMetaDown();
            if (ctrl && e.getCode() == KeyCode.S) {
                e.consume();
                salvar.run();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                Node foco = host.getScene() == null ? null : host.getScene().getFocusOwner();
                boolean listaAberta = foco != null && foco.getParent() != null
                        && foco.getParent().getStyleClass().contains("autocomplete-list");
                if (!listaAberta) {
                    e.consume();
                    cancelar.run();
                }
            }
        });
    }

    private static void focar(TextField busca) {
        busca.requestFocus();
        busca.selectAll();
    }
}
