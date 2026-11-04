package com.farmacia.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import java.time.LocalDate;
import java.time.Period;

public class Cliente {

    // Anos completos entre o nascimento e hoje; null se a data for ausente ou futura.
    public static Integer calcularIdade(LocalDate nascimento) {
        if (nascimento == null || nascimento.isAfter(LocalDate.now())) {
            return null;
        }
        return Period.between(nascimento, LocalDate.now()).getYears();
    }

    private final StringProperty codigo = new SimpleStringProperty();
    private final StringProperty nome = new SimpleStringProperty();
    private final StringProperty rua = new SimpleStringProperty();
    private final StringProperty numero = new SimpleStringProperty();
    private final StringProperty bairro = new SimpleStringProperty();
    private final StringProperty cep = new SimpleStringProperty();
    private final StringProperty complemento = new SimpleStringProperty();
    private final StringProperty estado = new SimpleStringProperty();
    private final StringProperty cidade = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> dataNascimento = new SimpleObjectProperty<>();
    private final ObjectProperty<Integer> idade = new SimpleObjectProperty<>();
    // Vínculo opcional: um dependente é um cliente com responsável (titular) e parentesco em relação a ele.
    private final ObjectProperty<Cliente> responsavel = new SimpleObjectProperty<>();
    private final StringProperty parentesco = new SimpleStringProperty();
    private final ObservableValue<String> telefoneDoResponsavel = responsavel.flatMap(Cliente::telefoneProperty);
    private final ReadOnlyStringWrapper nomeResponsavel = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper vinculo = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper telefoneContato = new ReadOnlyStringWrapper();
    private final StringProperty cpf = new SimpleStringProperty();
    private final StringProperty rg = new SimpleStringProperty();
    private final StringProperty telefone = new SimpleStringProperty();

    public Cliente() {
        // Acompanham o responsável: renomear ou trocar o telefone dele atualiza as tabelas automaticamente.
        nomeResponsavel.bind(responsavel.flatMap(Cliente::nomeProperty));
        vinculo.bind(Bindings.createStringBinding(
                () -> responsavel.get() == null ? "Titular"
                        : (parentesco.get() == null ? "Dependente" : parentesco.get()) + " de " + nomeResponsavel.get(),
                responsavel, parentesco, nomeResponsavel));
        // Telefone para contato: o próprio ou, na falta dele, o do responsável.
        telefoneContato.bind(Bindings.createStringBinding(
                () -> telefone.get() != null ? telefone.get() : telefoneDoResponsavel.getValue(),
                telefone, responsavel, telefoneDoResponsavel));
    }

    public boolean isDependente() {
        return responsavel.get() != null;
    }

    public Cliente getResponsavel() {
        return responsavel.get();
    }

    public void setResponsavel(Cliente valor) {
        responsavel.set(valor);
    }

    public ObjectProperty<Cliente> responsavelProperty() {
        return responsavel;
    }

    public String getParentesco() {
        return parentesco.get();
    }

    public void setParentesco(String valor) {
        parentesco.set(valor);
    }

    public StringProperty parentescoProperty() {
        return parentesco;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel.get();
    }

    public ReadOnlyStringProperty nomeResponsavelProperty() {
        return nomeResponsavel.getReadOnlyProperty();
    }

    public String getVinculo() {
        return vinculo.get();
    }

    public ReadOnlyStringProperty vinculoProperty() {
        return vinculo.getReadOnlyProperty();
    }

    public String getTelefoneContato() {
        return telefoneContato.get();
    }

    public ReadOnlyStringProperty telefoneContatoProperty() {
        return telefoneContato.getReadOnlyProperty();
    }

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

    public LocalDate getDataNascimento() {
        return dataNascimento.get();
    }

    public void setDataNascimento(LocalDate valor) {
        dataNascimento.set(valor);
    }

    public ObjectProperty<LocalDate> dataNascimentoProperty() {
        return dataNascimento;
    }

    public Integer getIdade() {
        return idade.get();
    }

    public void setIdade(Integer valor) {
        idade.set(valor);
    }

    public ObjectProperty<Integer> idadeProperty() {
        return idade;
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

    public String getRg() {
        return rg.get();
    }

    public void setRg(String valor) {
        rg.set(valor);
    }

    public StringProperty rgProperty() {
        return rg;
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
}
