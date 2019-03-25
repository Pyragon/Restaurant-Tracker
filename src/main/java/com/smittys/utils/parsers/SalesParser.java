package com.smittys.utils.parsers;

import com.smittys.Tracker;
import com.smittys.db.impl.InventoryConnection;
import com.smittys.entities.InventoryError;
import com.smittys.entities.RecipeItem;
import com.smittys.entities.SalesItem;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

@RequiredArgsConstructor
public class SalesParser {

    private final File file;

    public void parse() {
        try {
            Document doc = Jsoup.parse(file, "UTF-8", "");
            Elements elements = doc.getElementsByTag("tr");
            ArrayList<Element> itemElements = new ArrayList<>();
            Timestamp stamp = new Timestamp(new Date().getTime());
            l:
            for (Element element : elements) {
                Elements tds = element.getElementsByTag("td");
                for (Element td : tds) {
                    if (td.hasAttr("colspan") && td.attributes().get("colspan").equals("20")
                            && td.text().contains("Last day")) {
                        String dateString = td.text().replace("Last day ", "");
                        SimpleDateFormat format = new SimpleDateFormat("EEEE, MMMM d, yyyy");
                        stamp = new Timestamp(format.parse(dateString).getTime());
                        Object[] data = InventoryConnection.connection().handleRequest("get-sales-items-by-date",
                                stamp);
                        ArrayList<SalesItem> items = (ArrayList<SalesItem>) data[0];
                        if (items.size() > 0)
                            InventoryConnection.connection().handleRequest("delete-sales-items-by-date", stamp);
                    }
                    if (element.hasAttr("bgcolor"))
                        continue;
                    if (td.getElementsByTag("span").size() <= 0)
                        continue;
                    if (!td.hasAttr("colspan") || !td.attributes().get("colspan").equals("7"))
                        continue;
                    Element span = td.getElementsByTag("span").get(0);
                    if (!span.hasText())
                        continue;
                    itemElements.add(element);
                    continue l;
                }
            }
            double totalPrice = 0;
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);
            for (Element element : itemElements) {
                Elements tds = element.getElementsByTag("td");
                String name = tds.get(1).text();
                if (name.contains("page "))
                    continue;
                int quantity = Integer.parseInt(tds.get(17).text());
                double price = Double
                        .parseDouble(df.format(Double.parseDouble(tds.get(19).text().replace("$", "")) / quantity));
                Object[] data = InventoryConnection.connection().handleRequest("get-recipe-item-by-bill-name", name);
                RecipeItem item;
                if (data == null) {
                    item = new RecipeItem(-1, "", name, "", "", price, null);
                    InventoryConnection.connection().handleRequest("add-recipe-item", item);
                    Tracker.getInstance().getErrorManager().add(new InventoryError("missing-recipe-item-" + name + "",
                            "Missing recipe item data for " + name, "Missing recipe item for " + name));
                } else
                    item = (RecipeItem) data[0];
                totalPrice += item.getPrice() * quantity;
                SalesItem salesItem = new SalesItem(-1, name, quantity, price, stamp);
                InventoryConnection.connection().handleRequest("add-sales-item", salesItem);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
