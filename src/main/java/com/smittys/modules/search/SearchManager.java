package com.smittys.modules.search;

import com.google.gson.Gson;
import com.smittys.Tracker;
import com.smittys.entities.Filter;
import com.smittys.modules.WebModule;
import com.smittys.utils.Utilities;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static spark.Spark.get;
import static spark.Spark.post;

public class SearchManager {

    private HashMap<String, Class<?>> filters;

    public void load() {
        filters = new HashMap<>();
        try {
            for (Class<?> c : Utilities.getClasses("com.smittys.modules.search.impl")) {
                Object instance = c.newInstance();
                if (!(instance instanceof Filter))
                    continue;
                filters.put(((Filter) instance).name, c);
            }
        } catch (ClassNotFoundException | IOException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public Filter getFilter(String name) {
        if (!filters.containsKey(name)) return null;
        Class<?> o = filters.get(name);
        try {
            return (Filter) o.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void registerEndpoints() {
        post("/search/:module", (req, res) -> new Gson().toJson(handleEndpoint(req.params(":module"), req, res)));
        get("/search/:module", (req, res) -> new Gson().toJson(handleEndpoint(req.params(":module"), req, res)));
    }

    public static Properties handleEndpoint(String module, Request request, Response response) {
        String query = request.queryParams("query");
        String pageStr = request.queryParams("page");
        String archivedStr = request.queryParams("archived");
        String paramStr = request.queryParams("params");
        String searchName = request.queryParams("searchName");
        Properties prop = new Properties();
        if (Utilities.isNullOrEmpty(query, pageStr, archivedStr)) {
            prop.put("success", false);
            prop.put("error", "Missing some parameters required for searching.");
            return prop;
        }
        prop = search(module, query, Integer.parseInt(pageStr), Boolean.parseBoolean(archivedStr), request, response, paramStr == null ? new HashMap<>() : new Gson().fromJson(paramStr, HashMap.class), searchName);
        return prop;
    }

    public static Properties search(String module, String text, int page, boolean archived, Request request, Response response, HashMap<String, String> params, String searchName) {
        Optional<SearchEndpoints> optional = SearchEndpoints.getEndpoint(searchName);
        Properties prop = new Properties();
        if (!WebModule.isLoggedIn(request)) {
            prop.put("success", false);
            prop.put("error", "Must be logged in to use the search function.");
            return prop;
        }
        while (true) {
            if (!optional.isPresent()) {
                prop.put("success", false);
                prop.put("error", "Invalid endpoint! s" + searchName);
                break;
            }
            SearchEndpoints endpoint = optional.get();
            HashMap<String, Object> model = new HashMap<>();
            text = text.replaceAll(", ", ",");
            String[] queries = text.split(",");
            if (queries.length == 0) {
                prop.put("success", false);
                prop.put("error", "Invalid search parameters");
                break;
            }
            HashMap<String, Filter> filters = new HashMap<>();
            HashMap<String, String> sFilters = new HashMap<>();
            boolean incorrect = false;
            for (String query : queries) {
                if (!query.contains(":")) {
                    prop.put("success", false);
                    prop.put("error", "Invalid search parameters. Please read instructions on how to search.");
                    return prop;
                }
                String[] values = query.split(":");
                if (values.length != 2)
                    continue;
                String filterName = values[0];
                String value = values[1].toLowerCase();
                Filter filter = Tracker.getInstance().getSearchManager().getFilter(filterName);
                if (filter == null) {
                    incorrect = true;
                    continue;
                }
                if (filters.containsKey(filter.getName())) {
                    prop.put("success", false);
                    prop.put("error", "You cannot have two of the same filters.");
                    return prop;
                }
                if (!filter.appliesTo(searchName, archived)) {
                    incorrect = true;
                    continue;
                }
                if (value.startsWith(" "))
                    value = value.replaceFirst(" ", "");
                if (!filter.setValue(module, value)) {
                    incorrect = true;
                    continue;
                }
                filters.put(filter.getName(), filter);
                sFilters.put(filter.getName(), value);
            }
            if (filters.size() == 0 && incorrect == true) {
                prop.put("success", false);
                prop.put("error", "Search contains invalid keys.");
                return prop;
            }
            ArrayList<Filter> filterA = new ArrayList<>();
            filterA.addAll(filters.values());
            Object[] data = endpoint.getConnection().handleRequest("search", getQueryValue(module, filterA), page, archived, params, module);
            Object[] countData = endpoint.getConnection().handleRequest("search-results", getQueryValue(module, filterA), archived, params, module);
            if (data == null || countData == null) {
                prop.put("success", false);
                String results = "No search results found.";
                if (incorrect)
                    results += " Your search query contained invalid filters.";
                prop.put("error", results);
                return prop;
            }
            List<?> results = (List<?>) data[0];
            if (results.size() == 0) {
                prop.put("success", false);
                prop.put("error", "No search results found.");
                return prop;
            }
            for (Filter filter : filterA)
                results = filter.filterList(results);
            int resultSize = (int) countData[0];
            model.put(endpoint.getKey(), results);
            model.put("staff", endpoint.getRights() > 0);
            String html = null;
            try {
                html = WebModule.render(endpoint.getJadeFile(), model, request, response);
            } catch (Exception e) {
                e.printStackTrace();
                prop.put("success", false);
                prop.put("error", "Error loading search list.");
                return prop;
            }
            prop.put("success", true);
            prop.put("html", html);
            prop.put("pageTotal", resultSize);
            prop.put("filters", sFilters);
            return prop;
        }
        return prop;
    }

    public static Properties getQueryValue(String mod, ArrayList<Filter> filters) {
        if (filters.size() == 0) return null;
        List<Filter> applicable = filters.stream().filter(f -> f.getFilter(mod) != null).collect(Collectors.toList());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < applicable.size(); i++) {
            Filter filter = applicable.get(i);
            builder.append(filter.getFilter(mod));
            if (i != applicable.size() - 1)
                builder.append(" AND ");
        }
        ArrayList<Object> valueList = new ArrayList<>();
        for (Filter filter : applicable) {
            if (filter.getFilter(mod) != null && filter.getValue() != null) {
                if (filter.getValue() instanceof Object[]) {
                    for (Object obj : (Object[]) filter.getValue())
                        valueList.add(obj);
                } else
                    valueList.add(filter.getValue());
            }
        }
        Object[] values = valueList.toArray(new Object[valueList.size()]);
        Properties prop = new Properties();
        prop.put("query", builder.toString());
        prop.put("values", values);
        return prop;
    }

}
