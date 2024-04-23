import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class FSConsoleUI implements FSUserInterface {

    // Attributes go here
    private final Disk disk = new Disk(100);
    private final FileSystem fileSystem = new FileSystem(disk);
    private final Scanner scan = new Scanner(System.in);

    private enum Command {
        q, quit, echo, create, open, close, delete, read, write, seek, ls, current, peek, unpack, help, damn
    }

    // Main driver
    public void processCommands() {
        Command command;
        String[] commandParts;

        System.out.println("Mock file system started. Type HELP for commands.");

        loop:
        while (true) {
            try {
                System.out.print("> "); // Really looks epic, I think :)

                commandParts = scan.nextLine().split(" ");

                command = Command.valueOf(commandParts[0].toLowerCase());


                switch (command) {
                    case q, quit:
                        System.out.println("Exiting system.");
                        break loop;

                    case echo:
                        echo(String.join(" ", Arrays.stream(commandParts).toList().subList(1, commandParts.length)));
                        break;

                    case open:
                        if (commandParts.length == 3) {
                            fileSystem.open(commandParts[1], commandParts[2].toCharArray()[0]);
                            break;
                        } else {
                            System.out.println("Incorrect usage.\nUsage: open <path> <mode>");
                            break;
                        }


                    case close:
                        fileSystem.close();
                        break;

                    case create:
                        if (commandParts.length == 3) {
                            fileSystem.create(commandParts[1], commandParts[2].toCharArray()[0]);

                            System.out.println("\n");
                            fileSystem.displayTree(fileSystem.root, 0);
                            break;
                        } else {
                            System.out.println("Incorrect usage.\nUsage: create <name> <file type (U | D)>");
                            break;
                        }

                    case write:
                        if (commandParts.length >= 3) {
                            String[] subArray = Arrays.copyOfRange(commandParts, 2, commandParts.length);
                            fileSystem.write(Integer.parseInt(commandParts[1]), String.join(" ", subArray));
                            break;
                        } else {
                            System.out.println("Incorrect usage.\nUsage: write <n> <msg>");
                            break;
                        }

                    case delete:
                        if (commandParts.length == 2) {
                            fileSystem.delete(commandParts[1]);

                            System.out.println("\n");
                            fileSystem.displayTree(fileSystem.root, 0);
                            break;
                        } else {
                            System.out.println("Incorrect usage.\nUsage: delete <file>");
                            break;
                        }

                    case read:
                        if (commandParts.length == 2) {
                            fileSystem.read(Integer.parseInt(commandParts[1]));
                            break;
                        } else {
                            System.out.println("Incorrect usage.\nUsage: read <n>");
                            break;
                        }

                    case peek:
                        if (commandParts.length == 2) {
                            byte[] sector = disk.diskRead(Integer.parseInt(commandParts[1]));
                            for (byte b : sector) {
                                System.out.print(b + " ");
                            }
                            System.out.println("\n");
                            break;
                        } else {
                            System.out.println("Incorrect usage.\npeek <sector>");
                            break;
                        }

                    case unpack:
                        if (commandParts.length == 2) {
                            int count = 0;
                            byte[] data = disk.diskRead(Integer.parseInt(commandParts[1]));
                            List<byte[]> entries = fileSystem.deserializeDirectoryData(data);
                            for (byte[] e : entries) {
                                System.out.println(Arrays.toString(e));
                                count++;
                            }

                            System.out.println("Entry count: " + count);
                            break;

                        } else {
                            System.out.println("Incorrect usage.\nUsage: unpack <sector>");
                            break;
                        }

                    case current:
                        if (fileSystem.lastOpenedFile != null) {
                            System.out.println("File: " + fileSystem.lastOpenedFile.name + "is opened!");
                            break;
                        } else {
                            System.out.println("No file is open at the moment!");
                            break;
                        }

                    case help:
                        System.out.println(
                                "Welcome to the file system! \n" +
                                        "I did my best to simulate a full file system down to the wire\n" +
                                        "with proper 'emulation' of the disk and disk sectors.\n" +
                                        "\n" +
                                        "Here are the available commands (peek and unpack are for debugging\n" +
                                        "if you want to take a look at the internals). Thank you for your\n" +
                                        "time.\n" +
                                        "- Francis Lorenz Rosas\n" +
                                        "\n" +
                                        "open <path> <mode>\n" +
                                        "close\n" +
                                        "create <name> <file type (U | D)>\n" +
                                        "write <n> <msg>\n" +
                                        "delete <file>\n" +
                                        "read <n>\n" +
                                        "peek <sector> \n" +
                                        "unpack <sector> (only for directories... will break elsewhere)\n" +
                                        "quit\n" +
                                        "damn"
                        );
                        break;

                    case damn:
                        damn d = new damn();
                }

            } catch (InputMismatchException e) {
                System.out.println("Invalid command");
                scan.nextLine();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void echo(String str) {
        System.out.println(str);
    }
}