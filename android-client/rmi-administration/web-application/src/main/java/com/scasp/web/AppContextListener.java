package com.scasp.web;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.jsp.JspFactory;

import javax.sql.DataSource;
import java.util.logging.Logger;

/** Creates the repositories once at start-up from the container's pooled MySQL DataSource (JNDI jdbc/scasp). */
@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());
    @Resource(lookup = "jdbc/scasp") private DataSource dataSource;

    @Override public void contextInitialized(ServletContextEvent e) {
        e.getServletContext().setAttribute(Repositories.CONTEXT_KEY, new Repositories(dataSource));
        JspFactory.getDefaultFactory().getJspApplicationContext(e.getServletContext()).addELResolver(new RecordELResolver());
        LOG.info("SCASP web application started with DataSource jdbc/scasp");
    }
    @Override public void contextDestroyed(ServletContextEvent e) {
        e.getServletContext().removeAttribute(Repositories.CONTEXT_KEY);
        LOG.info("SCASP web application stopped");
    }
}
