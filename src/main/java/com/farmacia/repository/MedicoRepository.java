package com.farmacia.repository;

import com.farmacia.model.Medico;
import com.farmacia.model.Receita;

import java.util.List;
import java.util.Locale;

public final class MedicoRepository extends RepositorioEmMemoria<Medico> {

    private static final MedicoRepository INSTANCIA = new MedicoRepository();

    public static MedicoRepository getInstance() {
        return INSTANCIA;
    }

    private MedicoRepository() {
        super("MED", Medico::setCodigo);
        carregarInicial(List.of(
                criar("MED0001", "Dr. Ricardo Fontes", "CRM 45210-SP", "(11) 3456-7890",
                        "Avenida Paulista", "1578", "Bela Vista", "01310-200", "Conj. 142", "SP", "São Paulo"),
                criar("MED0002", "Dra. Patrícia Nogueira", "CRM 33876-SP", "(11) 99123-4567",
                        "Rua Augusta", "2200", "Consolação", "01304-000", null, "SP", "São Paulo"),
                criar("MED0003", "Dr. Marcelo Andrade", "CRM 51023-RJ", null,
                        "Rua Visconde de Pirajá", "550", "Ipanema", "22410-002", "Sala 301", "RJ", "Rio de Janeiro"),
                criar("MED0004", "Dra. Camila Rezende", "CRM 28710-MG", null,
                        null, null, null, null, null, null, null),
                criar("MED0005", "Dr. Eduardo Vasconcelos", "CRM 39654-SP", "(11) 3050-1234",
                        "Rua Pamplona", "818", "Jardim Paulista", "01405-001", "Conj. 9", "SP", "São Paulo"),
                criar("MED0006", "Dra. Beatriz Carvalho", "CRM 47281-PR", null,
                        "Rua Mateus Leme", "1970", "São Francisco", "80510-190", null, "PR", "Curitiba")
        ));
    }

    // Receitas emitidas pelo médico. O vínculo é o nome gravado na receita (nomeMedico), então renomear
    // o médico não muda as receitas antigas: elas continuam no histórico com o nome do momento do pedido.
    public List<Receita> receitasDe(Medico medico) {
        String nome = medico == null || medico.getNome() == null ? "" : medico.getNome().trim();
        if (nome.isEmpty()) {
            return List.of();
        }
        return ReceitaRepository.getInstance().getItens().stream()
                .filter(r -> r.getNomeMedico() != null && r.getNomeMedico().trim().equalsIgnoreCase(nome))
                .toList();
    }

    public int contarReceitas(Medico medico) {
        return receitasDe(medico).size();
    }

    // Outro médico (diferente de 'ignorar') com o mesmo CRM, em qualquer formato de digitação.
    public Medico buscarPorCrm(String crm, Medico ignorar) {
        String chave = chaveCrm(crm);
        if (chave.isEmpty()) {
            return null;
        }
        return getItens().stream()
                .filter(m -> m != ignorar && chaveCrm(m.getCrm()).equals(chave))
                .findFirst().orElse(null);
    }

    // Outro médico (diferente de 'ignorar') com o mesmo nome, sem diferenciar maiúsculas.
    public Medico buscarPorNome(String nome, Medico ignorar) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String alvo = nome.trim();
        return getItens().stream()
                .filter(m -> m != ignorar && m.getNome() != null && m.getNome().trim().equalsIgnoreCase(alvo))
                .findFirst().orElse(null);
    }

    // "CRM 45210-SP", "crm45210/sp" e "45210 SP" viram a mesma chave ("45210SP").
    private static String chaveCrm(String crm) {
        if (crm == null) {
            return "";
        }
        String chave = crm.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return chave.startsWith("CRM") ? chave.substring(3) : chave;
    }

    private static Medico criar(String codigo, String nome, String crm, String telefone,
                                 String rua, String numero, String bairro, String cep,
                                 String complemento, String estado, String cidade) {
        Medico medico = new Medico();
        medico.setCodigo(codigo);
        medico.setNome(nome);
        medico.setCrm(crm);
        medico.setTelefone(telefone);
        medico.setRua(rua);
        medico.setNumero(numero);
        medico.setBairro(bairro);
        medico.setCep(cep);
        medico.setComplemento(complemento);
        medico.setEstado(estado);
        medico.setCidade(cidade);
        return medico;
    }
}
