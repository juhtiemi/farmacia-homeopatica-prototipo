package com.farmacia.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class Produto {

    private final StringProperty codigo = new SimpleStringProperty();
    private final StringProperty nome = new SimpleStringProperty();
    private final StringProperty categoria = new SimpleStringProperty();
    private final ObjectProperty<BigDecimal> preco = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> quantidadeEstoque = new SimpleObjectProperty<>();

    public String getCodigo() {
        return codigo.get();
    }

    public void setCodigo(String valor) {
        codigo.set(valor);
    }

    public StringProperty codigoProperty() {
        return codigo;
    }

    public String getNome() {
        return nome.get();
    }

    public void setNome(String valor) {
        nome.set(valor);
    }

    public StringProperty nomeProperty() {
        return nome;
    }

    public String getCategoria() {
        return categoria.get();
    }

    public void setCategoria(String valor) {
        categoria.set(valor);
    }

    public StringProperty categoriaProperty() {
        return categoria;
    }

    public BigDecimal getPreco() {
        return preco.get();
    }

    public void setPreco(BigDecimal valor) {
        preco.set(valor);
    }

    public ObjectProperty<BigDecimal> precoProperty() {
        return preco;
    }

    public Integer getQuantidadeEstoque() {
        return quantidadeEstoque.get();
    }

    public void setQuantidadeEstoque(Integer valor) {
        quantidadeEstoque.set(valor);
    }

    public ObjectProperty<Integer> quantidadeEstoqueProperty() {
        return quantidadeEstoque;
    }
}
