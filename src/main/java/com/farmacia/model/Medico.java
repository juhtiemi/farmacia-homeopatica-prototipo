package com.farmacia.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Medico {

    private final StringProperty codigo = new SimpleStringProperty();
    private final StringProperty nome = new SimpleStringProperty();
    private final StringProperty crm = new SimpleStringProperty();
    private final StringProperty telefone = new SimpleStringProperty();
    private final StringProperty rua = new SimpleStringProperty();
    private final StringProperty numero = new SimpleStringProperty();
    private final StringProperty bairro = new SimpleStringProperty();
    private final StringProperty cep = new SimpleStringProperty();
    private final StringProperty complemento = new SimpleStringProperty();
    private final StringProperty estado = new SimpleStringProperty();
    private final StringProperty cidade = new SimpleStringProperty();

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

    public String getCrm() {
        return crm.get();
    }

    public void setCrm(String valor) {
        crm.set(valor);
    }

    public StringProperty crmProperty() {
        return crm;
    }

    public String getTelefone() {
        return telefone.get();
    }

    public void setTelefone(String valor) {
        telefone.set(valor);
    }

    public StringProperty telefoneProperty() {
        return telefone;
    }

    public String getRua() {
        return rua.get();
    }

    public void setRua(String valor) {
        rua.set(valor);
    }

    public StringProperty ruaProperty() {
        return rua;
    }

    public String getNumero() {
        return numero.get();
    }

    public void setNumero(String valor) {
        numero.set(valor);
    }

    public StringProperty numeroProperty() {
        return numero;
    }

    public String getBairro() {
        return bairro.get();
    }

    public void setBairro(String valor) {
        bairro.set(valor);
    }

    public StringProperty bairroProperty() {
        return bairro;
    }

    public String getCep() {
        return cep.get();
    }

    public void setCep(String valor) {
        cep.set(valor);
    }

    public StringProperty cepProperty() {
        return cep;
    }

    public String getComplemento() {
        return complemento.get();
    }

    public void setComplemento(String valor) {
        complemento.set(valor);
    }

    public StringProperty complementoProperty() {
        return complemento;
    }

    public String getEstado() {
        return estado.get();
    }

    public void setEstado(String valor) {
        estado.set(valor);
    }

    public StringProperty estadoProperty() {
        return estado;
    }

    public String getCidade() {
        return cidade.get();
    }

    public void setCidade(String valor) {
        cidade.set(valor);
    }

    public StringProperty cidadeProperty() {
        return cidade;
    }
}
