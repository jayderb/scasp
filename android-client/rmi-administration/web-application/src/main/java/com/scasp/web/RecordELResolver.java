package com.scasp.web;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotWritableException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;

/**
 * Lets JSP Expression Language read Java record components (${row.courseCode}). Expression Language 5.0 in
 * Jakarta EE 10 only understands JavaBean getters (getX()); record support was added in EL 6.0 (Jakarta EE 11).
 * Registered once by AppContextListener. Read-only: records are immutable.
 */
public class RecordELResolver extends ELResolver {
    private static RecordComponent component(Object base, Object property) {
        if (base == null || property == null || !base.getClass().isRecord()) return null;
        for (RecordComponent rc : base.getClass().getRecordComponents()) if (rc.getName().equals(property.toString())) return rc;
        return null;
    }
    @Override public Object getValue(ELContext ctx, Object base, Object property) {
        RecordComponent rc = component(base, property);
        if (rc == null) return null;
        try {
            Object v = rc.getAccessor().invoke(base);
            ctx.setPropertyResolved(base, property);
            return v;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new jakarta.el.ELException("Cannot read record component " + property, e);
        }
    }
    @Override public Class<?> getType(ELContext ctx, Object base, Object property) {
        RecordComponent rc = component(base, property);
        if (rc == null) return null;
        ctx.setPropertyResolved(base, property);
        return null; // read-only
    }
    @Override public void setValue(ELContext ctx, Object base, Object property, Object value) {
        if (component(base, property) != null) throw new PropertyNotWritableException("Records are immutable");
    }
    @Override public boolean isReadOnly(ELContext ctx, Object base, Object property) {
        if (component(base, property) == null) return false;
        ctx.setPropertyResolved(base, property);
        return true;
    }
    @Override public Class<?> getCommonPropertyType(ELContext ctx, Object base) {
        return base != null && base.getClass().isRecord() ? String.class : null;
    }
}
