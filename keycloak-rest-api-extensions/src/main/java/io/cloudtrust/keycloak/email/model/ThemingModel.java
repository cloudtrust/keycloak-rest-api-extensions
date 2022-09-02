package io.cloudtrust.keycloak.email.model;

import java.util.List;
import java.util.Map;

public class ThemingModel {
    private String themeRealmName;
    private String locale;
    private String subjectKey;
    private List<String> subjectParams;
    private String template;
    private Map<String, String> templateParams;

    public String getThemeRealmName(){
        return themeRealmName;
    }

    public void setThemeRealmName(String themeRealmName){
        this.themeRealmName = themeRealmName;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    public void setSubjectKey(String subjectKey) {
        this.subjectKey = subjectKey;
    }

    public List<String> getSubjectParameters() {
        return subjectParams;
    }

    public void setSubjectParameters(List<String> subjectParams) {
        this.subjectParams = subjectParams;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, String> getTemplateParameters() {
        return templateParams;
    }

    public void setTemplateParameters(Map<String, String> templateParams) {
        this.templateParams = templateParams;
    }
}
