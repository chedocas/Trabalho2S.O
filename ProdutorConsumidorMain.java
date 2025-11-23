import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

// =========================
// Classe Buffer (recurso compartilhado)
// =========================
/*
 * Paralelo com o conteúdo de sala:
 * - Esta classe representa o "recurso crítico" compartilhado entre threads.
 * - Aqui usamos:
 *   - Semaphore: para controlar quantos espaços vazios e quantos itens existem no buffer (problema clássico produtor-consumidor).
 *   - ReentrantLock (mutex): para garantir que somente UMA thread por vez altera o buffer (região crítica).
 * - Isso é exatamente o que foi visto em sincronização de threads, evitando condições de corrida.
 */
class Buffer {
    private final int[] dados;           // nosso buffer simples de inteiros
    private final int capacidade;        // tamanho máximo do buffer (7)
    private int quantidade;              // quantos itens estão atualmente no buffer

    private final Semaphore espacosVazios; // controla quantos espaços livres ainda existem
    private final Semaphore itensCheios;   // controla quantos itens já foram produzidos

    private final ReentrantLock mutex;   // garante exclusão mútua no acesso ao buffer

    private final BufferedWriter logWriter; // para escrever o log em arquivo texto

    public Buffer(int capacidade, String nomeArquivoLog) throws IOException {
        this.capacidade = capacidade;
        this.dados = new int[capacidade];
        this.quantidade = 0;

        // No início, todos os espaços estão vazios e não há nenhum item cheio
        this.espacosVazios = new Semaphore(capacidade);
        this.itensCheios = new Semaphore(0);

        this.mutex = new ReentrantLock();

        // Abre o arquivo de log (sobrescreve se já existir)
        this.logWriter = new BufferedWriter(new FileWriter(nomeArquivoLog));
    }

    // Método chamado pelo Produtor
    public void produzir() throws InterruptedException {
        // Espera até existir pelo menos um espaço vazio
        espacosVazios.acquire(); // paralelo: semáforo de espaços vazios, visto no problema clássico

        mutex.lock();            // entra na região crítica
        try {
            // Aqui não importa muito o valor do item, apenas que estamos ocupando espaço
            dados[quantidade] = 1; // coloca um “item” qualquer
            quantidade++;

            int espacosDisponiveis = capacidade - quantidade;

            escreverLog("Produtor - Inserido um item no buffer – espaços disponíveis: " + espacosDisponiveis);
        } finally {
            // Sai da região crítica
            mutex.unlock();

            // Sinaliza que agora existe pelo menos um item cheio
            itensCheios.release();
        }
    }

    // Método chamado pelo Consumidor
    public void consumir() throws InterruptedException {
        // Espera até existir pelo menos um item cheio para consumir
        itensCheios.acquire(); // paralelo: semáforo de itens, impede consumir buffer vazio

        mutex.lock();          // entra na região crítica
        try {
            // "Remove" um item: apenas decrementamos quantidade
            quantidade--;
            // opcionalmente poderíamos ler dados[quantidade], mas não é necessário aqui

            int espacosDisponiveis = capacidade - quantidade;

            escreverLog("Consumidor - Consumido um item no buffer – espaços disponíveis: " + espacosDisponiveis);
        } finally {
            // Sai da região crítica
            mutex.unlock();

            // Sinaliza que existe mais um espaço vazio
            espacosVazios.release();
        }
    }

    // Escreve no arquivo de log de forma segura
    private void escreverLog(String linha) {
        try {
            logWriter.write(linha);
            logWriter.newLine();
            logWriter.flush(); // garante que aparece no arquivo logo
        } catch (IOException e) {
            System.out.println("Erro ao escrever no log: " + e.getMessage());
        }
    }

    // Fecha o arquivo no final do programa
    public void fecharLog() {
        try {
            logWriter.close();
        } catch (IOException e) {
            System.out.println("Erro ao fechar o log: " + e.getMessage());
        }
    }
}

// =========================
// Classe Produtor (Thread)
// =========================
/*
 * Paralelo com o conteúdo de sala:
 * - Extende Thread, logo representa uma thread de execução independente.
 * - No run(), chamamos o método produzir() do Buffer.
 * - A lógica de sincronização (semáforo + mutex) está no Buffer, o Produtor só "pede" para produzir.
 * - Isso exemplifica o conceito de "threads concorrentes acessando um recurso compartilhado".
 */
class Produtor extends Thread {
    private final Buffer buffer;
    private final int totalParaProduzir = 15; // cada produtor produz até 15 itens

    public Produtor(Buffer buffer) {
        this.buffer = buffer;
        this.setName("Produtor"); // apenas para identificação se quiser
    }

    @Override
    public void run() {
        for (int i = 0; i < totalParaProduzir; i++) {
            try {
                buffer.produzir();
                // Pequena pausa só para ficar visualmente mais interessante
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Em um exemplo simples, só exibimos a mensagem
                System.out.println("Produtor interrompido: " + e.getMessage());
            }
        }
    }
}

// =========================
// Classe Consumidor (Thread)
// =========================
/*
 * Paralelo com o conteúdo de sala:
 * - Também extende Thread, representando outra linha de execução concorrente.
 * - No run(), chamamos consumir() do Buffer.
 * - Mostra o cenário clássico produtor x consumidor, onde a ordem depende do escalonamento do SO.
 */
class Consumidor extends Thread {
    private final Buffer buffer;
    private final int totalParaConsumir = 12; // cada consumidor consome até 12 itens

    public Consumidor(Buffer buffer) {
        this.buffer = buffer;
        this.setName("Consumidor");
    }

    @Override
    public void run() {
        for (int i = 0; i < totalParaConsumir; i++) {
            try {
                buffer.consumir();
                Thread.sleep(150); // pausa um pouco diferente do produtor
            } catch (InterruptedException e) {
                System.out.println("Consumidor interrompido: " + e.getMessage());
            }
        }
    }
}

// =========================
// Classe principal (main)
// =========================
/*
 * Paralelo com o conteúdo de sala:
 * - Aqui criamos o recurso compartilhado (Buffer) e as threads Produtor e Consumidor.
 * - start() inicia as threads, e join() faz a thread principal esperar o término delas.
 * - Mostra o ciclo de vida de threads e a interação com o recurso compartilhado.
 */
public class ProdutorConsumidorMain {

    public static void main(String[] args) {
        try {
            // Cria buffer com 7 posições e um arquivo de log
            Buffer buffer = new Buffer(7, "log_produtor_consumidor.txt");

            // Cria uma thread produtora e uma consumidora
            Produtor produtor = new Produtor(buffer);
            Consumidor consumidor = new Consumidor(buffer);

            // Inicia as threads
            produtor.start();
            consumidor.start();

            // Espera as duas terminarem
            produtor.join();
            consumidor.join();

            // Fecha o arquivo de log
            buffer.fecharLog();

            System.out.println("Execução finalizada. Verifique o arquivo log_produtor_consumidor.txt.");
        } catch (IOException e) {
            System.out.println("Erro ao criar o buffer/log: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Thread principal interrompida: " + e.getMessage());
        }
    }
}
