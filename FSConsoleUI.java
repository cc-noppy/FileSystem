import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class FSConsoleUI implements FSUserInterface{

    // Attributes go here
    private final Disk disk = new Disk(100);
    private final FileSystem fileSystem = new FileSystem(disk);
    private final Scanner scan = new Scanner(System.in);
    private enum Command {
        q, quit, echo, create, open, close, delete, read, write, seek, ls, current
    }

    // Main driver
    public void processCommands(){
        Command command;
        String[] commandParts;

        System.out.println("Mock file system started");

        loop: while(true){
            try{
                System.out.print("> "); // Really looks epic, I think :)

                commandParts = scan.nextLine().split(" ");

                command = Command.valueOf(commandParts[0].toLowerCase());


                switch(command){
                    case q, quit:
                        System.out.println("Exiting system."); break loop;

                    case echo:
                        echo(String.join(" ", Arrays.stream(commandParts).toList().subList(1, commandParts.length))); break;

                    case open:
                        fileSystem.open(commandParts[1], commandParts[2].toCharArray()[0]); break;

                    case create:
                        fileSystem.create(commandParts[1], commandParts[2].toCharArray()[0]);

                        System.out.println("\n");
                        fileSystem.printFileTree();
                        break;

                    case write:
                        String cool = "what the fuclHELPHELPHELPH?";
                        fileSystem.write(10, cool.getBytes(StandardCharsets.UTF_8));
                        break;

                    case read:
                        fileSystem.read(10);

                    case current:
                        System.out.println("Current open file: " + fileSystem.lastOpenedFile.file.name);
                }

            } catch (InputMismatchException e){
                System.out.println("Invalid command");
                scan.nextLine();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void echo(String str){
        System.out.println(str);
    }
}