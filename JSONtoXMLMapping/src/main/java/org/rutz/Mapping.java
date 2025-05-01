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
    private String parentXPath; // Add this field
    private int order; // Add this field for ordering


    // Constructor
    public Mapping(String jPath, String xPath, boolean isList, String jsonType, String xmlType, String exprsn, String namespace, String parentXPath, int order) {
        this.jPath = jPath;
        this.xPath = xPath;
        this.isList = isList;
        this.jsonType = jsonType;
        this.xmlType = xmlType;
        this.exprsn = exprsn;
        this.namespace = namespace;
        this.childMappings = new ArrayList<>();
        this.parentXPath = parentXPath;
        this.order = order; // Initialize order
    }

    public Mapping() {

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

    public String getXmlType() {
        return xmlType;
    }

    public void setXmlType(String xmlType) {
        this.xmlType = xmlType;
    }


    public String getParentXPath() {
        return parentXPath;
    }

    public void setParentXPath(String parentXPath) {
        this.parentXPath = parentXPath;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
