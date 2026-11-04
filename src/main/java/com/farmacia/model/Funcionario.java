package com.farmacia.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Funcionario {

    private final StringProperty codigo = new SimpleStringProperty();
    private final StringProperty nome = new SimpleStringProperty();
    private final StringProperty cargo = new SimpleStringProperty();
    private final StringProperty cpf = new SimpleStringProperty();
    private final StringProperty telefone = new SimpleStringProperty();
    private final StringProperty login = new SimpleStringProperty();

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

    public String getCargo() {
        return cargo.get();
    }

    public void setCargo(String valor) {
        cargo.set(valor);
    }

    public StringProperty cargoProperty() {
        return cargo;
    }

    public String getCpf() {
        return cpf.get();
    }

    public void setCpf(String valor) {
        cpf.set(valor);
    }

    public StringProperty cpfProperty() {
        return cpf;
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

    public String getLogin() {
        return login.get();
    }

    public void setLogin(String valor) {
        login.set(valor);
    }

    public StringProperty loginProperty() {
        return login;
    }
}
