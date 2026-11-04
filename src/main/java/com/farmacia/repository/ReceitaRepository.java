package com.farmacia.repository;

import com.farmacia.model.ItemReceita;
import com.farmacia.model.Receita;
import com.farmacia.model.StatusReceita;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReceitaRepository extends RepositorioEmMemoria<Receita> {

    private static final ReceitaRepository INSTANCIA = new ReceitaRepository();

    public static ReceitaRepository getInstance() {
        return INSTANCIA;
    }

    private ReceitaRepository() {
        super("REC", Receita::setCodigo);
        carregarInicial(List.of(
                comStatus(StatusReceita.ENTREGUE, criar("REC0001", LocalDate.of(2026, 8, 20),
                        "Maria Eduarda Santos", "12.345.678-9",
                        "Dr. Ricardo Fontes", "CRM 45210-SP",
                        item("Arnica Montana", "Glóbulo", "10g", "6CH", 1, "3 glóbulos, 3x ao dia",
                                LocalDate.of(2026, 8, 21), LocalDate.of(2026, 11, 21), "128.50"))),
                comStatus(StatusReceita.ENTREGUE, criar("REC0002", LocalDate.of(2026, 8, 25),
                        "João Pedro Oliveira", "23.456.789-0",
                        "Dra. Patrícia Nogueira", "CRM 33876-SP",
                        item("Belladonna", "Glóbulo", "10g", "30CH", 2, "5 glóbulos ao deitar",
                                null, null, "95.00"))),
                comStatus(StatusReceita.PRONTA, criar("REC0003", LocalDate.of(2026, 9, 1),
                        "Ana Beatriz Costa", null,
                        "Dr. Marcelo Andrade", "CRM 51023-RJ",
                        item("Sulphur", "Glóbulo", null, "200K", 1, "3 glóbulos 1x por semana",
                                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 12, 2), null),
                        item("Nux Vomica", "Gotas", "30ml", "12CH", 1, "10 gotas antes das refeições",
                                null, null, "42.00"))),
                // Paciente dependente: o nome na receita é o dela; o titular fica como responsável.
                comStatus(StatusReceita.EM_MANIPULACAO, comResponsavel("Maria Eduarda Santos", criar("REC0004", LocalDate.of(2026, 9, 10),
                        "Helena Santos Oliveira", null,
                        "Dra. Patrícia Nogueira", "CRM 33876-SP",
                        item("Chamomilla", "Gotas", "30ml", "6CH", 1, "5 gotas, 3x ao dia",
                                LocalDate.of(2026, 9, 11), LocalDate.of(2026, 12, 11), "58.00"))))
        ));
    }

    private static Receita comStatus(StatusReceita status, Receita receita) {
        receita.setStatus(status);
        return receita;
    }

    private static Receita comResponsavel(String responsavel, Receita receita) {
        receita.setNomeResponsavel(responsavel);
        return receita;
    }

    private static Receita criar(String codigo, LocalDate data, String paciente, String rgPaciente,
                                  String medico, String crm, ItemReceita... itens) {
        Receita receita = new Receita();
        receita.setCodigo(codigo);
        receita.setData(data);
        receita.setNomePaciente(paciente);
        receita.setPaciente(ClienteRepository.getInstance().getItens().stream()
                .filter(c -> paciente.equals(c.getNome())).findFirst().orElse(null));
        receita.setRgPaciente(rgPaciente);
        receita.setNomeMedico(medico);
        receita.setCrm(crm);
        receita.getItens().addAll(itens);
        return receita;
    }

    private static ItemReceita item(String remedio, String tipo, String peso, String dinamizacao, Integer quantidade,
                                    String posologia, LocalDate fabricacao, LocalDate validade, String valor) {
        ItemReceita item = new ItemReceita(remedio, tipo, peso, dinamizacao, quantidade, posologia);
        item.setDataFabricacao(fabricacao);
        item.setDataValidade(validade);
        item.setValor(valor == null ? null : new BigDecimal(valor));
        return item;
    }
}
