package dev.onvoid.webrtc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InterceptorRegistry {
    private final List<Interceptor> interceptors = Collections.synchronizedList(new ArrayList<>());

    public void add(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    public void remove(Interceptor interceptor) {
        interceptors.remove(interceptor);
    }

    public List<Interceptor> getInterceptors() {
        return Collections.unmodifiableList(new ArrayList<>(interceptors));
    }
}
