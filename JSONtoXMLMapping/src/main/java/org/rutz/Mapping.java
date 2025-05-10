package org.rutz;

import java.util.ArrayList;
import java.util.List;

public class Mapping {
    private String jPath;
    private String xPath;
    private boolean isList;
    private String jsonType;
    private String xmlType;
    private String exprsn;
    private String namespace;
    private List<Mapping> childMappings;
    private String parentXPath;
    private int order;
    
    // New fields for Collector Mappings extension
    private boolean isCollector;               // Indicates if this mapping is a collector
    private List<String> sourceFeedJPaths;     // List of JPaths that serve as data sources for collector
    private String conditionJPath;             // Used for derived fields - condition to check
    private String defaultValue;               // Default value for derived fields if condition is met

    // Constructor with all fields including the new collector-related ones
    public Mapping(String jPath, String xPath, boolean isList, String jsonType, String xmlType, 
                  String exprsn, String namespace, String parentXPath, int order,
                  boolean isCollector, List<String> sourceFeedJPaths,
                  String conditionJPath, String defaultValue) {
        this.jPath = jPath;
        this.xPath = xPath;
        this.isList = isList;
        this.jsonType = jsonType;
        this.xmlType = xmlType;
        this.exprsn = exprsn;
        this.namespace = namespace;
        this.childMappings = new ArrayList<>();
        this.parentXPath = parentXPath;
        this.order = order;
        this.isCollector = isCollector;
        this.sourceFeedJPaths = sourceFeedJPaths != null ? sourceFeedJPaths : new ArrayList<>();
        this.conditionJPath = conditionJPath;
        this.defaultValue = defaultValue;
    }
    
    // Constructor with original fields (for backward compatibility)
    public Mapping(String jPath, String xPath, boolean isList, String jsonType, String xmlType, 
                   String exprsn, String namespace, String parentXPath, int order) {
        this(jPath, xPath, isList, jsonType, xmlType, exprsn, namespace, parentXPath, order, 
             false, new ArrayList<>(), null, null);
    }

    public Mapping() {
        this.childMappings = new ArrayList<>();
        this.sourceFeedJPaths = new ArrayList<>();
    }

    // Getters and setters
    public String getJPath() { return jPath; }
    public void setJPath(String jPath) { this.jPath = jPath; }

    public String getXPath() { return xPath; }
    public void setXPath(String xPath) { this.xPath = xPath; }

    public boolean isList() { return isList; }
    public void setList(boolean isList) { this.isList = isList; }

    public String getJsonType() { return jsonType; }
    public void setJsonType(String jsonType) { this.jsonType = jsonType; }

    public String getExprsn() { return exprsn; }
    public void setExprsn(String exprsn) { this.exprsn = exprsn; }

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public List<Mapping> getChildMappings() { return childMappings; }
    public void setChildMappings(List<Mapping> childMappings) { this.childMappings = childMappings; }

    // Convenience method to add child mappings
    public void addChildMapping(Mapping childMapping) {
        this.childMappings.add(childMapping);
    }

    public String getXmlType() { return xmlType; }
    public void setXmlType(String xmlType) { this.xmlType = xmlType; }

    public String getParentXPath() { return parentXPath; }
    public void setParentXPath(String parentXPath) { this.parentXPath = parentXPath; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    
    // New getters and setters for collector functionality
    public boolean isCollector() { return isCollector; }
    public void setCollector(boolean collector) { isCollector = collector; }
    
    public List<String> getSourceFeedJPaths() { return sourceFeedJPaths; }
    public void setSourceFeedJPaths(List<String> sourceFeedJPaths) { 
        this.sourceFeedJPaths = sourceFeedJPaths; 
    }
    
    // Convenience method to add a feed JPath
    public void addSourceFeedJPath(String feedJPath) {
        if (this.sourceFeedJPaths == null) {
            this.sourceFeedJPaths = new ArrayList<>();
        }
        this.sourceFeedJPaths.add(feedJPath);
    }
    
    // Getters and setters for derived fields
    public String getConditionJPath() { return conditionJPath; }
    public void setConditionJPath(String conditionJPath) { 
        this.conditionJPath = conditionJPath; 
    }
    
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { 
        this.defaultValue = defaultValue; 
    }
}
