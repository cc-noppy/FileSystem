public class FSManager {
    public static void main (String[] args){
        FSUserInterface fsInterface;

        fsInterface = new FSConsoleUI();
        fsInterface.processCommands();
    }
}
