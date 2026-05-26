package com.nullify.supportportal.service;

import com.nullify.supportportal.domain.Ticket;
import com.nullify.supportportal.domain.TicketPriority;
import com.nullify.supportportal.domain.TicketStatus;
import com.nullify.supportportal.domain.User;
import com.nullify.supportportal.repository.TicketRepository;
import com.nullify.supportportal.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BulkImportService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public BulkImportService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<Long> importTickets(String xmlPayload) throws ParserConfigurationException, IOException, org.xml.sax.SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlPayload)));
        NodeList ticketNodes = doc.getElementsByTagName("ticket");

        List<Long> createdIds = new ArrayList<>();
        for (int i = 0; i < ticketNodes.getLength(); i++) {
            Element node = (Element) ticketNodes.item(i);
            String title = textOf(node, "title");
            String description = textOf(node, "description");
            String customerEmail = textOf(node, "customerEmail");
            if (title == null || title.isBlank() || customerEmail == null) {
                continue;
            }
            User customer = userRepository.findByEmail(customerEmail).orElse(null);
            if (customer == null) continue;
            Ticket ticket = new Ticket();
            ticket.setTitle(title);
            ticket.setDescription(description == null ? "" : description);
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setPriority(TicketPriority.NORMAL);
            ticket.setCustomer(customer);
            createdIds.add(ticketRepository.save(ticket).getId());
        }
        return createdIds;
    }

    private String textOf(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    @Transactional
    public List<Long> importCsv(String csvPayload) throws IOException {
        if (csvPayload == null || csvPayload.isBlank()) {
            throw new IllegalArgumentException("csv payload is empty");
        }
        List<Long> createdIds = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvPayload))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("csv has no header row");
            }
            Map<String, Integer> columns = new HashMap<>();
            String[] headerCols = splitCsvLine(header);
            for (int i = 0; i < headerCols.length; i++) {
                columns.put(headerCols[i].trim().toLowerCase(), i);
            }
            Integer titleIdx = columns.get("title");
            Integer descIdx = columns.get("description");
            Integer emailIdx = columns.get("customeremail");
            if (titleIdx == null || emailIdx == null) {
                throw new IllegalArgumentException("csv must have title and customerEmail columns");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = splitCsvLine(line);
                if (cols.length <= titleIdx || cols.length <= emailIdx) continue;
                String title = cols[titleIdx].trim();
                String customerEmail = cols[emailIdx].trim();
                String description = descIdx != null && cols.length > descIdx ? cols[descIdx].trim() : "";
                if (title.isEmpty() || customerEmail.isEmpty()) continue;
                User customer = userRepository.findByEmail(customerEmail).orElse(null);
                if (customer == null) continue;
                Ticket ticket = new Ticket();
                ticket.setTitle(title);
                ticket.setDescription(description);
                ticket.setStatus(TicketStatus.OPEN);
                ticket.setPriority(TicketPriority.NORMAL);
                ticket.setCustomer(customer);
                createdIds.add(ticketRepository.save(ticket).getId());
            }
        }
        return createdIds;
    }

    private String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out.toArray(new String[0]);
    }
}
