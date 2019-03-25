package com.smittys.managers;

import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.InventoryError;
import com.smittys.entities.Invoice;
import com.smittys.entities.InvoiceItem;
import com.smittys.entities.ItemData;
import com.smittys.utils.parsers.SalesParser;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class EmailManager {

    private static ArrayList<Date> checkedDates = new ArrayList<>();

    public static void main(String[] args) {
        Tracker tracker = new Tracker();
        tracker.startConnections();
        //readSales();
        readInvoices();
    }

    public static void readSales() {
        Session session = Session.getInstance(getEmailConfig(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Tracker.getInstance().getProperties().getProperty("email-user"), Tracker.getInstance().getProperties().getProperty("email-pass"));
            }
        });

        Store store;
        try {
            store = session.getStore("imaps");
            store.connect();
            Folder salesFolder = store.getDefaultFolder().getFolder("Sales Log");
            salesFolder.open(Folder.READ_WRITE);
            Message messages[] = salesFolder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            salesFolder.setFlags(messages, new Flags(Flags.Flag.SEEN), true);
            //ArrayUtils.reverse(messages);
            Stream.of(messages).forEach(e -> {
                try {
                    if (!salesFolder.isOpen()) salesFolder.open(Folder.READ_ONLY);
                    String subject = e.getSubject();
                    if (checkedDates.contains(e.getReceivedDate())) return;
                    if (!subject.toLowerCase().equals("sales email")) return;
                    Multipart part = (Multipart) e.getContent();
                    for (int i = 0; i < part.getCount(); i++) {
                        BodyPart body = part.getBodyPart(i);
                        if (!Part.ATTACHMENT.equalsIgnoreCase(body.getDisposition()) && StringUtils.isBlank(body.getFileName())) {
                            continue;
                        }
                        if (!body.getFileName().contains("SalesByDivision")) continue;
                        checkedDates.add(e.getReceivedDate());
                        InputStream is = body.getInputStream();
                        SimpleDateFormat format = new SimpleDateFormat("M-d-yyyy");
                        File file = new File("data/sales/" + format.format(e.getReceivedDate()) + ".html");
                        if (file.exists()) continue;
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buf = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buf)) != -1) fos.write(buf, 0, bytesRead);
                        fos.close();
                        SalesParser parser = new SalesParser(file);
                        parser.parse();
                    }
                } catch (MessagingException | IOException e1) {
                    e1.printStackTrace();
                }
            });
        } catch (MessagingException e) {
            e.printStackTrace();
        }

    }

    public static void checkEmails() {
        readSales();
        readInvoices();
    }

    public static void readInvoices() {
        Session session = Session.getInstance(getEmailConfig(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Tracker.getInstance().getProperties().getProperty("email-user"), Tracker.getInstance().getProperties().getProperty("email-pass"));
            }
        });

        try {

            Pattern invoiceIdPattern = Pattern.compile("Order:(.*?) ;");
            Pattern packSizePattern = Pattern.compile("\\d+\\.?\\d*");
            Store store = session.getStore("imaps");
            store.connect();
            Folder sysco = store.getDefaultFolder().getFolder("Sysco");
            sysco.open(Folder.READ_WRITE);
            Message messages[] = sysco.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            sysco.setFlags(messages, new Flags(Flags.Flag.SEEN), true);
            Stream.of(messages).forEach(m -> {
                try {
                    if (!sysco.isOpen()) sysco.open(Folder.READ_ONLY);
                    String subject = m.getSubject();
                    if (!subject.contains("SyscoSource Order Confirmation")) return;
                    Matcher matcher = invoiceIdPattern.matcher(subject);
                    if (!matcher.find()) return;
                    long invoiceId = Long.parseLong(matcher.group(1));
                    Object[] data = InventoryConnection.connection().handleRequest("get-invoice", invoiceId);
                    boolean invoiceExists = data != null;
                    String content = getTextFromMessage(m);
                    Document doc = Jsoup.parse(content);
                    Elements tables = doc.select("table");
                    Optional<Element> optional = tables.stream().filter(t -> t.attr("border").equals("0") && t.attr("cellspacing").equals("3") && t.attr("cellpadding").equals("5px")).findAny();
                    if (!optional.isPresent()) return;
                    Element table = optional.get();
                    int quantityShipped = 0;
                    int quantityOrdered = 0;
                    double price = 0;
                    Timestamp shipDate = null;
                    Elements elements = table.getElementsByTag("td");
                    for (int i = 0; i < elements.size(); i++) {
                        Element element = elements.get(i);
                        if (element.text().equals("Ship Date:")) {
                            String date = elements.get(i + 1).text();
                            shipDate = new Timestamp(new SimpleDateFormat("MM/dd/yyyy").parse(date).getTime());
                        } else if (element.text().equals("Quantity Ordered:"))
                            quantityOrdered = Integer.parseInt(elements.get(i + 1).text());
                        else if (element.text().equals("Quantity Shipped (Est.):"))
                            quantityShipped = Integer.parseInt(elements.get(i + 1).text());
                        else if (element.text().equals("Amount Shipped (Est.):"))
                            price = Double.parseDouble(elements.get(i + 1).text());
                    }
                    Invoice invoice = invoiceExists ? (Invoice) data[0] : new Invoice(-1, invoiceId, "Sysco", quantityOrdered, quantityShipped, price, shipDate, null, null);
                    if (!invoiceExists) InventoryConnection.connection().handleRequest("create-invoice", invoice);
                    else if (quantityOrdered != invoice.getQuantityOrdered() || quantityShipped != invoice.getQuantityShipped() || price != invoice.getPrice())
                        InventoryConnection.connection().handleRequest("edit-invoice-size", invoiceId, quantityOrdered, quantityShipped, price);
                    optional = tables.stream().filter(t -> t.attr("border").equals("1") && t.attr("cellspacing").equals("3") && t.attr("cellpadding").equals("0")).findAny();
                    if (!optional.isPresent())
                        return;
                    table = optional.get();
                    Elements rows = table.getElementsByTag("tr");
                    rows.remove(0);
                    for (Element row : rows) {
                        Elements values = row.getElementsByTag("td");
                        if (values.get(0).text().equals("")) continue;
                        String itemCode = values.get(0).text();
                        data = InventoryConnection.connection().handleRequest("get-item-in-invoice", invoiceId, itemCode);
                        if (data != null) continue;
                        data = InventoryConnection.connection().handleRequest("get-item-by-code", itemCode);
                        boolean needsCreate = data == null;
                        String upcCode = values.get(1).text();
                        int packCount = 0;
                        try {
                            packCount = Integer.parseInt(values.get(2).text());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                            return;
                        }
                        matcher = packSizePattern.matcher(values.get(3).text());
                        if (!matcher.find()) continue;
                        double packSize = Double.parseDouble(matcher.group(0));
                        String packUnit = values.get(3).text().replace(Double.toString(packSize), "");
                        String brand = values.get(4).text();
                        String itemName = values.get(5).text();
                        quantityOrdered = Integer.parseInt(values.get(6).text());
                        quantityShipped = Integer.parseInt(values.get(7).text());
                        String shipUnit = values.get(8).text();
                        price = Double.parseDouble(values.get(9).text());
                        double EXTPrice = Double.parseDouble(values.get(10).text());
                        String status = values.get(11).text();
                        ItemData itemData = needsCreate ? new ItemData(-1, itemCode, upcCode, price, itemName, "", brand, "Sysco", packCount, packSize, packUnit, "", null, null) : (ItemData) data[0];
                        if (needsCreate) {
                            InventoryConnection.connection().handleRequest("create-item", itemData);
                            InventoryError error = new InventoryError("missing-display-" + itemCode, "Missing display name for " + itemName, "Missing display name for " + itemName);
                            Tracker.getInstance().getErrorManager().add(error);
                        } else if (price != itemData.getPrice())
                            InventoryConnection.connection().handleRequest("change-price", itemCode, itemData.getPrice(), price, shipDate);
                        InventoryConnection.connection().handleRequest("add-invoice-item", new InvoiceItem(-1, invoiceId, itemCode, price, EXTPrice, quantityOrdered, quantityShipped, shipUnit, status));
                    }
                } catch (MessagingException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (ParseException e1) {
                    e1.printStackTrace();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            });
//            ArrayUtils.reverse(messages);
//            Stream.of(messages).skip(messages.length - 10).forEach(e -> {
//                try {
//                    if (!sysco.isOpen()) sysco.open(Folder.READ_ONLY);
//                    String subject = e.getSubject();
//                    if (!subject.contains("SyscoSource Order Confirmation")) return;
//                    Matcher matcher = invoiceIdPattern.matcher(subject);
//                    if (!matcher.find()) return;
//                    long invoiceId = Long.parseLong(matcher.group(1));
//                    Object[] data = InventoryConnection.connection().handleRequest("get-invoice", invoiceId);
//                    boolean invoiceExists = data != null;
//                    String content = getTextFromMessage(e);
//                    Document doc = Jsoup.parse(content);
//                    Elements tables = doc.select("table");
//                    Optional<Element> optional = tables.stream().filter(t -> t.attr("border").equals("0") && t.attr("cellspacing").equals("3") && t.attr("cellpadding").equals("5px")).findAny();
//                    if (!optional.isPresent()) return;
//                    Element table = optional.get();
//                    int quantityShipped = 0;
//                    int quantityOrdered = 0;
//                    double price = 0;
//                    Timestamp shipDate = null;
//                    Elements elements = table.getElementsByTag("td");
//                    for (int i = 0; i < elements.size(); i++) {
//                        Element element = elements.get(i);
//                        if (element.text().equals("Ship Date:")) {
//                            String date = elements.get(i + 1).text();
//                            shipDate = new Timestamp(new SimpleDateFormat("MM/dd/yyyy").parse(date).getTime());
//                        } else if (element.text().equals("Quantity Ordered:"))
//                            quantityOrdered = Integer.parseInt(elements.get(i + 1).text());
//                        else if (element.text().equals("Quantity Shipped (Est.):"))
//                            quantityShipped = Integer.parseInt(elements.get(i + 1).text());
//                        else if (element.text().equals("Amount Shipped (Est.):"))
//                            price = Double.parseDouble(elements.get(i + 1).text());
//                    }
//                    Invoice invoice = invoiceExists ? (Invoice) data[0] : new Invoice(-1, invoiceId, "Sysco", quantityOrdered, quantityShipped, price, shipDate, null, null);
//                    if (!invoiceExists) InventoryConnection.connection().handleRequest("create-invoice", invoice);
//                    else if (quantityOrdered != invoice.getQuantityOrdered() || quantityShipped != invoice.getQuantityShipped() || price != invoice.getPrice())
//                        InventoryConnection.connection().handleRequest("edit-invoice-size", invoiceId, quantityOrdered, quantityShipped, price);
//                    optional = tables.stream().filter(t -> t.attr("border").equals("1") && t.attr("cellspacing").equals("3") && t.attr("cellpadding").equals("0")).findAny();
//                    if (!optional.isPresent())
//                        return;
//                    table = optional.get();
//                    Elements rows = table.getElementsByTag("tr");
//                    rows.remove(0);
//                    for (Element row : rows) {
//                        Elements values = row.getElementsByTag("td");
//                        if (values.get(0).text().equals("")) continue;
//                        String itemCode = values.get(0).text();
//                        data = InventoryConnection.connection().handleRequest("get-item-in-invoice", invoiceId, itemCode);
//                        if (data != null) continue;
//                        data = InventoryConnection.connection().handleRequest("get-item-by-code", itemCode);
//                        boolean needsCreate = data == null;
//                        String upcCode = values.get(1).text();
//                        int packCount = 0;
//                        try {
//                            packCount = Integer.parseInt(values.get(2).text());
//                        } catch (Exception e1) {
//                            e1.printStackTrace();
//                            return;
//                        }
//                        matcher = packSizePattern.matcher(values.get(3).text());
//                        if (!matcher.find()) continue;
//                        double packSize = Double.parseDouble(matcher.group(0));
//                        String packUnit = values.get(3).text().replace(Double.toString(packSize), "");
//                        String brand = values.get(4).text();
//                        String itemName = values.get(5).text();
//                        quantityOrdered = Integer.parseInt(values.get(6).text());
//                        quantityShipped = Integer.parseInt(values.get(7).text());
//                        String shipUnit = values.get(8).text();
//                        price = Double.parseDouble(values.get(9).text());
//                        double EXTPrice = Double.parseDouble(values.get(10).text());
//                        String status = values.get(11).text();
//                        ItemData itemData = needsCreate ? new ItemData(-1, itemCode, upcCode, price, itemName, "", brand, packCount, packSize, packUnit, "", null, null) : (ItemData) data[0];
//                        if (needsCreate) {
//                            InventoryConnection.connection().handleRequest("create-item", itemData);
//                            InventoryError error = new InventoryError("missing-display-" + itemCode, "Missing display name for " + itemName, "Missing display name for " + itemName);
//                            Tracker.getInstance().getErrorManager().add(error);
//                        } else if (price != itemData.getPrice())
//                            InventoryConnection.connection().handleRequest("change-price", itemCode, itemData.getPrice(), price, shipDate);
//                        InventoryConnection.connection().handleRequest("add-invoice-item", new InvoiceItem(-1, invoiceId, itemCode, price, EXTPrice, quantityOrdered, quantityShipped, shipUnit, status));
//                    }
//                } catch (MessagingException e1) {
//                    e1.printStackTrace();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                } catch (ParseException e1) {
//                    e1.printStackTrace();
//                } catch (Exception e1) {
//                    e1.printStackTrace();
//                }
//            });
        } catch (MessagingException e) {
            e.printStackTrace();
            return;
        }

    }

    private static String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                result = result + "\n" + bodyPart.getContent().toString();
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }

    private static Properties getEmailConfig() {
        Properties config = new Properties();
        config.put("mail.host", "outlook.office365.com");
        config.put("mail.store.protocol", "imaps");
        return config;
    }

}
