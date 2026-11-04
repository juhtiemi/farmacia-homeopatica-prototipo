# Protótipo de interface — Farmácia de Manipulação Homeopática

Protótipo de alta fidelidade desenvolvido como artefato do Trabalho de Conclusão de Curso **"Modernização da Interface de um Sistema Legado para Gerenciamento de Farmácia de Manipulação Homeopática: uma Abordagem Fundamentada em Interação Humano-Computador"**, do curso de Bacharelado em Ciência da Computação da Universidade Estadual do Paraná (UNESPAR), *campus* Apucarana.

- **Autora:** Júlia Tiemi Moribe Kato
- **Orientador:** Prof. Me. João Paulo Biazotto

O protótipo propõe uma nova interface para o sistema legado de uma farmácia de manipulação homeopática, com base nas dez heurísticas de usabilidade de Nielsen. O trabalho trata apenas da interface: **todos os dados são fictícios e ficam em memória**, sem banco de dados nem persistência. Ao fechar o programa, as alterações são descartadas.

## Telas

- **Clientes:** consulta e cadastro de clientes e de seus dependentes.
- **Receitas:** consulta e cadastro de receitas, com o receituário composto por itens (um por medicamento) e a opção de duplicar uma receita anterior.
- **Médicos**, **Produtos** e **Funcionários:** consulta e cadastro.
- **Lixeira:** recuperação dos registros excluídos nas demais telas.

## Tecnologias

- Java 21
- JavaFX 21 (FXML e CSS)
- Maven (via Maven Wrapper, incluído no projeto)
- Padrão *Model-View-Controller* (MVC)

## Requisitos

- JDK 21 ou superior instalado (`java -version` para conferir).
- Conexão com a internet na primeira execução, para que o Maven baixe as dependências do JavaFX.

Não é preciso instalar o Maven nem o JavaFX separadamente.

## Como executar

No macOS ou no Linux, na pasta do projeto:

```bash
sh mvnw javafx:run
```

No Windows (Prompt de Comando ou PowerShell):

```bat
mvnw.cmd javafx:run
```

## Atalhos de teclado

| Atalho | Ação |
| --- | --- |
| `Ctrl N` | Novo registro |
| `Ctrl F` ou `/` | Ir para a busca |
| `Enter` | Abrir o registro selecionado |
| `Delete` | Mover o registro selecionado para a lixeira |
| `Ctrl D` | Duplicar a receita selecionada (tela Receitas) |
| `Ctrl S` | Salvar o formulário |
| `Esc` | Cancelar, fechar caixas ou limpar a busca |
| `F1` | Abrir a ajuda da tela |

## Estrutura do código

```
src/main/java/com/farmacia/
├── Main.java        ponto de entrada da aplicação
├── model/           entidades (Cliente, Receita, ItemReceita, Médico, Produto, Funcionário)
├── repository/      repositórios em memória com os dados fictícios
├── controller/      controllers das telas (camada Controller)
├── ui/              componentes de interface compartilhados (avisos, máscaras, erros, atalhos)
└── util/            utilitários e contexto de navegação entre telas
src/main/resources/
├── fxml/            descrição das telas (camada View)
└── css/             tema visual
```
