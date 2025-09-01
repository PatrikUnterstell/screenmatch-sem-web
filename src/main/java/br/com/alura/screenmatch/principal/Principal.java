package br.com.alura.screenmatch.principal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import br.com.alura.screenmatch.model.Categoria;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.model.Serie;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=9f47e3fa";
    private ConsumoAPI consumo = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();
    //private List<DadosSerie> dadosSeries = new ArrayList<>();
    private List<Serie> series = new ArrayList<>();
    private SerieRepository repositorio;
    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar séries por título
                    5 - Buscar séries por Ator
                    6 - Buscar Top 5 séries
                    7 - Buscar séries por categoria
                    8 - Filtrar séries
                    9 - Buscar episódio por trecho
                    10 - Top 5 episódios por série
                    11 - Buscar episódios a partir de uma data

                    0 - Sair
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriesPorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosPorData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //dadosSeries.add(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serie.isPresent()){
            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                .flatMap(d -> d.episodios().stream()
                    .map(e -> new Episodio(d.numero(), e)))
                .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }
        else {
            System.out.println("Série não encontrada.");
        }
    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
            .sorted(Comparator.comparing(Serie::getGenero))
            .forEach(System.out::println);
    }

    private void buscarSeriesPorTitulo(){
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBusca.isPresent()){
            System.out.println("Dados da série: "+serieBusca.get());
        }
        else{
            System.out.println("Série não encontrada!");
        }
    }

    private void buscarSeriesPorAtor(){
        System.out.println("Digite o nome de um ator: ");
        var ator = leitura.nextLine();
        System.out.println("Avaliação apartir de qual valor? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(ator, avaliacao);
        
        System.out.println("Séries de "+ator);
        seriesEncontradas.forEach(s -> 
            System.out.println(s.getTitulo() + " - Avaliação: "+s.getAvaliacao()));
    }

    private void buscarTop5Series(){
        List<Serie> series = repositorio.findTop5ByOrderByAvaliacaoDesc();
        System.out.println("Top 5 Séries:");
        series.forEach(s -> 
            System.out.println(s.getTitulo() + " - Avaliação: "+s.getAvaliacao()));
    }

    private void buscarSeriesPorCategoria(){
        System.out.println("Digite a categoria das séries: ");
        var nomaGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromCategoriaPortugues(nomaGenero);
        List<Serie> series = repositorio.findByGenero(categoria);
        System.out.println("Séries da categoria "+nomaGenero);
        series.forEach(s -> 
            System.out.println(s.getTitulo() + " - Avaliação: "+s.getAvaliacao()));
    }

    private void filtrarSeriesPorTemporadaEAvaliacao(){
        System.out.println("Filtrar séries até quantas temporadas? ");
        var totalTemporadas = leitura.nextInt();
        System.out.println("Com avaliação a partir de que valor? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        //List<Serie> filtroSeries = repositorio.findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(totalTemporadas, avaliacao);
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        System.out.println("*** Séries filtradas ***");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + "  - avaliação: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho(){
        System.out.println("Digite um trecho de episódio? ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodios = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodios.forEach(e -> 
            System.out.printf("Série: %s | Temporada: %s - %s\n", e.getSerie().getTitulo(), e.getTemporada(), e.getTitulo()));
    }

    private void topEpisodiosPorSerie(){
        buscarSeriesPorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> episodios = repositorio.topEpisodiosPorSerie(serie);
            System.out.println("Top 5 episódios da série "+ serieBusca.get().getTitulo() +":");
            episodios.forEach(e -> 
                System.out.println(e.getTitulo() + " | Temporada: " + e.getTemporada()+ " | Avaliação: "+e.getAvaliacao()));
        }
    }

    private void buscarEpisodiosPorData(){
        buscarSeriesPorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Informe a partir de qual ano deseja listar os episódios: ");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();
            List<Episodio> episodios = repositorio.buscarEpisodiosDeUmaSeriePorAno(serie, anoLancamento);
            System.out.println("Episódios a partir do ano "+ anoLancamento +":");
            episodios.forEach(e -> 
                System.out.println(e.getTitulo() + " | Temporada: " + e.getTemporada()+ " | Data: " + e.getDataLancamento()+ " | Avaliação: "+e.getAvaliacao()));
        }
    }
    
}
