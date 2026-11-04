package com.farmacia.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Receita {

    private final StringProperty codigo = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> data = new SimpleObjectProperty<>();
    private final StringProperty nomePaciente = new SimpleStringProperty();
    // Cadastro do paciente (quando conhecido): permite abrir a ficha e calcular a idade. O nome acima é o do momento do pedido.
    private final ObjectProperty<Cliente> paciente = new SimpleObjectProperty<>();
    private final ObjectProperty<StatusReceita> status = new SimpleObjectProperty<>(StatusReceita.EM_MANIPULACAO);
    private final StringProperty rgPaciente = new SimpleStringProperty();
    // Responsável (titular) quando o paciente é dependente; guardado como texto, como estava no momento do pedido.
    private final StringProperty nomeResponsavel = new SimpleStringProperty();
    private final StringProperty nomeMedico = new SimpleStringProperty();
    private final StringProperty crm = new SimpleStringProperty();
    private final ObservableList<ItemReceita> itens = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper resumoItens = new ReadOnlyStringWrapper();

    public Receita() {
        // Texto curto dos medicamentos para a lista de receitas; acompanha as mudanças na lista de itens.
        resumoItens.bind(Bindings.createStringBinding(this::montarResumo, itens));
    }

    private String montarResumo() {
        return itens.stream()
                .map(i -> i.getNomeRemedio() + (i.getDinamizacao() == null ? "" : " " + i.getDinamizacao()))
                .collect(Collectors.joining("; "));
    }

    public String getResumoItens() {
        return resumoItens.get();
    }

    public ReadOnlyStringProperty resumoItensProperty() {
        return resumoItens.getReadOnlyProperty();
    }

    // Tudo o que descreve o receituário (remédio, tipo, peso, dinamização, posologia), em minúsculas, para a busca.
    public String textoDoReceituario() {
        return itens.stream()
                .flatMap(i -> Stream.of(i.getNomeRemedio(), i.getTipo(), i.getPeso(), i.getDinamizacao(), i.getPosologia()))
                .filter(t -> t != null)
                .collect(Collectors.joining(" "))
                .toLowerCase(Locale.ROOT);
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

    public LocalDate getData() {
        return data.get();
    }

    public void setData(LocalDate valor) {
        data.set(valor);
    }

    public ObjectProperty<LocalDate> dataProperty() {
        return data;
    }

    public Cliente getPaciente() {
        return paciente.get();
    }

    public void setPaciente(Cliente valor) {
        paciente.set(valor);
    }

    public ObjectProperty<Cliente> pacienteProperty() {
        return paciente;
    }

    public StatusReceita getStatus() {
        return status.get();
    }

    public void setStatus(StatusReceita valor) {
        status.set(valor);
    }

    public ObjectProperty<StatusReceita> statusProperty() {
        return status;
    }

    public String getNomePaciente() {
        return nomePaciente.get();
    }

    public void setNomePaciente(String valor) {
        nomePaciente.set(valor);
    }

    public StringProperty nomePacienteProperty() {
        return nomePaciente;
    }

    public String getRgPaciente() {
        return rgPaciente.get();
    }

    public void setRgPaciente(String valor) {
        rgPaciente.set(valor);
    }

    public StringProperty rgPacienteProperty() {
        return rgPaciente;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel.get();
    }

    public void setNomeResponsavel(String valor) {
        nomeResponsavel.set(valor);
    }

    public StringProperty nomeResponsavelProperty() {
        return nomeResponsavel;
    }

    public String getNomeMedico() {
        return nomeMedico.get();
    }

    public void setNomeMedico(String valor) {
        nomeMedico.set(valor);
    }

    public StringProperty nomeMedicoProperty() {
        return nomeMedico;
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

    public ObservableList<ItemReceita> getItens() {
        return itens;
    }
}
