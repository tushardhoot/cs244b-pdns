package edu.cs244b.server;

import edu.cs244b.mappings.DNSMappingStore;
import org.apache.commons.cli.*;

import java.util.Arrays;


public class Main {
    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option portOption = new Option("p", "port", true, "port");
        portOption.setRequired(false);
        options.addOption(portOption);

        Option dnsOption = new Option("d", false, "Whether this server is backed by normal DNS resolution");
        dnsOption.setRequired(false);
        options.addOption(dnsOption);

        CommandLine commandLine;
        CommandLineParser parser = new DefaultParser();

        try
        {
            commandLine = parser.parse(options, args);
            int port = Integer.parseInt(commandLine.getOptionValue("port", "8980"));
            boolean dnsBacked = commandLine.hasOption("d");

            System.out.println("args " + Arrays.toString(args));
            System.out.println("dnsBacked " + dnsBacked);

            DomainLookupServer server;
            if (!dnsBacked) {
                server = new DomainLookupServer(port);
            } else {
                server = new DomainLookupServer(port, new DNSMappingStore());
            }

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
