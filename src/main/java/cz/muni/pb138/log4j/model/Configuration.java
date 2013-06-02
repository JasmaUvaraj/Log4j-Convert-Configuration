package cz.muni.pb138.log4j.model;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import cz.muni.pb138.log4j.AppUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.log4j.Level;

public class Configuration {

    private Map<String, Renderer> renderers = new HashMap<String, Renderer>();
    private Map<String, Logger> loggers = new HashMap<String, Logger>();
    private Map<String, Appender> appenders = new HashMap<String, Appender>();
    private Logger rootLogger;
    private Level threshold; // FOR MARTIN: Threshold nahradeny za org.apache.log4j.Level;
    private Boolean debug;
    private Boolean reset;
    private String wideThreshold;
    private ThrowableRenderer throwableRenderer;
    private LoggerFactory loggerFactory;

    public LoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    public void setLoggerFactory(LoggerFactory loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    public ThrowableRenderer getThrowableRenderer() {
        return throwableRenderer;
    }

    public void setThrowableRenderer(ThrowableRenderer throwableRenderer) {
        this.throwableRenderer = throwableRenderer;
    }

    public Map<String, Renderer> getRenderers() {
        return renderers;
    }

    public void addRenderer(Renderer renderer) {
        if (renderers.get(renderer.getRenderedClass()) == null) {
            renderers.put(renderer.getRenderedClass(), renderer);
        } else {
            AppUtils.crash("Two renderers for the class: " + renderer.getRenderedClass());
        }
    }

    public Map<String, Appender> getAppenders() {
        return appenders;
    }

    public void addAppender(Appender appender) {
        if (appenders.get(appender.getName()) == null) {
            appenders.put(appender.getName(), appender);
        } else {
            AppUtils.crash("Two appenders with the same name: " + appender.getName());
        }
    }

    public Map<String, Logger> getLoggers() {
        return loggers;
    }

    public void addLogger(Logger logger) {
        if (loggers.get(logger.getName()) == null) {
            loggers.put(logger.getName(), logger);
        } else {
            AppUtils.crash("Two loggers with the same name: " + logger.getName());
        }
    }

    public void setRootLogger(Logger root) {
        rootLogger = root;
    }
    
    public Logger getRootLogger() {
        return rootLogger;
    }

    public Level getThreshold() { // FOR MARTIN: Threshold nahradeny za org.apache.log4j.Level;
        return threshold;
    }

    public Boolean isDebug() {
        return debug;
    }

    public void setDebug(String debug) {
        if ("true".equalsIgnoreCase(debug)) {
            this.debug = true;
        } else if ("false".equalsIgnoreCase(debug)) {
            this.debug = false;
        } else {
            AppUtils.crash("You have specified wrong configuration debug parameter");
        }
    }

    public boolean isReset() {
        return reset;
    }

    public void setReset(String reset) {
        if ("true".equalsIgnoreCase(reset)) {
            this.reset = true;
        } else if ("false".equalsIgnoreCase(reset)) {
            this.reset = false;
        } else {
            AppUtils.crash("You have specified wrong configuration reset parameter");
        }
    }

    public void setThreshold(String threshold) {
        try {
            this.threshold = Level.toLevel(threshold); // FOR MARTIN: Nahrada za this.threshold = Threshold.valueOf(threshold)
        } catch (Exception ex) {
            AppUtils.crash("You have specified wrong configuration threshold", ex);
        }
    }

    public void addConfig(String key, String value) {
        if (key.startsWith("logger")) {
            String loggerName = key.substring(7);   // logger names often contain "."
            Logger logger = loggers.get(loggerName);
            if (logger == null) {
                logger = new Logger();
                logger.setName(loggerName);
                loggers.put(loggerName, logger);
            }
            logger.addConfig(value);
        } else if (key.startsWith("rootlogger")) {
            if (rootLogger == null) {
                rootLogger = new Logger();
                rootLogger.setRootLogger(true);
            }
            rootLogger.addConfig(value);
        } else if (key.startsWith("appender")) {
            String newKey = key.substring(9);
            String appenderName = newKey.split("\\.")[0];
            Appender appender = appenders.get(appenderName);
            if (appender == null) {
                appender = new Appender();
                appender.setName(appenderName);
                appenders.put(appenderName, appender);
            }
            appender.addConfig(newKey, value);
        } else if (key.startsWith("additivity")) {
            String loggerName = key.substring(11);
            Logger logger = loggers.get(loggerName);
            if (logger == null) {
                logger = new Logger();             // subelements level and appender-ref are not compulsory
                logger.setName(loggerName);
                loggers.put(loggerName, logger);
            }
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                AppUtils.crash("Wrong value for additivity has been given.");
            } else {
                logger.setAdditivity(value);
            }
        } else if (key.equals("threshold")) {
            try {
                wideThreshold = Level.toLevel(value).toString(); // FOR MARTIN: Nahrada za wideThreshold = Threshold.valueOf(value).toString()
            } catch (IllegalArgumentException ex) {
                AppUtils.crash("Wrong hierarchy-wide threshold has been given.", ex);
            }
        } else if (key.startsWith("renderer")) {
            String fullClassName = key.substring(9);
            Renderer renderer = new Renderer();
            renderer.setRenderingClass(fullClassName);
            renderer.setRenderedClass(value);
            addRenderer(renderer);
        } else if (key.startsWith("throwableRenderer") || key.startsWith("throwablerenderer")) {
            if (throwableRenderer == null) {                    // max. one throwableRenderrer possible
                throwableRenderer = new ThrowableRenderer();
            }
            if (!key.substring(17).isEmpty() && key.substring(17).contains(".")) {
                throwableRenderer.addParam(key.substring(18), value);
            } else {
                throwableRenderer.setClassName(value);
            }
        }

    }

