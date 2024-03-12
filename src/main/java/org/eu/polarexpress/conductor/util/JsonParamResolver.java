package org.eu.polarexpress.conductor.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class JsonParamResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        var jsonBody = getRequestBody(webRequest);
        return Optional.ofNullable(parameter.getParameterAnnotation(JsonParam.class))
                .filter(jsonParam -> jsonBody != null && !jsonBody.isEmpty())
                .map(JsonParam::value)
                .map(path -> JsonPath.parse(
                        jsonBody,
                        Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS)
                ).read(path, parameter.getParameterType()))
                .orElse(null);
    }

    private String getRequestBody(NativeWebRequest webRequest) {
        return Optional.ofNullable(webRequest.getNativeRequest(HttpServletRequest.class))
                .map(httpServletRequest -> {
                    try {
                        var json = (String) httpServletRequest.getAttribute("JSON_BODY_ATTRIBUTE");
                        if (json == null) {
                            json = new String(
                                    httpServletRequest.getInputStream().readAllBytes(),
                                    StandardCharsets.UTF_8
                            );
                            httpServletRequest.setAttribute("JSON_BODY_ATTRIBUTE", json);
                        }
                        return json;
                    } catch (IOException ignored) {
                        return null;
                    }
                })
                .orElseThrow(() -> new RuntimeException("Invalid json body."));
    }
}
