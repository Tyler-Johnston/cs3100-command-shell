import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assign3 {

    static int ptime = 0;
    static ArrayList<String> commands = new ArrayList<String>();
    static String currentDir = System.getProperty("user.dir");

    public static void main(String[] args) {

        while (true) {
            System.out.println("[" + currentDir + "]: ");
            Scanner scanner = new Scanner(System.in);
            String data = scanner.nextLine();
            cases(data, false);
        }
    }

    // method to run built-in and external commands
    public static void cases (String data, boolean isCaret) {

        // return if no input is given
        if (data.equals("")) return;

        // if & is used, separate it from the command, and declare a boolean to toggle off the child waiting process
        boolean wait = true;
        if (data.charAt(data.length()-1) == '&') {
            wait = false;
            data = data.substring(0, data.length()-1);
        }
        // separate the command from the parameters
        String[] arr = data.split(" ");
        String command = arr[0];
        StringBuilder strBuilder = new StringBuilder();
        for (int i=1; i<arr.length; i++) {
            strBuilder.append(arr[i]);
            strBuilder.append(" ");
        }
        String params = String.valueOf(strBuilder).stripTrailing();

        // navigate to the correct method depending on the command given
        switch (command) {
            case "ptime" -> ptime(isCaret);
            case "history" -> history(isCaret);
            case "^" -> caret(params, isCaret);
            case "list" -> list(isCaret);
            case "cd" -> cd(params, isCaret);
            case "mdir" -> mdir(params, isCaret);
            case "rdir" -> rdir(params, isCaret);
            case "exit" -> System.exit(0);
            default -> externalCommand(data, wait, isCaret);
        }
    }

    public static void ptime(boolean isCaret) {
        System.out.println("Total time in child processes: " + ptime + " ms");
        if (!isCaret) commands.add("ptime");
    }

    public static void history(boolean isCaret) {
        if (!isCaret) commands.add("history");
        System.out.println("--- Command History ---");
        for (int i=0; i<commands.size(); i++) {
            System.out.println(i+1 + ": " + commands.get(i));
        }
    }

    public static void caret(String params, boolean isCaret) {
        try {
            int num = Integer.parseInt(params);
            cases(commands.get(num-1), true);
            if (!isCaret) commands.add("^ " + num);
        }
        catch (Exception e) {
        }
    }

    public static void list(boolean isCaret) {

        File dir = new File(currentDir);
        File[] directoryListing = dir.listFiles();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        if (directoryListing != null) {
            for (File item : directoryListing) {

                // get File isDirectory / canRead / canWrite / canExecute information
                String fileInfo = String.valueOf(item.isDirectory() ? 'd' : '-') +
                        (item.canRead() ? 'r' : '-') +
                        (item.canWrite() ? 'w' : '-') +
                        (item.canExecute() ? 'x' : '-');
                System.out.print(fileInfo);

                // print Byte info for the File
                System.out.printf("%" + 10 + "s", item.length());

                // print time File last modified
                long time = item.lastModified();
                System.out.print(" " + sdf.format(time));

                // print the File name
                System.out.print(" " + item.getName());
                System.out.println();
            }
        }
        if (!isCaret) commands.add("list");
    }

    /**
     * Split the user command by spaces, but preserving them when inside double-quotes.
     * Code Adapted from: https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
     */
    public static String[] splitCommand(String command) {
        java.util.List<String> matchList = new java.util.ArrayList<>();

        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[matchList.size()]);
    }

    public static void cd(String params, boolean isCaret) {

        // get the path in the format of a string while preserving quotes
        String[] splitParams = splitCommand(params);
        StringBuilder strBuilder = new StringBuilder();
        for (String param : splitParams) {
            strBuilder.append(param);
        }
        String path = String.valueOf(strBuilder);

        // if the user only enters 'cd' re-direct to the home directory
        if (path.equals("")) {
            String home = System.getProperty("user.home");
            System.setProperty("user.dir", home);
            currentDir = System.getProperty("user.home");
        }
        // if the user enters ".." go back a directory
        else if (path.equals("..")) {
            String parentDir = new File(currentDir).getParent();
            java.nio.file.Path proposed = java.nio.file.Paths.get(parentDir);
            currentDir = proposed.toString();
            System.setProperty("user.dir", currentDir);
        }
        // otherwise, navigate to the chosen directory
        else {
            java.nio.file.Path proposed = java.nio.file.Paths.get(currentDir, path);
            if (proposed.toFile().isDirectory()) {
                currentDir = proposed.toString();
                System.setProperty("user.dir", currentDir);
            } else {
                System.out.println("cd: no such file or directory: " + proposed);
            }
        }
        if (!isCaret) commands.add("cd " + params);
    }

    public static void mdir(String params, boolean isCaret) {

        String[] splitParams = splitCommand(params);
        try {
            for (int i=0; i< splitParams.length; i++) {
                java.nio.file.Path proposed = java.nio.file.Paths.get(currentDir, splitParams[i]);
                if (!Files.exists(proposed)) {
                    Files.createDirectory(proposed);
                }
                else {
                    System.out.println("mdir" + params + ": File Exists");
                }
            }
            if (!isCaret) commands.add("mdir " + params);
        }
        catch (Exception e) {
        }
    }

    public static void rdir(String params, boolean isCaret) {

        String[] splitParams = splitCommand(params);
        try {
            for (int i=0; i< splitParams.length; i++) {
                java.nio.file.Path proposed = java.nio.file.Paths.get(currentDir, splitParams[i]);
                if (Files.exists(proposed)) {
                    Files.delete(proposed);
                }
                else {
                    System.out.println("rdir " + params + ": File Does Not Exist");
                }
            }
            if (!isCaret) commands.add("rdir" + params);
        }
        catch (Exception e) {
        }
    }

    public static boolean isPipe(String data, boolean isCaret) {

        String[] splitParams = splitCommand(data);
        for (int i=0; i<splitParams.length; i++) {
            if (splitParams[i].equals("|")) {
                pipe(splitParams, isCaret, i);
                return true;
            }
        }
        return false;
    }

    // code modified from PipeDemo.java given in class
    public static void pipe(String[] splitParams, boolean isCaret, int index) {

        // convert each command & params into String[] format
        String[] params1 = new String[index];
        for (int i=0; i<index; i++) {
            params1[i] = splitParams[i];
        }
        String[] params2 = new String[splitParams.length-(index+1)];
        for (int i=0; i<(splitParams.length-(index+1)); i++) {
            params2[i] = splitParams[index+1+i];
        }

        ProcessBuilder pb1 = new ProcessBuilder(params1);
        ProcessBuilder pb2 = new ProcessBuilder(params2);

        // Use the parent process's I/O channels
        pb1.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb2.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        try {
            Process p1 = pb1.start();
            Process p2 = pb2.start();

            // this might feel backwards, but it is our program's input and output
            // thus p1's output is our input and our output is p2's input
            java.io.InputStream in = p1.getInputStream();
            java.io.OutputStream out = p2.getOutputStream();

            // Read the data from p1 and feed to p2.
            int data;
            while ((data = in.read()) != -1) {
                out.write(data);
            }

            // if we don't do this p2 won't know when we're done
            out.flush();
            out.close();

            // Java version of wait()
            p1.waitFor();
            p2.waitFor();

            // get the command in string format to add to the history list
            StringBuilder strBuilder = new StringBuilder();
            for (int i=0; i< splitParams.length; i++) {
                strBuilder.append(splitParams[i] + " ");
            }
            String command = String.valueOf(strBuilder).stripTrailing();
            if (!isCaret) commands.add(command);
        }
        catch (Exception e) {
        }
    }

    public static void externalCommand(String data, boolean wait, boolean isCaret) {

        if (isPipe(data, isCaret)) return;

        boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (windows) builder.command("cmd", "/c", data);
        else builder.command("sh", "-c", data);

        builder.directory(new File(System.getProperty("user.dir")));
        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        try {
            long start = System.currentTimeMillis();
            Process process = builder.start();
            if (wait) {
                int exitCode = process.waitFor();
                long end = System.currentTimeMillis();
                long totalTime = end-start;
                ptime += totalTime;
                if (exitCode > 0) {
                    System.out.println("Invalid Command. Please try again.");
                }
                System.out.println("Process terminated with exit code " + exitCode);
            }
            if (!isCaret) commands.add(data);
        }
        catch (Exception e) {
        }
    }
}