    public void verify() {
        // verify loggers
        for (Logger logger : loggers.values()) {
            logger.verify();
        }

        // verify appenders + layouts
        for (Appender appender : appenders.values()) {
            appender.verify();
        }

        //renderers
        for (Renderer renderer : renderers.values()) {
            renderer.verify();
        }

        if (throwableRenderer != null) {
            throwableRenderer.verify();
        }

        if (loggerFactory != null) {
            loggerFactory.verify();
        }

        if (rootLogger != null) {
            rootLogger.verify();
        }


        if (wideThreshold != null && wideThreshold.contains(" ")) {
            AppUtils.crash("Wide treshold contains a space");
        }
    }

    public Document toXML() {
        Document document = DocumentHelper.createDocument();
        document.addDocType("log4j:configuration", null, "log4j.dtd");
        Element rootElement = document.addElement("log4j:configuration");
        rootElement.addNamespace("log4j", "http://jakarta.apache.org/log4j/");
        if (wideThreshold != null) {
            rootElement.addAttribute("threshold", wideThreshold.toLowerCase());
        }

        for (Renderer renderer : renderers.values()) {
            renderer.addThisToElement(rootElement);
        }
        if(throwableRenderer != null) {
            throwableRenderer.addThisToElement(rootElement);
        }
        for (Appender appender : appenders.values()) {
            appender.addThisToElement(rootElement);
        }
        for (Logger logger : loggers.values()) {
            logger.addThisToElement(rootElement);
        }
        if (rootLogger != null) {
            rootLogger.addThisToElement(rootElement);
        }
        return document;
    }

    public List<String> toProperty() {
        List<String> prop = new ArrayList<String>();

        //root element at first   

        if (rootLogger != null && rootLogger.isRootLogger()) {
                rootLogger.toProperty(prop);
        }

        //loggers
        for (cz.muni.pb138.log4j.model.Logger logger : loggers.values()) {
            if (!logger.isRootLogger()) {
                logger.toProperty(prop);
            }
        }

        //appenders
        for (cz.muni.pb138.log4j.model.Appender appender : appenders.values()) {
            appender.toProperty(prop);

        }
        //renderers
        for (cz.muni.pb138.log4j.model.Renderer renderer : renderers.values()) {
            renderer.toProperty(prop);

        }

        //another params

        if (threshold != null) {
            prop.add(AppUtils.prefix("threshold = " + threshold.toString().toLowerCase()));
        }
        if (debug != null) {
            prop.add(AppUtils.prefix("debug = " + debug));
        }

        if (reset != null) {
            prop.add(AppUtils.prefix("reset = " + reset));
        }

        if (wideThreshold != null && !wideThreshold.isEmpty()) {
            prop.add(AppUtils.prefix("wideThreshold = " + wideThreshold));
        }

        if (throwableRenderer != null) {
            throwableRenderer.toProperty(prop, "");
        }
        if (loggerFactory != null) {
            loggerFactory.toProperty(prop, "");
        }

        return prop;
    }

