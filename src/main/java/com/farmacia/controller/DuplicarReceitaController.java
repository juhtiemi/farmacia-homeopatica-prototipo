package com.farmacia.controller;

import com.farmacia.model.ItemReceita;
import com.farmacia.model.Receita;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Controller do diálogo "Duplicar receita": o usuário marca quais medicamentos vão para a nova receita.
public class DuplicarReceitaController {

    @FXML
    private Label lblTitulo;
    @FXML
    private CheckBox chkTodos;
    @FXML
    private ListView<ItemReceita> listItens;
    @FXML
    private Label lblContagem;

    private final Map<ItemReceita, BooleanProperty> marcados = new LinkedHashMap<>();
    private boolean atualizandoTodos = false;

    public void configurar(Receita origem) {
        lblTitulo.setText("Duplicar a receita " + origem.getCodigo() + " — " + origem.getNomePaciente());
        for (ItemReceita item : origem.getItens()) {
            BooleanProperty marcado = new SimpleBooleanProperty(true);
            marcado.addListener((obs, antigo, novo) -> atualizarResumo());
            marcados.put(item, marcado);
        }
        listItens.setItems(javafx.collections.FXCollections.observableArrayList(origem.getItens()));
        listItens.setCellFactory(CheckBoxListCell.forListView(marcados::get, new StringConverter<>() {
            @Override
            public String toString(ItemReceita i) {
                return descrever(i);
            }

            @Override
            public ItemReceita fromString(String texto) {
                return null;
            }
        }));
        atualizarResumo();
    }

    private String descrever(ItemReceita i) {
        StringBuilder sb = new StringBuilder(i.getNomeRemedio());
        anexar(sb, i.getDinamizacao());
        anexar(sb, i.getTipo());
        anexar(sb, i.getPeso());
        if (i.getQuantidade() != null) {
            sb.append("  ·  Qtd ").append(i.getQuantidade());
        }
        if (i.getPosologia() != null) {
            sb.append("  ·  ").append(i.getPosologia());
        }
        return sb.toString();
    }

    private void anexar(StringBuilder sb, String parte) {
        if (parte != null) {
            sb.append(' ').append(parte);
        }
    }

    @FXML
    private void handleSelecionarTodos() {
        boolean valor = chkTodos.isSelected();
        atualizandoTodos = true;
        try {
            marcados.values().forEach(p -> p.set(valor));
        } finally {
            atualizandoTodos = false;
        }
        atualizarResumo();
    }

    private void atualizarResumo() {
        if (atualizandoTodos) {
            return;
        }
        long n = marcados.values().stream().filter(BooleanProperty::get).count();
        lblContagem.setText(n + " de " + marcados.size() + " itens selecionados");
        chkTodos.setSelected(n == marcados.size());
    }

    // Verdadeiro enquanto nenhum item estiver marcado (o diálogo desabilita o botão de confirmar).
    public BooleanBinding nenhumSelecionado() {
        return Bindings.createBooleanBinding(
                () -> marcados.values().stream().noneMatch(BooleanProperty::get),
                marcados.values().toArray(new BooleanProperty[0]));
    }

    // Cópias independentes dos itens marcados, na ordem original.
    public List<ItemReceita> itensSelecionados() {
        return marcados.entrySet().stream()
                .filter(e -> e.getValue().get())
                .map(e -> e.getKey().copiar())
                .toList();
    }
}
