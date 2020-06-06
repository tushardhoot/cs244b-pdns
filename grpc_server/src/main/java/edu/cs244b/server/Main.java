package edu.cs244b.server;

import org.apache.commons.cli.*;

import java.util.Arrays;


public class Main {
    public static void main(String[] args) throws Exception {
        final Options options = new Options();

        final Option portOption = new Option("p", "port", true, "port");
        portOption.setRequired(false);
        options.addOption(portOption);

        final Option dnsOption = new Option("d", false, "Whether this server is backed by normal DNS resolution");
        dnsOption.setRequired(false);
        options.addOption(dnsOption);

        final Option serverConfigOption = new Option("c", "config", true, "server config file");
        serverConfigOption.setRequired(true);
        options.addOption(serverConfigOption);

        CommandLine commandLine;
        CommandLineParser parser = new DefaultParser();

        try
        {
            commandLine = parser.parse(options, args);
            final int port = Integer.parseInt(commandLine.getOptionValue("port", "8980"));
            final String serverConfigFile = commandLine.getOptionValue("config");
            final boolean dnsBacked = commandLine.hasOption("d");

            System.out.println("args " + Arrays.toString(args));
            System.out.println("dnsBacked " + dnsBacked);
            System.out.println("serverConfigFile " + serverConfigFile);

            final DomainLookupServer server = new DomainLookupServer(port, dnsBacked, serverConfigFile);
            server.start();
            server.blockUntilShutdown();
        }
        catch (ParseException e)
        {
            System.out.print("Parse error: ");
            System.out.println(e.getMessage());

            System.exit(1);
        }
    }
}
