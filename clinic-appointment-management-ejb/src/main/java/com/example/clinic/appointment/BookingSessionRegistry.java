package com.example.clinic.appointment;

import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.EJBException;
import javax.naming.InitialContext;
import jakarta.ws.rs.NotFoundException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class BookingSessionRegistry {

    private final ConcurrentHashMap<String, BookingSessionBean> sessions = new ConcurrentHashMap<>();

    @Lock(LockType.WRITE)
    public String createSession() {
        try {
            InitialContext ic = new InitialContext();
            BookingSessionBean bean = (BookingSessionBean) ic.lookup("java:module/BookingSessionBean");
            String sessionId = UUID.randomUUID().toString();
            sessions.put(sessionId, bean);
            return sessionId;
        } catch (Exception e) {
            throw new EJBException("Failed to create booking session", e);
        }
    }

    @Lock(LockType.READ)
    public BookingSessionBean getSession(String sessionId) {
        BookingSessionBean bean = sessions.get(sessionId);
        if (bean == null) {
            throw new NotFoundException("Booking session not found or expired");
        }
        return bean;
    }

    @Lock(LockType.WRITE)
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
