package com.farmacia.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ItemReceita {

    private final StringProperty nomeRemedio = new SimpleStringProperty();
    private final StringProperty tipo = new SimpleStringProperty();
    private final StringProperty peso = new SimpleStringProperty();
    private final StringProperty dinamizacao = new SimpleStringProperty();
    private final ObjectProperty<Integer> quantidade = new SimpleObjectProperty<>();
    private final StringProperty posologia = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dataFabricacao = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> dataValidade = new SimpleObjectProperty<>();
    private final ObjectProperty<BigDecimal> valor = new SimpleObjectProperty<>();

    public ItemReceita() {
    }

    public ItemReceita(String nomeRemedio, String tipo, String peso, String dinamizacao,
                        Integer quantidade, String posologia) {
        setNomeRemedio(nomeRemedio);
        setTipo(tipo);
        setPeso(peso);
        setDinamizacao(dinamizacao);
        setQuantidade(quantidade);
        setPosologia(posologia);
    }

    // Cópia independente para duplicar um pedido: fabricação, validade e valor variam a cada manipulação e não são copiados.
    public ItemReceita copiar() {
        return new ItemReceita(getNomeRemedio(), getTipo(), getPeso(), getDinamizacao(), getQuantidade(), getPosologia());
    }

    public String getNomeRemedio() {
        return nomeRemedio.get();
    }

    public void setNomeRemedio(String valor) {
        nomeRemedio.set(valor);
    }

    public StringProperty nomeRemedioProperty() {
        return nomeRemedio;
    }

    public String getTipo() {
        return tipo.get();
    }

    public void setTipo(String valor) {
        tipo.set(valor);
    }

    public StringProperty tipoProperty() {
        return tipo;
    }

    public String getPeso() {
        return peso.get();
    }

    public void setPeso(String valor) {
        peso.set(valor);
    }

    public StringProperty pesoProperty() {
        return peso;
    }

    public String getDinamizacao() {
        return dinamizacao.get();
    }

    public void setDinamizacao(String valor) {
        dinamizacao.set(valor);
    }

    public StringProperty dinamizacaoProperty() {
        return dinamizacao;
    }

    public Integer getQuantidade() {
        return quantidade.get();
    }

    public void setQuantidade(Integer valor) {
        quantidade.set(valor);
    }

    public ObjectProperty<Integer> quantidadeProperty() {
        return quantidade;
    }

    public String getPosologia() {
        return posologia.get();
    }

    public void setPosologia(String valor) {
        posologia.set(valor);
    }

    public StringProperty posologiaProperty() {
        return posologia;
    }

    public LocalDate getDataFabricacao() {
        return dataFabricacao.get();
    }

    public void setDataFabricacao(LocalDate valor) {
        dataFabricacao.set(valor);
    }

    public ObjectProperty<LocalDate> dataFabricacaoProperty() {
        return dataFabricacao;
    }

    public LocalDate getDataValidade() {
        return dataValidade.get();
    }

    public void setDataValidade(LocalDate valor) {
        dataValidade.set(valor);
    }

    public ObjectProperty<LocalDate> dataValidadeProperty() {
        return dataValidade;
    }

    public BigDecimal getValor() {
        return valor.get();
    }

    public void setValor(BigDecimal novoValor) {
        valor.set(novoValor);
    }

    public ObjectProperty<BigDecimal> valorProperty() {
        return valor;
    }
}
