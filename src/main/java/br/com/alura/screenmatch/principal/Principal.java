package br.com.alura.screenmatch.principal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.springframework.cglib.core.Local;

import br.com.alura.screenmatch.model.DadosEpisodio;
import br.com.alura.screenmatch.model.DadosSerie;
import br.com.alura.screenmatch.model.DadosTemporada;
import br.com.alura.screenmatch.model.Episodio;
import br.com.alura.screenmatch.service.ConsumoAPI;
import br.com.alura.screenmatch.service.ConverteDados;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=9f47e3fa";
    private ConsumoAPI consumo = new ConsumoAPI();
    private ConverteDados conversor = new ConverteDados();

    public void exibeMenu(){
        System.out.println("Digite o nome da série para busca: ");
        var nomeSerie = leitura.nextLine();
        
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dadosSerie = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dadosSerie);

        List<DadosTemporada> temporadas = new ArrayList<>();
		
		for (int i = 1; i <= dadosSerie.totalTemporadas(); i++) {
			json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);
			DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
			temporadas.add(dadosTemporada);
		}

		//temporadas.forEach(System.out::println);
        
        // for(int i = 0; i < dadosSerie.totalTemporadas(); i++ ){
        //     List<DadosEpisodio> episodiosTemporada = temporadas.get(i).episodios(); 
        //     for(int j = 0; j < episodiosTemporada.size(); j++){
        //         System.out.println(episodiosTemporada.get(j).titulo());
        //     }
        // }

        // System.out.println("________________________________________________________");
        // temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));

        // List<DadosEpisodio> dadosEpisodios = temporadas.stream()
        //     .flatMap(t -> t.episodios().stream())
        //     .collect(Collectors.toList());
        
        // dadosEpisodios.stream()
        //     .filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
        //     .sorted(Comparator.comparing(DadosEpisodio::avaliacao).reversed())
        //     .limit(5)
        //     .forEach(System.out::println);

        List<Episodio> episodios = temporadas.stream()
            .flatMap(t -> t.episodios().stream()
                .map(d -> new Episodio(t.numero(), d)))
            .collect(Collectors.toList());

        episodios.forEach(System.out::println);

        System.out.println("A partir de que ano deseja ver os episódios?");
        var ano = leitura.nextInt();
        leitura.nextLine();

        LocalDate dataBusca = LocalDate.of(ano, 1, 1);
        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        episodios.stream()
            .filter(e -> e.getDataLancamento() != null && e.getDataLancamento().isAfter(dataBusca))
            .forEach(e -> System.out.println(
                "Temporada: " + e.getTemporada() +
                    " Episódio: " + e.getTitulo() +
                    " Data lançamento; " + e.getDataLancamento().format(formatador)   
            ));
    }

}