    public void setUpFromElement(Element rootElement) {
        String thresholdAtt = rootElement.attributeValue("threshold");
        String debugAtt = rootElement.attributeValue("debug");
        String resetAtt = rootElement.attributeValue("reset");

        if (thresholdAtt != null) {

            if (!thresholdAtt.equalsIgnoreCase("null")) {
                thresholdAtt = thresholdAtt.toLowerCase(Locale.ENGLISH);
                setThreshold(thresholdAtt);
            }
        }

        if (debugAtt != null) {
            if (!debugAtt.equalsIgnoreCase("null")) {
                debugAtt = debugAtt.toLowerCase(Locale.ENGLISH);
                setDebug(debugAtt);
            }
        }

        if (resetAtt != null) {
            if (!resetAtt.equalsIgnoreCase("null")) {
                resetAtt = resetAtt.toLowerCase(Locale.ENGLISH);
                setReset(resetAtt);
            }
        }

        for (Element e : (List<Element>) rootElement.elements("renderer")) {
            Renderer renderer = new Renderer();
            renderer.setUpFromElement(e);
            addRenderer(renderer);
        }

        if (rootElement.element("throwableRenderer") != null) {
            ThrowableRenderer tRenderer = new ThrowableRenderer();
            tRenderer.setUpFromElement(rootElement.element("throwableRenderer"));
            setThrowableRenderer(tRenderer);
        }

        for (Element e : (List<Element>) rootElement.elements("appender")) {
            Appender appender = new Appender();
            appender.setUpFromElement(e);
            addAppender(appender);
        }

        for (Element e : (List<Element>) rootElement.elements("logger")) {
            cz.muni.pb138.log4j.model.Logger logger = new cz.muni.pb138.log4j.model.Logger();
            logger.setUpFromElement(e);
            addLogger(logger);
        }


        if (rootElement.element("loggerFactory") != null) {
            LoggerFactory loggerFactory = new LoggerFactory();
            loggerFactory.setUpFromElement(rootElement.element("loggerFactory"));
            setLoggerFactory(loggerFactory);
        }
        if (rootElement.element("root") != null) {
            cz.muni.pb138.log4j.model.Logger rootLogger = new cz.muni.pb138.log4j.model.Logger();
            rootLogger.setUpFromElement(rootElement.element("root"));
            setRootLogger(rootLogger);
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Configuration other = (Configuration) obj;
        if (this.renderers != other.renderers && (this.renderers == null || !this.renderers.equals(other.renderers))) {
            return false;
        }
        if (this.loggers != other.loggers && (this.loggers == null || !this.loggers.equals(other.loggers))) {
            return false;
        }
        if (this.appenders != other.appenders && (this.appenders == null || !this.appenders.equals(other.appenders))) {
            return false;
        }
        if (this.rootLogger != other.rootLogger && (this.rootLogger == null || !this.rootLogger.equals(other.rootLogger))) {
            return false;
        }
        if (this.threshold != other.threshold && (this.threshold == null || !this.threshold.equals(other.threshold))) {
            return false;
        }
        if (this.debug != other.debug && (this.debug == null || !this.debug.equals(other.debug))) {
            return false;
        }
        if (this.reset != other.reset && (this.reset == null || !this.reset.equals(other.reset))) {
            return false;
        }
        if ((this.wideThreshold == null) ? (other.wideThreshold != null) : !this.wideThreshold.equals(other.wideThreshold)) {
            return false;
        }
        if (this.throwableRenderer != other.throwableRenderer && (this.throwableRenderer == null || !this.throwableRenderer.equals(other.throwableRenderer))) {
            return false;
        }
        if (this.loggerFactory != other.loggerFactory && (this.loggerFactory == null || !this.loggerFactory.equals(other.loggerFactory))) {
            return false;
        }
        return true;
    }
}
