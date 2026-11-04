package com.farmacia.ui;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.skin.VirtualFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Guarda o estado de uma lista (busca, abas, filtros, ordenação, linha escolhida e posição da rolagem) quando o usuário
// abre um cadastro, e o devolve ao voltar: a lista continua onde estava, em vez de recomeçar do início.
// O estado vale para uma única volta; sair pelo menu lateral esquece tudo (ver MenuLateralController).
public final class EstadoLista {

    private static final Map<String, Estado> GUARDADOS = new HashMap<>();

    private EstadoLista() {
    }

    public static final class Estado {
        private String busca = "";
        private int aba;
        private final List<int[]> ordenacao = new ArrayList<>();
        private Object selecionado;
        private int primeiraLinha;
        private final Map<String, Object> extras = new HashMap<>();

        // Valores de filtros que não são a busca nem as abas (listas suspensas, por exemplo).
        public Estado extra(String chave, Object valor) {
            extras.put(chave, valor);
            return this;
        }

        public Object extra(String chave) {
            return extras.get(chave);
        }
    }

    // Fotografia da lista no momento em que o usuário a deixa.
    public static Estado capturar(TextField busca, ToggleGroup abas, TableView<?> tabela) {
        Estado e = new Estado();
        e.busca = busca == null || busca.getText() == null ? "" : busca.getText();
        if (abas != null) {
            e.aba = Math.max(0, abas.getToggles().indexOf(abas.getSelectedToggle()));
        }
        for (TableColumn<?, ?> coluna : tabela.getSortOrder()) {
            e.ordenacao.add(new int[]{tabela.getColumns().indexOf(coluna),
                    coluna.getSortType() == TableColumn.SortType.DESCENDING ? 1 : 0});
        }
        e.selecionado = tabela.getSelectionModel().getSelectedItem();
        e.primeiraLinha = primeiraLinhaVisivel(tabela);
        return e;
    }

    public static void guardar(String chave, Estado estado) {
        GUARDADOS.put(chave, estado);
    }

    // Entrega o estado guardado (uma única vez) ou null se o usuário não está voltando de um cadastro.
    public static Estado retomar(String chave) {
        return GUARDADOS.remove(chave);
    }

    public static void esquecerTudo() {
        GUARDADOS.clear();
    }

    // Reaplica busca, aba, ordenação, seleção e rolagem. Os filtros extras ficam por conta de quem chama.
    @SuppressWarnings("unchecked")
    public static <T> void aplicar(Estado e, TextField busca, ToggleGroup abas, TableView<T> tabela) {
        if (busca != null) {
            busca.setText(e.busca);
        }
        if (abas != null && e.aba < abas.getToggles().size()) {
            Toggle toggle = abas.getToggles().get(e.aba);
            toggle.setSelected(true);
        }
        List<TableColumn<T, ?>> ordem = new ArrayList<>();
        for (int[] o : e.ordenacao) {
            if (o[0] >= 0 && o[0] < tabela.getColumns().size()) {
                TableColumn<T, ?> coluna = tabela.getColumns().get(o[0]);
                coluna.setSortType(o[1] == 1 ? TableColumn.SortType.DESCENDING : TableColumn.SortType.ASCENDING);
                ordem.add(coluna);
            }
        }
        if (!ordem.isEmpty()) {
            tabela.getSortOrder().setAll(ordem);
        }
        if (e.selecionado != null && tabela.getItems().contains(e.selecionado)) {
            tabela.getSelectionModel().select((T) e.selecionado);
        }
        int linha = Math.min(e.primeiraLinha, Math.max(0, tabela.getItems().size() - 1));
        if (linha > 0) {
            depoisDeExibida(tabela, () -> tabela.scrollTo(linha));
        }
    }

    private static int primeiraLinhaVisivel(TableView<?> tabela) {
        try {
            if (tabela.lookup(".virtual-flow") instanceof VirtualFlow<?> fluxo && fluxo.getFirstVisibleCell() != null) {
                return Math.max(0, fluxo.getFirstVisibleCell().getIndex());
            }
        } catch (RuntimeException ignorado) {
            // Sem a posição, a lista volta ao topo, mas com busca e filtros preservados.
        }
        return 0;
    }

    // A tabela só sabe rolar depois de ganhar a aparência (skin), o que acontece no primeiro ciclo de exibição.
    private static void depoisDeExibida(TableView<?> tabela, Runnable acao) {
        if (tabela.getSkin() != null) {
            Platform.runLater(acao);
            return;
        }
        tabela.skinProperty().addListener(new javafx.beans.value.ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.control.Skin<?>> obs,
                                javafx.scene.control.Skin<?> antigo, javafx.scene.control.Skin<?> novo) {
                if (novo != null) {
                    tabela.skinProperty().removeListener(this);
                    Platform.runLater(() -> Platform.runLater(acao));
                }
            }
        });
    }
}
