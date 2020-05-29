package edu.cs244b.server;

import edu.cs244b.mappings.DNSMappingStore;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.ParseException;


public class Main {
    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option portOption = new Option("p", "port", true, "port");
        portOption.setRequired(false);
        options.addOption(portOption);

        Option dnsOption = new Option("d", "dnsBacked", false, "Whether this server is backed by normal DNS resolution");
        dnsOption.setRequired(false);
        options.addOption(dnsOption);

        CommandLine commandLine;
        CommandLineParser parser = new BasicParser();

        try
        {
            commandLine = parser.parse(options, args);
            int port = Integer.parseInt(commandLine.getOptionValue("port", "8980"));
            boolean dnsBacked = Boolean.parseBoolean(commandLine.getOptionValue("dnsBacked", "false"));

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
