package com.example.clinic.api;

import com.example.clinic.common.ApiError;
import com.example.clinic.appointment.BookingValidationException;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        Exception unwrapped = exception;
        if (exception instanceof EJBException ejbEx && ejbEx.getCausedByException() instanceof Exception cause) {
            unwrapped = cause;
        }

        if (unwrapped instanceof BookingValidationException bvEx) {
            String correlationId = UUID.randomUUID().toString();
            ApiError error = new ApiError(bvEx.getErrorCode(), bvEx.getMessage(), correlationId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }

        if (unwrapped instanceof WebApplicationException webEx) {
            String correlationId = UUID.randomUUID().toString();
            ApiError error = new ApiError("REQUEST_ERROR", webEx.getMessage(), correlationId);
            return Response.status(webEx.getResponse().getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }

        ApiError error = new ApiError("INTERNAL_ERROR", "Unexpected server error", UUID.randomUUID().toString());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
