package com.farmacia.repository;

import com.farmacia.model.Cliente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ClienteRepository extends RepositorioEmMemoria<Cliente> {

    private static final ClienteRepository INSTANCIA = new ClienteRepository();

    public static ClienteRepository getInstance() {
        return INSTANCIA;
    }

    private ClienteRepository() {
        super("CLI", Cliente::setCodigo);
        List<Cliente> titulares = List.of(
                criar("CLI0001", "Maria Eduarda Santos", "(11) 98765-4321",
                        "Rua das Flores", "245", "Jardim Primavera", "01310-100", "Apto 52", "SP", "São Paulo",
                        LocalDate.of(1985, 3, 14), "123.456.789-09", "12.345.678-9"),
                criar("CLI0002", "João Pedro Oliveira", "(11) 91234-5678",
                        "Avenida Brasil", "1020", "Centro", "01001-000", null, "SP", "São Paulo",
                        LocalDate.of(1990, 7, 22), "234.567.890-92", "23.456.789-0"),
                criar("CLI0003", "Beatriz Nascimento Lima", "(21) 99887-6655",
                        "Rua do Sol", "88", "Copacabana", "22020-000", "Casa 2", "RJ", "Rio de Janeiro",
                        LocalDate.of(1978, 11, 5), null, null),
                criar("CLI0004", "Pedro Henrique Rocha", "(31) 98888-1234",
                        null, null, null, null, null, null, null,
                        null, "345.678.901-75", "90.123.456-7"),
                criar("CLI0005", "Ana Beatriz Costa", "(11) 97777-1010",
                        "Rua Oscar Freire", "310", "Jardins", "01426-001", null, "SP", "São Paulo",
                        LocalDate.of(1992, 1, 30), "456.789.012-49", "34.567.890-1"),
                criar("CLI0006", "Carlos Eduardo Lima", "(19) 98123-4455",
                        "Rua Barão de Jaguara", "980", "Centro", "13015-001", "Sala 4", "SP", "Campinas",
                        LocalDate.of(1969, 9, 12), "567.890.123-03", "45.678.901-2"),
                criar("CLI0007", "Fernanda Almeida Souza", "(41) 99654-3210",
                        "Rua XV de Novembro", "1500", "Centro", "80020-310", null, "PR", "Curitiba",
                        LocalDate.of(2001, 5, 27), null, "56.789.012-3"),
                criar("CLI0008", "Rafael Henrique Pereira", "(31) 98456-7788",
                        "Avenida Afonso Pena", "2020", "Funcionários", "30130-007", "Apto 801", "MG", "Belo Horizonte",
                        LocalDate.of(1983, 12, 3), "678.901.234-69", "67.890.123-4"),
                criar("CLI0009", "Juliana Ribeiro Alves", "(71) 99321-0099",
                        "Rua Chile", "12", "Centro Histórico", "40020-000", null, "BA", "Salvador",
                        LocalDate.of(1996, 8, 19), null, "78.901.234-5"),
                criar("CLI0010", "Lucas Gabriel Martins", "(51) 98100-2233",
                        null, null, null, null, null, null, null,
                        LocalDate.of(2008, 4, 2), "789.012.345-05", "89.012.345-6")
        );

        List<Cliente> todos = new ArrayList<>(titulares);
        todos.add(dependente("CLI0011", "Helena Santos Oliveira", "Filho(a)", titulares.get(0),
                LocalDate.of(2015, 6, 10), "987.654.321-00"));
        todos.add(dependente("CLI0012", "Camila Ferreira Oliveira", "Cônjuge/Companheiro(a)", titulares.get(1),
                LocalDate.of(1991, 2, 18), null));
        todos.add(dependente("CLI0013", "Antônio Nascimento Lima", "Pai/Mãe", titulares.get(2),
                null, null));
        carregarInicial(todos);
    }

    public List<Cliente> dependentesDe(Cliente responsavel) {
        return getItens().stream().filter(c -> c.getResponsavel() == responsavel).toList();
    }

    // Dependentes são clientes com responsável; telefone e endereço ficam em branco e valem os do responsável.
    private static Cliente dependente(String codigo, String nome, String parentesco, Cliente responsavel,
                                      LocalDate dataNascimento, String cpf) {
        Cliente dependente = criar(codigo, nome, null, null, null, null, null, null, null, null, dataNascimento, cpf, null);
        dependente.setParentesco(parentesco);
        dependente.setResponsavel(responsavel);
        return dependente;
    }

    private static Cliente criar(String codigo, String nome, String telefone, String rua, String numero,
                                  String bairro, String cep, String complemento, String estado, String cidade,
                                  LocalDate dataNascimento, String cpf, String rg) {
        Cliente cliente = new Cliente();
        cliente.setCodigo(codigo);
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        cliente.setRua(rua);
        cliente.setNumero(numero);
        cliente.setBairro(bairro);
        cliente.setCep(cep);
        cliente.setComplemento(complemento);
        cliente.setEstado(estado);
        cliente.setCidade(cidade);
        cliente.setDataNascimento(dataNascimento);
        cliente.setIdade(Cliente.calcularIdade(dataNascimento));
        cliente.setCpf(cpf);
        cliente.setRg(rg);
        return cliente;
    }
}
