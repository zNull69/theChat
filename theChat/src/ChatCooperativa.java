import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

class Chat
{
    private volatile String messaggioA;
    private volatile String messaggioB;
    private volatile boolean messaggioAPronto = false;
    private volatile boolean messaggioBPronto = false;
    private volatile boolean terminato = false;
    private BufferedWriter fileWriter;

    public Chat(String nomeFile) throws IOException
    {
        fileWriter = new BufferedWriter(new FileWriter(nomeFile, false));
        fileWriter.write("cronologia della chat\n");
        fileWriter.flush();
    }

    private synchronized void scriviSuFile(String nomeUtente, String msg) throws IOException
    {
        if (!msg.equalsIgnoreCase("fine"))
        {
            fileWriter.write(nomeUtente + ": " + msg + "\n");
            fileWriter.flush();
        }
    }

    public synchronized void scriviA(String msg) throws InterruptedException
    {
        while (messaggioAPronto && !terminato)
        {
            wait();
        }

        if (terminato)
        {
            return;
        }

        messaggioA = msg;
        messaggioAPronto = true;

        System.out.printf("UtenteA ha scritto: %s%n", msg);

        try
        {
            scriviSuFile("UtenteA", msg);
        } catch (IOException e) {
            System.err.println("Errore durante la scrittura su file: " + e.getMessage());
        }

        if (msg.equalsIgnoreCase("fine"))
        {
            terminato = true;
        }

        notifyAll();
    }

    public synchronized void scriviB(String msg) throws InterruptedException
    {
        while (messaggioBPronto && !terminato)
        {
            wait();
        }

        if (terminato)
        {
            return;
        }

        messaggioB = msg;
        messaggioBPronto = true;

        System.out.printf("UtenteB ha scritto: %s%n", msg);

        try
        {
            scriviSuFile("UtenteB", msg);
        } catch (IOException e) {
            System.err.println("Errore durante la scrittura su file: " + e.getMessage());
        }

        if (msg.equalsIgnoreCase("fine"))
        {
            terminato = true;
        }

        notifyAll();
    }

    public synchronized String leggiA() throws InterruptedException
    {
        while (!messaggioAPronto && !terminato)
        {
            wait();
        }

        if (terminato && !messaggioAPronto)
        {
            return "fine";
        }

        String msgLetto = messaggioA;
        messaggioAPronto = false;

        System.out.printf("UtenteB ha letto: %s%n", msgLetto);

        notifyAll();

        return msgLetto;
    }

    public synchronized String leggiB() throws InterruptedException
    {
        while (!messaggioBPronto && !terminato)
        {
            wait();
        }

        if (terminato && !messaggioBPronto)

        {
            return "fine";
        }

        String msgLetto = messaggioB;
        messaggioBPronto = false;

        System.out.printf("UtenteA ha letto: %s%n", msgLetto);

        notifyAll();

        return msgLetto;
    }

    public synchronized boolean isTerminato()
    {
        return terminato;
    }

    public synchronized void chiudiFile()
    {
        try
        {
            if (fileWriter != null)
            {
                fileWriter.write("fine\n");
                fileWriter.close();
            }
        } catch (IOException e)
        {
            System.err.println("Errore durante la chiusura del file: " + e.getMessage());
        }
    }
}

class UtenteA implements Runnable
{
    private Chat chat;
    private Scanner scanner;

    public UtenteA(Chat chat, Scanner scanner)
    {
        this.chat = chat;
        this.scanner = scanner;
    }

    @Override
    public void run()
    {
        try
        {
            while (!chat.isTerminato())
            {
                System.out.print("\n> UtenteA, scrivi il tuo messaggio: ");
                String messaggio = scanner.nextLine();

                chat.scriviA(messaggio);

                if (messaggio.equalsIgnoreCase("fine"))
                {
                    break;
                }

                String risposta = chat.leggiB();

                if (risposta.equalsIgnoreCase("fine"))
                {
                    break;
                }
            }
        } catch (InterruptedException e)
        {
            System.out.println("UtenteA interrotto.");
        }
    }
}

class UtenteB implements Runnable
{
    private Chat chat;
    private Scanner scanner;

    public UtenteB(Chat chat, Scanner scanner)
    {
        this.chat = chat;
        this.scanner = scanner;
    }

    @Override
    public void run()
    {
        try
        {
            while (!chat.isTerminato())
            {
                String messaggio = chat.leggiA();

                if (messaggio.equalsIgnoreCase("fine"))
                {
                    break;
                }

                System.out.print("\n> UtenteB, scrivi la tua risposta: ");
                String risposta = scanner.nextLine();

                chat.scriviB(risposta);

                if (risposta.equalsIgnoreCase("fine"))
                {
                    break;
                }
            }
        } catch (InterruptedException e)
        {
            System.out.println("UtenteB interrotto.");
        }
    }
}

public class ChatCooperativa
{
    public static void main(String[] args)
    {
        System.out.println("=== CHAT CON SINCRONIZZAZIONE COOPERATIVA ===");
        System.out.println("Scrivi 'fine' per terminare la conversazione");
        System.out.println("I messaggi verranno salvati in 'chat_log.txt'\n");

        Chat chat = null;
        Scanner scanner = new Scanner(System.in);

        try {
            chat = new Chat("chat_log.txt");

            Thread threadA = new Thread(new UtenteA(chat, scanner), "ThreadUtenteA");
            Thread threadB = new Thread(new UtenteB(chat, scanner), "ThreadUtenteB");

            threadA.start();
            threadB.start();

            threadA.join();
            threadB.join();

        } catch (IOException e)
        {
            System.err.println("Errore durante la creazione del file di log: " + e.getMessage());
        } catch (InterruptedException e)
        {
            System.err.println("Errore durante l'attesa dei thread: " + e.getMessage());
        } finally {
            if (chat != null)
            {
                chat.chiudiFile();
            }
            scanner.close();
            System.out.println("\nChat terminata. Log salvato in 'chat_log.txt'");
        }
    }
}