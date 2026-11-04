package com.farmacia.repository;

import com.farmacia.model.Funcionario;

import java.util.List;
import java.util.Optional;

public final class FuncionarioRepository extends RepositorioEmMemoria<Funcionario> {

    private static final FuncionarioRepository INSTANCIA = new FuncionarioRepository();

    public static FuncionarioRepository getInstance() {
        return INSTANCIA;
    }

    private FuncionarioRepository() {
        super("FUN", Funcionario::setCodigo);
        carregarInicial(List.of(
                criar("FUN0001", "Helena Prado Lacerda", "Farmacêutico(a) Responsável", "111.222.333-96", "(11) 3456-1000", "helena.prado"),
                criar("FUN0002", "Marcos Vinícius Teixeira", "Atendente de Balcão", null, "(11) 98765-2200", "marcos.teixeira"),
                criar("FUN0003", "Renata Albuquerque Gomes", "Gerente", "222.333.444-05", null, "renata.gomes"),
                criar("FUN0004", "Paulo César Mendes", "Auxiliar de Manipulação", null, null, "paulo.mendes")
        ));
    }

    // Login identifica o operador: a busca ignora maiúsculas/minúsculas e espaços nas pontas.
    public Optional<Funcionario> buscarPorLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        String procurado = login.trim();
        return getItens().stream()
                .filter(f -> f.getLogin() != null && f.getLogin().trim().equalsIgnoreCase(procurado))
                .findFirst();
    }

    // A Lixeira restaura cadastros sem revalidar nada; por isso o login e o CPF de quem está lá continuam reservados
    // (se fossem reaproveitados, restaurar geraria duplicidade). Estas buscas olham só a lixeira.
    public Optional<Funcionario> buscarPorLoginNaLixeira(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        String procurado = login.trim();
        return getLixeira().stream()
                .filter(f -> f.getLogin() != null && f.getLogin().trim().equalsIgnoreCase(procurado))
                .findFirst();
    }

    // 'ignorar' é o funcionário que está sendo editado (ele não conta como duplicado de si mesmo); pode ser nulo.
    public Optional<Funcionario> buscarPorCpf(String cpf, Funcionario ignorar) {
        return procurarCpf(getItens(), cpf, ignorar);
    }

    public Optional<Funcionario> buscarPorCpfNaLixeira(String cpf) {
        return procurarCpf(getLixeira(), cpf, null);
    }

    // O CPF é comparado só pelos dígitos, para "123.456.789-09" e "12345678909" serem o mesmo.
    private static Optional<Funcionario> procurarCpf(List<Funcionario> lista, String cpf, Funcionario ignorar) {
        String digitos = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return Optional.empty();
        }
        return lista.stream()
                .filter(f -> f != ignorar && f.getCpf() != null && f.getCpf().replaceAll("\\D", "").equals(digitos))
                .findFirst();
    }

    private static Funcionario criar(String codigo, String nome, String cargo, String cpf, String telefone, String login) {
        Funcionario funcionario = new Funcionario();
        funcionario.setCodigo(codigo);
        funcionario.setNome(nome);
        funcionario.setCargo(cargo);
        funcionario.setCpf(cpf);
        funcionario.setTelefone(telefone);
        funcionario.setLogin(login);
        return funcionario;
    }
}
