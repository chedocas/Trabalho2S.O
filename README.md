# Trabalho2S.O
# Aqui fica a explicação do código!

## Como o código está organizado (estrutura das classes)

1. Buffer

Representa o recurso compartilhado, que é o buffer com 7 posições.

Tem:

int[] dados → array simples para os itens.

int capacidade → tamanho (7).

int quantidade → quantos itens estão lá dentro agora.

Semaphore espacosVazios → começa com 7 (capacidade). Cada produzir() dá um acquire() (consome um espaço vazio).

Semaphore itensCheios → começa com 0. Cada consumir() dá um acquire() (consome um item).

ReentrantLock mutex → garante que só uma thread por vez entra na região crítica (altera quantidade e o array).

BufferedWriter logWriter → escreve as mensagens no arquivo de texto.

Método produzir()

Passos:

espacosVazios.acquire();

Se o buffer estiver cheio (0 espaços vazios), o produtor bloqueia aqui.

Isso implementa a restrição: produtor não adiciona em buffer cheio.

mutex.lock();

Entra na região crítica (ninguém mais altera quantidade ao mesmo tempo).

Dentro do try:

dados[quantidade] = 1;

quantidade++;

Calcula espacosDisponiveis = capacidade - quantidade;

Chama escreverLog("Produtor - Inserido um item no buffer – espaços disponíveis: " + espacosDisponiveis);

mutex.unlock(); no finally

Sai da região crítica.

itensCheios.release();

Sinaliza que agora existe mais um item pronto para ser consumido.

Método consumir()

Passos:

itensCheios.acquire();

Se não houver itens (itensCheios == 0), o consumidor bloqueia aqui.

Isso implementa a restrição: consumidor não remove de buffer vazio.

mutex.lock();

Entra na região crítica.

Dentro do try:

quantidade--; (remove o último item)

Calcula espacosDisponiveis = capacidade - quantidade;

Chama escreverLog("Consumidor - Consumido um item no buffer – espaços disponíveis: " + espacosDisponiveis);

mutex.unlock();

Sai da região crítica.

espacosVazios.release();

Libera um espaço vazio a mais para o produtor.

2. Produtor (extende Thread)

Tem um Buffer buffer recebido no construtor.

Tem totalParaProduzir = 15; → produz 15 itens.

No run():

for (int i = 0; i < totalParaProduzir; i++) {
    buffer.produzir();
    Thread.sleep(100);
}


Cada iteração chama produzir() do buffer.

O sleep(100) é só para deixar a execução mais “visível” e menos agressiva.

Paralelo com a teoria:

Representa uma thread que concorre por um recurso compartilhado usando semáforo e mutex, como visto no problema produtor–consumidor em sala.

3. Consumidor (extende Thread)

Também recebe o Buffer no construtor.

Tem totalParaConsumir = 12; → consome 12 itens.

No run():

for (int i = 0; i < totalParaConsumir; i++) {
    buffer.consumir();
    Thread.sleep(150);
}


O sleep(150) faz ele andar em um ritmo diferente do produtor (só para ilustrar melhor).

Paralelo com a teoria:

Mostra duas threads (produtor e consumidor) que trabalham em ritmos diferentes, mas mesmo assim não quebram o buffer porque os semáforos garantem as condições corretas.

4. ProdutorConsumidorMain (classe com main)

Cria o Buffer com capacidade 7 e arquivo log_produtor_consumidor.txt.

Cria um Produtor e um Consumidor passando o mesmo objeto buffer.

Chama start() nos dois (iniciando as threads).

Usa join() para esperar que as duas acabem.

Ao final, chama buffer.fecharLog();.

Isso mostra:

Criação de threads.

Ciclo de vida (start, execução em paralelo, join).

Uso de um objeto compartilhado entre elas.

5. Sobre o arquivo de saída (log)

O arquivo log_produtor_consumidor.txt vai ter linhas do tipo:

Produtor - Inserido um item no buffer – espaços disponíveis: 6

Consumidor - Consumido um item no buffer – espaços disponíveis: 7

Sempre com o texto exatamente como pedido no enunciado, apenas mudando o número x de espaços disponíveis.
