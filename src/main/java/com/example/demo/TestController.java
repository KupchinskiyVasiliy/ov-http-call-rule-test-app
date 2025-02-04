package com.example.demo;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Response;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.coyote.ActionHook;
import org.apache.coyote.http11.Http11Processor;
import org.apache.tomcat.util.net.SocketWrapperBase;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

@RestController
public class TestController {

    @GetMapping(value = "/error-code", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void errorCodeNoBody() {
    }

    @GetMapping(value = "/interrupt-connection", produces = MediaType.TEXT_PLAIN_VALUE)
    public void interruptConnection(HttpServletResponse response) {
        closeConnectionNow(response);
    }

    @GetMapping(value = "/partial-body-interrupt-connection-no-length", produces = MediaType.TEXT_PLAIN_VALUE)
    public void partialBodyInterruptConnection(HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        for (int i = 1; i <= 500; i++) {
            writer.println("some body line " + i);
        }

        response.flushBuffer();

        closeConnectionNow(response);
    }

    @GetMapping(value = "/partial-body-interrupt-connection-with-length", produces = MediaType.TEXT_PLAIN_VALUE)
    public void partialBodyInterruptConnectionWithLength(HttpServletResponse response) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            stringBuilder.append("some body line ")
                         .append(i)
                         .append("\n");
        }

        String str = stringBuilder.toString();

        response.addHeader("Content-Length", String.valueOf(str.length()));
        response.getWriter().print(str.substring(0, str.length() / 2));
        response.flushBuffer();

        closeConnectionNow(response);
    }

    private void closeConnectionNow(HttpServletResponse response) {
        ResponseFacade responseFacade = getField(response, "response", ResponseFacade.class);
        Response response1 = getField(responseFacade, "response", Response.class);
        ActionHook actionHook = getField(response1.getCoyoteResponse(), "hook", ActionHook.class);

        Http11Processor processor = (Http11Processor) actionHook;
        SocketWrapperBase<?> socketWrapperBase = getField(processor, "socketWrapper", SocketWrapperBase.class);
        socketWrapperBase.close();
    }

    private <T> T getField(Object instance, String fieldName, Class<T> targetClass) {
        Field field = ReflectionUtils.findField(instance.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        return targetClass.cast(ReflectionUtils.getField(field, instance));
    }

    @GetMapping(value = "/empty-body", produces = MediaType.TEXT_PLAIN_VALUE)
    public void emptyBody() {
    }

    @GetMapping(value = "/empty-body-no-length", produces = MediaType.TEXT_PLAIN_VALUE)
    public void emptyBodyWithoutLength(HttpServletResponse response) throws IOException {
        response.flushBuffer();
    }

    @GetMapping(value = "/full-body-no-length", produces = MediaType.TEXT_PLAIN_VALUE)
    public void fullBodyWithoutContentLength(HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        for (int i = 1; i <= 1000; i++) {
            writer.println("some body line " + i);
        }
    }

    @GetMapping(value = "/full-body-no-length-interrupt-connection", produces = MediaType.TEXT_PLAIN_VALUE)
    public void fullBodyWithoutContentLengthInterruptConnection(HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        for (int i = 1; i <= 1000; i++) {
            writer.println("some body line " + i);
        }

        response.flushBuffer();
        closeConnectionNow(response);
    }

    @GetMapping(value = "/full-body-with-length", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String fullBodyWithContentLength() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            stringBuilder.append("some body line ")
                         .append(i)
                         .append("\n");
        }

        return stringBuilder.toString();
    }

    @GetMapping(value = "/full-body-with-length-interrupt-connection", produces = MediaType.TEXT_PLAIN_VALUE)
    public void fullBodyWithContentLengthInterrupt(HttpServletResponse response) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            stringBuilder.append("some body line ")
                         .append(i)
                         .append("\n");
        }

        String str = stringBuilder.toString();

        response.addHeader("Content-Length", String.valueOf(str.length()));
        response.getWriter().print(str);
        response.flushBuffer();

        closeConnectionNow(response);
    }

    @GetMapping(value = "/empty-body-gzipped", produces = MediaType.TEXT_HTML_VALUE)
    public void emptyBodyGzipped(HttpServletResponse response) throws IOException {
        response.addHeader("Content-Encoding", "gzip");
        response.getWriter().close();
    }

    @GetMapping(value = "/full-body-gzipped", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String fullBodyGzipped() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i <= 1000; i++) {
            stringBuilder.append("{\"access_token\":\"test masking\"}")
                         .append(i)
                         .append("\n");
        }

        return stringBuilder.toString();
    }

}
